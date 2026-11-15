# Aram — Product & Architecture Spec

> A save-first money companion for people who are tired of budgeting apps that feel like spreadsheets.
> Offline-first. Single user today, sync-ready tomorrow.

- **Status:** Skeleton scaffolded (splash, welcome, navigation shell, core data layer)
- **Platform:** Android (minSdk 26, compileSdk 37, targetSdk 36)
- **Stack:** Kotlin 2.4.21 · AGP 9.5.1 (built-in Kotlin) · Jetpack Compose (Material 3) · Hilt · Room · DataStore · Navigation Compose

---

## 1. Vision & positioning

Most finance apps are built around *tracking* money. Aram is built around *keeping* money.

| Typical budgeting app | Aram |
|---|---|
| Enter data, see charts, feel guilty | Answer one question: **"Can I spend this?"** |
| Dozens of settings before first value | Value in under 60 seconds of use |
| Cloud account required | Fully offline; no account required, ever (until sync is added on your terms) |
| Grey dashboards and dense tables | One number, one chart, one next action |

**Target user:** Gen Z / younger millennials (18–32) who want to save toward concrete things — a trip, an emergency fund, moving out — and who abandon traditional budgeting apps within two weeks.

**Differentiators**
1. **Safe-to-spend number** — the hero of the home screen. Income minus committed spending (bills, recurring charges, budgets) divided across remaining days.
2. **Goal streaks** — saving becomes a habit loop, not a database.
3. **Calm visual language** — one accent chart at a time, generous spacing, no red-alarm dashboards.
4. **Privacy by default** — data never leaves the device. No analytics SDK in the MVP.

---

## 2. Feature scope

### P0 — MVP (build first)
- Local profile (single user), default currency, Gregorian/Jalali calendar choice
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
- Biometric app lock
- CSV export; CSV import for bank statements
- Home-screen widget (safe-to-spend)
- Reminders via WorkManager (log daily spend, monthly review)
- Insight cards ("You spent 23% less on food than last month")

### P2 — Product-grade / backend era
- Remote accounts + real-time sync (see §8)
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
       ├─ No profile ─→ Welcome
       │                  └─ enter name → create profile (+ default categories, Cash account)
       └─ Profile exists ─→ Main shell (bottom nav: Home · Transactions · Budgets · Goals · Settings)
```

**Welcome page** — one gradient headline ("Aram"), one tagline (*Save smarter. Spend calmer.*), one text field (name), one button. Privacy note: "Your data stays on this device."

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
  :database              Room: entities, DAOs, AramDatabase, DatabaseModule
  :datastore             DataStore Preferences: session storage
  :domain                repository interfaces (AuthRepository, ...)
  :data                  repository implementations, mappers, seed data, DI
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
│   ├── domain/repository/   AuthRepository — interfaces only
│   ├── data/
│   │   ├── repository/  AuthRepositoryImpl
│   │   ├── mapper/      entity → model mappers
│   │   ├── defaults/    seed categories + Cash account
│   │   └── di/          Hilt modules
│   ├── database/
│   │   ├── entity/ · dao/
│   │   ├── AramDatabase
│   │   └── DatabaseModule
│   ├── datastore/       SessionDataStore
│   └── designsystem/    AramTheme · Typography · components
└── feature/<name>/presentation/   Screen · UiState · ViewModel (MVVM)
```

**Dependency rules**
- `:feature:*` → `:core:domain`, `:core:model`, `:core:money`, `:core:designsystem` — **never** `:core:data` or `:core:database`
- `:core:domain` → `:core:model` only (pure Kotlin)
- Any module may depend on `:core:logging`; it depends on nothing
- `:core:data` → `:core:domain`, `:core:database`, `:core:datastore`, `:core:model`, `:core:money`
- `:app` wires features together through lambdas; features never navigate into each other
- Enforced by Gradle module graphs; `:core:model`/`:core:money`/`:core:common` are pure Kotlin so they could later be shared with a KMP backend client

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
`userId` (owner), `createdAt`, `updatedAt` (epoch millis), `deletedAt` (nullable). Rows are soft-deleted; queries filter `deletedAt IS NULL`. This lets a future backend resolve changes without tombstones being lost.

### `users`
| column | type | notes |
|---|---|---|
| id | TEXT PK | UUID |
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

### Today — local profile ("single-user, no account")
- `SessionDataStore` stores `user_id` in Preferences DataStore.
- On welcome, `AuthRepository.createLocalProfile(name)`:
  1. creates a `users` row (UUID),
  2. seeds 12 default categories + a "Cash" account,
  3. writes `user_id` to DataStore.
- `MainViewModel` observes `AuthRepository.observeSession()` and maps to `Loading → LoggedOut → Ready(profile)`. Splash is held only during `Loading`.
- No passwords, no network, nothing to sync yet. "Sign out" (P1) just clears the session id.

### Tomorrow — backend auth without touching features
Features depend only on `:core:domain`'s interface:

```kotlin
interface AuthRepository {
    fun observeSession(): Flow<UserProfile?>
    suspend fun createLocalProfile(displayName: String): UserProfile
    // later: suspend fun signInWithEmail(...), suspend fun signInWithGoogle(...), fun signOut()
}
```

The swap is a DI binding in `:core:data`: replace `AuthRepositoryImpl` with a remote-aware implementation that keeps the same session id in DataStore and mirrors the profile into Room. No feature module changes. A migration path for existing local users: on first remote sign-in, upload the local `userId`'s rows, then adopt the server user id (or keep client ids as UUIDs so there is no rewrite).

---

## 8. Future sync design (planned, not built)

- **Identity:** every row already has `userId` + UUID primary keys (client-generated → safe to push before server ack).
- **Outbox:** new `sync_outbox` table records local mutations (`entity`, `entityId`, `op`, `payload`, `attempts`). A WorkManager worker flushes it.
- **Timestamps:** client `updatedAt` for ordering; server assigns `serverUpdatedAt` on accept. Conflict policy: last-write-wins per row, with tombstones from `deletedAt`.
- **DTOs:** snake_case JSON, mapped in `:core:data`; DTOs live in a future `:core:network` module, keeping Room entities private to `:core:database`.
- **Realtime:** start with polling/pull-on-open; add WebSocket/SSE later via a `RemoteDataSource` abstraction.
- **Security:** the backend must verify `userId` ownership server-side; client-supplied ids are opaque UUIDs. Tokens in `EncryptedSharedPreferences`, not DataStore preferences.

---

## 9. Testing strategy

| Layer | Tool | What |
|---|---|---|
| Pure Kotlin | JUnit 4 | `Money` arithmetic/formatting (done), future date/period calculators, safe-to-spend logic |
| Repositories | JUnit + Turbine + fakes | `AuthRepositoryImpl` session flow, seeding, validation |
| DAOs | Room `MigrationTestHelper` / in-memory DB (androidTest) | schema + query behavior, migrations from v1 onward |
| ViewModels | coroutines-test | state transitions, validation errors |
| UI | Compose UI tests | Welcome happy path + errors, nav shell |
| Screenshots | Roborazzi/Paparazzi (P1) | theme regression |

Rule: business math is pure Kotlin and lives in `:core:money` / `:core:domain` — no Android dependency, fast tests.

---

## 10. Milestones

**M0 — Skeleton (this commit)**
- [x] Multi-module Gradle setup, version catalog
- [x] Splash + Welcome + main shell navigation
- [x] Room schema v1 + DAOs, DataStore session, local auth repo
- [x] Design system (teal/amber M3 theme, dark mode)

**M1 — Transactions end-to-end (≈2 weeks)**
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

- No INTERNET permission in the MVP is a feature, not a limitation; add it only with sync.
- Room file stays app-private; optional encryption via SQLCipher is P1 if users request it.
- Biometric lock gates app entry using `BiometricPrompt` (auth **device** gate, not network auth).
- CSV export writes through Storage Access Framework; no broad storage permissions.
- No PII beyond a display name.

---

## 12. Glossary

- **Minor units** — integer smallest currency unit (e.g. 1234 = $12.34).
- **Safe to spend** — discretionary money per day after commitments.
- **Streak** — consecutive weeks with a goal contribution.
- **Tombstone** — soft-deleted row kept for sync reconciliation.
