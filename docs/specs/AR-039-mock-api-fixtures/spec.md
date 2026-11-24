# AR-039 · Mock API and auth fixtures for alpha

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/42
- Phase / Sprint: Phase 1 · Online transactions core / Sprint 1
- Estimate: 3 pts · P0
- Branch: `feat/AR-039-mock-api-fixtures`
- Status: Review (device check batched)

## Problem

`docs/API_CONTRACT.md` (and `:core:network`) describe a server that does not
exist: `/api/v1`, snake_case, epoch-millis dates, rotating refresh tokens,
`{data, meta}` / `{error}` envelopes, `Idempotency-Key`, and soft-delete
tombstones. The real backend is `Aram-Api` (.NET 10, FastEndpoints). Every
feature built against the old contract would need rework, and no mock exists, so
alpha has nothing to talk to.

## Goals

- Rewrite the client-facing contract to the real backend: `/api` prefix,
  camelCase, ISO-8601 UTC strings, `Result<T>` envelope, FluentValidation error
  body, 24 h JWT with no refresh, IRR ledger amounts with entered currency.
- Align `:core:network` DTOs, paths, JSON, error mapping, and auth header with
  that contract. No refresh flow: a 401 clears the session.
- Ship a Ktor `MockEngine` that implements every route in the contract with
  seeded fixtures and switchable failure scenarios, alpha only.
- Keep the network module Android-free and host-free (existing guardrails).

## Non-goals

- Wiring auth/cache/sync into features (AR-040 … AR-043).
- Mock DTOs beyond the transaction entity for sync push (backend pushes
  transactions only).
- Bank integrations, multi-currency conversion beyond the static IRR catalog.

## Acceptance criteria

- [x] Alpha can register and sign in against the mock (register → 201 token
      + profile; login → 200 or 401; `GET /api/auth/me` → profile).
- [x] List/add/edit/delete works for transactions, budgets, goals and goal
      contributions; categories, accounts, dashboard, settings, notifications
      and profile endpoints answer with the contract shapes.
- [x] Pull (`GET /api/sync/pull?since=`) and push (`POST /api/sync/push`
      `{items}`) work end-to-end against in-memory state.
- [x] Failure scenarios (offline, server error, unauthorized, slow) are
      selectable at runtime, alpha/debug only.
- [x] Mock data never ships in beta/prod: `ARAM_API_MODE=remote` is the default
      outside alpha and release builds reject `mock`.
- [x] No real network path is used while the mock is active (one Hilt binding).
- [x] `./gradlew :app:assembleAlphaDebug test` passes.

## Technical design

### Wire contract (source of truth: `Aram-Api/src/Aram.Api/Contracts/AramContracts.cs`)

- Base URL is the host only; [EndpointProvider] adds `/api`.
- JSON: camelCase, unknown keys ignored, nulls omitted on encode.
- Dates: ISO-8601 UTC strings (`2026-11-21T09:30:00Z`); clients parse with
  `Instant`.
- Money: decimal IRR (`amountIrr`), plus `enteredCurrency` / `enteredAmount` /
  `fxRate` when the user typed another currency. No minor units on the wire.
- Months are UTC; `year` / `month` query params default to the current UTC
  month.
- Success body: `{ "isSuccess": true, "message": "", "errors": [], "data": … }`.
  Validation failure (400) is FastEndpoints' shape:
  `{ "statusCode": 400, "message": "…", "errors": { "field": ["msg"] } }`.
- Auth: `Authorization: Bearer <jwt>`; 24 h expiry; no refresh or logout
  endpoint. 401 → clear session.
- Endpoints: `/auth/{register,login,forgot-password,reset-password,
  change-password,me,profile,account}`, `/profile/{stats,accounts,export}`,
  `/categories`, `/transactions[/summary|/insights/weekly|/{id}]`,
  `/budgets[/overview|/categories|/{id}]`, `/goals[/{id}|/{id}/contributions]`,
  `/notifications[...]`, `/dashboard`, `/settings[/currencies|/locales]`,
  `/rates`, `/settings/convert`, `/sync/{push,pull}`, `/health`.

### Module layout

- `dto/` — one file per resource mirroring the C# contracts.
- `api/SafeApiCall` — parses `Result<T>` and bare bodies; maps HTTP status and
  both error shapes into `ApiError`.
- `http/AramClientPlugin` — headers and bearer token; `TokenProvider` now only
  supplies a token and reports unauthorized.
- `mock/` — `MockApiEngine` (routing, latency, scenarios), `MockApiStore`
  (seeded in-memory ledger, pure and unit-testable), `MockScenario`.

### Mock scenarios

`Normal`, `Offline` (throws IOException), `ServerError` (500 envelope),
`Unauthorized` (401 on the next request), `Slow` (3 s). Lives behind
`MockScenarioController`; the alpha build can drive it from the developer
section while debug builds can also switch it. The mock is only constructed when
`ApiMode.MOCK`, and that mode is only reachable in alpha debug.

### Fixtures

Demo user `demo@aram.app` / `demo1234`, three accounts (Cash default, Card,
Auto-pay), the ten backend categories (expense + salary), ~30 transactions over
three UTC months, two budgets, two goals, one unread notification.

### Runtime wiring

`NetworkModule` binds one `HttpClient`: the mock engine when
`ApiMode.MOCK`, OkHttp otherwise. `MockScenarioController` and `MockApiStore`
are singletons. Debug alpha builds show Profile → Developer → *Network
scenario* (chips bound to the controller) and the welcome screen shows the demo
account; `demoCredentials`/`debugScenario` are null outside alpha, so nothing
leaks into beta or prod.

## Data and migrations

No Room changes in this task. DTOs are new; nothing persisted yet.

## Test plan

- Unit: fixture parsing for every DTO family; envelope + validation error
  parsing; path builder; token header; mock store CRUD/sync; mock engine
  end-to-end (register → login → me → transactions → sync) and scenario
  failures.
- Build: `:app:assembleAlphaDebug test`.
- Device: deferred to the batched run agreed with the user.

## Risks and open questions

- The backend has no refresh tokens; the contract's “refresh” wording is
  replaced by re-login. If the backend later adds refresh, that is an additive
  change.
- Mock amounts are IRR decimals; features still display minor units, so the
  mapping layer (AR-040+) must convert.
- FastEndpoints global 401 body may be empty; the client must not depend on it.
