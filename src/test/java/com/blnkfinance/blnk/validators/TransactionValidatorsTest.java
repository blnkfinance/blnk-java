package com.blnkfinance.blnk.validators;

import com.blnkfinance.blnk.types.TransactionConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link TransactionValidators}: create, bulk, update-status,
 * refund, inflight commit/void, and queue-recovery payload validation.
 */
class TransactionValidatorsTest {

  /** Builds the common scalar fields shared by most create payloads. */
  private static Map<String, Object> baseFields() {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("precision", 100);
    fields.put("reference", "ref_split_001");
    fields.put("description", "Split transaction");
    fields.put("currency", "USD");
    return fields;
  }

  private static Map<String, Object> leg(String identifier) {
    Map<String, Object> leg = new LinkedHashMap<>();
    leg.put("identifier", identifier);
    return leg;
  }

  private static Map<String, Object> distLeg(String identifier, Object distribution) {
    Map<String, Object> leg = leg(identifier);
    leg.put("distribution", distribution);
    return leg;
  }

  private static Map<String, Object> preciseLeg(String identifier, Object preciseDistribution) {
    Map<String, Object> leg = leg(identifier);
    leg.put("precise_distribution", preciseDistribution);
    return leg;
  }

  @Nested
  @DisplayName("atomic on split transactions")
  class AtomicOnSplitTransactions {

    @Test
    @DisplayName("allows atomic on split create payloads")
    void allowsAtomicOnSplitCreatePayloads() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("amount", 1000);
      data.put("precision", 100);
      data.put("reference", "ref_atomic_split");
      data.put("description", "Atomic split");
      data.put("currency", "USD");
      data.put("source", "@FundingPool");
      data.put("destinations", List.of(
          distLeg("bln_fee", "50%"),
          distLeg("bln_recipient", "left")));
      data.put("atomic", true);

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects non-boolean atomic")
    void rejectsNonBooleanAtomic() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("amount", 1000);
      data.put("precision", 100);
      data.put("reference", "ref_bad_atomic");
      data.put("description", "Bad atomic");
      data.put("currency", "USD");
      data.put("source", "@FundingPool");
      data.put("destinations", List.of(
          distLeg("bln_fee", "50%"),
          distLeg("bln_recipient", "left")));
      data.put("atomic", "true");

      assertEquals(
          "atomic must be a boolean if provided.",
          TransactionValidators.validateCreateTransactions(data));
    }
  }

  @Nested
  @DisplayName("Split-transaction validator")
  class SplitTransactionValidator {

    @Test
    @DisplayName("allows multiple sources with a single destination")
    void allowsMultipleSourcesWithASingleDestination() {
      Map<String, Object> data = baseFields();
      data.put("amount", 30000);
      data.put("sources", List.of(
          distLeg("bln_alice", "10%"),
          distLeg("bln_bob", "20000"),
          distLeg("bln_charlie", "left")));
      data.put("destination", "bln_sarah");

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("allows a single source with multiple destinations")
    void allowsASingleSourceWithMultipleDestinations() {
      Map<String, Object> data = baseFields();
      data.put("amount", 30000);
      data.put("source", "bln_sarah");
      data.put("destinations", List.of(
          distLeg("bln_alice", "10%"),
          distLeg("bln_bob", "20000"),
          distLeg("bln_charlie", "left")));

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects sources without destination")
    void rejectsSourcesWithoutDestination() {
      Map<String, Object> data = baseFields();
      data.put("amount", 30000);
      data.put("sources", List.of(distLeg("bln_alice", "100%")));

      assertEquals(
          "'destination' is required when using 'sources'.",
          TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects destinations without source")
    void rejectsDestinationsWithoutSource() {
      Map<String, Object> data = baseFields();
      data.put("amount", 30000);
      data.put("destinations", List.of(distLeg("bln_alice", "100%")));

      assertEquals(
          "'source' is required when using 'destinations'.",
          TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects sources with destinations array")
    void rejectsSourcesWithDestinationsArray() {
      Map<String, Object> data = baseFields();
      data.put("amount", 30000);
      data.put("sources", List.of(distLeg("bln_alice", "100%")));
      data.put("destination", "bln_sarah");
      data.put("destinations", List.of(distLeg("bln_bob", "left")));

      assertEquals(
          "Both 'destination' and 'destinations' cannot be provided together.",
          TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects sources combined with destinations routing")
    void rejectsSourcesCombinedWithDestinationsRouting() {
      Map<String, Object> data = baseFields();
      data.put("amount", 30000);
      data.put("sources", List.of(distLeg("bln_alice", "100%")));
      data.put("destinations", List.of(distLeg("bln_bob", "100%")));

      assertEquals(
          "'sources' requires a single 'destination'; use 'destination' instead of"
              + " 'destinations'.",
          TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("uses correct error message when destination and destinations are both set")
    void usesCorrectErrorMessageWhenDestinationAndDestinationsAreBothSet() {
      Map<String, Object> data = baseFields();
      data.put("amount", 1000);
      data.put("source", "bln_source");
      data.put("destination", "bln_dest_a");
      data.put("destinations", List.of(distLeg("bln_dest_b", "100%")));

      assertEquals(
          "Both 'destination' and 'destinations' cannot be provided together.",
          TransactionValidators.validateCreateTransactions(data));
      assertNotEquals(
          "Both 'source' and 'sources' cannot be provided together.",
          TransactionValidators.validateCreateTransactions(data),
          "must not reuse the source/sources error message");
    }

    @Test
    @DisplayName("allows precise_amount-only payloads")
    void allowsPreciseAmountOnlyPayloads() {
      Map<String, Object> data = baseFields();
      data.put("precise_amount", 3000000);
      data.put("source", "@FundingPool");
      data.put("destination", "bln_recipient");

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("allows precise_amount-only with multiple sources split")
    void allowsPreciseAmountOnlyWithMultipleSourcesSplit() {
      Map<String, Object> data = baseFields();
      data.put("precise_amount", 3000000);
      data.put("sources", List.of(
          distLeg("bln_alice", "10%"),
          distLeg("bln_bob", "2000000"),
          distLeg("bln_charlie", "left")));
      data.put("destination", "bln_sarah");

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects when neither amount nor precise_amount is provided")
    void rejectsWhenNeitherAmountNorPreciseAmountIsProvided() {
      Map<String, Object> data = baseFields();
      data.put("source", "bln_a");
      data.put("destination", "bln_b");

      assertEquals(
          "Either 'amount' or 'precise_amount' must be provided.",
          TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("allows split legs that use precise_distribution only")
    void allowsSplitLegsThatUsePreciseDistributionOnly() {
      Map<String, Object> data = baseFields();
      data.put("precise_amount", 10000);
      data.put("source", "bln_sarah");
      data.put("destinations", List.of(
          preciseLeg("bln_merchant", "9733"),
          preciseLeg("bln_fee", "267")));

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("allows mixed precise_distribution and percentage distribution legs")
    void allowsMixedPreciseDistributionAndPercentageDistributionLegs() {
      Map<String, Object> data = baseFields();
      data.put("precise_amount", "189207535698279000");
      data.put("source", "bln_sarah");
      data.put("destinations", List.of(
          preciseLeg("bln_alice", "37841507139655800"),
          distLeg("bln_bob", "20%"),
          distLeg("bln_charlie", "left")));

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("validates precise_distribution strings beyond Number.MAX_SAFE_INTEGER exactly")
    void validatesPreciseDistributionStringsBeyondNumberMaxSafeIntegerExactly() {
      String legA = "9007199254740992";
      String legB = "1";
      String total = "9007199254740993";

      Map<String, Object> valid = baseFields();
      valid.put("precise_amount", total);
      valid.put("source", "bln_sarah");
      valid.put("destinations", List.of(
          preciseLeg("bln_alice", legA),
          preciseLeg("bln_bob", legB)));

      Map<String, Object> invalid = baseFields();
      invalid.put("precise_amount", total);
      invalid.put("source", "bln_sarah");
      invalid.put("destinations", List.of(
          preciseLeg("bln_alice", legA),
          preciseLeg("bln_bob", "2")));

      assertNull(TransactionValidators.validateCreateTransactions(valid));
      // The total is 2^53 + 1: double-precision math would round it, so the
      // sum check must use exact integer arithmetic.
      assertNotNull(
          TransactionValidators.validateCreateTransactions(invalid),
          "sum must match exactly; Number() would mis-parse " + total);
    }

    @Test
    @DisplayName("allows precise_amount as a string for large integers")
    void allowsPreciseAmountAsAStringForLargeIntegers() {
      Map<String, Object> data = baseFields();
      data.put("precise_amount", "9007199254740993");
      data.put("source", "@FundingPool");
      data.put("destination", "bln_recipient");

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects invalid precise_amount string values")
    void rejectsInvalidPreciseAmountStringValues() {
      Map<String, Object> data = baseFields();
      data.put("precise_amount", "12.5");
      data.put("source", "bln_a");
      data.put("destination", "bln_b");

      assertEquals(
          "precise_amount must be a non-negative integer string or number.",
          TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects split legs missing both distribution and precise_distribution")
    void rejectsSplitLegsMissingBothDistributionAndPreciseDistribution() {
      Map<String, Object> data = baseFields();
      data.put("amount", 1000);
      data.put("source", "bln_sarah");
      data.put("destinations", List.of(leg("bln_alice")));

      assertEquals(
          "Each destination leg must include either 'distribution' or 'precise_distribution'.",
          TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects invalid precise_distribution values")
    void rejectsInvalidPreciseDistributionValues() {
      Map<String, Object> data = baseFields();
      data.put("precise_amount", 1000);
      data.put("source", "bln_sarah");
      data.put("destinations", List.of(preciseLeg("bln_alice", "not-a-number")));

      assertEquals(
          "Invalid precise_distribution for leg: bln_alice.",
          TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("uses amount for distribution math when both amount and precise_amount are provided")
    void usesAmountForDistributionMathWhenBothAmountAndPreciseAmountAreProvided() {
      Map<String, Object> validWithAmount = baseFields();
      validWithAmount.put("amount", 10000);
      validWithAmount.put("precise_amount", 999999);
      validWithAmount.put("source", "bln_sarah");
      validWithAmount.put("destinations", List.of(
          preciseLeg("bln_merchant", "9733"),
          preciseLeg("bln_fee", "267")));

      Map<String, Object> invalidIfPreciseAmountUsed = baseFields();
      invalidIfPreciseAmountUsed.put("amount", 10000);
      invalidIfPreciseAmountUsed.put("precise_amount", 999999);
      invalidIfPreciseAmountUsed.put("source", "bln_sarah");
      invalidIfPreciseAmountUsed.put("destinations", List.of(
          preciseLeg("bln_merchant", "999998"),
          preciseLeg("bln_fee", "1")));

      assertNull(TransactionValidators.validateCreateTransactions(validWithAmount));
      assertNotNull(
          TransactionValidators.validateCreateTransactions(invalidIfPreciseAmountUsed),
          "amount (10000) should take precedence over precise_amount (999999)");
    }
  }

  @Nested
  @DisplayName("Create transaction request fields")
  class CreateTransactionRequestFields {

    private Map<String, Object> scalarBase() {
      Map<String, Object> data = baseFields();
      data.put("amount", 1000);
      data.put("source", "@FundingPool");
      data.put("destination", "@Recipient");
      return data;
    }

    @Test
    @DisplayName("allows skip_queue on create payloads")
    void allowsSkipQueueOnCreatePayloads() {
      Map<String, Object> data = scalarBase();
      data.put("skip_queue", true);

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("allows effective_date as an ISO string")
    void allowsEffectiveDateAsAnIsoString() {
      Map<String, Object> data = scalarBase();
      data.put("effective_date", "2025-02-15T10:30:00Z");

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("allows inflight_commit_date as Core example string")
    void allowsInflightCommitDateAsCoreExampleString() {
      Map<String, Object> data = scalarBase();
      data.put("inflight", true);
      data.put("inflight_commit_date", "2024-04-22T15:28:03+00:00");

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("allows inflight_commit_date as a Date")
    void allowsInflightCommitDateAsADate() {
      Map<String, Object> data = scalarBase();
      data.put("inflight", true);
      data.put("inflight_commit_date", Instant.parse("2025-06-01T12:00:00.000Z"));

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("allows effective_date with a numeric timezone offset")
    void allowsEffectiveDateWithANumericTimezoneOffset() {
      Map<String, Object> data = scalarBase();
      data.put("effective_date", "2024-04-22T15:28:03+00:00");

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects date-only effective_date strings")
    void rejectsDateOnlyEffectiveDateStrings() {
      Map<String, Object> data = scalarBase();
      data.put("effective_date", "2025-02-15");

      assertEquals(
          "Invalid effective_date.",
          TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects invalid skip_queue values")
    void rejectsInvalidSkipQueueValues() {
      Map<String, Object> data = scalarBase();
      data.put("skip_queue", "true");

      assertEquals(
          "skip_queue must be a boolean if provided.",
          TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects invalid effective_date values")
    void rejectsInvalidEffectiveDateValues() {
      Map<String, Object> data = scalarBase();
      data.put("effective_date", "not-a-date");

      assertEquals(
          "Invalid effective_date.",
          TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects invalid inflight_commit_date values")
    void rejectsInvalidInflightCommitDateValues() {
      Map<String, Object> data = scalarBase();
      data.put("inflight_commit_date", "bad-date");

      assertEquals(
          "Invalid inflight_commit_date.",
          TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("allows scheduled_for as an ISO string")
    void allowsScheduledForAsAnIsoString() {
      Map<String, Object> data = scalarBase();
      data.put("scheduled_for", "2025-12-31T23:59:59Z");

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects date-only scheduled_for strings")
    void rejectsDateOnlyScheduledForStrings() {
      Map<String, Object> data = scalarBase();
      data.put("scheduled_for", "2025-12-31");

      assertEquals(
          "Invalid scheduled date.",
          TransactionValidators.validateCreateTransactions(data));
    }
  }

  @Nested
  @DisplayName("ISO date strings and Distribution handling")
  class IsoDateStringsAndDistribution {

    @Test
    @DisplayName("allows scheduled_for as an ISO string on create")
    void allowsScheduledForAsAnIsoStringOnCreate() {
      Map<String, Object> data = baseFields();
      data.put("amount", 1000);
      data.put("source", "@FundingPool");
      data.put("destination", "@Recipient");
      data.put("scheduled_for", "2025-07-01T08:00:00Z");

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("allows inflight_expiry_date as an ISO string on create")
    void allowsInflightExpiryDateAsAnIsoStringOnCreate() {
      Map<String, Object> data = baseFields();
      data.put("amount", 1000);
      data.put("source", "@FundingPool");
      data.put("destination", "@Recipient");
      data.put("inflight", true);
      data.put("inflight_expiry_date", "2025-08-01T08:00:00Z");

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("allows decimal fixed distribution strings such as 240.23")
    void allowsDecimalFixedDistributionStringsSuchAs24023() {
      Map<String, Object> data = baseFields();
      data.put("amount", 1000);
      data.put("source", "@FundingPool");
      data.put("destinations", List.of(
          distLeg("bln_fee", "240.23"),
          distLeg("bln_recipient", "left")));

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("allows decimal distribution when precise_distribution is on another leg")
    void allowsDecimalDistributionWhenPreciseDistributionIsOnAnotherLeg() {
      Map<String, Object> data = baseFields();
      data.put("amount", 1000);
      data.put("source", "@FundingPool");
      data.put("destinations", List.of(
          distLeg("bln_fee", "240.23"),
          preciseLeg("bln_recipient", "500"),
          distLeg("bln_treasury", "left")));

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("allows exact decimal sum without left")
    void allowsExactDecimalSumWithoutLeft() {
      Map<String, Object> data = baseFields();
      data.put("amount", 1000);
      data.put("source", "@FundingPool");
      data.put("destinations", List.of(
          distLeg("bln_fee", "240.23"),
          distLeg("bln_recipient", "759.77")));

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects scientific notation distribution strings")
    void rejectsScientificNotationDistributionStrings() {
      Map<String, Object> data = baseFields();
      data.put("amount", 1000);
      data.put("source", "@FundingPool");
      data.put("destinations", List.of(
          distLeg("bln_fee", "1e3"),
          distLeg("bln_recipient", "left")));

      assertEquals(
          "Invalid distribution type for leg: bln_fee.",
          TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects hex distribution strings")
    void rejectsHexDistributionStrings() {
      Map<String, Object> data = baseFields();
      data.put("amount", 1000);
      data.put("source", "@FundingPool");
      data.put("destinations", List.of(
          distLeg("bln_fee", "0x10"),
          distLeg("bln_recipient", "left")));

      assertEquals(
          "Invalid distribution type for leg: bln_fee.",
          TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects whitespace-padded distribution strings")
    void rejectsWhitespacePaddedDistributionStrings() {
      Map<String, Object> data = baseFields();
      data.put("amount", 1000);
      data.put("source", "@FundingPool");
      data.put("destinations", List.of(
          distLeg("bln_fee", " 240.23 "),
          distLeg("bln_recipient", "left")));

      assertEquals(
          "Invalid distribution type for leg: bln_fee.",
          TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects Infinity distribution strings")
    void rejectsInfinityDistributionStrings() {
      Map<String, Object> data = baseFields();
      data.put("amount", 1000);
      data.put("source", "@FundingPool");
      data.put("destinations", List.of(
          distLeg("bln_fee", "Infinity"),
          distLeg("bln_recipient", "left")));

      assertEquals(
          "Invalid distribution type for leg: bln_fee.",
          TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects malformed decimal distribution strings")
    void rejectsMalformedDecimalDistributionStrings() {
      Map<String, Object> data = baseFields();
      data.put("amount", 1000);
      data.put("source", "@FundingPool");
      data.put("destinations", List.of(
          distLeg("bln_fee", "240.23.1"),
          distLeg("bln_recipient", "left")));

      assertEquals(
          "Invalid distribution type for leg: bln_fee.",
          TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("allows decimal percentage distributions with precise_amount")
    void allowsDecimalPercentageDistributionsWithPreciseAmount() {
      Map<String, Object> data = baseFields();
      data.put("precise_amount", 30000);
      data.put("source", "@FundingPool");
      data.put("destinations", List.of(
          distLeg("bln_a", "33.33%"),
          distLeg("bln_b", "66.67%")));

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("allows decimal percentage with left under precise_distribution split")
    void allowsDecimalPercentageWithLeftUnderPreciseDistributionSplit() {
      Map<String, Object> data = baseFields();
      data.put("amount", 30000);
      data.put("source", "@FundingPool");
      data.put("destinations", List.of(
          distLeg("bln_a", "33.33%"),
          preciseLeg("bln_b", "5000"),
          distLeg("bln_c", "left")));

      assertNull(TransactionValidators.validateCreateTransactions(data));
    }

    @Test
    @DisplayName("rejects decimal distributions with precise_amount beyond MAX_SAFE_INTEGER")
    void rejectsDecimalDistributionsWithPreciseAmountBeyondMaxSafeInteger() {
      Map<String, Object> data = baseFields();
      data.put("precise_amount", "9007199254740993");
      data.put("source", "@FundingPool");
      data.put("destinations", List.of(
          distLeg("bln_a", "33.33%"),
          distLeg("bln_b", "left")));

      // Note: the message names Number.MAX_SAFE_INTEGER (2^53 - 1, the bound
      // beyond which double math is inexact); callers depend on this exact string.
      assertEquals(
          "Decimal distribution values are not supported with precise amounts beyond"
              + " Number.MAX_SAFE_INTEGER.",
          TransactionValidators.validateCreateTransactions(data));
    }
  }

  @Nested
  @DisplayName("Bulk transaction request fields")
  class BulkTransactionRequestFields {

    private Map<String, Object> baseBulkTxn() {
      Map<String, Object> txn = baseFields();
      txn.put("amount", 1000);
      txn.put("source", "@FundingPool");
      txn.put("destination", "@Recipient");
      return txn;
    }

    @Test
    @DisplayName("allows skip_queue on bulk payloads")
    void allowsSkipQueueOnBulkPayloads() {
      Map<String, Object> first = baseBulkTxn();
      first.put("reference", "bulk_ref_001");
      Map<String, Object> second = baseBulkTxn();
      second.put("reference", "bulk_ref_002");
      second.put("amount", 2000);

      Map<String, Object> data = new LinkedHashMap<>();
      data.put("skip_queue", true);
      data.put("transactions", List.of(first, second));

      assertNull(TransactionValidators.validateBulkTransactions(data));
    }

    @Test
    @DisplayName("rejects invalid skip_queue on bulk payloads")
    void rejectsInvalidSkipQueueOnBulkPayloads() {
      Map<String, Object> first = baseBulkTxn();
      first.put("reference", "bulk_ref_001");
      Map<String, Object> second = baseBulkTxn();
      second.put("reference", "bulk_ref_002");
      second.put("amount", 2000);

      Map<String, Object> data = new LinkedHashMap<>();
      data.put("skip_queue", "true");
      data.put("transactions", List.of(first, second));

      assertEquals(
          "skip_queue must be a boolean if provided.",
          TransactionValidators.validateBulkTransactions(data));
    }

    @Test
    @DisplayName("rejects oversized transactions array")
    void rejectsOversizedTransactionsArray() {
      List<Map<String, Object>> transactions = new ArrayList<>();
      for (int i = 0; i < TransactionConstants.MAX_BULK_CREATE_ITEMS + 1; i++) {
        Map<String, Object> txn = baseBulkTxn();
        txn.put("reference", "bulk_ref_" + i);
        transactions.add(txn);
      }

      Map<String, Object> data = new LinkedHashMap<>();
      data.put("transactions", transactions);

      assertEquals(
          "Too many transactions; max is " + TransactionConstants.MAX_BULK_CREATE_ITEMS + ".",
          TransactionValidators.validateBulkTransactions(data));
    }
  }

  @Nested
  @DisplayName("updateStatus precise_amount on partial commit")
  class UpdateStatusPreciseAmountOnPartialCommit {

    @Test
    @DisplayName("allows commit with precise_amount only")
    void allowsCommitWithPreciseAmountOnly() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("status", "commit");
      data.put("precise_amount", 50000);

      assertNull(TransactionValidators.validateUpdateTransactions(data));
    }

    @Test
    @DisplayName("allows precise_amount as a string for large integers")
    void allowsPreciseAmountAsAStringForLargeIntegers() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("status", "commit");
      data.put("precise_amount", "9007199254740993");

      assertNull(TransactionValidators.validateUpdateTransactions(data));
    }

    @Test
    @DisplayName("allows full commit without amount or precise_amount")
    void allowsFullCommitWithoutAmountOrPreciseAmount() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("status", "commit");

      assertNull(TransactionValidators.validateUpdateTransactions(data));
    }

    @Test
    @DisplayName("allows amount and precise_amount together")
    void allowsAmountAndPreciseAmountTogether() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("status", "commit");
      data.put("amount", 500);
      data.put("precise_amount", 50000);

      assertNull(TransactionValidators.validateUpdateTransactions(data));
    }

    @Test
    @DisplayName("rejects invalid precise_amount string values")
    void rejectsInvalidPreciseAmountStringValues() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("status", "commit");
      data.put("precise_amount", "12.5");

      assertEquals(
          "precise_amount must be a non-negative integer string or number.",
          TransactionValidators.validateUpdateTransactions(data));
    }

    @Test
    @DisplayName("rejects unknown fields")
    void rejectsUnknownFields() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("status", "commit");
      data.put("precise_amount", 50000);
      data.put("currency", "USD");

      assertEquals(
          "Invalid field: currency",
          TransactionValidators.validateUpdateTransactions(data));
    }

    @Test
    @DisplayName("allows skip_queue on update payloads")
    void allowsSkipQueueOnUpdatePayloads() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("status", "commit");
      data.put("skip_queue", true);

      assertNull(TransactionValidators.validateUpdateTransactions(data));
    }

    @Test
    @DisplayName("rejects invalid skip_queue on update payloads")
    void rejectsInvalidSkipQueueOnUpdatePayloads() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("status", "commit");
      data.put("skip_queue", "true");

      assertEquals(
          "skip_queue must be a boolean if provided.",
          TransactionValidators.validateUpdateTransactions(data));
    }
  }

  @Nested
  @DisplayName("Refund transaction request fields")
  class RefundTransactionRequestFields {

    @Test
    @DisplayName("allows skip_queue on refund payloads")
    void allowsSkipQueueOnRefundPayloads() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("skip_queue", true);

      assertNull(TransactionValidators.validateRefundTransaction(data));
    }

    @Test
    @DisplayName("allows empty refund options object")
    void allowsEmptyRefundOptionsObject() {
      Map<String, Object> data = new LinkedHashMap<>();

      assertNull(TransactionValidators.validateRefundTransaction(data));
    }

    @Test
    @DisplayName("rejects invalid skip_queue on refund payloads")
    void rejectsInvalidSkipQueueOnRefundPayloads() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("skip_queue", "true");

      assertEquals(
          "skip_queue must be a boolean if provided.",
          TransactionValidators.validateRefundTransaction(data));
    }

    @Test
    @DisplayName("rejects unknown fields on refund payloads")
    void rejectsUnknownFieldsOnRefundPayloads() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("skip_queue", true);
      data.put("amount", 100);

      assertEquals(
          "Invalid field: amount",
          TransactionValidators.validateRefundTransaction(data));
    }
  }

  @Nested
  @DisplayName("bulkCommitInflight validation")
  class BulkCommitInflightValidation {

    private Map<String, Object> item(String transactionId) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("transaction_id", transactionId);
      return item;
    }

    @Test
    @DisplayName("allows valid bulk commit inflight payloads")
    void allowsValidBulkCommitInflightPayloads() {
      Map<String, Object> second = item("txn_22222222-2222-4222-8222-222222222222");
      second.put("amount", 40);
      second.put("precise_amount", "125034");

      Map<String, Object> data = new LinkedHashMap<>();
      data.put("transactions", List.of(
          item("txn_11111111-1111-4111-8111-111111111111"),
          second));

      assertNull(TransactionValidators.validateBulkCommitInflight(data));
    }

    @Test
    @DisplayName("rejects empty transactions array")
    void rejectsEmptyTransactionsArray() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("transactions", List.of());

      assertEquals(
          "Transactions array cannot be empty.",
          TransactionValidators.validateBulkCommitInflight(data));
    }

    @Test
    @DisplayName("rejects oversized transactions array")
    void rejectsOversizedTransactionsArray() {
      List<Map<String, Object>> transactions = new ArrayList<>();
      for (int i = 0; i < TransactionConstants.MAX_BULK_INFLIGHT_ITEMS + 1; i++) {
        transactions.add(item("txn_test"));
      }

      Map<String, Object> data = new LinkedHashMap<>();
      data.put("transactions", transactions);

      assertEquals(
          "Too many transactions; max is " + TransactionConstants.MAX_BULK_INFLIGHT_ITEMS + ".",
          TransactionValidators.validateBulkCommitInflight(data));
    }

    @Test
    @DisplayName("rejects invalid precise_amount")
    void rejectsInvalidPreciseAmount() {
      Map<String, Object> first = item("txn_11111111-1111-4111-8111-111111111111");
      first.put("precise_amount", "-1");

      Map<String, Object> data = new LinkedHashMap<>();
      data.put("transactions", List.of(first));

      assertEquals(
          "precise_amount must be a non-negative integer string or number at index 0.",
          TransactionValidators.validateBulkCommitInflight(data));
    }

    @Test
    @DisplayName("allows skip_queue on bulk commit payloads")
    void allowsSkipQueueOnBulkCommitPayloads() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("skip_queue", true);
      data.put("transactions", List.of(item("txn_11111111-1111-4111-8111-111111111111")));

      assertNull(TransactionValidators.validateBulkCommitInflight(data));
    }

    @Test
    @DisplayName("rejects invalid skip_queue on bulk commit payloads")
    void rejectsInvalidSkipQueueOnBulkCommitPayloads() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("skip_queue", "true");
      data.put("transactions", List.of(item("txn_11111111-1111-4111-8111-111111111111")));

      assertEquals(
          "skip_queue must be a boolean if provided.",
          TransactionValidators.validateBulkCommitInflight(data));
    }
  }

  @Nested
  @DisplayName("bulkVoidInflight validation")
  class BulkVoidInflightValidation {

    @Test
    @DisplayName("allows valid bulk void inflight payloads")
    void allowsValidBulkVoidInflightPayloads() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("transaction_ids", List.of(
          "txn_11111111-1111-4111-8111-111111111111",
          "txn_22222222-2222-4222-8222-222222222222"));

      assertNull(TransactionValidators.validateBulkVoidInflight(data));
    }

    @Test
    @DisplayName("rejects empty transaction_ids array")
    void rejectsEmptyTransactionIdsArray() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("transaction_ids", List.of());

      assertEquals(
          "transaction_ids array cannot be empty.",
          TransactionValidators.validateBulkVoidInflight(data));
    }

    @Test
    @DisplayName("rejects oversized transaction_ids array")
    void rejectsOversizedTransactionIdsArray() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("transaction_ids",
          Collections.nCopies(TransactionConstants.MAX_BULK_INFLIGHT_ITEMS + 1, "txn_test"));

      assertEquals(
          "Too many transaction_ids; max is "
              + TransactionConstants.MAX_BULK_INFLIGHT_ITEMS + ".",
          TransactionValidators.validateBulkVoidInflight(data));
    }

    @Test
    @DisplayName("rejects missing transaction_id")
    void rejectsMissingTransactionId() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("transaction_ids", List.of(""));

      assertEquals(
          "transaction_id is required at index 0.",
          TransactionValidators.validateBulkVoidInflight(data));
    }

    @Test
    @DisplayName("allows skip_queue on bulk void payloads")
    void allowsSkipQueueOnBulkVoidPayloads() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("skip_queue", true);
      data.put("transaction_ids", List.of("txn_11111111-1111-4111-8111-111111111111"));

      assertNull(TransactionValidators.validateBulkVoidInflight(data));
    }

    @Test
    @DisplayName("rejects invalid skip_queue on bulk void payloads")
    void rejectsInvalidSkipQueueOnBulkVoidPayloads() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("skip_queue", "true");
      data.put("transaction_ids", List.of("txn_11111111-1111-4111-8111-111111111111"));

      assertEquals(
          "skip_queue must be a boolean if provided.",
          TransactionValidators.validateBulkVoidInflight(data));
    }
  }

  @Nested
  @DisplayName("recoverQueue")
  class RecoverQueue {

    @Test
    @DisplayName("allows valid threshold durations")
    void allowsValidThresholdDurations() {
      Map<String, Object> fiveMinutes = new LinkedHashMap<>();
      fiveMinutes.put("threshold", "5m");
      Map<String, Object> oneHour = new LinkedHashMap<>();
      oneHour.put("threshold", "1h");
      Map<String, Object> combined = new LinkedHashMap<>();
      combined.put("threshold", "2h45m");
      Map<String, Object> empty = new LinkedHashMap<>();

      List<Map<String, Object>> cases = List.of(fiveMinutes, oneHour, combined, empty);
      for (Map<String, Object> data : cases) {
        assertNull(TransactionValidators.validateRecoverQueue(data));
      }
    }

    @Test
    @DisplayName("rejects invalid threshold")
    void rejectsInvalidThreshold() {
      Map<String, Object> bogus = new LinkedHashMap<>();
      bogus.put("threshold", "bogus");
      assertEquals(
          "threshold must be a valid duration string (e.g. 5m, 1h).",
          TransactionValidators.validateRecoverQueue(bogus));

      Map<String, Object> emptyString = new LinkedHashMap<>();
      emptyString.put("threshold", "");
      assertEquals(
          "threshold must be a valid duration string (e.g. 5m, 1h).",
          TransactionValidators.validateRecoverQueue(emptyString));
    }

    @Test
    @DisplayName("rejects unknown fields")
    void rejectsUnknownFields() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("threshold", "5m");
      data.put("amount", 1);

      assertEquals(
          "Invalid field: amount",
          TransactionValidators.validateRecoverQueue(data));
    }
  }
}
