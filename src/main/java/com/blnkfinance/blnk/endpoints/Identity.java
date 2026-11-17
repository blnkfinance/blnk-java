package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.FormatResponseFn;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.types.DetokenizeIdentityData;
import com.blnkfinance.blnk.types.IdentityData;
import com.blnkfinance.blnk.types.TokenizeIdentityData;
import com.blnkfinance.blnk.util.IdentitySerialization;
import com.blnkfinance.blnk.util.PathSegments;
import com.blnkfinance.blnk.util.Loggers;
import com.blnkfinance.blnk.validators.IdentityValidators;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Endpoint service for identities: CRUD plus field tokenization and
 * detokenization.
 *
 * <p>Every method returns an {@link ApiResponse} value and NEVER throws: a
 * validation failure yields {@code (400, message, null)} without any HTTP
 * call, and runtime errors are converted to an error response reported under
 * the calling method's own name. No identity method passes header options.
 * Path ids and field names are validated and percent-encoded with
 * {@link PathSegments#segment}: a value that is empty, {@code .} or
 * {@code ..}, or contains {@code / \ ? # %} or a control character yields
 * {@code (400, message, null)} and no request is sent.
 */
public class Identity {

  protected final BlnkRequest request;
  protected final BlnkLogger logger;
  protected final FormatResponseFn formatResponse;

  public Identity(BlnkRequest request, BlnkLogger logger, FormatResponseFn formatResponse) {
    this.request = request;
    this.logger = logger;
    this.formatResponse = formatResponse;
  }

  /**
   * Creates an identity — {@code POST identities}. Validates, then sends the
   * SERIALIZED payload: a dob value is rendered as a UTC ISO-8601 timestamp
   * without fractional seconds, and an absent dob is omitted entirely.
   */
  public ApiResponse<JsonNode> create(IdentityData data) {
    try {
      String validatorResponse = IdentityValidators.validateIdentity(data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }
      ObjectNode payload =
          BlnkJson.toObjectNode(IdentitySerialization.serializeIdentityData(data.toMap()));
      return request.call("identities", payload, "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "create");
    }
  }

  /**
   * Retrieves an identity — {@code GET identities/{id}}. The id is validated
   * and URL-encoded; an empty id returns {@code 400}. No body.
   */
  public ApiResponse<JsonNode> get(String id) {
    try {
      return request.call("identities/" + PathSegments.segment(id), null, "GET", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "get");
    }
  }

  /** Lists identities — {@code GET identities}, no body. */
  public ApiResponse<JsonNode> list() {
    try {
      return request.call("identities", null, "GET", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "list");
    }
  }

  /**
   * Updates an identity — {@code PUT identities/{id}}. Validates the body,
   * then validates and URL-encodes the id, then sends the serialized payload.
   */
  public ApiResponse<JsonNode> update(String id, IdentityData data) {
    try {
      String validatorResponse = IdentityValidators.validateIdentity(data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }
      ObjectNode payload =
          BlnkJson.toObjectNode(IdentitySerialization.serializeIdentityData(data.toMap()));
      return request.call("identities/" + PathSegments.segment(id), payload, "PUT", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "update");
    }
  }

  /**
   * Deletes an identity — {@code DELETE identities/{id}} with the id
   * validated and URL-encoded. No body.
   */
  public ApiResponse<JsonNode> delete(String id) {
    try {
      String validatorResponse = IdentityValidators.validateIdentityId(id);
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      return request.call("identities/" + PathSegments.segment(id), null, "DELETE", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "delete");
    }
  }

  /**
   * Lists an identity's tokenized fields —
   * {@code GET identities/{id}/tokenized-fields}. Encoded id, no body.
   */
  public ApiResponse<JsonNode> getTokenizedFields(String id) {
    try {
      String validatorResponse = IdentityValidators.validateIdentityId(id);
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      return request.call(
          "identities/" + PathSegments.segment(id) + "/tokenized-fields", null, "GET", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "getTokenizedFields");
    }
  }

  /**
   * Tokenizes a single field — {@code POST identities/{id}/tokenize/{field}}.
   * Both the id and the field name are validated and URL-encoded; the field
   * name is not restricted to the known field list. No body.
   */
  public ApiResponse<JsonNode> tokenizeField(String id, String field) {
    try {
      String validatorResponse = IdentityValidators.validateTokenizeIdentityField(id, field);
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      return request.call(
          "identities/" + PathSegments.segment(id) + "/tokenize/" + PathSegments.segment(field),
          null,
          "POST",
          null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "tokenizeField");
    }
  }

  /**
   * Tokenizes a set of fields — {@code POST identities/{id}/tokenize}. The
   * body is the data as-is (no copy/transform): exactly
   * {@code {"fields": [...]}}.
   */
  public ApiResponse<JsonNode> tokenize(String id, TokenizeIdentityData data) {
    try {
      String validatorResponse =
          IdentityValidators.validateTokenizeIdentityData(id, data == null ? null : data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      return request.call(
          "identities/" + PathSegments.segment(id) + "/tokenize", data.toJson(), "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "tokenize");
    }
  }

  /**
   * Detokenizes a single field —
   * {@code GET identities/{id}/detokenize/{field}}. Note the verb asymmetry:
   * detokenizeField is a GET (retry-eligible) while tokenizeField is a POST.
   * Encoded id and field name; no body.
   */
  public ApiResponse<JsonNode> detokenizeField(String id, String field) {
    try {
      String validatorResponse = IdentityValidators.validateTokenizeIdentityField(id, field);
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      return request.call(
          "identities/" + PathSegments.segment(id) + "/detokenize/" + PathSegments.segment(field),
          null,
          "GET",
          null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "detokenizeField");
    }
  }

  /**
   * Detokenizes a set of fields — {@code POST identities/{id}/detokenize}.
   * The body is the data as-is. An empty {@code fields} array detokenizes all
   * currently tokenized fields.
   */
  public ApiResponse<JsonNode> detokenize(String id, DetokenizeIdentityData data) {
    try {
      String validatorResponse =
          IdentityValidators.validateDetokenizeIdentityData(
              id, data == null ? null : data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      return request.call(
          "identities/" + PathSegments.segment(id) + "/detokenize", data.toJson(), "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "detokenize");
    }
  }
}
