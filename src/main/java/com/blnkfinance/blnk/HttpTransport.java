package com.blnkfinance.blnk;

import com.blnkfinance.blnk.util.HttpClientUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default {@link BlnkTransport} backed by {@code java.net.http.HttpClient}.
 * No extra dependencies.
 */
public final class HttpTransport implements BlnkTransport {

  private final HttpClient client;

  public HttpTransport() {
    this.client = HttpClient.newBuilder().build();
  }

  public HttpTransport(HttpClient client) {
    this.client = client;
  }

  @Override
  public TransportResponse execute(String url, TransportRequest request) {
    try {
      HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url));
      for (Map.Entry<String, String> header : request.headers().entrySet()) {
        builder.header(header.getKey(), header.getValue());
      }
      // Per-attempt timeout. A zero or negative timeout deliberately becomes
      // an effectively-immediate 1 ms deadline rather than being rejected;
      // non-finite or enormous values become an effectively-unbounded one.
      double timeoutMs = request.timeoutMs();
      Duration timeout = timeoutMs > 0 && timeoutMs < 9.0E15
          ? Duration.ofNanos((long) (timeoutMs * 1_000_000.0))
          : (timeoutMs <= 0 ? Duration.ofMillis(1) : Duration.ofDays(365_000));
      builder.timeout(timeout);
      HttpRequest.BodyPublisher bodyPublisher = request.body() == null
          ? HttpRequest.BodyPublishers.noBody()
          : HttpRequest.BodyPublishers.ofByteArray(request.body());
      builder.method(request.method(), bodyPublisher);

      HttpResponse<String> response =
          client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      int status = response.statusCode();
      Map<String, String> responseHeaders = new LinkedHashMap<>();
      response.headers().map().forEach(
          (name, values) -> responseHeaders.put(name, String.join(", ", values)));
      String body = response.body();
      return TransportResponse.builder()
          .ok(status >= 200 && status < 300)
          .status(status)
          .statusText(HttpClientUtils.reasonPhrase(status))
          .headers(responseHeaders)
          .text(() -> body == null ? "" : body)
          .build();
    } catch (HttpTimeoutException e) {
      throw new BlnkAbortException("The operation was aborted.", e);
    } catch (IOException e) {
      throw new BlnkTransportException(e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BlnkAbortException("The operation was aborted.", e);
    }
  }
}
