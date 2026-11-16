# Aram — master build plan

Everything left to build, from the current skeleton to a shipped MVP, plus the
Kotlin Multiplatform migration that comes after it. The design source is
[SPEC.md](SPEC.md), the client/server handoff is
[API_CONTRACT.md](API_CONTRACT.md), sprint narratives live in
[ROADMAP.md](ROADMAP.md), and the live status of every task is on the
[project board](https://github.com/users/alireza-moradbakhti/projects/6).

## Task codes

Every issue has a stable key: `AR-001` … `AR-044`, assigned in delivery order and
never reused. The GitHub issue number stays as the URL/id, but all planning
artifacts, branches, commits, and comments use the key.

- Branch: `feat/AR-005-transactions-list`
- Commit: `feat(transactions): group list by month (AR-005)`
- Board card title: `AR-005 · Transactions list with month headers and day groups`

`AR-001 … AR-036` map 1:1 to issues `#2 … #37`; `AR-037` (issue `#38`) is the
safety baseline; `AR-038 … AR-044` (issues `#41 … #47`) are the backend-era tasks
added when the app became online. New work starts at `AR-045`.

## Online model (decided)

The app now talks to a real backend owned by a teammate. The server is the
source of truth, Room is the on-device cache, and every write survives a dead
network.

| Concern | Decision |
| --- | --- |
| Reads | Cache-first: UI observes Room. Refresh pulls `/sync` and upserts into Room, so screens update without a spinner. |
| Writes | Optimistic local write + `sync_outbox` row; a WorkManager worker pushes when online. Failed pushes retry with backoff; permanent 4xx errors mark the row `FAILED` and surface a retry action. |
| Offline | Cached data stays readable, with an offline/stale banner and last-synced time. Writes are queued, not rejected. |
| Conflict | Last-write-wins per row on `updated_at`; tombstones travel through sync. |
| Auth | Email + password, JWT access (15 min) + rotating refresh token. Alpha mocks all auth and data routes by default; `beta` uses the stage API; `prodRelease` uses production. |
| Environments | Every variant resolves the stage host except `prodRelease`, which resolves production. Both run the same contract; stage data is disposable. `ApiEnvironment` + `EndpointProvider` live in `:core:network`; hosts come from `gradle.properties`, never from Kotlin. |
| Transport | `:core:network` with Ktor + kotlinx.serialization. `ARAM_API_MODE=mock\|remote` selects the engine in DI — features never branch on it. |
| Contract | [API_CONTRACT.md](API_CONTRACT.md) is the source of truth for endpoints, envelopes, ids, money, and sync. |

## Where we are

| Phase | Scope | State |
| --- | --- | --- |
| Phase 0 · Skeleton | multi-module Gradle, theme, splash, welcome, nav shell, Room v1, local session, CI, variants, signing config | ✅ done |
| Phase 1 · Online transactions core | AR-037 (safety), AR-038 … AR-044 (network, mock, auth, cache, sync, tests), AR-001 … AR-011, AR-015 | next |
| Phase 2 · Dashboard & insight | AR-012 … AR-018 | planned |
| Phase 3 · Budgets & goals | AR-019 … AR-029 | planned |
| Phase 4 · Polish & release | AR-030, AR-031, AR-034 … AR-036 | planned, smaller |
| Post-MVP · Backlog | biometric lock, widget, and the items in §Post-MVP | parked |
| Post-MVP · KMP | shared core, iOS/desktop shell, shared network client | planned, see §KMP |

## Delivery model

- **Sprints:** six two-week sprints, Nov 16 2026 → Feb 7 2027. Backend
  integration adds ~26 net points, so the timeline grew by one sprint; biometric
  lock and the home-screen widget moved to the post-MVP backlog instead of
  stretching every sprint.

| Sprint | Dates | Capacity | Focus |
| --- | --- | --- | --- |
| S1 | Nov 16 – Nov 29 | 31 | safety, network + mock, transactions foundation |
| S2 | Nov 30 – Dec 13 | 29 | transactions end-to-end, online auth, cache-first repos |
| S3 | Dec 14 – Dec 27 | 21 | dashboard data, outbox, pull sync (holiday-degraded) |
| S4 | Dec 28 – Jan 10 | 23 | dashboard polish, charts, sync tests, budgets start |
| S5 | Jan 11 – Jan 24 | 21 | budgets and goals |
| S6 | Jan 25 – Feb 7 | 22 | goals polish, settings, recurring, CSV, release |

- **Estimation:** Fibonacci — XS 1, S 2, M 3, L 5, XL 8.
- **Alpha gate:** every change ships as `alphaDebug`, built by CI, tested on a
  real device. Alpha uses the mock API; nothing else is built in CI.
- **Release gate:** `beta`/`prod` builds are cut only from a green, device-tested
  `dev`, against the real API, and only when a release is being prepared.
- **Flow:** Todo → In Progress → Review → Done, moved by `/ship`.
- **Definition of done:** acceptance criteria met · unit tests for changed logic ·
  `./gradlew :app:assembleAlphaDebug test` green · CI green · real-device pass
  (including airplane mode for cache/write behavior) · issue comment with
  evidence · docs/spec updated.

## Phase 1 — Online transactions core (S1–S4)

Exit criteria: sign in (mock in alpha), record income/expense/transfer, see them
grouped and balanced, read cached data offline, queue writes offline and sync
them on reconnect — and no screen can crash, hang, or lose a write.

| Key | Task | Pts | Priority | Sprint | Depends on |
| --- | --- | --- | --- | --- | --- |
| AR-037 | Safe error handling across screens and layers | 5 | P0 | S1 (first) | — |
| AR-038 | Network client, API config, and error mapping | 5 | P0 | S1 | AR-037 |
| AR-039 | Mock API and auth fixtures for alpha | 3 | P0 | S1 | AR-038 |
| AR-001 | Transaction domain contract and use cases | 3 | P0 | S1 | AR-037 |
| AR-002 | Room-backed transaction repository (cache layer) | 3 | P0 | S1 | AR-001 |
| AR-003 | Locale-aware money input component | 2 | P1 | S1 | — |
| AR-004 | Add-transaction screen | 5 | P0 | S1 | AR-002, AR-003 |
| AR-005 | Transactions list with month headers and day groups | 5 | P0 | S1 | AR-002 |
| AR-006 | Edit and delete transactions with undo | 3 | P0 | S2 | AR-005 |
| AR-007 | Derived account balances | 3 | P0 | S2 | AR-002 |
| AR-008 | Transaction routes and FAB navigation | 2 | P1 | S2 | AR-004, AR-005 |
| AR-009 | Atomic transfers between accounts | 3 | P0 | S2 | AR-002, AR-007 |
| AR-010 | Search and filter transactions | 5 | P1 | S2 | AR-005 |
| AR-040 | Online auth flow and secure token session | 5 | P0 | S2 | AR-038, AR-039 |
| AR-041 | Cache-first repositories with pull-to-refresh | 5 | P0 | S2 | AR-038, AR-002 |
| AR-011 | Repository and ViewModel tests with Turbine | 3 | P1 | S2 | AR-002 |
| AR-042 | Offline write outbox and sync worker | 8 | P0 | S3 | AR-002, AR-041 |
| AR-043 | Pull sync, conflicts, and tombstones | 5 | P0 | S3 | AR-038, AR-041 |
| AR-044 | Network, cache, and sync test suite | 3 | P1 | S4 | AR-040 … AR-043 |
| AR-015 | Room instrumented tests for transaction queries | 2 | P1 | S4 | AR-002 |

## Phase 2 — Dashboard & insight (S3–S4)

Exit criteria: a trustworthy safe-to-spend number, month in/out, and charts that
tell the month's story — from cached data, refreshed by sync.

| Key | Task | Pts | Priority | Sprint | Depends on |
| --- | --- | --- | --- | --- | --- |
| AR-012 | Safe-to-spend calculator with a testable breakdown | 3 | P0 | S3 | AR-007 |
| AR-013 | Dashboard shell with safe-to-spend and month in/out | 5 | P0 | S3 | AR-012 |
| AR-014 | Safe-to-spend explainer sheet | 2 | P1 | S4 | AR-013 |
| AR-016 | Category donut chart on Canvas | 3 | P1 | S4 | AR-013 |
| AR-017 | Six-month trend chart on Canvas | 3 | P1 | S4 | AR-013 |
| AR-018 | Recent transactions on the dashboard | 2 | P1 | S4 | AR-005, AR-013 |

## Phase 3 — Budgets & goals (S4–S6)

Exit criteria: budgets track real spend with 80/100% alerts; goals have progress,
streaks, and projection; everything reads from cache and syncs.

| Key | Task | Pts | Priority | Sprint | Depends on |
| --- | --- | --- | --- | --- | --- |
| AR-019 | Budgets repository and monthly progress | 3 | P0 | S4 | AR-002 |
| AR-020 | Budgets screen with month selector | 5 | P0 | S4 | AR-019 |
| AR-021 | Budget editor with 80% and 100% alert states | 3 | P0 | S5 | AR-019 |
| AR-022 | Shared progress and amount components | 2 | P1 | S5 | — |
| AR-023 | Savings goals and contributions (domain + data) | 3 | P0 | S5 | AR-002 |
| AR-024 | Goals list with progress arcs and streak badges | 5 | P0 | S5 | AR-023, AR-022 |
| AR-025 | Add-money sheet and goal editor | 3 | P0 | S5 | AR-023 |
| AR-026 | Streak and projected-completion calculator | 3 | P1 | S5 | AR-023 |
| AR-027 | Goal completion celebration | 2 | P2 | S5 | AR-024 |
| AR-028 | Empty states and first-run guidance across tabs | 3 | P1 | S6 | AR-020, AR-024 |
| AR-029 | Settings — profile, currency, calendar, sign out, sync status | 3 | P1 | S6 | AR-040 |

## Phase 4 — Polish & release (S6)

Exit criteria: pleasant to live with, safe to install, ready to show publicly.

| Key | Task | Pts | Priority | Sprint | Depends on |
| --- | --- | --- | --- | --- | --- |
| AR-030 | Recurring rules and WorkManager scheduling | 5 | P1 | S6 | AR-004 |
| AR-031 | CSV export through the Storage Access Framework | 3 | P1 | S6 | AR-002 |
| AR-034 | Signing, versioning, and store assets | 3 | P0 | S6 | — |
| AR-035 | Real screenshots, demo GIF, and case study | 2 | P1 | S6 | Phases 1–3 |
| AR-036 | Room migrations and an emulator CI job | 3 | P1 | S6 | AR-002 |

## Release checklist

1. `dev` is green in CI; every issue in the milestone is Done with a shipped comment.
2. Point beta at the staging API (`ARAM_API_MODE=remote`, staging base URL) and
   run the full device script: fresh install, partial sync, airplane mode, queued
   write, reconnect, sign-out/sign-in.
3. Bump `aram.versionName` / `aram.versionCode`.
4. Build `betaRelease`, install on a clean device (fresh install and upgrade over alpha).
5. Run the release workflow by tagging `v*` from `dev`; confirm the signed
   `prodRelease` APK, the real API base URL, and the archived R8 mapping.
6. Fast-forward `main` to the tag; update the README status and roadmap.

## Post-MVP backlog (beyond KMP)

- Biometric app lock (moved from Phase 4)
- Safe-to-spend home-screen widget (moved from Phase 4)
- CSV import for bank statements
- Insight cards and subscription detection
- Realtime sync (WebSocket/SSE) and multi-device presence
- Shared budgets / households
- Multi-currency conversion with rate snapshots
- Bank aggregator integration

## Kotlin Multiplatform migration

**Why:** the domain logic is already pure Kotlin, and the network client will be
built on Ktor — the same code can serve an iOS or desktop client later.
**When:** after Phase 4, as its own epic; never mid-sprint.

**Target shape**

```
:shared
  core/model        commonMain — models, enums
  core/money        commonMain — Money, CurrencyCode, formatting (expect/actual for locale)
  core/common       commonMain — dispatchers, clock
  core/domain       commonMain — repository interfaces, use cases
  core/data         commonMain — repositories, mappers, outbox; platform DB driver
  core/network      commonMain — Ktor client, DTOs, auth interceptor, sync API
  core/designsystem commonMain — only if Compose Multiplatform is adopted
:app                Android shell — activity, navigation, permissions, widgets, WorkManager
```

**DI (decided 2026-11-16): Hilt stays.** Every Hilt annotation lives in
Android-only modules (`:app`, `:core:data`, `:core:database`, `:core:datastore`,
`:feature:*`). The KMP-target modules (`model`, `money`, `common`, `logging`,
`domain`, `network`) expose plain constructors and interfaces and never import
Hilt — that is the seam. When a second shell ships (K4), add Koin or a manual
graph for the shared layer without editing a single shared file. Koin is not
adopted now: it would trade Hilt's compile-time graph verification and
first-party Compose/WorkManager integration for a benefit that only exists once
an iOS client does.

**Stages**

| Stage | Work | Exit criteria |
| --- | --- | --- |
| K0 · Portability guardrails | Ban `java.*`/Android types in shared modules; inject `Clock`; prefer `kotlinx-datetime`; keep formatting behind an interface | New code in `core/model`, `core/money`, `core/common`, `core/domain`, `core/network` compiles without Android imports |
| K1 · Pure modules | Apply the KMP plugin; `androidTarget` + `jvm()` (add Apple targets when a client exists); migrate tests to `kotlin.test`. `:core:logging` and `:core:network` move first | Models, money, common, logging, network build for all targets; unit tests green |
| K2 · Domain & data | Add `:core:domain` to KMP; Room KMP (reuses schema, DAOs); DataStore KMP or `multiplatform-settings`; platform driver/`expect` builders | Repository + DAO tests pass on JVM and Android; no Android types in `commonMain` |
| K3 · Presentation | ViewModels in `commonMain` with `kotlinx-coroutines`; UI stays Jetpack Compose unless iOS becomes a target | Screens stay stateless; Android behavior unchanged |
| K4 · Second shell (optional) | Compose Multiplatform UI or a native SwiftUI shell; expect/actual for Keychain vs Keystore | App boots on a second platform with the same domain tests |
| K5 · Sync everywhere | Shared outbox worker interface per platform (WorkManager vs BGTaskScheduler); tokens in Keystore/Keychain | Sync spec exercised against the staging backend on two platforms |

**Decisions to lock at K2**

- Room KMP vs SQLDelight — Room keeps the schema and migration tests.
- Compose Multiplatform vs native UI — only if a second platform ships.
- DataStore KMP vs multiplatform-settings — prefer the official artifact.

**Risks**

- KSP/Room KMP plugin churn; pin versions and migrate one module per PR.
- Platform schedulers differ for the outbox; keep the worker logic shared and
  the scheduling thin.
- The API contract must stay additive while two clients exist.
- Migration must never block MVP fixes: land it on a long-lived branch with CI on both targets.

**Non-goals:** rewriting the Android UI, changing the database schema, or adding features just to justify KMP.

## Working agreement

1. Pick the top unblocked P0 in the current sprint, then P1.
2. Run `/ship start task AR-0NN`; it branches from `dev`, writes spec + context, implements, verifies, and hands over a device script.
3. One issue per branch; link the issue in the commit and PR.
4. Merge to `dev` only after CI is green and the device pass is reported.
5. Backend changes land behind [API_CONTRACT.md](API_CONTRACT.md); alpha stays on
   mock until the contract tests pass against staging.
6. Sprint end: close done work, move the rest back to the backlog, write a three-line retro.
