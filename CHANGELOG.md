# Changelog

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
