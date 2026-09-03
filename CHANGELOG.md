# Changelog

## 1.4.0 — Core 0.15.3

Aligns the Java SDK with [Blnk Core 0.15.3](https://docs.blnkfinance.com/changelog/blnk-core).

### Added

- Optional `dryRun(boolean)` (`dry_run`) on create, bulk create, refund, inflight
  update, bulk commit, and bulk void. Previews always return HTTP `200`; a
  projected rejection is `would_apply: false`, not an HTTP error.
- Typed dry-run response types: `DryRunTransactionResponse`,
  `DryRunRejection`, `DryRunBalanceProjection`, and
  `DryRunBulkTransactionResponse`.
- Optional `indicator(String)` on `CreateLedgerBalance` for creating internal
  balances with `ledgerId("general_ledger_id")`.
- Optional `description` and `metaData` on `RefundTransactionRequest`.
- `BlnkErrorCodes.TXN_INVALID_AMOUNT` and `BlnkErrorCodes.GEN_CONFLICT` for
  structured Core rejections.

### Confirmed

- `hooks().list()` with no type filter lists every hook (PRE and POST).
