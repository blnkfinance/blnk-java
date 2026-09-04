package com.blnkfinance.blnk.validators;

import com.blnkfinance.blnk.types.TransactionConstants;
import com.blnkfinance.blnk.util.DateSerialization;
import com.blnkfinance.blnk.util.ValueFormat;
import com.blnkfinance.blnk.util.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validators for the transaction endpoints.
 *
 * <p>Pure validation — no HTTP, no logging. Every public validator receives
 * the request payload as a raw {@code Map<String,Object>} (an absent key means
 * the field was not provided; a key present with {@code null} is an explicit
 * null) and returns {@code null} when the payload is valid, otherwise the
 * exact failure message surfaced to callers. The first failing check wins.
 *
 * <p>Certain malformed inputs deliberately raise an unchecked exception
 * instead of returning a message; the calling endpoint's try/catch converts
 * that into a 500-shaped error response via {@code handleError}.
 *
 * <p>Precise (arbitrarily large) integer amounts use {@link BigInteger}
 * arithmetic; ordinary numeric amounts use {@code double} arithmetic.
 */
public final class TransactionValidators {

  private TransactionValidators() {}

  // -------------------------------------------------------------------------
  // Regexes & constants
  // -------------------------------------------------------------------------

  /** Digits only — a non-negative integer string. */
  private static final Pattern NON_NEGATIVE_INTEGER_STRING = Pattern.compile("^\\d+$");

  /**
   * Fixed-amount split legs: non-negative integer or decimal strings only.
   * Rejects exponent ({@code 1e3}), hex ({@code 0x10}), and whitespace-padded
   * values.
   */
  private static final Pattern FIXED_AMOUNT_DISTRIBUTION =
      Pattern.compile("^(?:\\d+|\\d+\\.\\d+)$");

  /** Percentage split legs such as {@code 20%} or {@code 33.33%}. */
  private static final Pattern PERCENTAGE_DISTRIBUTION =
      Pattern.compile("^(?:\\d+|\\d+\\.\\d+)%$");

  /** Building block — note {@code µs} is the literal U+00B5 MICRO SIGN + s. */
  private static final String GO_DURATION_UNIT = "(?:ns|us|µs|ms|s|m|h)";

  private static final Pattern GO_DURATION_PATTERN =
      Pattern.compile("^(?:\\d+(?:\\.\\d+)?" + GO_DURATION_UNIT + ")+$");

  private static final double DISTRIBUTION_SUM_EPSILON = 1e-9;

  /**
   * The largest integer exactly representable as a double (2^53 - 1 =
   * 9007199254740991); precise values above this threshold cannot round-trip
   * through double arithmetic.
   */
  private static final BigInteger MAX_SAFE_INTEGER = BigInteger.valueOf(9007199254740991L);

  private static final BigInteger ONE_HUNDRED = BigInteger.valueOf(100);

  // -------------------------------------------------------------------------
  // Loose property access on leg/item entries
  // -------------------------------------------------------------------------

  /**
   * Reads a field from a leg/item entry: a map returns its value (an absent
   * key reads as null); a null entry throws, which the calling endpoint
   * converts into an error response; any other value has no fields and reads
   * as null.
   */
  private static Object prop(Object obj, String key) {
    if (obj instanceof Map<?, ?> map) {
      return map.get(key);
    }
    if (obj == null) {
      throw new NullPointerException("Cannot read properties of null (reading '" + key + "')");
    }
    return null;
  }

  /** Whether the entry explicitly carries the key (regardless of its value). */
  private static boolean hasOwn(Object obj, String key) {
    if (obj instanceof Map<?, ?> map) {
      return map.containsKey(key);
    }
    if (obj == null) {
      throw new NullPointerException("Cannot read properties of null (reading '" + key + "')");
    }
    return false;
  }

  /** Whether the entry carries the key with a non-null value. */
  private static boolean hasOwnDefined(Object obj, String key) {
    if (obj instanceof Map<?, ?> map) {
      return map.containsKey(key) && map.get(key) != null;
    }
    if (obj == null) {
      throw new NullPointerException("Cannot read properties of null (reading '" + key + "')");
    }
    return false;
  }

  /**
   * List view of an array value. Only {@code List} and {@code Object[]} shapes
   * are supported; anything else fails with {@link ClassCastException}, which
   * the calling endpoint converts into an error response.
   */
  private static List<?> asList(Object value) {
    if (value instanceof List<?> list) {
      return list;
    }
    if (value instanceof Object[] array) {
      return Arrays.asList(array);
    }
    return (List<?>) value;
  }

  // -------------------------------------------------------------------------
  // validateOptionalDateField
  // -------------------------------------------------------------------------

  /**
   * Validates an optional date field: an absent key means "not provided" and
   * passes, while a key present with a null value fails the date check.
   */
  private static String validateOptionalDateField(
      Map<String, Object> data, String key, String fieldName) {
    if (!data.containsKey(key)) {
      return null;
    }
    if (!DateSerialization.isValidTransactionDateInput(data.get(key))) {
      return "Invalid " + fieldName + ".";
    }
    return null;
  }

  // -------------------------------------------------------------------------
  // parsePreciseInteger / transaction total
  // -------------------------------------------------------------------------

  /**
   * The resolved transaction total, carrying either an exact
   * {@link BigInteger} value or a {@code double} value depending on the
   * arithmetic mode.
   */
  private record TransactionTotal(boolean bigint, BigInteger big, double num) {
    static TransactionTotal ofBigint(BigInteger value) {
      return new TransactionTotal(true, value, 0);
    }

    static TransactionTotal ofNumber(double value) {
      return new TransactionTotal(false, null, value);
    }
  }

  /**
   * Parses a non-negative integer for precise amount/distribution fields,
   * exact beyond the double-safe range via {@link BigInteger}.
   *
   * <p>Strings are trimmed, so whitespace-padded values and leading zeros are
   * accepted; {@code "+5"}, {@code "-1"}, {@code "12.5"}, {@code ""}, and
   * {@code "1e3"} are rejected. A value that is neither a string nor a number
   * throws ({@code ClassCastException} / {@code NullPointerException}) by
   * design; the calling endpoint converts that into an error response.
   */
  private static BigInteger parsePreciseInteger(Object value) {
    if (value instanceof Number number) {
      // Numbers must be finite, non-negative integers.
      if (number instanceof BigInteger bigInteger) {
        return bigInteger.signum() < 0 ? null : bigInteger;
      }
      if (number instanceof Integer || number instanceof Long
          || number instanceof Short || number instanceof Byte) {
        long longValue = number.longValue();
        return longValue < 0 ? null : BigInteger.valueOf(longValue);
      }
      double d = number.doubleValue();
      if (Double.isNaN(d) || Double.isInfinite(d) || d < 0 || Math.floor(d) != d) {
        return null;
      }
      // Exact integer value of the integral double.
      return new BigDecimal(d).toBigIntegerExact();
    }

    // Non-strings throw here by design — see the class note on throw paths.
    String trimmed = ((String) value).strip();
    if (!NON_NEGATIVE_INTEGER_STRING.matcher(trimmed).matches()) {
      return null;
    }
    // Digits-only and non-empty by the regex, so this parse cannot fail.
    return new BigInteger(trimmed);
  }

  /** Whether a leg carries a non-null {@code precise_distribution}. */
  private static boolean hasPreciseDistribution(Object leg) {
    return hasOwnDefined(leg, "precise_distribution");
  }

  /** Whether any leg in the sources/destinations array carries a precise distribution. */
  private static boolean someLegHasPreciseDistribution(Object legs) {
    if (legs == null) {
      return false; // No legs provided.
    }
    for (Object leg : asList(legs)) { // A non-list value throws by design.
      if (hasPreciseDistribution(leg)) {
        return true;
      }
    }
    return false;
  }

  /** Whether the payload opts into exact integer (BigInteger) arithmetic. */
  private static boolean usesPreciseIntegerArithmetic(Map<String, Object> data) {
    if (someLegHasPreciseDistribution(data.get("sources"))) {
      return true;
    }
    if (someLegHasPreciseDistribution(data.get("destinations"))) {
      return true;
    }
    if (data.get("precise_amount") instanceof String) {
      return true;
    }
    if (data.containsKey("precise_amount") && data.get("precise_amount") != null
        && !(data.get("amount") instanceof Number)) {
      return true;
    }
    return false;
  }

  /**
   * Converts a numeric amount to {@link BigInteger}: fractional amounts are
   * silently truncated toward zero; non-finite amounts throw
   * {@link NumberFormatException}, which the calling endpoint converts into an
   * error response.
   */
  private static BigInteger bigIntFromTruncatedNumber(Number amount) {
    if (amount instanceof BigInteger bigInteger) {
      return bigInteger;
    }
    if (amount instanceof Integer || amount instanceof Long
        || amount instanceof Short || amount instanceof Byte) {
      return BigInteger.valueOf(amount.longValue());
    }
    // new BigDecimal(double) throws NumberFormatException for NaN/Infinity;
    // toBigInteger() truncates the fraction toward zero.
    return new BigDecimal(amount.doubleValue()).toBigInteger();
  }

  /**
   * Resolves the transaction total used for distribution checks. When both
   * {@code amount} and {@code precise_amount} are provided, {@code amount}
   * takes precedence.
   */
  private static TransactionTotal resolveTransactionTotal(Map<String, Object> data) {
    boolean hasAmount = data.get("amount") instanceof Number;
    boolean hasPreciseAmount =
        data.containsKey("precise_amount") && data.get("precise_amount") != null;

    if (!hasAmount && !hasPreciseAmount) {
      return null;
    }

    if (usesPreciseIntegerArithmetic(data)) {
      if (hasAmount) {
        return TransactionTotal.ofBigint(
            bigIntFromTruncatedNumber((Number) data.get("amount")));
      }

      BigInteger parsed = parsePreciseInteger(data.get("precise_amount")); // may throw by design
      if (parsed == null) {
        return null;
      }
      return TransactionTotal.ofBigint(parsed);
    }

    // A numeric amount is guaranteed here: a precise_amount without a numeric
    // amount always routes through the precise-arithmetic path above.
    return TransactionTotal.ofNumber(((Number) data.get("amount")).doubleValue());
  }

  // -------------------------------------------------------------------------
  // Split-leg routing
  // -------------------------------------------------------------------------

  /**
   * Truthiness-and-length check: falsy values count as absent; lists, strings,
   * and arrays are measured by their length; other truthy values have no
   * length and also count as absent. Note: an empty array counts as absent
   * here but still trips the non-empty-array error during leg validation.
   */
  private static boolean hasNonEmptyArrayLike(Object value) {
    if (ValueFormat.isFalsy(value)) {
      return false;
    }
    if (value instanceof List<?> list) {
      return !list.isEmpty();
    }
    if (value instanceof String s) {
      return !s.isEmpty();
    }
    if (value.getClass().isArray()) {
      return java.lang.reflect.Array.getLength(value) > 0;
    }
    if (value instanceof com.fasterxml.jackson.databind.node.ArrayNode arrayNode) {
      return arrayNode.size() > 0;
    }
    return false; // No measurable length — treated as absent.
  }

  /** Enforces the mutual-exclusion rules between source/sources and destination/destinations. */
  private static String validateSplitLegRouting(Map<String, Object> data) {
    boolean hasSource = ValueFormat.isTruthy(data.get("source"));
    boolean hasSources = hasNonEmptyArrayLike(data.get("sources"));
    boolean hasDestination = ValueFormat.isTruthy(data.get("destination"));
    boolean hasDestinations = hasNonEmptyArrayLike(data.get("destinations"));

    if (hasSource && hasSources) {
      return "Both 'source' and 'sources' cannot be provided together.";
    }

    if (hasDestination && hasDestinations) {
      return "Both 'destination' and 'destinations' cannot be provided together.";
    }

    if (hasSources) {
      if (hasDestinations) {
        return "'sources' requires a single 'destination'; use 'destination' instead of"
            + " 'destinations'.";
      }
      if (!hasDestination) {
        return "'destination' is required when using 'sources'.";
      }
    }

    if (hasDestinations && !hasSource) {
      return "'source' is required when using 'destinations'.";
    }

    return null;
  }

  // -------------------------------------------------------------------------
  // ValidateCreateTransactions
  // -------------------------------------------------------------------------

  /** Validates the payload for creating a transaction. */
  public static String validateCreateTransactions(Map<String, Object> data) {
    TransactionTotal transactionTotal = resolveTransactionTotal(data);
    if (transactionTotal == null) {
      if (data.containsKey("precise_amount") && data.get("precise_amount") != null
          && !(data.get("amount") instanceof Number)) {
        return "precise_amount must be a non-negative integer string or number.";
      }
      return "Either 'amount' or 'precise_amount' must be provided.";
    }

    if (data.containsKey("amount") && !(data.get("amount") instanceof Number)) {
      return "Amount must be a number.";
    }

    if (data.containsKey("precise_amount") && data.get("precise_amount") != null) {
      Object preciseAmount = data.get("precise_amount");
      boolean isValidPreciseAmount =
          preciseAmount instanceof Number || preciseAmount instanceof String;
      if (!isValidPreciseAmount) {
        // Reachable only when a numeric amount is also present — otherwise
        // the earlier total resolution throws first.
        return "precise_amount must be a string or number.";
      }
      if (parsePreciseInteger(preciseAmount) == null) {
        return "precise_amount must be a non-negative integer string or number.";
      }
    }

    if (!StringUtils.isValidNumber(data.get("precision"))) {
      return "Precision must be a number."; // Required; any Number, incl. NaN, passes.
    }
    if (!StringUtils.isValidString(data.get("reference"))) {
      return "Reference must be a string.";
    }
    if (!StringUtils.isValidString(data.get("description"))) {
      return "Description must be a string.";
    }
    if (!StringUtils.isValidString(data.get("currency"))) {
      return "Invalid currency."; // Type check only — an empty string passes.
    }

    String splitLegError = validateSplitLegRouting(data);
    if (splitLegError != null) {
      return splitLegError;
    }

    // Truthiness-guarded — falsy non-string values (0, "", false) pass.
    if (ValueFormat.isTruthy(data.get("source")) && !(data.get("source") instanceof String)) {
      return "Invalid source.";
    }

    if (ValueFormat.isTruthy(data.get("destination"))
        && !(data.get("destination") instanceof String)) {
      return "Destination must be a string.";
    }

    if (ValueFormat.isTruthy(data.get("sources"))) {
      String sourcesError = validateSplitLegs(data.get("sources"), transactionTotal, "source");
      if (sourcesError != null) {
        return sourcesError;
      }
    }

    if (ValueFormat.isTruthy(data.get("destinations"))) {
      String destinationsError =
          validateSplitLegs(data.get("destinations"), transactionTotal, "destination");
      if (destinationsError != null) {
        return destinationsError;
      }
    }

    if (data.containsKey("inflight") && !(data.get("inflight") instanceof Boolean)) {
      return "Inflight must be a boolean if provided.";
    }

    String inflightExpiryError =
        validateOptionalDateField(data, "inflight_expiry_date", "inflight expiry date");
    if (inflightExpiryError != null) {
      return inflightExpiryError;
    }

    String scheduledForError =
        validateOptionalDateField(data, "scheduled_for", "scheduled date");
    if (scheduledForError != null) {
      return scheduledForError;
    }

    String effectiveDateError =
        validateOptionalDateField(data, "effective_date", "effective_date");
    if (effectiveDateError != null) {
      return effectiveDateError;
    }

    String inflightCommitError =
        validateOptionalDateField(data, "inflight_commit_date", "inflight_commit_date");
    if (inflightCommitError != null) {
      return inflightCommitError;
    }

    if (data.containsKey("skip_queue") && !(data.get("skip_queue") instanceof Boolean)) {
      return "skip_queue must be a boolean if provided.";
    }

    if (data.containsKey("dry_run") && !(data.get("dry_run") instanceof Boolean)) {
      return "dry_run must be a boolean if provided.";
    }

    if (data.containsKey("atomic") && !(data.get("atomic") instanceof Boolean)) {
      // Note: lowercase here, capitalized in the bulk validator — callers
      // depend on the exact text.
      return "atomic must be a boolean if provided.";
    }

    if (data.containsKey("allow_overdraft")
        && !(data.get("allow_overdraft") instanceof Boolean)) {
      return "Allow overdraft must be a boolean if provided.";
    }

    if (data.containsKey("meta_data")
        && !LedgerBalanceValidators.isValidMetaData(data.get("meta_data"))) {
      // No trailing period — callers depend on the exact text.
      return "meta_data must be a valid object if provided";
    }

    // No unknown-field check here — extra fields flow through.
    return null;
  }

  // -------------------------------------------------------------------------
  // Split legs
  // -------------------------------------------------------------------------

  /**
   * Validates a split-leg array against the transaction total; legLabel is
   * "source" or "destination".
   */
  private static String validateSplitLegs(
      Object legsValue, TransactionTotal total, String legLabel) {
    String legArrayName = legLabel.equals("source") ? "sources" : "destinations";

    if (!StringUtils.isValidArray(legsValue) || asList(legsValue).isEmpty()) {
      return "'" + legArrayName + "' must be a non-empty array.";
    }
    List<?> legs = asList(legsValue);

    for (Object leg : legs) {
      if (!StringUtils.isValidString(prop(leg, "identifier"))) {
        return "Each " + legLabel + " leg must include a valid identifier.";
      }

      boolean hasDistribution = hasOwnDefined(leg, "distribution");
      boolean hasPreciseDistributionValue = hasPreciseDistribution(leg);

      if (!hasDistribution && !hasPreciseDistributionValue) {
        return "Each " + legLabel
            + " leg must include either 'distribution' or 'precise_distribution'.";
      }

      Object preciseDistribution = prop(leg, "precise_distribution");
      if (hasPreciseDistributionValue
          && !(preciseDistribution instanceof String)
          && !(preciseDistribution instanceof Number)) {
        return "precise_distribution must be a string or number for leg: "
            + prop(leg, "identifier") + ".";
      }
    }

    if (total.bigint()) {
      return validateDistributionLegsBigInt(legs, total.big());
    }

    return validateDistributionLegsNumber(legs, total.num());
  }

  // -------------------------------------------------------------------------
  // Distribution parsing helpers
  // -------------------------------------------------------------------------

  /** Parses a percentage distribution — no trimming; range 0..100 inclusive. */
  private static Double parsePercentageDistribution(Object distribution) {
    String s = (String) distribution;
    if (!PERCENTAGE_DISTRIBUTION.matcher(s).matches()) {
      return null;
    }

    double value = ValueFormat.coerceNumber(s.substring(0, s.length() - 1));
    if (Double.isNaN(value) || Double.isInfinite(value) || value < 0 || value > 100) {
      return null;
    }

    return value;
  }

  /**
   * Parses a fixed-amount distribution string. The trim-equality guard is
   * redundant with the regex but kept deliberately: the cast is what throws
   * when a null distribution reaches this helper, and the calling endpoint
   * converts that into an error response. Very large strings lose float
   * precision — only {@code precise_distribution} is exact.
   */
  private static Double parseFixedAmountDistribution(Object distribution) {
    String s = (String) distribution; // Non-strings throw here by design.
    if (!s.strip().equals(s)) {
      return null;
    }

    if (!FIXED_AMOUNT_DISTRIBUTION.matcher(s).matches()) {
      return null;
    }

    double value = ValueFormat.coerceNumber(s);
    if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
      return null;
    }

    return value;
  }

  /** Approximate equality within the distribution-sum epsilon. */
  private static boolean distributionTotalsApproximatelyEqual(double sum, double amount) {
    return Math.abs(sum - amount) <= DISTRIBUTION_SUM_EPSILON;
  }

  /** Whether the distribution is a fixed amount with a fractional part. */
  private static boolean isFixedDecimalDistribution(Object distribution) {
    Double parsed = parseFixedAmountDistribution(distribution);
    return parsed != null && Math.floor(parsed) != parsed;
  }

  /** Whether the distribution is a percentage with a fractional part. */
  private static boolean hasDecimalPercentageDistribution(Object distribution) {
    Double parsed = parsePercentageDistribution(distribution);
    return parsed != null && Math.floor(parsed) != parsed;
  }

  /**
   * Whether the leg's distribution carries a decimal component. Called only
   * from the BigInteger dispatch; a present-but-null or non-string
   * distribution reaches {@code parseFixedAmountDistribution} and throws.
   */
  private static boolean legUsesDecimalDistribution(Object leg) {
    if (!hasOwn(leg, "distribution")) {
      return false; // Distribution not provided.
    }

    Object distribution = prop(leg, "distribution");
    return isFixedDecimalDistribution(distribution)
        || hasDecimalPercentageDistribution(distribution);
  }

  // -------------------------------------------------------------------------
  // Distribution leg validation — bigint / number paths
  // -------------------------------------------------------------------------

  /** Validates distribution legs with exact integer arithmetic. */
  private static String validateDistributionLegsBigInt(List<?> legs, BigInteger total) {
    boolean hasDecimalDistribution = false;
    for (Object leg : legs) { // Short-circuits on the first decimal distribution.
      if (legUsesDecimalDistribution(leg)) {
        hasDecimalDistribution = true;
        break;
      }
    }

    if (hasDecimalDistribution && total.compareTo(MAX_SAFE_INTEGER) > 0) {
      return "Decimal distribution values are not supported with precise amounts beyond"
          + " Number.MAX_SAFE_INTEGER.";
    }

    if (hasDecimalDistribution && total.compareTo(MAX_SAFE_INTEGER) <= 0) {
      // Converting to double is exact for totals within the safe range.
      return validateDistributionLegsWithDecimals(legs, total.doubleValue());
    }

    BigInteger sum = BigInteger.ZERO;
    boolean hasLeft = false;

    for (Object leg : legs) {
      if (hasPreciseDistribution(leg)) {
        BigInteger preciseValue = parsePreciseInteger(prop(leg, "precise_distribution"));
        if (preciseValue == null) {
          return "Invalid precise_distribution for leg: " + prop(leg, "identifier") + ".";
        }
        sum = sum.add(preciseValue);
        continue;
      }

      Object distribution = prop(leg, "distribution");
      if (ValueFormat.isFalsy(distribution) || !StringUtils.isValidString(distribution)) {
        return "Invalid distribution type for leg: " + prop(leg, "identifier") + ".";
      }
      String distributionString = (String) distribution;

      if (distributionString.endsWith("%")) {
        Double percentageValue = parsePercentageDistribution(distributionString);
        if (percentageValue == null) {
          return "Invalid percentage value in leg: " + prop(leg, "identifier") + ".";
        }
        // Integer division truncates toward zero. Decimal percentages never
        // reach this path, so the long cast is exact.
        sum = sum.add(
            total.multiply(BigInteger.valueOf((long) percentageValue.doubleValue()))
                .divide(ONE_HUNDRED));
      } else if (distributionString.equals("left")) {
        if (hasLeft) {
          return "Multiple 'left' distribution types are not allowed.";
        }
        hasLeft = true;
      } else {
        Double fixedAmount = parseFixedAmountDistribution(distributionString);
        if (fixedAmount == null || Math.floor(fixedAmount) != fixedAmount) {
          return "Invalid distribution type for leg: " + prop(leg, "identifier") + ".";
        }
        // Exact integer value of the integral double (very large fixed strings
        // already lost precision during float parsing).
        sum = sum.add(new BigDecimal(fixedAmount.doubleValue()).toBigIntegerExact());
      }
    }

    if (hasLeft) {
      BigInteger remaining = total.subtract(sum);
      if (remaining.signum() < 0) {
        return "Total distribution exceeds the specified amount.";
      }
    } else if (!sum.equals(total)) {
      // Values interpolate as plain decimal digits.
      return "Total distribution sum (" + sum + ") does not equal the specified amount ("
          + total + ").";
    }

    return null;
  }

  /** Double-arithmetic dispatch — delegates to the decimals-aware validator. */
  private static String validateDistributionLegsNumber(List<?> legs, double amount) {
    return validateDistributionLegsWithDecimals(legs, amount);
  }

  /** Validates distribution legs with double arithmetic. */
  private static String validateDistributionLegsWithDecimals(List<?> legs, double amount) {
    double sum = 0;
    boolean hasLeft = false;

    for (Object leg : legs) {
      if (hasPreciseDistribution(leg)) {
        BigInteger preciseValue = parsePreciseInteger(prop(leg, "precise_distribution"));
        if (preciseValue == null) {
          return "Invalid precise_distribution for leg: " + prop(leg, "identifier") + ".";
        }
        if (preciseValue.compareTo(MAX_SAFE_INTEGER) > 0) {
          // Distinct check, same message as the parse failure — callers depend
          // on the shared text.
          return "Invalid precise_distribution for leg: " + prop(leg, "identifier") + ".";
        }
        sum += preciseValue.doubleValue();
        continue;
      }

      Object distribution = prop(leg, "distribution");
      if (ValueFormat.isFalsy(distribution) || !StringUtils.isValidString(distribution)) {
        return "Invalid distribution type for leg: " + prop(leg, "identifier") + ".";
      }
      String distributionString = (String) distribution;

      if (distributionString.endsWith("%")) {
        Double percentageValue = parsePercentageDistribution(distributionString);
        if (percentageValue == null) {
          return "Invalid percentage value in leg: " + prop(leg, "identifier") + ".";
        }
        sum += (percentageValue / 100) * amount;
      } else if (distributionString.equals("left")) {
        if (hasLeft) {
          return "Multiple 'left' distribution types are not allowed.";
        }
        hasLeft = true;
      } else {
        Double fixedAmount = parseFixedAmountDistribution(distributionString);
        if (fixedAmount == null) {
          return "Invalid distribution type for leg: " + prop(leg, "identifier") + ".";
        }
        sum += fixedAmount;
      }
    }

    if (hasLeft) {
      double remaining = amount - sum;
      if (remaining < -DISTRIBUTION_SUM_EPSILON) {
        return "Total distribution exceeds the specified amount.";
      }
    } else if (!distributionTotalsApproximatelyEqual(sum, amount)) {
      // Numbers are formatted via ValueFormat.formatNumber so integral sums
      // render without a decimal point.
      return "Total distribution sum (" + ValueFormat.formatNumber(sum)
          + ") does not equal the specified amount (" + ValueFormat.formatNumber(amount)
          + ").";
    }

    return null;
  }

  // -------------------------------------------------------------------------
  // ValidateUpdateTransactions
  // -------------------------------------------------------------------------

  /**
   * Validates the payload for updating a transaction. Any string is accepted
   * as a status — the value is not checked against a status list.
   */
  public static String validateUpdateTransactions(Map<String, Object> data) {
    if (!StringUtils.isValidString(data.get("status"))) {
      return "Status must be a string.";
    }

    if (data.containsKey("amount") && !(data.get("amount") instanceof Number)) {
      return "Amount must be a number.";
    }

    if (data.containsKey("precise_amount") && data.get("precise_amount") != null) {
      Object preciseAmount = data.get("precise_amount");
      boolean isValidPreciseAmount =
          preciseAmount instanceof Number || preciseAmount instanceof String;
      if (!isValidPreciseAmount) {
        return "precise_amount must be a string or number.";
      }
      if (parsePreciseInteger(preciseAmount) == null) {
        return "precise_amount must be a non-negative integer string or number.";
      }
    }

    if (data.containsKey("meta_data")
        && !LedgerBalanceValidators.isValidMetaData(data.get("meta_data"))) {
      return "meta_data must be a valid object if provided";
    }

    if (data.containsKey("skip_queue") && !(data.get("skip_queue") instanceof Boolean)) {
      return "skip_queue must be a boolean if provided.";
    }

    if (data.containsKey("dry_run") && !(data.get("dry_run") instanceof Boolean)) {
      return "dry_run must be a boolean if provided.";
    }

    List<String> allowedFields =
        List.of("status", "amount", "precise_amount", "meta_data", "skip_queue", "dry_run");
    for (String key : data.keySet()) { // Insertion order decides which field is reported.
      if (!allowedFields.contains(key)) {
        return "Invalid field: " + key; // No trailing period — callers depend on the text.
      }
    }

    return null;
  }

  // -------------------------------------------------------------------------
  // ValidateRefundTransaction
  // -------------------------------------------------------------------------

  /** Validates the refund payload; an empty map is valid. */
  public static String validateRefundTransaction(Map<String, Object> data) {
    if (data.containsKey("skip_queue") && !(data.get("skip_queue") instanceof Boolean)) {
      return "skip_queue must be a boolean if provided.";
    }

    if (data.containsKey("dry_run") && !(data.get("dry_run") instanceof Boolean)) {
      return "dry_run must be a boolean if provided.";
    }

    if (data.containsKey("description") && !StringUtils.isValidString(data.get("description"))) {
      return "description must be a valid string if provided";
    }

    if (data.containsKey("meta_data")
        && !LedgerBalanceValidators.isValidMetaData(data.get("meta_data"))) {
      return "meta_data must be a valid object if provided";
    }

    List<String> allowedFields = List.of("skip_queue", "dry_run", "description", "meta_data");
    for (String key : data.keySet()) {
      if (!allowedFields.contains(key)) {
        return "Invalid field: " + key;
      }
    }

    return null;
  }

  // -------------------------------------------------------------------------
  // ValidateBulkVoidInflight
  // -------------------------------------------------------------------------

  /** Validates the payload for voiding inflight transactions in bulk. */
  public static String validateBulkVoidInflight(Map<String, Object> data) {
    if (data.containsKey("skip_queue") && !(data.get("skip_queue") instanceof Boolean)) {
      return "skip_queue must be a boolean if provided.";
    }

    if (data.containsKey("dry_run") && !(data.get("dry_run") instanceof Boolean)) {
      return "dry_run must be a boolean if provided.";
    }

    if (!StringUtils.isValidArray(data.get("transaction_ids"))) {
      return "transaction_ids must be an array.";
    }
    List<?> transactionIds = asList(data.get("transaction_ids"));

    if (transactionIds.isEmpty()) {
      return "transaction_ids array cannot be empty.";
    }

    if (transactionIds.size() > TransactionConstants.MAX_BULK_INFLIGHT_ITEMS) {
      return "Too many transaction_ids; max is "
          + TransactionConstants.MAX_BULK_INFLIGHT_ITEMS + ".";
    }

    for (int i = 0; i < transactionIds.size(); i++) {
      Object id = transactionIds.get(i);

      if (!(id instanceof String s) || s.strip().isEmpty()) {
        return "transaction_id is required at index " + i + ".";
      }
    }

    List<String> allowedFields = List.of("skip_queue", "dry_run", "transaction_ids");
    for (String key : data.keySet()) {
      if (!allowedFields.contains(key)) {
        return "Invalid field: " + key;
      }
    }

    return null;
  }

  // -------------------------------------------------------------------------
  // ValidateBulkCommitInflight
  // -------------------------------------------------------------------------

  /** Validates the payload for committing inflight transactions in bulk. */
  public static String validateBulkCommitInflight(Map<String, Object> data) {
    if (data.containsKey("skip_queue") && !(data.get("skip_queue") instanceof Boolean)) {
      return "skip_queue must be a boolean if provided.";
    }

    if (data.containsKey("dry_run") && !(data.get("dry_run") instanceof Boolean)) {
      return "dry_run must be a boolean if provided.";
    }

    if (!StringUtils.isValidArray(data.get("transactions"))) {
      return "Transactions must be an array.";
    }
    List<?> transactions = asList(data.get("transactions"));

    if (transactions.isEmpty()) {
      return "Transactions array cannot be empty.";
    }

    if (transactions.size() > TransactionConstants.MAX_BULK_INFLIGHT_ITEMS) {
      return "Too many transactions; max is "
          + TransactionConstants.MAX_BULK_INFLIGHT_ITEMS + ".";
    }

    for (int i = 0; i < transactions.size(); i++) {
      // A null element throws on field access; the calling endpoint converts
      // that into an error response.
      Object item = transactions.get(i);

      Object transactionId = prop(item, "transaction_id");
      if (!(transactionId instanceof String s) || s.strip().isEmpty()) {
        return "transaction_id is required at index " + i + ".";
      }

      if (hasOwn(item, "amount") && !(prop(item, "amount") instanceof Number)) {
        return "amount must be a number at index " + i + "."; // Lowercase — callers depend on the text.
      }

      if (hasOwnDefined(item, "precise_amount")) {
        Object preciseAmount = prop(item, "precise_amount");
        boolean isValidPreciseAmount =
            preciseAmount instanceof Number || preciseAmount instanceof String;
        if (!isValidPreciseAmount) {
          return "precise_amount must be a string or number at index " + i + ".";
        }
        if (parsePreciseInteger(preciseAmount) == null) {
          return "precise_amount must be a non-negative integer string or number at index "
              + i + ".";
        }
      }
    }

    List<String> allowedFields = List.of("skip_queue", "dry_run", "transactions");
    for (String key : data.keySet()) {
      if (!allowedFields.contains(key)) {
        return "Invalid field: " + key;
      }
    }

    return null;
  }

  // -------------------------------------------------------------------------
  // ValidateBulkTransactions
  // -------------------------------------------------------------------------

  /**
   * Validates the payload for creating transactions in bulk. There is no
   * unknown-field check at the top level — extra fields flow through.
   */
  @SuppressWarnings("unchecked")
  public static String validateBulkTransactions(Map<String, Object> data) {
    if (data.containsKey("atomic") && !(data.get("atomic") instanceof Boolean)) {
      // Note: capitalized here, lowercase in the single-transaction validator
      // — callers depend on the exact text.
      return "Atomic must be a boolean if provided.";
    }

    if (data.containsKey("inflight") && !(data.get("inflight") instanceof Boolean)) {
      return "Inflight must be a boolean if provided.";
    }

    if (data.containsKey("run_async") && !(data.get("run_async") instanceof Boolean)) {
      return "Run_async must be a boolean if provided."; // Capitalized — callers depend on the text.
    }

    if (data.containsKey("skip_queue") && !(data.get("skip_queue") instanceof Boolean)) {
      return "skip_queue must be a boolean if provided.";
    }

    if (data.containsKey("dry_run") && !(data.get("dry_run") instanceof Boolean)) {
      return "dry_run must be a boolean if provided.";
    }

    if (!StringUtils.isValidArray(data.get("transactions"))) {
      return "Transactions must be an array.";
    }
    List<?> transactions = asList(data.get("transactions"));

    if (transactions.isEmpty()) {
      return "Transactions array cannot be empty.";
    }

    if (transactions.size() > TransactionConstants.MAX_BULK_CREATE_ITEMS) {
      return "Too many transactions; max is "
          + TransactionConstants.MAX_BULK_CREATE_ITEMS + ".";
    }

    for (int i = 0; i < transactions.size(); i++) {
      Object element = transactions.get(i);
      // A non-map element carries no fields, so it fails the per-item checks
      // with a 400 message instead of crashing the validator.
      @SuppressWarnings("unchecked")
      Map<String, Object> transaction =
          element instanceof Map
              ? (Map<String, Object>) element
              : (element == null ? null : Map.of());
      String validationError = validateCreateTransactions(transaction);
      if (validationError != null) {
        return "Transaction at index " + i + ": " + validationError;
      }
    }

    // Set-based exact string equality; references are guaranteed strings by
    // the per-item validation above.
    Set<Object> uniqueReferences = new HashSet<>();
    for (Object transaction : transactions) {
      uniqueReferences.add(prop(transaction, "reference"));
    }
    if (transactions.size() != uniqueReferences.size()) {
      return "All transactions must have unique references within the bulk request.";
    }

    return null;
  }

  // -------------------------------------------------------------------------
  // ValidateRecoverQueue
  // -------------------------------------------------------------------------

  /** A valid duration is trimmed, non-empty, and composed of Go-style duration segments. */
  private static boolean isValidGoDuration(String value) {
    String trimmed = value.strip();
    return !trimmed.isEmpty() && GO_DURATION_PATTERN.matcher(trimmed).matches();
  }

  /** Validates the payload for recovering the transaction queue. */
  public static String validateRecoverQueue(Map<String, Object> data) {
    // Unlike the other validators in this class, the unknown-field check runs
    // first here.
    List<String> allowedFields = List.of("threshold");
    for (String key : data.keySet()) {
      if (!allowedFields.contains(key)) {
        return "Invalid field: " + key;
      }
    }

    if (!data.containsKey("threshold")) {
      return null;
    }

    Object threshold = data.get("threshold");
    if (!(threshold instanceof String s) || !isValidGoDuration(s)) {
      return "threshold must be a valid duration string (e.g. 5m, 1h).";
    }

    return null;
  }
}
