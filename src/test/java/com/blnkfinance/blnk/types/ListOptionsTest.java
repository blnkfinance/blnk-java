package com.blnkfinance.blnk.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@link ListOptions}: field map and query-string rendering. */
class ListOptionsTest {

  @Test
  @DisplayName("unset fields have no key in the map")
  void unsetFieldsAbsent() {
    assertEquals(Map.of(), ListOptions.create().toMap());
    assertEquals(Map.of("limit", 5), ListOptions.create().limit(5).toMap());
  }

  @Test
  @DisplayName("toQueryString is empty when nothing is set")
  void emptyQueryString() {
    assertEquals("", ListOptions.create().toQueryString());
  }

  @Test
  @DisplayName("toQueryString renders fields in the order they were set")
  void rendersInInsertionOrder() {
    assertEquals("?limit=10&offset=30", ListOptions.create().limit(10).offset(30).toQueryString());
    assertEquals("?offset=30&limit=10", ListOptions.create().offset(30).limit(10).toQueryString());
  }

  @Test
  @DisplayName("setting a field twice keeps the last value")
  void lastWriteWins() {
    assertEquals("?limit=7", ListOptions.create().limit(3).limit(7).toQueryString());
  }
}
