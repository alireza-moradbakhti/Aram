# Context — AR-019 · Budgets repository and monthly progress

## Starting point

- Base branch `dev` at `origin/dev` HEAD; work happens on
  `chore/AR-019-budgets-repository`.
- Budgets were schema-only: `BudgetEntity` + `BudgetDao` stub, no repository,
  no DI binding, no tests.

## Relevant code

- `core/database/.../entity/BudgetEntity.kt` — `id`, `userId`, `categoryId`,
  `periodStartEpochDay`, `limitMinor`, `currencyCode`, `createdAt`,
  `updatedAt`, `deletedAt`; unique index on `(userId, categoryId,
  periodStartEpochDay)`.
- `core/database/.../dao/BudgetDao.kt` — rewritten: `upsert` (`REPLACE`),
  `find` (tombstones included), `observeForPeriod`, `softDelete: Int`,
  `deleteByUser`.
- `core/data/.../repository/TransactionRepositoryImpl.kt` — the pattern this
  follows: session `userId` flow → `flatMapLatest` → Room flow → map →
  `catch { emit(emptyList()) }` → `flowOn(dispatchers.io)`; `currentUserId()`
  helper; `safeOutcome` + `toStorageAwareError()`.
- `core/database/.../dao/CategoryDao.kt` — `findById` (excludes tombstones)
  validates the category at upsert; `observeByUser` feeds progress so archived
  categories drop out reactively.
- `core/database/.../dao/TransactionDao.kt` — `observeRange(userId,
  fromInclusive, toExclusive)` supplies the month's rows.
- `core/money` — `CurrencyCode.fromIsoCode` / `fractionDigits` / `isoCode`.
- `core/domain/.../di/DomainModule.kt` — provides `Clock.systemDefaultZone()`;
  `:core:data` gets it via constructor injection.

## Conventions

- Money is integer minor units (`Long`) + ISO currency; no floats.
- Writes return `Outcome<…>`; validation errors carry field messages.
- Flow repos never throw to collectors: log + emit empty.
- Audit timestamps (`createdAt`/`updatedAt`/`deletedAt`) use
  `System.currentTimeMillis()`, not the injected `Clock` (the clock is for
  date math — month windows here).
- No outbox for budgets yet: the backend has no budget endpoints / sync
  entries, so no `SyncOutboxEntity` rows are queued.

## Dependencies

- New: `:core:data` → `:core:money` (`implementation`).
- Hilt: `DataModule.bindBudgetRepository`.

## Decisions log

- **Local-first, no outbox.** The sync contract (`/sync/pull`, outbox slugs)
  has no budget entity; writing outbox rows would park them forever. Revisit
  when the API grows budgets.
- **Upsert revives, never duplicates.** `find` looks at tombstones too and the
  write reuses `existing.id`/`createdAt` with `deletedAt = null`; a fresh row
  gets a deterministic `nameUUIDFromBytes("budget:$userId:$categoryId:$epochDay")`
  id so a re-upsert after a partial state cannot race into two rows.
- **Archived categories: hidden, kept.** Progress drops budgets whose
  category is archived or missing, but the row stays in the table so
  un-archiving restores the budget; upserting into an archived category is
  rejected with `Validation("categoryId")`.
- **Currency mismatch: budget ignores foreign-currency spend.** Spend is
  grouped by (category, currency); a budget only reads its own currency's
  sum. Cross-currency conversion is out of scope (no rates in the app).
- **Half-open month in `clock.zone`.** `[1st 00:00, next 1st 00:00)` matches
  `TransactionDao.observeRange` semantics used by the dashboard.

## Verification log

- Focused: `./gradlew :core:data:testDebugUnitTest :core:database:testDebugUnitTest`
  — `BudgetRepositoryImplTest tests=9 failures=0 errors=0`,
  `TransactionRepositoryImplTest tests=14 failures=0`.
- CI-equivalent: `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261128 --console=plain`
  — BUILD SUCCESSFUL in 59s (574 actionable tasks: 72 executed, 502 up-to-date).
- Device: no new UI in this task; the Budgets tab is still the AR-020
  placeholder, so device verification is deferred to AR-020.