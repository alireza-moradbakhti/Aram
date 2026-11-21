# Context — AR-039 · Mock API and auth fixtures for alpha

## Starting point

- Base branch: `dev` at the commit current on 2026-11-21.
- Branch: `feat/AR-039-mock-api-fixtures`.
- `:core:network` is wired into the app through `NetworkModule` only; no feature
  consumes it yet, so the contract rewrite has no downstream blast radius.

## Relevant code

- `core/network/src/main/kotlin/com/aram/core/network/`
  - `AramJson` (snake_case, must become camelCase)
  - `config/EndpointProvider` (`/api/v1`, must become `/api` plus the real path
    set)
  - `dto/*` (fictional resources, replaced)
  - `api/SafeApiCall` (parses `{data, meta}` / `{error}`; replaced with
    `Result<T>` + validation body)
  - `http/AramClientPlugin` + `TokenProvider` (refresh on 401; removed)
- `app/src/main/java/com/aram/app/di/NetworkModule` — placeholder MockEngine,
  replaced by `MockApiEngine`.
- `app/build.gradle.kts` — API matrix already forces alpha=mock and forbids
  mock in release; reused unchanged.
- Backend source of truth: `C:\Users\AshenOne\Desktop\Project\Root\Aram-Api`
  (`Contracts/AramContracts.cs`, `Features/*`.

## Conventions

- `:core:network` stays Android-free; no host literals in Kotlin.
- No payloads, amounts, notes, emails or tokens in logs.
- DTO timestamps stay ISO strings; formatting/parsing helpers are pure Kotlin.
- Mock code lives in `main` but is only instantiated for `ApiMode.MOCK`.

## Dependencies

- None inside the sprint; AR-040 auth, AR-041 cache-first, AR-042 outbox and
  AR-043 pull rely on these DTOs.

## Decisions log

1. Replace the old contract outright instead of writing an adapter: no feature
   consumes the network layer yet, and an adapter would freeze the wrong shapes.
2. Drop `Idempotency-Key`, `ApiMeta`, refresh tokens and tombstones — the
   backend does not implement them.
3. `TokenProvider` exposes `accessToken()` and `onUnauthorized()`; the “refresh”
   step is a re-login, owned by AR-040.
4. Amounts stay `Double` in DTOs (backend decimals) and are converted to the
   model at the boundary.
5. Mock latency is randomized 300–800 ms, overridden to 0 in tests.
6. ID routes (`/{id}`, `/{id}/read`, `/{id}/contributions`) match `segments.size`
   2/3 — the first router draft assumed a leading empty segment and 404'd.
7. Demo credentials live in `MockDemoAccount` (top-level object) because the
   store's companion is private.
8. Scenario switching is exposed through strings (`DebugScenarioOption`) so
   `:feature:settings` does not depend on `:core:network`.

## Verification log

- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261121` — BUILD SUCCESSFUL
  (509 tasks). `:core:network` unit tests: 64 pass, 0 fail (9 suites).
- Device smoke deferred to the batched run (AR-039 … AR-043) agreed with the
  user.
