# Aram — master build plan

Everything left to build, from the current skeleton to a shipped MVP, plus the
Kotlin Multiplatform migration that comes after it. The design source is
[SPEC.md](SPEC.md); sprint narratives live in [ROADMAP.md](ROADMAP.md); the live
status of every task lives on the
[project board](https://github.com/users/alireza-moradbakhti/projects/6).

## Task codes

Every issue has a stable key: `AR-001` … `AR-037`, assigned in delivery order and
never reused. The GitHub issue number stays as the URL/id, but all planning
artifacts, branches, commits, and comments use the key.

- Branch: `feat/AR-005-transactions-list`
- Commit: `feat(transactions): group list by month (AR-005)`
- Board card title: `AR-005 · Transactions list with month headers and day groups`

`AR-001 … AR-036` map 1:1 to issues `#2 … #37`; `AR-037` (issue `#38`) is the
safety task added after sprint planning. New work starts at `AR-038`.

## Where we are

| Phase | Scope | State |
| --- | --- | --- |
| Phase 0 · Skeleton | multi-module Gradle, theme, splash, welcome, nav shell, Room v1, local session, CI, variants, signing config | ✅ done |
| Phase 1 · Transactions core | AR-037 (safety baseline), AR-001 … AR-011, AR-015 | next |
| Phase 2 · Dashboard & insight | AR-012 … AR-018 | planned |
| Phase 3 · Budgets & goals | AR-019 … AR-029 | planned |
| Phase 4 · Polish & release | AR-030 … AR-036 | planned |
| Post-MVP · KMP | shared core, iOS/desktop shell, sync client | planned, see §8 |

## Delivery model

- **Sprints:** five two-week sprints (S1 Nov 16–29 2026 → S5 Jan 11–24 2027), capacity 31 / 23 / 21 / 22 / 24 points. S1 gained the 5-point safety baseline (AR-037) on top of its original 26; if velocity suffers, AR-008 slips to S2 first.
- **Estimation:** Fibonacci — XS 1, S 2, M 3, L 5, XL 8.
- **Alpha gate:** every change ships as `alphaDebug`, built by CI, tested on a real device. Nothing else is built in CI.
- **Release gate:** `beta`/`prod` builds are cut only from a green, device-tested `dev`, and only when a release is being prepared.
- **Flow:** Todo → In Progress → Review → Done, moved by `/ship`.
- **Definition of done:** acceptance criteria met · unit tests for changed logic · `./gradlew :app:assembleAlphaDebug test` green · CI green · real-device pass · issue comment with evidence · docs/spec updated.

## Phase 1 — Transactions core (S1–S2)

Exit criteria: record income/expense end-to-end, see them correctly grouped, balances derived, transfers atomic — and no screen can crash or hang on a failed load.

| Key | Task | Pts | Priority | Sprint | Depends on |
| --- | --- | --- | --- | --- | --- |
| AR-037 | Safe error handling across screens and layers | 5 | P0 | S1 (first) | — |
| AR-001 | Transaction domain contract and use cases | 3 | P0 | S1 | AR-037 |
| AR-002 | Room-backed transaction repository | 3 | P0 | S1 | AR-001 |
| AR-003 | Locale-aware money input component | 2 | P1 | S1 | — |
| AR-004 | Add-transaction screen | 5 | P0 | S1 | AR-002, AR-003 |
| AR-005 | Transactions list with month headers and day groups | 5 | P0 | S1 | AR-002 |
| AR-006 | Edit and delete transactions with undo | 3 | P0 | S1 | AR-005 |
| AR-007 | Derived account balances | 3 | P0 | S1 | AR-002 |
| AR-008 | Transaction routes and FAB navigation | 2 | P1 | S1 | AR-004, AR-005 |
| AR-009 | Atomic transfers between accounts | 3 | P0 | S2 | AR-002, AR-007 |
| AR-010 | Search and filter transactions | 5 | P1 | S2 | AR-005 |
| AR-011 | Repository and ViewModel tests with Turbine | 3 | P1 | S2 | AR-002 |
| AR-015 | Room instrumented tests for transaction queries | 2 | P1 | S2 | AR-002 |

## Phase 2 — Dashboard & insight (S2–S3)

Exit criteria: a trustworthy safe-to-spend number, month in/out, and charts that tell the month's story.

| Key | Task | Pts | Priority | Sprint | Depends on |
| --- | --- | --- | --- | --- | --- |
| AR-012 | Safe-to-spend calculator with a testable breakdown | 3 | P0 | S2 | AR-007 |
| AR-013 | Dashboard shell with safe-to-spend and month in/out | 5 | P0 | S2 | AR-012 |
| AR-014 | Safe-to-spend explainer sheet | 2 | P1 | S2 | AR-013 |
| AR-016 | Category donut chart on Canvas | 3 | P1 | S3 | AR-013 |
| AR-017 | Six-month trend chart on Canvas | 3 | P1 | S3 | AR-013 |
| AR-018 | Recent transactions on the dashboard | 2 | P1 | S3 | AR-005, AR-013 |

## Phase 3 — Budgets & goals (S3–S4)

Exit criteria: budgets track real spend with 80/100% alerts; goals have progress, streaks, and projection.

| Key | Task | Pts | Priority | Sprint | Depends on |
| --- | --- | --- | --- | --- | --- |
| AR-019 | Budgets repository and monthly progress | 3 | P0 | S3 | AR-002 |
| AR-020 | Budgets screen with month selector | 5 | P0 | S3 | AR-019 |
| AR-021 | Budget editor with 80% and 100% alert states | 3 | P0 | S3 | AR-019 |
| AR-022 | Shared progress and amount components | 2 | P1 | S3 | — |
| AR-023 | Savings goals and contributions (domain + data) | 3 | P0 | S4 | AR-002 |
| AR-024 | Goals list with progress arcs and streak badges | 5 | P0 | S4 | AR-023, AR-022 |
| AR-025 | Add-money sheet and goal editor | 3 | P0 | S4 | AR-023 |
| AR-026 | Streak and projected-completion calculator | 3 | P1 | S4 | AR-023 |
| AR-027 | Goal completion celebration | 2 | P2 | S4 | AR-024 |
| AR-028 | Empty states and first-run guidance across tabs | 3 | P1 | S4 | AR-020, AR-024 |
| AR-029 | Settings — profile, currency, calendar, about | 3 | P1 | S4 | — |

## Phase 4 — Polish & release (S5)

Exit criteria: pleasant to live with, safe to install, ready to show publicly.

| Key | Task | Pts | Priority | Sprint | Depends on |
| --- | --- | --- | --- | --- | --- |
| AR-030 | Recurring rules and WorkManager scheduling | 5 | P1 | S5 | AR-004 |
| AR-031 | CSV export through the Storage Access Framework | 3 | P1 | S5 | AR-002 |
| AR-032 | Biometric app lock | 3 | P2 | S5 | AR-029 |
| AR-033 | Safe-to-spend home-screen widget | 5 | P2 | S5 | AR-012 |
| AR-034 | Signing, versioning, and store assets | 3 | P0 | S5 | — |
| AR-035 | Real screenshots, demo GIF, and case study | 2 | P1 | S5 | Phases 1–3 |
| AR-036 | Room migrations and an emulator CI job | 3 | P1 | S5 | AR-002 |

## Release checklist

1. `dev` is green in CI; every issue in the milestone is Done with a shipped comment.
2. Bump `aram.versionName` / `aram.versionCode`.
3. Build `betaRelease`, install on a clean device (fresh install and upgrade over alpha).
4. Run the release workflow by tagging `v*` from `dev`; confirm the signed `prodRelease` APK and the archived R8 mapping.
5. Fast-forward `main` to the tag; update the README status and roadmap.

## Post-MVP: Kotlin Multiplatform migration

**Why:** the domain logic is already pure Kotlin. Sharing it unlocks an iOS or
desktop client later, and makes the sync client reusable — without a rewrite.
**When:** after Phase 4, as its own epic; never mid-sprint.

**Target shape**

```
:shared
  core/model        commonMain — models, enums
  core/money        commonMain — Money, CurrencyCode, formatting (expect/actual for locale)
  core/common       commonMain — dispatchers, clock
  core/domain       commonMain — repository interfaces, use cases
  core/data         commonMain — repositories, mappers; platform DB driver
  core/designsystem commonMain — only if Compose Multiplatform is adopted
:app                Android shell — activity, navigation, permissions, widgets, WorkManager
```

**Stages**

| Stage | Work | Exit criteria |
| --- | --- | --- |
| K0 · Portability guardrails | Ban `java.*`/Android types in shared modules; inject `Clock`; prefer `kotlinx-datetime`; keep formatting behind an interface | New code in `core/model`, `core/money`, `core/common`, `core/domain` compiles without Android imports |
| K1 · Pure modules | Apply the KMP plugin; `androidTarget` + `jvm()` (add `iosArm64`/`iosSimulatorArm64` when a client exists); migrate tests to `kotlin.test`. `:core:logging` is already platform-free and moves here | `:core:model`, `:core:money`, `:core:common`, `:core:logging` build for all targets; unit tests green |
| K2 · Domain & data | Add `:core:domain` to KMP; choose Room KMP (reuses schema, DAOs) over SQLDelight; DataStore via its KMP artifact or `multiplatform-settings`; platform driver/`expect` builders | Repository + DAO tests pass on JVM and Android; no Android types in `commonMain` |
| K3 · Presentation | ViewModels in `commonMain` with `kotlinx-coroutines`; UI stays Jetpack Compose unless iOS becomes a target | Screens stay stateless; Android app behavior unchanged |
| K4 · Second shell (optional) | Compose Multiplatform UI or a native SwiftUI shell over `:shared`; expect/actual for Keychain vs Keystore | App boots on a second platform with the same domain tests |
| K5 · Sync client | `:core:network` with Ktor in `commonMain`; outbox worker per platform; tokens in Keystore/Keychain | Sync spec §8 exercised against a test backend |

**Decisions to lock at K2**

- Room KMP vs SQLDelight — Room keeps the existing schema and migration tests; SQLDelight is lighter but a rewrite of the DAO layer.
- Compose Multiplatform vs native UI — only if a second platform ships; shared UI is not a portfolio goal by itself.
- DataStore KMP vs multiplatform-settings — prefer the official DataStore artifact.

**Risks**

- KSP/Room KMP plugin churn; pin versions and migrate one module per PR.
- Locale/currency formatting differs per platform — keep it behind `MoneyFormatter`.
- WorkManager, widgets, and biometrics stay Android-only behind thin interfaces.
- Migration must never block MVP fixes: land it on a long-lived branch with CI on both targets.

**Non-goals:** rewriting the Android UI, changing the database schema, or adding sync just to justify KMP.

## Post-MVP backlog (beyond KMP)

- CSV import for bank statements
- Insight cards and subscription detection
- Remote accounts + sync (SPEC §8)
- Shared budgets / households
- Multi-currency conversion with rate snapshots
- Bank aggregator integration

## Working agreement

1. Pick the top unblocked P0 in the current sprint, then P1.
2. Run `/ship start task AR-0NN`; it branches from `dev`, writes spec + context, implements, verifies, and hands over a device script.
3. One issue per branch; link the issue in the commit and PR.
4. Merge to `dev` only after CI is green and the device pass is reported.
5. Sprint end: close done work, move the rest back to the backlog, write a three-line retro.
