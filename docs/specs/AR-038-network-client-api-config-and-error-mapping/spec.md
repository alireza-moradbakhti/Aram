# AR-038 · Network client, API config, and error mapping

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/41
- Phase / Sprint: Phase 1 · Online transactions core / Sprint 1
- Estimate: 5 pts · P0 · Size L
- Branch: `chore/AR-038-network-client-api-config-and-error-mapping`
- Status: In review — device pass passed

## Problem

The app is offline-only today. `docs/API_CONTRACT.md` defines the v1 REST
surface (auth, user, accounts, categories, transactions, budgets, goals,
contributions, sync) plus two environments, but no code can speak it:

- There is no `:core:network` module, no DTOs, and no shared HTTP client.
- There is no build-time API configuration: hosts are not in
  `gradle.properties`, and `BuildConfig` has no `API_ENV` / `API_MODE` /
  `API_BASE_URL` fields.
- AR-039 (mock API), AR-040 (auth + tokens), AR-041 (cache-first repos), and
  AR-042/43 (outbox + sync) all need one gate and one error model first;
  without it every task would invent its own client and its own failure kinds.
- `AppError` (AR-037) already mirrors the contract's error codes, but nothing
  maps HTTP outcomes onto it.

## Goals / Non-goals

**Goals**

- Create `:core:network` (Ktor 3.6.1 + kotlinx.serialization, plain Kotlin, no
  Hilt/Compose/Android imports) with DTOs for every contract resource.
- One gate (`safeApiCall`) and one error model (`ApiResult` / `ApiError`) that
  every future endpoint must use.
- Per-variant API configuration (environment, mode, base URL) resolved from
  `gradle.properties`, with configuration-time safety rails.
- Auth, idempotency, retry, headers, timeouts, and a sanitized logger.
- Wire the client in `:app` DI so alpha boots on a mock engine with zero real
  network traffic.

**Non-goals**

- Mock route implementations and fixtures (AR-039), token storage and the auth
  screens (AR-040), repository/outbox integration (AR-041/42), sync (AR-043).
- Any feature-module or UI change.
- Retrying writes inside the client; the outbox owns write retries.

## Acceptance criteria

Copied verbatim from the issue; tick as they land.

- [x] `:core:network` compiles without Android UI or Compose imports (KMP
      guardrail, see MASTER_PLAN K0) — enforced by a source-scan test.
- [x] Every contract resource has a DTO; fixture JSON parses in unit tests.
- [x] Every API call goes through `safeApiCall`; no raw `client.request` exists
      outside the gate — enforced by a source-scan test.
- [x] MockEngine tests prove each mapping: no network → `Offline`; timeout →
      `Timeout`; 400 → `Validation`; 401 → `Unauthorized`; 403 → `Forbidden`;
      404 → `NotFound`; 409 → `Conflict`; 429 → `RateLimited`; 5xx → `Server`;
      unparsable → `Unknown`.
- [x] A test asserts the logging interceptor output contains no token, email,
      amount, note, or body.
- [x] Auth interceptor refreshes exactly once on 401 and does not loop.
- [x] Alpha boots with `ARAM_API_MODE=mock` and makes zero real network calls
      (device pass in airplane mode: no sockets, no network log lines).
- [x] `alphaDebug` with `remote` mode resolves
      `https://test-myendpoint/api/v1/auth/login`; `beta*` and `prodDebug`
      resolve stage; `prodRelease` resolves production.
- [x] `./gradlew :app:assembleProdRelease -PapiMode=mock` fails with a clear
      message, and no host string is hardcoded in Kotlin.
- [x] Unit tests cover URL resolution for both environments and reject a
      non-HTTPS config outside the debug override.

## Technical design

### Module layout

`:core:network` — Android library container (like `:core:logging`, KMP
migration stays K1), `namespace com.aram.core.network`, sources under
`src/main/kotlin`. Plugins: `android-library`, `kotlin-serialization`.
Dependencies: `:core:model`, `:core:logging`, ktor-client-core,
ktor-client-content-negotiation, ktor-serialization-kotlinx-json,
ktor-client-mock, kotlinx-serialization-json, kotlinx-coroutines-core.
No Hilt, no Compose, no `android.*` imports.

```
api/         ApiResult, ApiError, ApiMeta, safeApiCall, ApiError-to-AppError
config/      ApiEnvironment, ApiMode, ApiConfig, EndpointProvider
dto/         one file per resource + envelope + sync payloads
http/        AramHttpClient factory, auth/idempotency/log interceptors,
             TokenProvider seam, sanitized NetworkLog
```

### Configuration

- `ApiEnvironment { STAGE, PRODUCTION }`; `isProduction` is the only production
  check. `ApiMode { MOCK, REMOTE }`.
- `ApiConfig(environment, mode, baseUrl, appVersion, allowCleartext)` validates on
  construction: HTTPS required unless `allowCleartext` (debug override only);
  `PRODUCTION` + non-prod host is rejected; blank host rejected.
- `EndpointProvider(config)` owns every path: `url("/auth/login")` →
  `"$baseUrl/api/v1/auth/login"`. No host or path prefix appears in Kotlin
  outside this file.
- Hosts live in `gradle.properties`:
  `aram.api.stageBaseUrl=https://test-myendpoint`,
  `aram.api.prodBaseUrl=https://myendpoint` (placeholders until DNS exists).

### Build configuration matrix

Set per variant with the AGP variant API (`variant.buildConfigFields`) so a
single flavor cannot drift from its build type.

| Variant | API_ENV | API_MODE (default) | API_BASE_URL |
| --- | --- | --- | --- |
| alphaDebug / alphaRelease | stage | mock | stage host |
| betaDebug / betaRelease | stage | remote | stage host |
| prodDebug | stage | remote | stage host |
| prodRelease | production | remote | prod host |

Overrides: `ARAM_API_MODE` / `-PapiMode` (`mock|remote`) for every variant;
`ARAM_API_BASE_URL` only for debug variants (sets `allowCleartext = true`).

Safety rails, checked during configuration for the variant being assembled
(task names are matched, so `assembleAlphaDebug` is never blocked by an
unrelated release variant):

- Any `Release` variant with `MOCK` fails:
  `Release builds cannot use the mock API; pass -PapiMode=remote.`
- `prodRelease` whose resolved host is not `prodBaseUrl` fails.
- `prodRelease` with a non-HTTPS host fails.
- No Kotlin source contains a host literal; hosts exist only in
  `gradle.properties` and generated `BuildConfig`.

Manifest: `INTERNET` (already present) and
`android:networkSecurityConfig="@xml/network_security_config"`; main config
forbids cleartext, a `debug` source-set overlay allows it for local servers.

### The gate

```kotlin
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T, val meta: ApiMeta? = null) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

suspend fun <T> safeApiCall(
    client: HttpClient,
    logger: Logger,
    method: HttpMethod,
    path: String,              // relative, e.g. "/accounts"
    requestId: String = UUID.randomUUID().toString(),
    retryPolicy: RetryPolicy = RetryPolicy.default,
    block: suspend () -> HttpResponse,
): ApiResult<T>
```

`safeApiCall` is the only place that reads responses:

1. attaches `X-Request-Id`, executes `block()`;
2. on `CancellationException` — rethrow (never swallow);
3. on timeout exceptions — `Timeout`; on `IOException` /
   `UnresolvedAddressException` (and no response) — `Offline`;
4. parses `{"data":…,"meta":…}` on success; on error status parses the error
   envelope, falling back to status-only kinds when the body is unparsable;
5. unexpected throwables → `Unknown`, logged once at error with the request id;
6. GET only: up to 2 attempts on IO/timeout/5xx with jittered backoff
   (200–600 ms); writes are never retried here.

`ApiError` (sealed): `Offline`, `Timeout`, `Unauthorized`, `Forbidden`,
`NotFound`, `Validation(fields)`, `Conflict(currentRow: JsonElement?)`,
`RateLimited(retryAfterSeconds: Long?)`, `Server(requestId: String?)`,
`Unknown(cause)`. `ApiError.toAppError()` maps 1:1 onto the AR-037 kinds so the
data layer can pass failures through untouched.

### Interceptors (order matters)

1. **Headers** — `X-Platform: android`, `X-App-Version`, per-call
   `X-Request-Id` (also returned by the server and logged).
2. **Auth** — attaches `Bearer` from `TokenProvider.accessToken()`; on a 401
   for a token-bearing request it refreshes once behind a `Mutex` and retries
   the original request with the new token, then surfaces `Unauthorized`. A
   per-request `authRetried` attribute prevents loops. Concurrent 401s share
   one refresh (generation counter: if the token changed while waiting, retry
   without refreshing).
3. **Idempotency** — `POST` / `PATCH` / `DELETE` get a UUID
   `Idempotency-Key` header; the header survives the auth retry so a replayed
   write reuses the same key.
4. **Logging** — emits `method path status durationMs requestId` only; query
   values, headers, bodies, tokens, emails, amounts, and notes are never
   logged. Ktor's default `Logging` plugin stays off.

Timeouts: connect 15 s, socket 20 s, request 30 s (`HttpTimeout`).

### DI wiring (`:app`, Hilt)

`NetworkModule` provides:

- `ApiConfig` from `BuildConfig.API_ENV`, `API_MODE`, `API_BASE_URL`,
  `VERSION_NAME`, and `allowCleartext = BuildConfig.DEBUG && base-url override`;
- `TokenProvider` — `TokenProvider.None` until AR-040 binds a real one;
- `HttpClient` — `MockEngine` placeholder (responds `501` to everything) when
  mode is `MOCK`; OkHttp engine otherwise. The engine choice happens here, not
  in `:core:network`, so the engine API stays out of shared code.

Alpha still boots fully offline: the mock engine never touches a socket, and
no feature calls the API yet.

## Data and migrations

None. No Room schema change, no DataStore keys.

## Test plan

Unit tests (`:core:network`, JVM):

| Area | Test |
| --- | --- |
| DTOs | every fixture JSON in the test class parses; unknown keys ignored; snake_case fields land correctly |
| Gate | MockEngine mapping matrix: IOException → `Offline`, timeout → `Timeout`, 400/422 → `Validation` (field map), 401/403/404 → kinds, 409 → `Conflict` with row, 429 → `RateLimited` (Retry-After), 5xx → `Server` (request id), broken JSON → `Unknown` |
| Retry | GET retried once on 5xx then fails; POST never retried |
| Cancellation | `CancellationException` rethrown, not mapped |
| Auth | 401 → one refresh → retry succeeds; second 401 → `Unauthorized`; two concurrent 401s → `refresh()` called once |
| Idempotency | writes carry a UUID key; GET does not; retried write keeps the same key |
| Logging | captured lines contain method/path/status/request id and none of the seeded token, email, amount, note, or body |
| Config | both environments resolve `/api/v1/...` URLs; non-HTTPS rejected unless `allowCleartext`; production rejects stage host; blank host rejected |
| Mapping | `ApiError` → `AppError` 1:1 |
| Guardrails | source scan: no `android.` / `androidx.compose` imports, and no direct `client.request`/`get`/`post` calls in main sources |

Gradle verification (recorded in `context.md`):

- `./gradlew :app:assembleAlphaDebug test -PbuildDate=…`
- `./gradlew :app:assembleProdRelease -PapiMode=mock` → fails with the mock
  message
- generated `BuildConfig` for `alphaDebug` (remote), `betaDebug`, `prodDebug`,
  `prodRelease` shows the expected `API_ENV` / `API_MODE` / `API_BASE_URL`;
  `grep -R "test-myendpoint\|myendpoint" --include=*.kt .` returns no main-source
  hits.

Manual/device (Galaxy A53 5G, Android 16):

1. Install alpha and launch → app works exactly as before (mock mode).
2. Airplane mode on → app still works; logcat shows no network stack errors.
3. Logcat filtered by `Network` shows no bodies, tokens, emails, or amounts —
   no API calls happen yet (this task adds no endpoints).
4. `assembleProdRelease -PapiMode=mock` on the workstation fails with the clear
   message.

## Risks and open questions

- **Serialization plugin under AGP built-in Kotlin.** AGP 9.5.1 resolves KGP
  2.4.21 in this project, so `org.jetbrains.kotlin.plugin.serialization` 2.4.21
  should apply; if the plugin refuses to register with built-in Kotlin, fall
  back to explicit `@Serializable` serializers via a `JsonNamingStrategy`
  without the compiler plugin is *not* possible — the fallback is pinning the
  plugin to AGP's bundled KGP version and, only if that fails, switching the
  module to the Android KMP library plugin. Confirm with a compile right after
  the gate is scaffolded.
- **Placeholder hosts** (`test-myendpoint`, `myendpoint`) do not resolve; remote
  mode is only meaningful once the backend publishes DNS. The configuration
  rails must not pretend otherwise.
- **Mock engine placeholder** returns `501` until AR-039; it exists only to
  prove alpha boots without sockets.
- **`alphaRelease` is now unbuildable by default** (mock + release is rejected
  by design). Local work and CI only build `alphaDebug`, so this is intentional;
  document it in the PR.
- **Ktor 3.6.1 / serialization 1.13.0** are the latest stable releases as of
  2026-11-16; both require Kotlin 2.2+, satisfied by 2.4.21.
