package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.FormatResponseFn;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.Matcher;
import com.blnkfinance.blnk.types.RunInstantReconData;
import com.blnkfinance.blnk.types.RunReconData;
import com.blnkfinance.blnk.util.Loggers;
import com.blnkfinance.blnk.util.MultipartBody;
import com.blnkfinance.blnk.validators.ReconciliationValidators;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Endpoint service for reconciliation: file uploads, matching-rule
 * management, and starting or inspecting reconciliation runs.
 */
public class Reconciliation {

  protected final BlnkRequest request;
  protected final BlnkLogger logger;
  protected final FormatResponseFn formatResponse;

  public Reconciliation(BlnkRequest request, BlnkLogger logger, FormatResponseFn formatResponse) {
    this.request = request;
    this.logger = logger;
    this.formatResponse = formatResponse;
  }

  /**
   * Uploads a local file for reconciliation —
   * {@code POST reconciliation/upload} as multipart form data.
   *
   * <p>A missing local file is reported as an SDK-synthesized
   * {@code (404, "File does not exist at path: {filePath}", null)} response
   * without any HTTP call being made. Multipart parts are appended in order:
   * {@code file} first, then {@code source}. Never throws: runtime errors are
   * converted to an error response, reported under the name {@code "upload"}.
   */
  public ApiResponse<JsonNode> upload(String filePath, String source) {
    try {
      MultipartBody formData = MultipartBody.create();
      if (!fileExists(filePath)) {
        return formatResponse.format(
            404, "File does not exist at path: " + filePath, null, null);
      }
      formData.appendFile("file", Path.of(filePath));
      formData.append("source", source);
      return request.call("reconciliation/upload", formData, "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "upload");
    }
  }

  /**
   * Uploads a reconciliation file from a stream. A {@code null} stream yields
   * {@code (400, "Invalid read stream provided", null)} without any HTTP call.
   */
  public ApiResponse<JsonNode> upload(InputStream fileStream, String source) {
    try {
      MultipartBody formData = MultipartBody.create();
      if (fileStream == null) {
        return formatResponse.format(400, "Invalid read stream provided", null, null);
      }
      formData.appendStream("file", fileStream, "file");
      formData.append("source", source);
      return request.call("reconciliation/upload", formData, "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "upload");
    }
  }

  private static boolean fileExists(String filePath) {
    // An empty path must read as "missing file", not as the current directory.
    if (filePath == null || filePath.isEmpty()) {
      return false;
    }
    try {
      return Files.exists(Path.of(filePath));
    } catch (InvalidPathException e) {
      return false; // an unparseable path is treated as a missing file
    }
  }

  /**
   * Creates a matching rule — validates the matcher, then
   * {@code POST reconciliation/matching-rules} with the data forwarded
   * untouched.
   */
  public ApiResponse<JsonNode> createMatchingRule(Matcher data) {
    try {
      String validatorResponse =
          ReconciliationValidators.validateMatcher(data == null ? null : data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      return request.call("reconciliation/matching-rules", data.toJson(), "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "createMatchingRule");
    }
  }

  /**
   * Updates a matching rule — a null or empty id is rejected BEFORE payload
   * validation; {@code PUT reconciliation/matching-rules/{ruleId}} with the
   * id NOT URL-encoded.
   */
  public ApiResponse<JsonNode> updateMatchingRule(String ruleId, Matcher data) {
    try {
      if (ruleId == null || ruleId.isEmpty()) {
        return formatResponse.format(400, "matching rule id is required", null, null);
      }

      String validatorResponse =
          ReconciliationValidators.validateMatcher(data == null ? null : data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      return request.call("reconciliation/matching-rules/" + ruleId, data.toJson(), "PUT", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "updateMatchingRule");
    }
  }

  /**
   * Deletes a matching rule —
   * {@code DELETE reconciliation/matching-rules/{ruleId}}, no body, id NOT
   * URL-encoded.
   */
  public ApiResponse<JsonNode> deleteMatchingRule(String ruleId) {
    try {
      if (ruleId == null || ruleId.isEmpty()) {
        return formatResponse.format(400, "matching rule id is required", null, null);
      }

      return request.call("reconciliation/matching-rules/" + ruleId, null, "DELETE", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "deleteMatchingRule");
    }
  }

  /**
   * Starts a reconciliation run — {@code POST reconciliation/start}. Note:
   * the payload is intentionally NOT validated; it is forwarded untouched.
   */
  public ApiResponse<JsonNode> run(RunReconData data) {
    try {
      return request.call(
          "reconciliation/start", data == null ? null : data.toJson(), "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "run");
    }
  }

  /**
   * Starts an instant reconciliation run — validates the payload, then
   * {@code POST reconciliation/start-instant} with the data forwarded
   * untouched.
   */
  public ApiResponse<JsonNode> runInstant(RunInstantReconData data) {
    try {
      String validatorResponse =
          ReconciliationValidators.validateRunInstantReconData(
              data == null ? null : data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      return request.call("reconciliation/start-instant", data.toJson(), "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "runInstant");
    }
  }

  /**
   * Retrieves a reconciliation run —
   * {@code GET reconciliation/{reconciliationId}}. A null or empty id is
   * rejected; no body; the id is NOT URL-encoded.
   */
  public ApiResponse<JsonNode> get(String reconciliationId) {
    try {
      if (reconciliationId == null || reconciliationId.isEmpty()) {
        return formatResponse.format(400, "reconciliation id is required", null, null);
      }

      return request.call("reconciliation/" + reconciliationId, null, "GET", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "get");
    }
  }
}
