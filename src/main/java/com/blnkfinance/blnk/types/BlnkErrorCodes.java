package com.blnkfinance.blnk.types;

/**
 * Core {@code error_detail.code} values, mirroring {@code internal/apierror/codes.go}
 * in Blnk Core 0.15.4. Compare against {@link BlnkApiErrorDetail#code()}. Each comment
 * gives the HTTP status Core sends with the code; unlisted codes still pass through.
 */
public final class BlnkErrorCodes {

  private BlnkErrorCodes() {}

  // GEN

  /** 400, body could not be decoded. */
  public static final String GEN_MALFORMED_REQUEST = "GEN_MALFORMED_REQUEST";

  /** 400. */
  public static final String GEN_VALIDATION_ERROR = "GEN_VALIDATION_ERROR";

  /** 400. */
  public static final String GEN_MISSING_PARAMETER = "GEN_MISSING_PARAMETER";

  /** 400. */
  public static final String GEN_BAD_REQUEST = "GEN_BAD_REQUEST";

  /** 404. */
  public static final String GEN_NOT_FOUND = "GEN_NOT_FOUND";

  /** 409, duplicate GL indicator + currency, or a multi-leg refund that failed part-way (0.15.4+). */
  public static final String GEN_CONFLICT = "GEN_CONFLICT";

  /** 423. */
  public static final String GEN_RESOURCE_LOCKED = "GEN_RESOURCE_LOCKED";

  /** 413. */
  public static final String GEN_PAYLOAD_TOO_LARGE = "GEN_PAYLOAD_TOO_LARGE";

  /** 429. */
  public static final String GEN_RATE_LIMITED = "GEN_RATE_LIMITED";

  /** 500. */
  public static final String GEN_INTERNAL = "GEN_INTERNAL";

  // AUTH

  /** 401. */
  public static final String AUTH_MISSING_API_KEY = "AUTH_MISSING_API_KEY";

  /** 401. */
  public static final String AUTH_INVALID_API_KEY = "AUTH_INVALID_API_KEY";

  /** 401. */
  public static final String AUTH_EXPIRED_API_KEY = "AUTH_EXPIRED_API_KEY";

  /** 401. */
  public static final String AUTH_MISSING_PRINCIPAL = "AUTH_MISSING_PRINCIPAL";

  /** 403. */
  public static final String AUTH_INSUFFICIENT_PERMISSIONS = "AUTH_INSUFFICIENT_PERMISSIONS";

  /** 403. */
  public static final String AUTH_UNKNOWN_RESOURCE = "AUTH_UNKNOWN_RESOURCE";

  /** 403. */
  public static final String AUTH_MASTER_KEY_REQUIRED = "AUTH_MASTER_KEY_REQUIRED";

  /** 403. */
  public static final String AUTH_CROSS_OWNER_ACCESS = "AUTH_CROSS_OWNER_ACCESS";

  /** 403, key granted scopes it does not hold. */
  public static final String AUTH_SCOPE_ESCALATION = "AUTH_SCOPE_ESCALATION";

  /** 401. */
  public static final String AUTH_METRICS_TOKEN_REQUIRED = "AUTH_METRICS_TOKEN_REQUIRED";

  /** 401. */
  public static final String AUTH_INVALID_BEARER_TOKEN = "AUTH_INVALID_BEARER_TOKEN";

  /** 403. */
  public static final String AUTH_METRICS_DISABLED = "AUTH_METRICS_DISABLED";

  // APIKEY

  /** 404. */
  public static final String APIKEY_NOT_FOUND = "APIKEY_NOT_FOUND";

  /** 400. */
  public static final String APIKEY_OWNER_REQUIRED = "APIKEY_OWNER_REQUIRED";

  /** 400. */
  public static final String APIKEY_INVALID = "APIKEY_INVALID";

  // TXN

  /** 404, by id or reference. */
  public static final String TXN_NOT_FOUND = "TXN_NOT_FOUND";

  /** 400, and overdraft is off. */
  public static final String TXN_INSUFFICIENT_FUNDS = "TXN_INSUFFICIENT_FUNDS";

  /** 400. Some Core versions report this as {@link #TXN_VALIDATION_ERROR}; match either. */
  public static final String TXN_INVALID_AMOUNT = "TXN_INVALID_AMOUNT";

  /** 400. */
  public static final String TXN_PRECISION_NOT_INTEGER = "TXN_PRECISION_NOT_INTEGER";

  /** 400, multi-source/destination legs do not add up. */
  public static final String TXN_INVALID_DISTRIBUTION = "TXN_INVALID_DISTRIBUTION";

  /** 409. */
  public static final String TXN_DUPLICATE_REFERENCE = "TXN_DUPLICATE_REFERENCE";

  /** 400. */
  public static final String TXN_NOT_INFLIGHT = "TXN_NOT_INFLIGHT";

  /** 409. */
  public static final String TXN_ALREADY_COMMITTED = "TXN_ALREADY_COMMITTED";

  /** 409. */
  public static final String TXN_ALREADY_VOIDED = "TXN_ALREADY_VOIDED";

  /** 409, refunding twice or refunding a refund (0.15.4+). */
  public static final String TXN_ALREADY_REFUNDED = "TXN_ALREADY_REFUNDED";

  /** 400. */
  public static final String TXN_COMMIT_AMOUNT_EXCEEDED = "TXN_COMMIT_AMOUNT_EXCEEDED";

  /** 400, status is not {@code commit} or {@code void}. */
  public static final String TXN_INVALID_STATUS_ACTION = "TXN_INVALID_STATUS_ACTION";

  /** 400. */
  public static final String TXN_BULK_EMPTY = "TXN_BULK_EMPTY";

  /** 400. */
  public static final String TXN_BULK_LIMIT_EXCEEDED = "TXN_BULK_LIMIT_EXCEEDED";

  /** 400, includes both {@code sources} and {@code destinations} on one request (0.15.4+). */
  public static final String TXN_VALIDATION_ERROR = "TXN_VALIDATION_ERROR";

  // BAL

  /** 404. Since 0.15.4 a transaction naming a missing balance returns this, not {@link #TXN_NOT_FOUND}. */
  public static final String BAL_NOT_FOUND = "BAL_NOT_FOUND";

  /** 404, no history at the requested timestamp. */
  public static final String BAL_HISTORY_NOT_FOUND = "BAL_HISTORY_NOT_FOUND";

  /** 400. */
  public static final String BAL_INVALID_TIMESTAMP = "BAL_INVALID_TIMESTAMP";

  /** 400. */
  public static final String BAL_VALIDATION_ERROR = "BAL_VALIDATION_ERROR";

  /** 404. */
  public static final String BAL_MONITOR_NOT_FOUND = "BAL_MONITOR_NOT_FOUND";

  // LGR

  /** 404. */
  public static final String LGR_NOT_FOUND = "LGR_NOT_FOUND";

  /** 409. */
  public static final String LGR_DUPLICATE = "LGR_DUPLICATE";

  // ACC

  /** 404. */
  public static final String ACC_NOT_FOUND = "ACC_NOT_FOUND";

  /** 409. */
  public static final String ACC_DUPLICATE = "ACC_DUPLICATE";

  /** 500. */
  public static final String ACC_GENERATION_FAILED = "ACC_GENERATION_FAILED";

  // IDT

  /** 404. */
  public static final String IDT_NOT_FOUND = "IDT_NOT_FOUND";

  /** 400. */
  public static final String IDT_VALIDATION_ERROR = "IDT_VALIDATION_ERROR";

  /** 400. */
  public static final String IDT_FIELD_NOT_TOKENIZABLE = "IDT_FIELD_NOT_TOKENIZABLE";

  /** 409. */
  public static final String IDT_FIELD_ALREADY_TOKENIZED = "IDT_FIELD_ALREADY_TOKENIZED";

  /** 400. */
  public static final String IDT_FIELD_NOT_TOKENIZED = "IDT_FIELD_NOT_TOKENIZED";

  /** 400. */
  public static final String IDT_FIELD_NOT_FOUND = "IDT_FIELD_NOT_FOUND";

  /** 403. */
  public static final String IDT_TOKENIZATION_DISABLED = "IDT_TOKENIZATION_DISABLED";

  // RECON

  /** 404. */
  public static final String RECON_NOT_FOUND = "RECON_NOT_FOUND";

  /** 404. */
  public static final String RECON_RULE_NOT_FOUND = "RECON_RULE_NOT_FOUND";

  /** 400. */
  public static final String RECON_UPLOAD_FAILED = "RECON_UPLOAD_FAILED";

  /** 500. */
  public static final String RECON_UPLOAD_PROCESSING_FAILED = "RECON_UPLOAD_PROCESSING_FAILED";

  /** 400. */
  public static final String RECON_UPLOAD_URL_INVALID = "RECON_UPLOAD_URL_INVALID";

  /** 400. */
  public static final String RECON_UPLOAD_HOST_NOT_ALLOWED = "RECON_UPLOAD_HOST_NOT_ALLOWED";

  /** 400. */
  public static final String RECON_RULE_INVALID = "RECON_RULE_INVALID";

  /** 400. */
  public static final String RECON_MATCHING_RULES_REQUIRED = "RECON_MATCHING_RULES_REQUIRED";

  /** 400. */
  public static final String RECON_EXTERNAL_TXNS_REQUIRED = "RECON_EXTERNAL_TXNS_REQUIRED";

  /** 500. */
  public static final String RECON_START_FAILED = "RECON_START_FAILED";

  // META

  /** 404. */
  public static final String META_ENTITY_NOT_FOUND = "META_ENTITY_NOT_FOUND";

  /** 400. */
  public static final String META_UNSUPPORTED_ENTITY = "META_UNSUPPORTED_ENTITY";

  /** 400. */
  public static final String META_INVALID_ENTITY_ID = "META_INVALID_ENTITY_ID";

  // HOOK

  /** 404. */
  public static final String HOOK_NOT_FOUND = "HOOK_NOT_FOUND";

  /** 400. */
  public static final String HOOK_INVALID = "HOOK_INVALID";

  /** 500. */
  public static final String HOOK_OPERATION_FAILED = "HOOK_OPERATION_FAILED";

  // QUEUE

  /** 503, retry later. */
  public static final String QUEUE_BACKPRESSURE = "QUEUE_BACKPRESSURE";

  // SRCH

  /** 400. */
  public static final String SRCH_QUERY_INVALID = "SRCH_QUERY_INVALID";

  /** 500. */
  public static final String SRCH_FAILED = "SRCH_FAILED";

  /** 409. */
  public static final String SRCH_REINDEX_IN_PROGRESS = "SRCH_REINDEX_IN_PROGRESS";

  /** 404. */
  public static final String SRCH_REINDEX_NOT_STARTED = "SRCH_REINDEX_NOT_STARTED";

  // ADMIN

  /** 500. */
  public static final String ADMIN_BACKUP_FAILED = "ADMIN_BACKUP_FAILED";
}
