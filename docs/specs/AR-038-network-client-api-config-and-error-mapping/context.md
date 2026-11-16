# Context — AR-038 · Network client, API config, and error mapping

## Starting point

- Base branch: `dev` at `5c29e68` (AR-037 merged; `INTERNET` permission added
  on `dev` as prep for this plan).
- Branch: `chore/AR-038-network-client-api-config-and-error-mapping`.

## Relevant code

- `docs/API_CONTRACT.md` — the contract source of truth: envelopes, error
  codes, resources, sync payloads, environments, alpha mock rules.
- `docs/MASTER_PLAN.md:197-216` — Hilt stays in Android-only modules; shared
  modules (`model`, `money`, `common`, `logging`, `domain`, `network`) expose
  plain constructors and never import Hilt (K0/K1).
- `core/model/.../AppError.kt:10` — AR-037 failure kinds, already aligned with
  the contract codes (`Validation`, `Unauthorized`, `Forbidden`, `NotFound`,
  `Conflict`, `RateLimited`, `Offline`, `Timeout`, `Server`, `Storage`,
  `Permission`, `Unknown`).
- `core/common/.../error/ErrorMapping.kt:12` — existing `Throwable.toAppError()`
  and `safeOutcome(...)`; the network gate mirrors its cancellation rule and
  logging discipline.
- `core/logging/.../Logger.kt:15` — structured logger; `e(...)` takes a
  throwable. Network logging must reuse it, never `println`/`Log`.
- `@core/logging/src/main/kotlin` shows the `kotlin` source-dir convention;
  `core/common` shows the `java` convention. New module uses `kotlin/`
  (like logging).
- `app/src/main/java/com/aram/app/di/LoggingModule.kt:20` — Hilt provider
  pattern to follow for `NetworkModule`.
- `app/src/main/java/com/aram/app/AramApplication.kt:13` — injection style.
- `app/build.gradle.kts:78-103` — flavor matrix (`alpha`/`beta`/`prod`,
  `BUILD_STAGE`, `IS_PRE_RELEASE`); `:121-132` — per-variant output naming via
  `androidComponents.onVariants`, the place to add `API_*` fields.
- `gradle.properties:20-22` — `aram.versionName` / `aram.versionCode`; add
  `aram.api.stageBaseUrl` / `aram.api.prodBaseUrl` beside them.
- `gradle/libs.versions.toml` — no Ktor/serialization entries yet; Kotlin
  plugin versions live under `[plugins]` (`kotlin-compose` already pulls KGP
  2.4.21 into the build classpath).
- `build.gradle.kts:18-56` — `verifyNoNotNullAssertions` source-scan task; the
  same style is used for the network guardrail tests.
- `app/src/main/AndroidManifest.xml` — `INTERNET` already declared; add
  `android:networkSecurityConfig`.

## Conventions

- Money is integer minor units (`Long`); dates are epoch millis (`Long`) — DTOs
  follow the contract and never use floats or strings for time.
- No analytics; logging must never include amounts, notes, emails, tokens, or
  account numbers.
- Shared modules: no Hilt, no Compose, no `android.*`; plain constructors.
- Errors are typed (`Outcome`/`AppError`); `!!` is banned in `src/main`.
- Stateless screens; this task changes no UI.
- Tests: JUnit4 + `kotlinx-coroutines-test` (`StandardTestDispatcher`,
  `runTest`), Turbine available.

## Dependencies

Latest stable as of 2026-11-16 (Maven Central metadata):

| Artifact | Version |
| --- | --- |
| `io.ktor:ktor-client-core` | 3.6.1 |
| `io.ktor:ktor-client-okhttp` | 3.6.1 |
| `io.ktor:ktor-client-content-negotiation` | 3.6.1 |
| `io.ktor:ktor-serialization-kotlinx-json` | 3.6.1 |
| `io.ktor:ktor-client-mock` | 3.6.1 |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.13.0 |
| `org.jetbrains.kotlin.plugin.serialization` | 2.4.21 (matches KGP in this build) |

Existing: coroutines 1.11.0, JUnit 4.13.2.

## Decisions log

- **2026-11-16** — `:core:network` is an Android library container with pure
  Kotlin sources (same as `:core:logging`); KMP plugin migration stays K1 per
  MASTER_PLAN.
- **2026-11-16** — Engine selection lives in `:app` DI (`MockEngine` for
  `MOCK`, OkHttp for `REMOTE`); `:core:network` exposes a factory that takes an
  `HttpClientEngine`, keeping OkHttp/Android APIs out of shared code.
- **2026-11-16** — Alpha's mock engine is a placeholder that answers `501`; real
  routes/fixtures are AR-039. It exists so alpha boots with zero sockets.
- **2026-11-16** — `ApiError.toAppError()` lives in `:core:network` (it only
  needs `:core:model`) so the data layer can map without duplicating the table.
- **2026-11-16** — Retry is implemented in `safeApiCall` (GET only, 2 attempts)
  rather than Ktor's `HttpRequestRetry`, so writes provably never retry and the
  policy is unit-testable.
- **2026-11-16** — `-PapiMode=mock` with a release variant fails during
  configuration by matching the requested task names; `assembleAlphaDebug` and
  CI are unaffected.
- **2026-11-16** — Hosts stay placeholders in `gradle.properties` until the
  backend publishes DNS; no Kotlin file may contain a host literal.
- **2026-11-16** — The Myket Maven proxy (`https://maven.myket.ir/`) is the
  last repository in both `pluginManagement` and
  `dependencyResolutionManagement` in `settings.gradle.kts`. This machine
  cannot reach Google Maven (every path returns 404), so artifacts that are
  not already cached (e.g. OkHttp 5.5.1's `startup-runtime:1.2.0`) come from
  the mirror. Official repos stay first, so CI and machines with working
  Google Maven never depend on the proxy; Gradle falls through on the 404s.
  The interim `startup-runtime:1.1.1` strict pin in `app/build.gradle.kts` was
  removed once the mirror proved the real 1.2.0 resolves.
- **2026-11-16** — `allowCleartext` is computed at build time into
  `BuildConfig.API_ALLOW_CLEARTEXT` (debug host override only) instead of being
  re-derived in Kotlin.
- **2026-11-16** — Request ids and idempotency keys use `java.util.UUID`
  (contract requires UUID v4 strings); K1 adds an `expect/actual` when this
  module moves to `commonMain`.
- **2026-11-16** — `safeApiCall` takes the method, an absolute URL, and a
  `configure` hook instead of a caller-supplied response block, so both the GET
  retry and the auth retry re-execute the same request deterministically.
- **2026-11-16** — Timeout mapping is exercised through the real `HttpTimeout`
  plugin with `runTest` virtual time, not by throwing the exception by hand.

## Verification log

### 2026-11-16 — build, tests, config matrix

- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261116` — BUILD
  SUCCESSFUL; **87 unit tests, 0 failures / 0 errors** (45 in `:core:network`).
  APK: `app/build/outputs/apk/alpha/debug/aram-alpha-debug-1-20261116.apk`
  (16.9 MB).
- `:core:network` suite: DTO fixtures for every resource + sync/push envelopes;
  gate matrix (offline, timeout, 400/422, 401, 403, 404, 409 + canonical row,
  429 + Retry-After, 5xx + request id, broken JSON); GET retried once on 5xx
  and on a dropped connection, writes never retried; cancellation rethrown;
  concurrent 401s share one refresh; idempotency key stable across the auth
  retry and absent on GET; log lines contain no payload; config validation and
  URL resolution; `ApiError → AppError` 1:1; source guardrails (no
  android/androidx/dagger/javax.inject imports, raw `client.request` only in
  `SafeApiCall.kt`, no host literals).
- `./gradlew :app:assembleProdRelease -PapiMode=mock` — fails during
  configuration with
  `Release builds cannot use the mock API; pass -PapiMode=remote.`
- Generated `BuildConfig` (verified): `alphaDebug -PapiMode=remote` →
  STAGE / REMOTE / stage host · `betaDebug` → STAGE / REMOTE / stage ·
  `prodDebug` → STAGE / REMOTE / stage · `prodRelease` → PRODUCTION / REMOTE /
  production host · `ARAM_API_BASE_URL=http://10.0.2.2:8080` on alphaDebug →
  base URL override with `API_ALLOW_CLEARTEXT = true`.
- Merged `alphaDebug` manifest carries `INTERNET` and
  `networkSecurityConfig="@xml/network_security_config"`; the debug overlay
  permits cleartext, the main config forbids it.
- Serialization plugin compiled under AGP's built-in Kotlin (KGP 2.4.21) on the
  first scaffold compile — the documented risk did not materialize.

### 2026-11-16 — Myket mirror, pin removal

- `https://maven.myket.ir/` added to `settings.gradle.kts` as the last
  repository in both blocks; `startup-runtime:1.1.1` strict pin removed from
  `app/build.gradle.kts`.
- `:app:dependencies --configuration alphaDebugRuntimeClasspath` →
  `androidx.startup:startup-runtime:1.1.1 -> 1.2.0`.
- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261116` → BUILD
  SUCCESSFUL; 87 tests, 0 failures / 0 errors (20 result files). APK
  `aram-alpha-debug-1-20261116.apk` is 21.9 MB after the repository refresh
  (the earlier 16.9 MB measurement predates it; debug dex, no functional
  change).
- The mirror is listed last on purpose: stale Gradle metadata from the failed
  Google-Maven attempt keeps the module pinned to `dl.google.com`, so the
  first build on this machine needs `--refresh-dependencies` once; plain
  builds are green afterwards.

### Device pass — passed

Galaxy A53 5G (SM-A536E, Android 16), USB adb.

- Installed `aram-alpha-debug-1-20261116.apk` (`:app:installAlphaDebug`, build
  date 20261116) — success, twice (initial build and after the repository
  refresh).
- Cold launch in MOCK mode: `am start -W` finished in 1331 ms; Welcome screen
  renders as before (Safe-to-Spend card, name field, Get started).
- Airplane mode on (`settings global airplane_mode_on=1`): app relaunches and
  renders identically (screenshot taken), status bar shows the airplane icon.
- App-PID logcat: no `FATAL`, no `ANR`, zero lines matching
  `http|network|ktor|okhttp|cleartext|SocketTimeout|UnknownHost|ConnectException`.
- `netstat -tnp` filtered by the app PID: no sockets — the mock engine never
  opens one, and no endpoint code exists yet.
- Airplane mode restored to off afterwards.
