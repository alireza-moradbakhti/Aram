# Aram — Product & Architecture Spec

> A save-first money companion for people who are tired of budgeting apps that feel like spreadsheets.
> Online account, offline cache, sync-ready writes.

- **Status:** Skeleton scaffolded; moving to the online data layer (network + mock API first)
- **Platform:** Android (minSdk 26, compileSdk 37, targetSdk 36)
- **Stack:** Kotlin 2.4.21 · AGP 9.5.1 (built-in Kotlin) · Jetpack Compose (Material 3) · Hilt · Room · DataStore · Navigation Compose

---

## 1. Vision & positioning

Most finance apps are built around *tracking* money. Aram is built around *keeping* money.

| Typical budgeting app | Aram |
|---|---|
| Enter data, see charts, feel guilty | Answer one question: **"Can I spend this?"** |
| Dozens of settings before first value | Value in under 60 seconds of use |
| Cloud account required | Sign in once; cached data still opens offline, and writes queue until you reconnect |
| Grey dashboards and dense tables | One number, one chart, one next action |

**Target user:** Gen Z / younger millennials (18–32) who want to save toward concrete things — a trip, an emergency fund, moving out — and who abandon traditional budgeting apps within two weeks.

**Differentiators**
1. **Safe-to-spend number** — the hero of the home screen. Income minus committed spending (bills, recurring charges, budgets) divided across remaining days.
2. **Goal streaks** — saving becomes a habit loop, not a database.
3. **Calm visual language** — one accent chart at a time, generous spacing, no red-alarm dashboards.
4. **Private by default** — data syncs to the user's own account over TLS; no
   analytics SDK, no third-party trackers.

---

## 2. Feature scope

### P0 — MVP (build first)
- Online account (email + password) with a swappable mock API for alpha builds
- Cached reads offline, pull-to-refresh on reconnect, queued writes via an outbox
- Default currency, Gregorian/Jalali calendar choice
- Accounts (cash / bank / wallet / savings / investment) with opening balances
- Income & expense transactions with categories, notes, dates
- Transfers between accounts (one logical transfer, two ledger rows)
- Monthly budgets per category with progress bars and threshold alerts (80% / 100%)
- Savings goals with contributions and streak tracking
- Dashboard: safe-to-spend, month in/out, goal progress
- Charts: category donut + 6-month trend (custom Canvas, no chart library)
- Search & filter transactions (text, category, date range, type)
- Dark mode, edge-to-edge, splash screen, welcome/onboarding

### P1 — Post-MVP
- Recurring rules (daily/weekly/monthly/yearly) and subscription detection heuristics
- CSV export; CSV import for bank statements
- Reminders via WorkManager (log daily spend, monthly review)
- Insight cards ("You spent 23% less on food than last month")

### P2 — Backlog
- Biometric app lock
- Home-screen widget (safe-to-spend)
- Shared budgets / households
- Multi-currency conversion with rate snapshots
- Bank aggregator integration (region dependent)

### Explicitly out of scope
- SMS reading for transactions (Play policy risk; use CSV import instead)
- Investment portfolio tracking
- Direct open-banking integrations in MVP
- Ads, analytics, or any third-party tracking

---

## 3. UX flows

```
Launch
  └─ Splash (system splash, green field + wallet mark, held while session loads)
       ├─ No session ─→ Welcome (sign in / register)
       │                  └─ success → load cached profile → sync in background
       └─ Session exists ─→ Main shell (bottom nav: Home · Transactions · Budgets · Goals · Settings)
```

**Welcome page** — one gradient headline ("Aram"), one tagline (*Save smarter. Spend calmer.*), email + password, a register toggle, and (alpha only) a demo-account shortcut. Privacy note: "Synced to your account; readable offline."

**Home (dashboard)**
1. Greeting + avatar chip
2. Safe-to-spend card (big number, days remaining, tapping explains the math)
3. Month in / out stat cards
4. Goal progress card(s)
5. Category donut + recent transactions

**Transactions** — sticky month header, day groups, swipe or long-press to edit/delete; FAB for quick add; filter sheet.

**Budgets** — month selector, per-category progress ring/bar, summary of total allocated vs. spent.

**Goals** — cards with progress arc, streak flame count, projected completion date; "Add money" bottom sheet.

**Settings** — profile, currency, calendar, biometric lock, export, about.

---

## 4. Module structure & packaging

**Module graph**

```
:app                     application module — Application, MainActivity, nav host, session gate
:core
  :model                 pure Kotlin domain models + enums (no Android)
  :money                 Money value type, CurrencyCode, formatting (pure Kotlin)
  :common                CoroutineDispatchers abstraction (pure Kotlin)
  :logging               structured logger, platform-free (pure Kotlin)
  :network               Ktor client, DTOs, auth interceptor, sync API, MockEngine
  :database              Room: entities, DAOs, AramDatabase, DatabaseModule
  :datastore             DataStore Preferences: session, token metadata, last sync
  :domain                repository interfaces (AuthRepository, ...)
  :data                  repository implementations, mappers, outbox, seed data, DI
  :designsystem          AramTheme, color schemes, typography, reusable components
:feature
  :onboarding            Welcome screen + ViewModel
  :dashboard             Home screen
  :transactions          Transactions list + add/edit
  :budgets               Budgets list + editor
  :goals                 Savings goals
  :settings              Settings
```

**Package layout** (unified root `com.aram`)

```
com.aram
├── app
│   ├── AramApplication · MainActivity
│   ├── navigation/      AramApp — Scaffold, NavHost, bottom-tab routes
│   └── session/         MainViewModel · SessionState
├── core
│   ├── model/           domain models (pure Kotlin)
│   ├── money/           Money · CurrencyCode (pure Kotlin)
│   ├── common/          CoroutineDispatchers (pure Kotlin)
│   ├── logging/         Logger, sinks (pure Kotlin)
│   ├── network/         AramApi, Ktor engine, DTOs, error mapping, MockEngine
│   ├── domain/repository/   AuthRepository — interfaces only
│   ├── data/
│   │   ├── repository/  cache-first repository implementations
│   │   ├── mapper/      DTO/entity → model mappers
│   │   ├── sync/        outbox, sync coordinator, WorkManager worker
│   │   ├── defaults/    seed categories + Cash account
│   │   └── di/          Hilt modules (mock vs remote binding)
│   ├── database/
│   │   ├── entity/ · dao/
│   │   ├── AramDatabase
│   │   └── DatabaseModule
│   ├── datastore/       SessionDataStore
│   └── designsystem/    AramTheme · Typography · components
└── feature/<name>/presentation/   Screen · UiState · ViewModel (MVVM)
```

**Dependency rules**
- `:feature:*` → `:core:domain`, `:core:model`, `:core:money`, `:core:designsystem` — **never** `:core:data`, `:core:database`, or `:core:network`
- `:core:domain` → `:core:model` only (pure Kotlin)
- Any module may depend on `:core:logging`; it depends on nothing
- `:core:network` → `:core:model`, `:core:logging`; implements [API_CONTRACT.md](API_CONTRACT.md) and hides the mock/remote choice behind DI
- `:core:data` → `:core:domain`, `:core:database`, `:core:datastore`, `:core:network`, `:core:model`, `:core:money`
- `:app` wires features together through lambdas; features never navigate into each other
- Enforced by Gradle module graphs; `:core:model`/`:core:money`/`:core:common`/`:core:network` are Kotlin-first so they can later be shared with a KMP client

**Packaging & MVVM conventions**
- One package per responsibility: `presentation`, `repository`, `mapper`, `defaults`, `di`, `entity`, `dao`.
- Screens are stateless; state is an immutable `UiState` exposed as `StateFlow` from a `ViewModel`.
- `*Route` composables own the ViewModel wiring; `*Screen` composables take state + lambdas (previewable and testable).
- Dependencies point inward: presentation → domain ← data.

**Navigation** — each feature exposes a `*Route` composable taking callbacks (`onOpenX: () -> Unit`, `onBack: () -> Unit`). The `:app` module owns `NavHost` and route constants. One activity. Bottom nav uses `saveState`/`restoreState` per tab.

---

## 5. Money handling rules

- **Never** use `Double` for money. Amounts are `Long` in minor units (cents, fils, rials).
- `Money(amountMinor, currency)` with arithmetic, comparison, `abs`, `formatted(locale)`.
- `CurrencyCode` carries `fractionDigits` — including 0-decimal currencies (JPY, IRR).
- Major→minor conversion through `BigDecimal` with `HALF_UP`, never float multiplication.
- Cross-currency arithmetic throws; conversion belongs in an explicit FX layer (P2).
- Formatting via `NumberFormat.getCurrencyInstance(locale)` per currency.

---

## 6. Database schema (Room v1, `arya.db`)

Every table carries sync + soft-delete columns:
`userId` (owner), `createdAt`, `updatedAt` (epoch millis), `deletedAt` (nullable). Rows are soft-deleted; queries filter `deletedAt IS NULL`. The backend resolves changes from these columns and the tombstones — Room holds the device-side cache, not the only copy.

### `users`
| column | type | notes |
|---|---|---|
| id | TEXT PK | UUID |
| email | TEXT? | cached from the account; null for local-only legacy rows |
| displayName | TEXT | |
| defaultCurrency | TEXT | ISO code |
| calendarSystem | TEXT | `GREGORIAN` \| `JALALI` |
| createdAt / updatedAt | INTEGER | |

### `accounts`
| column | type | notes |
|---|---|---|
| id | TEXT PK | |
| userId | TEXT | index |
| name | TEXT | |
| type | TEXT | `CASH`,`BANK`,`WALLET`,`SAVINGS`,`INVESTMENT` |
| currencyCode | TEXT | |
| openingBalanceMinor | INTEGER | Long minor units |
| colorArgb | INTEGER | |
| iconKey | TEXT | maps to icon set |
| sortOrder | INTEGER | |
| archived | INTEGER | bool |
| createdAt / updatedAt / deletedAt | INTEGER | index (userId, archived) |

### `categories`
| column | type | notes |
|---|---|---|
| id | TEXT PK | |
| userId | TEXT | index |
| name | TEXT | |
| kind | TEXT | `INCOME` \| `EXPENSE` |
| parentId | TEXT? | subcategories later |
| colorArgb | INTEGER | drives donut chart |
| iconKey | TEXT | |
| archived | INTEGER | |
| createdAt / updatedAt / deletedAt | INTEGER | |

### `transactions`
| column | type | notes |
|---|---|---|
| id | TEXT PK | |
| userId | TEXT | index with occurredAt |
| accountId | TEXT | index |
| categoryId | TEXT? | index; null for transfers |
| type | TEXT | `INCOME` \| `EXPENSE` \| `TRANSFER` |
| amountMinor | INTEGER | always positive |
| currencyCode | TEXT | |
| occurredAt | INTEGER | epoch millis |
| note | TEXT? | |
| transferGroupId | TEXT? | index; links the two rows of a transfer |
| recurringRuleId | TEXT? | provenance for generated rows |
| createdAt / updatedAt / deletedAt | INTEGER | |

### `budgets`
| column | type | notes |
|---|---|---|
| id | TEXT PK | |
| userId | TEXT | |
| categoryId | TEXT | |
| periodStartEpochDay | INTEGER | first day of budget month |
| limitMinor | INTEGER | |
| currencyCode | TEXT | |
| createdAt / updatedAt / deletedAt | INTEGER | unique index (userId, categoryId, periodStartEpochDay) |

### `savings_goals`
| column | type | notes |
|---|---|---|
| id | TEXT PK | |
| userId | TEXT | |
| name | TEXT | |
| targetMinor | INTEGER | |
| currencyCode | TEXT | |
| deadlineEpochDay | INTEGER? | |
| status | TEXT | `ACTIVE`,`COMPLETED`,`ARCHIVED` |
| createdAt / updatedAt / deletedAt | INTEGER | |

### `goal_contributions`
| column | type | notes |
|---|---|---|
| id | TEXT PK | |
| userId | TEXT | |
| goalId | TEXT | index |
| amountMinor | INTEGER | |
| occurredAt | INTEGER | powers streaks + projection |
| createdAt / updatedAt / deletedAt | INTEGER | |

### `recurring_rules`
| column | type | notes |
|---|---|---|
| id | TEXT PK | |
| userId | TEXT | index with active |
| accountId | TEXT | |
| categoryId | TEXT? | |
| type | TEXT | |
| amountMinor | INTEGER | |
| currencyCode | TEXT | |
| frequency | TEXT | `DAILY`,`WEEKLY`,`MONTHLY`,`YEARLY` |
| intervalCount | INTEGER | every N periods |
| nextDueEpochDay | INTEGER | index; WorkManager reads due rules |
| isSubscription | INTEGER | bool; powers subscription list |
| active | INTEGER | |
| createdAt / updatedAt / deletedAt | INTEGER | |

### Derived values (not stored)
- Account balance = `openingBalanceMinor + Σ income − Σ expense ± transfers`
- Budget spent = `Σ expense` for `categoryId` within `[periodStart, nextMonth)`
- Goal progress = `Σ goal_contributions` per goal
- Safe to spend = `(monthly income − spent − remaining budget allocations − upcoming recurring bills) / days remaining`

---

## 7. Auth & identity

### Alpha — mocked API, real seam
- `AuthRepository` grows `register`, `signIn`, `signOut`, and a session flow; the
  screen talks only to the interface.
- Alpha builds serve `/auth/*` from a Ktor `MockEngine` (demo account, rotated
  fake tokens, realistic latency). Switching to the real backend is a DI binding,
  not a UI change.
- Session states stay `Loading → LoggedOut → Ready(UserProfile)`. Splash is held
  only during `Loading`.
- On `401`, the client refreshes once and retries; a failed refresh signs the
  user out and keeps cached data readable until the next sign-in.
- Sign-out revokes the refresh token, clears tokens, and clears the user-scoped
  cache (single account per device). Pending outbox writes must be flushed or
  explicitly discarded before sign-out.
- Existing local profiles migrate on first sign-in: rows keep their client UUIDs
  and are pushed through the outbox, then the server user id becomes the session.

### Tokens
- Access JWT (15 min) + rotating refresh token (30 days), per
  [API_CONTRACT.md](API_CONTRACT.md).
- Tokens live behind a `TokenStore` interface. Alpha may keep them in memory;
  beta/prod use Keystore-backed encrypted storage — never plain DataStore, never
  logs, never crash reports.
- Passwords are never stored. Argon2id hashing is server-side.

### Welcome flow
Email + password with a register toggle; a "Use demo account" shortcut exists in
alpha only. The old name-only local profile becomes the post-sign-in display
name. Privacy copy changes from "stays on this device" to "synced to your
account; readable offline."

---

## 8. Online data flow, cache, and sync

```text
UI → ViewModel → Repository (:core:domain)
                    ├── Room cache  ← observed by the UI
                    └── AramApi (:core:network) ── pull ──→ backend
Outbox worker (:core:data) → sync_outbox → /sync/push → backend
```

**Reads (cache-first)**
- The UI observes Room only; it never waits on the network to render.
- Opening a screen, or pull-to-refresh, triggers a sync when the last successful
  pull is older than 15 minutes (or always, for pull-to-refresh).
- A refresh upserts rows into Room, so the same Flow updates the screen.
- Pull failures never blank the cache: they surface `Offline`/`Stale` states with
  the last-synced timestamp and a retry action.

**Writes (outbox)**
- A create/update/delete writes the row and an outbox entry in one Room
  transaction. Transfers write both ledger rows and one outbox entry containing
  both mutations, keeping the transfer atomic on the server.
- Rows created locally carry a pending marker; the UI shows a subtle "pending
  sync" chip rather than blocking the action.
- The WorkManager worker flushes FIFO with exponential backoff (30 s → 1 h),
  reusing the original `Idempotency-Key` on retries.
- `409 conflict` replaces the local row with the canonical row and clears the
  outbox entry. Permanent `4xx` marks the entry `FAILED` and surfaces
  "couldn't sync — retry or discard"; it never retries forever.
- New app versions must not drop outbox entries; the schema is append-only.

**Pull merge**
- Pull with `/sync?since=last_since`, apply rows and tombstones, and persist
  `last_since` only after a complete page set.
- Last-write-wins per row on `updated_at`; tombstones (`deleted_at`) are kept so
  deletes propagate to other devices.
- Outbox wins over a pull for rows with unflushed local changes until the push
  resolves; a conflict response is the tie-breaker.

**Offline UX**
- Offline reads: cached content + offline banner + last-synced time.
- Offline writes: accepted and queued, with a "will sync when online" message.
- Reconnect: worker flushes, then a pull refreshes screens; no manual step.
- Airplane mode is part of every feature's device script.

**Room schema additions**
- `sync_outbox` (`id`, `entity`, `entity_id`, `op`, `payload`, `attempts`,
  `next_attempt_at`, `last_error`, `created_at`).
- Rows gain no server-only fields; `updated_at` stays authoritative for ordering.
  A migration ships with the first schema change (AR-036 covers migration tests).

---

## 9. Testing strategy

| Layer | Tool | What |
|---|---|---|
| Pure Kotlin | JUnit 4 | `Money` arithmetic/formatting (done), date/period calculators, safe-to-spend logic |
| Network | Ktor `MockEngine` + contract fixtures | DTO shapes against [API_CONTRACT.md](API_CONTRACT.md), auth refresh, error mapping, timeouts |
| Repositories | JUnit + Turbine + fakes | Cache-first reads, refresh upserts, offline fallback, outbox writes, conflict handling |
| Outbox | coroutines-test + fake worker | FIFO ordering, idempotency reuse, backoff, permanent-failure state |
| DAOs | Room `MigrationTestHelper` / in-memory DB (androidTest) | schema + query behavior, migrations from v1 onward |
| ViewModels | coroutines-test | state transitions, validation errors, offline/pending states |
| UI | Compose UI tests | Welcome sign-in/register, offline banner, pending chip |
| Screenshots | Roborazzi/Paparazzi (P1) | theme regression |

Rule: business math is pure Kotlin and lives in `:core:money` / `:core:domain` — no Android dependency, fast tests.

---

## 10. Milestones

**M0 — Skeleton (done)**
- [x] Multi-module Gradle setup, version catalog
- [x] Splash + Welcome + main shell navigation
- [x] Room schema v1 + DAOs, DataStore session, local auth repo
- [x] Design system (teal/amber M3 theme, dark mode)

**M1 — Online transactions end-to-end (≈4 weeks)**
- [ ] Network module with mock API, real API later
- [ ] Email/password sign-in and secure token session
- [ ] Cache-first transactions with pull-to-refresh and offline reads
- [ ] Outbox + sync worker for offline writes
- [ ] Add/edit/delete transaction with account + category pickers and money input
- [ ] Transactions list with month headers and search
- [ ] Account balance calculations; transfer support
- [ ] Repository use cases + tests

**M2 — Dashboard (≈1.5 weeks)**
- [ ] Safe-to-spend computation + explainer sheet
- [ ] Month in/out, category donut (Canvas), 6-month trend
- [ ] Recent transactions section

**M3 — Budgets & goals (≈2 weeks)**
- [ ] Budget CRUD, progress, 80/100% alerts
- [ ] Goal CRUD, contributions, streaks, projection
- [ ] Empty states + onboarding polish

**M4 — Product polish (≈2 weeks)**
- [ ] Recurring rules + WorkManager
- [ ] CSV export/import
- [ ] Biometric lock, widget, reminders
- [ ] Launcher icon, Play Store assets, README case study

---

## 11. Privacy & security posture

- The app requests `INTERNET` and talks only to the configured HTTPS API. No
  other network calls, no ads, no analytics SDKs.
- Room stays app-private; it is a cache of the user's server data. Sign-out
  clears the user-scoped cache.
- Tokens live in Keystore-backed storage; access tokens are short-lived and
  refresh tokens rotate. Nothing sensitive is logged — no amounts, notes, emails,
  tokens, or account numbers.
- Cleartext HTTP is disabled in release builds; a debug-only network security
  config may allow a local backend.
- Server-side ownership checks are mandatory: a client-supplied `user_id` is
  ignored in favor of the token subject.
- CSV export writes through Storage Access Framework; no broad storage permissions.
- Biometric lock remains a device gate (post-MVP), separate from account auth.

---

## 12. Glossary

- **Minor units** — integer smallest currency unit (e.g. 1234 = $12.34).
- **Safe to spend** — discretionary money per day after commitments.
- **Streak** — consecutive weeks with a goal contribution.
- **Tombstone** — soft-deleted row kept for sync reconciliation.
- **Outbox** — local queue of mutations waiting to be pushed to the server.
- **Cache-first** — read from Room immediately, refresh from the API in the background.
- **Last-write-wins** — conflict rule: the row with the latest `updated_at` survives.
