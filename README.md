# Blnk Java SDK

Official-style Java client for the [Blnk Finance](https://docs.blnkfinance.com) open-source
ledger. It covers the full Core API: ledgers, balances, transactions (including bulk,
inflight, and refunds), identities and tokenization, balance monitors, reconciliation,
search, metadata, hooks, API keys, and system health.

## Requirements

- JDK 17+
- Maven 3.8+

## Installation

Available on [Maven Central](https://central.sonatype.com/artifact/com.blnkfinance/blnk-java):

```xml
<dependency>
  <groupId>com.blnkfinance</groupId>
  <artifactId>blnk-java</artifactId>
  <version>1.3.0</version>
</dependency>
```

Gradle:

```kotlin
implementation("com.blnkfinance:blnk-java:1.3.0")
```

The only runtime dependency is Jackson Databind; HTTP uses the JDK's built-in
`java.net.http.HttpClient`.

To build from source:

```sh
mvn install
```

## Quickstart

```java
import com.blnkfinance.blnk.Blnk;
import com.blnkfinance.blnk.BlnkClientOptions;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.CreateLedger;
import com.fasterxml.jackson.databind.JsonNode;

Blnk blnk = Blnk.init("<secret_key_if_set>",
    BlnkClientOptions.builder()
        .baseUrl("http://localhost:5001")   // trailing "/" appended automatically
        .build());

ApiResponse<JsonNode> newLedger = blnk.ledgers().create(
    CreateLedger.create()
        .name("Customer Savings Account")
        .metaData(java.util.Map.of("project_owner", "YOUR_APP_NAME")));

System.out.println("status:  " + newLedger.status());   // 201
System.out.println("message: " + newLedger.message());  // "Success"
System.out.println("ledger:  " + newLedger.data());     // parsed JSON body
```

A follow-up flow — create a balance and move money into it:

```java
import com.blnkfinance.blnk.types.CreateLedgerBalance;
import com.blnkfinance.blnk.types.CreateTransactions;

String ledgerId = newLedger.data().get("ledger_id").asText();

ApiResponse<JsonNode> balance = blnk.ledgerBalances().create(
    CreateLedgerBalance.create()
        .ledgerId(ledgerId)
        .currency("USD"));

ApiResponse<JsonNode> deposit = blnk.transactions().create(
    CreateTransactions.create()
        .amount(750)
        .precision(100)
        .currency("USD")
        .reference("ref_001adcfgf")
        .description("First deposit")
        .source("@WorldUSD")
        .destination(balance.data().get("balance_id").asText())
        .allowOverdraft(true));
```

## Authentication

Pass your Blnk secret key as the first argument to `Blnk.init`. When set, every request
carries it in the `X-Blnk-Key` header; pass an empty string for unsecured self-hosted
instances.

## Services

Services are created lazily and cached per client instance:

`ledgers()`, `ledgerBalances()`, `transactions()`, `balanceMonitor()`,
`reconciliation()`, `search()`, `identity()`, `system()`, `metadata()`, `hooks()`,
`apiKeys()`.

Request bodies are built with fluent builder types in `com.blnkfinance.blnk.types`;
fields you don't set are omitted from the JSON entirely. Date-typed fields accept
`Instant`, `Date`, or preformatted strings and are sent as UTC ISO-8601 timestamps
without fractional seconds (`2026-12-31T23:59:59Z`), the format Blnk Core expects.

## Configuration

| Option | Default | Notes |
|---|---|---|
| `baseUrl` | required | `IllegalArgumentException` if missing; `/` appended if absent |
| `timeout` | `10000` ms | per attempt; a timeout produces a synthetic `408` response and is never retried |
| `retryCount` | `1` | TOTAL attempts including the first; retries apply to `GET` requests only |
| `retryDelayMs` | `2000` | linear backoff: `delay × attemptNumber` |
| `logger` | console | any `BlnkLogger`; sensitive keys (API keys, tokens, cookies) are redacted from log metadata |

## Error handling

SDK methods **never throw** for request or validation failures — they return an
`ApiResponse<T>` value you can branch on:

- `status()` — the HTTP status; `400` for client-side validation failures, `408` for
  timeouts, `500` for transport errors.
- `message()` — `"Success"`, a specific validation message, or the error text. When
  Blnk Core returns a structured `error_detail` body, its message is used.
- `data()` — the parsed JSON body (`null` on failure or empty body).
- `error()` — a structured `BlnkApiErrorDetail {code, message, details}` when the Core
  returned a JSON error body.

Client-side validation runs before any request is sent: an invalid payload returns a
`400` response immediately and the HTTP layer is never invoked. The only throwing paths
are programmer errors: constructing a client without a `baseUrl`
(`IllegalArgumentException`) and requesting an unregistered service
(`IllegalStateException`).

## Tests

```sh
mvn test                 # 489 tests: 444 offline unit tests, 45 live-gated
BLNK_E2E=1 mvn test      # also runs integration + e2e against http://localhost:5001
```

Unit tests inject a mock transport and run fully offline. The live suites need a running
Blnk Core (`docker compose up` in the [blnk](https://github.com/blnkfinance/blnk) repo).

## Project layout

- `src/main/java/com/blnkfinance/blnk/` — client (`Blnk`, `BlnkClientOptions`),
  `endpoints/` (one class per service), `types/` (builder-pattern request DTOs and
  response records), `validators/` (client-side payload validation), `util/` (retry
  policy, log redaction, JSON and multipart helpers).
- `src/test/java/` — unit suites per endpoint/validator, `testsupport/` fixtures and
  mocks, `integration/` and `e2e/` live suites (environment-gated).
