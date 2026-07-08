package com.blnkfinance.blnk;

import com.blnkfinance.blnk.endpoints.ApiKeys;
import com.blnkfinance.blnk.endpoints.BalanceMonitor;
import com.blnkfinance.blnk.endpoints.BlnkSystem;
import com.blnkfinance.blnk.endpoints.Hooks;
import com.blnkfinance.blnk.endpoints.Identity;
import com.blnkfinance.blnk.endpoints.LedgerBalances;
import com.blnkfinance.blnk.endpoints.Ledgers;
import com.blnkfinance.blnk.endpoints.Metadata;
import com.blnkfinance.blnk.endpoints.Reconciliation;
import com.blnkfinance.blnk.endpoints.Search;
import com.blnkfinance.blnk.endpoints.Transactions;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkApiErrorDetail;
import com.blnkfinance.blnk.types.BlnkApiErrors;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.util.ClientDefaults;
import com.blnkfinance.blnk.util.CustomLogger;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.blnkfinance.blnk.util.ValueFormat;
import com.blnkfinance.blnk.util.Loggers;
import com.blnkfinance.blnk.util.MultipartBody;
import com.blnkfinance.blnk.util.RequestRetry;
import com.blnkfinance.blnk.util.SafeLogMeta;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The core Blnk client: holds the normalized configuration, wires the service
 * registry, and executes every HTTP request.
 *
 * <p>Endpoint methods and {@link #request} NEVER throw for request/validation
 * failures — they always return an {@code ApiResponse}; only the constructor
 * and {@link #getService} throw.
 */
public final class Blnk {

  /**
   * The client's stored options: the logger is kept separately, baseUrl has a
   * trailing slash appended, and the timeout/retry values are normalized.
   */
  record StoredOptions(String baseUrl, double timeout, int retryCount, double retryDelayMs) {}

  private static final ExecutorService TRANSPORT_EXECUTOR =
      Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "blnk-transport");
        thread.setDaemon(true);
        return thread;
      });

  final String apiKey;
  final StoredOptions options;
  final BlnkLogger logger;
  final Map<String, ServiceFactory> services;
  final Map<String, Object> serviceInstances = new LinkedHashMap<>();
  final FormatResponseFn formatResponse;
  final BlnkTransport thirdPartyRequest;

  public Blnk(
      String apiKey,
      BlnkClientOptions options,
      Map<String, ServiceFactory> services,
      FormatResponseFn formatResponse,
      BlnkTransport thirdPartyRequest) {
    if (options == null || options.baseUrl() == null || options.baseUrl().isEmpty()) {
      throw new IllegalArgumentException("baseUrl is required for self-hosted Blnk SDK.");
    }

    // Make sure baseUrl ends with "/" so endpoint paths append cleanly. The
    // caller's options object is never modified; the normalized value lives
    // on this client.
    String baseUrl =
        options.baseUrl().endsWith("/") ? options.baseUrl() : options.baseUrl() + "/";

    this.apiKey = apiKey;
    double timeout =
        options.timeout() != null ? options.timeout() : ClientDefaults.DEFAULT_TIMEOUT_MS;
    int retryCount = RequestRetry.normalizeRetryCount(options.retryCount());
    double retryDelayMs = RequestRetry.normalizeRetryDelayMs(options.retryDelayMs());
    this.options = new StoredOptions(baseUrl, timeout, retryCount, retryDelayMs);

    // No logger configured → the plain console logger. Blnk.init injects a
    // CustomLogger first, so CONSOLE only appears when constructing Blnk
    // directly.
    this.logger = options.logger() != null ? options.logger() : Loggers.CONSOLE;
    this.services = services;
    this.formatResponse = formatResponse;
    this.thirdPartyRequest = thirdPartyRequest;
  }

  /**
   * Creates a client wired with the default service registry, response
   * formatter, and HTTP transport. When {@code options} carries no logger, a
   * {@link CustomLogger} is injected.
   */
  public static Blnk init(String apiKey, BlnkClientOptions options) {
    BlnkClientOptions effective =
        (options != null && options.logger() == null) ? options.withLogger(new CustomLogger())
            : options;
    return new Blnk(
        apiKey,
        effective,
        ServiceRegistry.defaultServices(),
        HttpClientUtils.FORMAT_RESPONSE,
        new HttpTransport());
  }

  /**
   * The single HTTP entry point every service call funnels through: builds
   * headers and the body, applies the retry policy, and formats the terminal
   * outcome. Package-private so tests in the same package can call it
   * directly. Never throws.
   */
  ApiResponse<JsonNode> request(
      String endpoint, Object data, String method, Map<String, String> headerOptions) {
    double timeoutMs = this.options.timeout();
    int maxAttempts = RequestRetry.normalizeRetryCount((double) this.options.retryCount());
    double retryDelayMs = RequestRetry.normalizeRetryDelayMs(this.options.retryDelayMs());

    byte[] body = null;
    Map<String, String> formDataHeaders = new LinkedHashMap<>();
    boolean isMultipart = MultipartBody.isMultipart(data);

    if (ValueFormat.isTruthy(data)) {
      if (data instanceof MultipartBody multipart) {
        body = multipart.encode();
        formDataHeaders.putAll(multipart.getHeaders());
      } else {
        body = BlnkJson.stringify(data).getBytes(StandardCharsets.UTF_8);
      }
    }

    boolean canRetry =
        !isMultipart && maxAttempts > 1 && RequestRetry.isRetryableHttpMethod(method);

    // Header precedence (later put wins): X-Blnk-Key only when the api key is
    // non-empty, JSON content-type only when not multipart, then
    // headerOptions, then the multipart form headers.
    Map<String, String> headers = new LinkedHashMap<>();
    if (apiKey != null && !apiKey.isEmpty()) {
      headers.put("X-Blnk-Key", apiKey);
    }
    if (!isMultipart) {
      headers.put("content-type", "application/json");
    }
    if (headerOptions != null) {
      headers.putAll(headerOptions);
    }
    headers.putAll(formDataHeaders);

    String url = this.options.baseUrl() + endpoint;

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      if (attempt > 1) {
        double delayMs = RequestRetry.retryDelayForAttempt(attempt - 1, retryDelayMs);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("attempt", attempt);
        meta.put("maxAttempts", maxAttempts);
        meta.put("delayMs", delayMs);
        logger.info("Retrying request to " + endpoint, meta);
        RequestRetry.sleep((long) delayMs);
      }

      TransportRequest transportRequest =
          new TransportRequest(method, headers, body, isMultipart, timeoutMs);

      try {
        TransportResponse response = executeWithTimeout(url, transportRequest, timeoutMs);

        if (!response.ok()) {
          JsonNode errorResult = null;
          try {
            errorResult = HttpClientUtils.readResponseJsonBody(response);
          } catch (RuntimeException ignored) {
            errorResult = null;
          }
          BlnkApiErrorDetail structuredError = BlnkApiErrors.parseBlnkApiErrorBody(errorResult);

          if (canRetry && RequestRetry.isRetryableHttpStatus(response.status())
              && attempt < maxAttempts) {
            logger.info(
                "Request to " + endpoint + " failed with status " + response.status()
                    + "; retrying.");
            continue;
          }

          logger.error("Request to " + endpoint + " failed with status " + response.status() + ".");
          return formatResponse.format(
              response.status(),
              structuredError != null ? structuredError.message() : response.statusText(),
              errorResult,
              structuredError);
        }

        if (response.status() == 204 || response.status() == 205) {
          return formatResponse.format(response.status(), "Success", null, null);
        }

        // A JSON parse failure here is intentionally NOT caught locally — it
        // falls into the catch below and becomes a 500 via handleError.
        JsonNode jsonResponse = HttpClientUtils.readResponseJsonBody(response);
        return formatResponse.format(response.status(), "Success", jsonResponse, null);
      } catch (BlnkAbortException abort) {
        // Timeouts are intentionally not retried; ANY abort takes this path.
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("endpoint", endpoint);
        meta.put("timeoutMs", timeoutMs);
        logger.error("Request timed out", SafeLogMeta.redactSensitiveLogMeta(meta));
        return formatResponse.format(
            408, "Request timed out after " + ValueFormat.formatNumber(timeoutMs) + "ms", null,
            null);
      } catch (RuntimeException error) {
        if (canRetry && RequestRetry.isRetryableTransportError(error) && attempt < maxAttempts) {
          logger.info(
              "Request to " + endpoint + " failed; retrying.", SafeLogMeta.safeLogMeta(error));
          continue;
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("endpoint", endpoint);
        meta.put("error", error);
        logger.error("Request failed", SafeLogMeta.redactSensitiveLogMeta(meta));
        return Loggers.handleError(error, logger, formatResponse, "request");
      }
    }

    // Defensive fallback — every attempt above either returns or retries, so
    // this is unreachable in practice.
    return formatResponse.format(
        500, "Request failed after " + maxAttempts + " attempts", null, null);
  }

  /**
   * Runs the transport call under the per-attempt timeout. A timed-out call
   * is cancelled (interrupted) and surfaces as {@link BlnkAbortException}.
   */
  private TransportResponse executeWithTimeout(
      String url, TransportRequest transportRequest, double timeoutMs) {
    Future<TransportResponse> future =
        TRANSPORT_EXECUTOR.submit(() -> thirdPartyRequest.execute(url, transportRequest));
    try {
      long timeoutNanos = timeoutMs <= 0 ? 0
          : (timeoutMs >= 9.0E15 ? Long.MAX_VALUE : (long) (timeoutMs * 1_000_000.0));
      return future.get(timeoutNanos, TimeUnit.NANOSECONDS);
    } catch (TimeoutException timeoutException) {
      future.cancel(true);
      throw new BlnkAbortException("The operation was aborted.");
    } catch (ExecutionException executionException) {
      Throwable cause = executionException.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new BlnkTransportException(cause.getMessage(), cause);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
      throw new BlnkTransportException("request interrupted", interruptedException);
    }
  }

  /**
   * Resolves a service by its registry name: an unknown name throws
   * {@code Service {name} is not registered}; instances are cached per
   * client. Package-private — callers use the typed getters below.
   */
  Object getService(String serviceName) {
    ServiceFactory factory = services == null ? null : services.get(serviceName);
    if (factory == null) {
      throw new IllegalStateException("Service " + serviceName + " is not registered");
    }
    Object instance = serviceInstances.get(serviceName);
    if (instance == null) {
      instance = factory.create(this::request, this.logger, this.formatResponse);
      serviceInstances.put(serviceName, instance);
    }
    return instance;
  }

  // Typed service accessors — instances are created lazily and cached.

  public Ledgers ledgers() {
    return (Ledgers) getService("Ledgers");
  }

  public LedgerBalances ledgerBalances() {
    return (LedgerBalances) getService("LedgerBalances");
  }

  public Transactions transactions() {
    return (Transactions) getService("Transactions");
  }

  public BalanceMonitor balanceMonitor() {
    return (BalanceMonitor) getService("BalanceMonitor");
  }

  public Reconciliation reconciliation() {
    return (Reconciliation) getService("Reconciliation");
  }

  public Search search() {
    return (Search) getService("Search");
  }

  public Identity identity() {
    return (Identity) getService("Identity");
  }

  public BlnkSystem system() {
    return (BlnkSystem) getService("System");
  }

  public Metadata metadata() {
    return (Metadata) getService("Metadata");
  }

  public Hooks hooks() {
    return (Hooks) getService("Hooks");
  }

  public ApiKeys apiKeys() {
    return (ApiKeys) getService("ApiKeys");
  }
}
