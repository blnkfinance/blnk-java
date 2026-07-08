package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Shared Jackson configuration and JSON conversion helpers.
 *
 * <p>All request serialization in the SDK goes through this class so the wire
 * format stays consistent:
 * <ul>
 *   <li>compact output (no spaces), key insertion order preserved;</li>
 *   <li>absent optional fields omitted (build maps/ObjectNodes without the key);</li>
 *   <li>integral doubles print without a decimal point ({@code 100} not
 *       {@code 100.0}); non-finite doubles become JSON {@code null}.</li>
 * </ul>
 */
public final class BlnkJson {

  private static final ObjectMapper MAPPER =
      JsonMapper.builder().serializationInclusion(JsonInclude.Include.NON_NULL).build();

  private BlnkJson() {}

  public static ObjectMapper mapper() {
    return MAPPER;
  }

  public static ObjectNode objectNode() {
    return MAPPER.createObjectNode();
  }

  public static ArrayNode arrayNode() {
    return MAPPER.createArrayNode();
  }

  /** Serializes a request body to compact JSON. Unchecked on failure. */
  public static String stringify(Object data) {
    try {
      return MAPPER.writeValueAsString(data instanceof JsonNode ? data : toJsonValue(data));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  /** Parses JSON text into a tree; wraps Jackson's checked exception (message preserved). */
  public static JsonNode parse(String text) {
    try {
      return MAPPER.readTree(text);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  /**
   * Converts a {@code Map<String,Object>} field view to an ObjectNode using
   * the SDK's number rules (see {@link #toJsonValue}). Absent keys stay
   * absent; explicit nulls become JSON null.
   */
  public static ObjectNode toObjectNode(Map<String, ?> map) {
    ObjectNode node = MAPPER.createObjectNode();
    for (Map.Entry<String, ?> entry : map.entrySet()) {
      node.set(entry.getKey(), toJsonValue(entry.getValue()));
    }
    return node;
  }

  /**
   * Recursively converts a Java value to a JsonNode. Integral doubles become
   * integer nodes; NaN and infinities become JSON null.
   */
  public static JsonNode toJsonValue(Object value) {
    JsonNodeFactory f = MAPPER.getNodeFactory();
    if (value == null) {
      return f.nullNode();
    }
    if (value instanceof JsonNode node) {
      return node;
    }
    if (value instanceof String s) {
      return f.textNode(s);
    }
    if (value instanceof Boolean b) {
      return f.booleanNode(b);
    }
    if (value instanceof Integer || value instanceof Long || value instanceof Short
        || value instanceof Byte) {
      return f.numberNode(((Number) value).longValue());
    }
    if (value instanceof BigInteger bi) {
      return f.numberNode(bi);
    }
    if (value instanceof BigDecimal bd) {
      return f.numberNode(bd);
    }
    if (value instanceof Double || value instanceof Float) {
      double d = ((Number) value).doubleValue();
      if (!Double.isFinite(d)) {
        return f.nullNode(); // non-finite numbers serialize as JSON null
      }
      if (d == Math.rint(d) && Math.abs(d) < 9.007199254740992E15) {
        return f.numberNode((long) d); // integral value: emit without a decimal point
      }
      return f.numberNode(d);
    }
    if (value instanceof Map<?, ?> map) {
      ObjectNode node = MAPPER.createObjectNode();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        node.set(String.valueOf(entry.getKey()), toJsonValue(entry.getValue()));
      }
      return node;
    }
    if (value instanceof List<?> list) {
      ArrayNode node = MAPPER.createArrayNode();
      for (Object item : list) {
        node.add(toJsonValue(item));
      }
      return node;
    }
    if (value instanceof Object[] array) {
      ArrayNode node = MAPPER.createArrayNode();
      for (Object item : array) {
        node.add(toJsonValue(item));
      }
      return node;
    }
    // Fallback: let Jackson figure it out (POJOs; date objects must be
    // serialized to strings by module code BEFORE reaching here).
    return MAPPER.valueToTree(value);
  }
}
