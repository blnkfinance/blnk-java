# Changelog

## 1.5.0 — Core 0.15.4

Aligns the Java SDK error catalogue with [Blnk Core 0.15.4](https://docs.blnkfinance.com/changelog/blnk-core).

### Added

- `BlnkErrorCodes` now mirrors Core's full `error_detail.code` catalogue
  (`internal/apierror/codes.go`), grouped by prefix: `GEN_`, `AUTH_`, `APIKEY_`,
  `TXN_`, `BAL_`, `LGR_`, `ACC_`, `IDT_`, `RECON_`, `META_`, `HOOK_`, `QUEUE_`,
  `SRCH_`, and `ADMIN_`. Each constant documents the HTTP status Core pairs it with.
- Codes introduced or re-routed in Core 0.15.4 that callers should branch on:
  - `TXN_ALREADY_REFUNDED` (`409`): refunding a transaction twice, or refunding a
    refund. Previously the reversal went through.
  - `BAL_NOT_FOUND` (`404`): a transaction naming a missing balance. Previously
    reported as `TXN_NOT_FOUND`.
  - `TXN_VALIDATION_ERROR` (`400`) is now also returned for a split request that
    carries both `sources` and `destinations`.
  - `GEN_CONFLICT` (`409`) is now also returned when a multi-leg refund fails.
- `search().multiSearch(MultiSearchParams)`, wrapping Core's `POST /multi-search`
  (`api/api.go`: `router.POST("/multi-search", a.MultiSearch)`). That route has
  been in Core since v0.10.0 (`b396752`); this SDK release is aligned with Core
  0.15.4. Core binds the body to Typesense's `MultiSearchSearchesParameter` and
  forwards it unchanged, so the wire shape is
  `{"searches": [{"collection": ..., "q": ..., ...}]}`. The response has
  `results` in the same order as `searches`. Each entry is validated client-side
  with the same rules as `search()`, and failures name the entry
  (`searches[1].collection ...`).
- `ledgers().list()` and `ledgerBalances().list()`, wrapping Core's `GET /ledgers`
  and `GET /balances` (in Core since ~0.14.x; this SDK release is aligned with
  Core 0.15.4). Both take an optional `ListOptions` with `limit` (at least `1`)
  and `offset` (at least `0`), sent as query parameters; unset fields fall back
  to Core's defaults of `10` and `0`. Invalid pagination is rejected client-side
  with a `400` before any request is made (`limit` at least `1`, `offset` at
  least `0`, integers only). Those bounds match behaviour observed against Core
  0.15.4; the published OpenAPI spec does not currently declare these query
  parameters. Live tests (gated on `BLNK_E2E=1`) create two rows and assert
  `limit=1&offset=1` returns a different id than `limit=1&offset=0`, so
  pagination is not a no-op against that Core. `ListOptions` only has `int`
  `limit`/`offset` setters (unknown builder keys are N/A); map-shaped payloads
  still reject extra query keys rather than dropping them.
- `transactions().list()`, wrapping Core's `GET /transactions` (in Core since
  ~0.14.x; this SDK release is aligned with Core 0.15.4), with the same
  optional `ListOptions`. Core's default page here is `limit=20`. Core's
  `GetAllTransactions` handler silently falls back to its defaults on invalid
  pagination; the SDK rejects it with a `400` instead so mistakes are visible.
- `balanceMonitor().listByBalanceId(balanceId)`, wrapping Core's
  `GET /balance-monitors/balances/:balance_id`
  (`api/api.go`: `router.GET("/balance-monitors/balances/:balance_id",
  a.GetBalanceMonitorsByBalanceID)`). That route has been in Core since ~0.14.x;
  this SDK release is aligned with Core 0.15.4. The balance id is
  percent-encoded. Existing `balanceMonitor().list()` (all monitors) is
  unchanged. This method takes no query map, so unknown list keys are N/A.

### Unchanged

- The three existing constants (`TXN_INVALID_AMOUNT`, `GEN_CONFLICT`,
  `TXN_VALIDATION_ERROR`) keep their values; no caller changes are needed.

## 1.4.0 — Core 0.15.3

Aligns the Java SDK with [Blnk Core 0.15.3](https://docs.blnkfinance.com/changelog/blnk-core).

### Added

- Optional `dryRun(boolean)` (`dry_run`) on create, bulk create, refund, inflight
  update, bulk commit, and bulk void. Previews always return HTTP `200`; a
  projected rejection is `would_apply: false`, not an HTTP error.
- Typed dry-run response types: `DryRunTransactionResponse`,
  `DryRunRejection`, `DryRunBalanceProjection`, `DryRunLegProjection`, and
  `DryRunBulkTransactionResponse`.
- `notes()` on both dry-run wrappers, exposing Core's advisory messages such as
  a currency mismatch or legs being projected independently. A preview can carry
  notes while `would_apply` is still `true`, so read them before acting on a
  projection.
- `legs()` returns typed `DryRunLegProjection` entries (keyed by `identifier`,
  since a leg may name an internal `@` balance that does not exist yet).
- Optional `indicator(String)` on `CreateLedgerBalance` for creating internal
  balances with `ledgerId("general_ledger_id")`.
- Optional `description` and `metaData` on `RefundTransactionRequest`.
- `BlnkErrorCodes.TXN_INVALID_AMOUNT` and `BlnkErrorCodes.GEN_CONFLICT` for
  structured Core rejections.

### Confirmed

- `hooks().list()` with no type filter lists every hook (PRE and POST).
- Maven coordinates. `com.blnkfinance:blnk-java` is already the published
  coordinate as of `1.3.0` (Central, 2026-07-21), so this release continues it
  rather than introducing it. The older `com.blnkfinance:blnk-sdk:1.3.0` remains
  permanently resolvable for existing builds; migrate by changing only the
  `artifactId` to `blnk-java`, as the package names are unchanged.
