package com.blnkfinance.blnk.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the identity search response shape ({@code SearchResponse<SearchIdentityDocument>}). */
@DisplayName("SearchIdentityResponse API shape")
class SearchIdentityResponseTest {

  @Test
  @DisplayName("identity document includes indexed fields")
  void identityDocumentIncludesIndexedFields() {
    SearchResponse<SearchIdentityDocument> response =
        SearchResponse.<SearchIdentityDocument>create()
            .found(1)
            .outOf(13)
            .page(1)
            .requestParams(
                SearchRequestParams.create()
                    .collectionName("identities")
                    .q("*")
                    .perPage(1))
            .searchTimeMs(5)
            .hits(
                List.of(
                    SearchHit.<SearchIdentityDocument>create()
                        .document(
                            SearchIdentityDocument.create()
                                .id("idt_fbf6a26c-82c6-46fb-9237-8fbba55a23c0")
                                .identityId("idt_fbf6a26c-82c6-46fb-9237-8fbba55a23c0")
                                .identityType("organization")
                                .createdAt(1781225782L)
                                .dob(-62135596800L) // Negative epoch dob: year 0001 placeholder.
                                .metaData(new LinkedHashMap<>()))
                        .highlights(List.of())));

    assertTrue(response.hits().get(0).document().createdAt() instanceof Long);
    assertTrue(response.hits().get(0).document().identityId().startsWith("idt_"));
  }
}
