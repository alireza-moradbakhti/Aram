# AR-019 · Budgets repository and monthly progress

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/20
- Phase / Sprint: Phase 3 · Budgets & goals / Sprint 4
- Estimate: 3 pts · P0
- Branch: `chore/AR-019-budgets-repository`
- Status: Done

## Problem

`BudgetEntity`/`BudgetDao` existed only as schema (the budgets table and its
unique index came with the initial Room database), but nothing read or wrote
it. Budgets need a domain contract and a local-first implementation so the
Budgets screen (AR-020) can show per-category monthly progress.

## Goals / non-goals

Goals

- `BudgetRepository` in `:core:domain`: observe progress for a month, upsert a
  per-category limit, soft-delete a budget.
- Enforce one budget per user/category/month through the existing unique index
  (upsert into the existing row instead of inserting a duplicate).
- Progress = the budget's limit vs. the expense sum for that category in the
  month, in the budget's currency.
- Tests for month boundaries, duplicate upsert, currency mismatch, archived
  categories.

Non-goals

- Outbox/sync: budgets are not in the backend or sync contract yet, so writes
  stay local (no outbox entry). When the API grows budgets, a follow-up wires
  them into sync like transactions.
- UI. AR-020 builds the Budgets screen on top of this repository.

## Acceptance criteria

- [x] The unique index is honored by upsert.
- [x] Spending after the month end does not affect the budget.
- [x] Archived categories are handled deliberately.

## Technical design

`:core:domain`

- `budget/BudgetDraft.kt` — `categoryId`, `month: YearMonth`, `limitMinor`,
  `currencyCode`.
- `budget/BudgetProgress.kt` — wraps `Budget` + `Category` + `spentMinor`,
  exposing `limitMinor`, `remainingMinor` (`coerceAtLeast 0`), `isOver`,
  `fraction` (0..1, 0 when limit ≤ 0) for the progress bar.
- `repository/BudgetRepository.kt` — `observeProgress(month): Flow<List<…>>`,
  `suspend upsert(draft): Outcome<Budget>`, `suspend delete(id): Outcome<Unit>`.

`:core:database`

- `BudgetDao` — `upsert` (`REPLACE`), `find` (by user/category/period epoch
  day, tombstones included so a deleted budget can be revived),
  `observeForPeriod` (live rows only), `softDelete` (returns changed count),
  `deleteByUser`.

`:core:data`

- `BudgetRepositoryImpl`:
  - month range is half-open in the injected `Clock`'s zone
    (`[1st 00:00, next 1st 00:00)`); transactions use the same epoch millis.
  - `observeProgress` combines budget rows, the category directory and the
    month's ledger rows; spending only counts `EXPENSE` rows with a category,
    grouped by (categoryId, currencyCode), so a budget only sees spending in
    its own currency (IRR budget ignores USD rows, and vice versa).
  - `upsert` validates `limitMinor > 0` and a known ISO currency, requires the
    category to exist, be `EXPENSE`, and not archived, then finds the existing
    row for the period and reuses its id/`createdAt` (a deterministic
    `nameUUIDFromBytes` id is used when none exists), writing with
    `deletedAt = null` — so re-saving a month replaces the limit and revives a
    soft-deleted budget without creating a second row.
  - `delete` soft-deletes; a second delete returns `NotFound`.
  - error mapping: missing row/category → `NotFound`, wrong-kind or archived
    category → `Validation("categoryId")`, storage failures → `Storage`.
- `mapper/BudgetMappers.kt` — `BudgetEntity.toModel()`.
- `DataModule` binds `BudgetRepositoryImpl`; `:core:data` depends on
  `:core:money` for `CurrencyCode`.

## Data and migrations

No schema change. `budgets` already had `(userId, categoryId,
periodStartEpochDay)` unique from schema v1; the tests assert upsert replaces
rather than duplicates.

## Test plan

- Unit (`core/data`): `BudgetRepositoryImplTest` on fakes —
  - upsert creates one row, re-upsert replaces the limit keeping id/createdAt;
  - a soft-deleted budget is revived with the same id;
  - progress sums only the month's expenses in the budget currency (excludes
    other currency, deleted rows, income/transfer, Dec 1 and Oct 31 23:59:59);
  - month boundaries are half-open;
  - archived category is hidden from progress but its budget row is kept;
  - income/archived categories rejected; non-positive limit and unknown
    currency rejected;
  - delete tombstones once, second delete is `NotFound`;
  - no session → empty progress, `Unauthorized` upsert.
- CI-equivalent: `./gradlew :app:assembleAlphaDebug test -PbuildDate=…`.

## Risks and open questions

- Budgets are local-only until the backend grows them; a future sync task must
  reconcile the deterministic id scheme with whatever the API returns.
- `Clock` zone drives the month range; if the user changes timezone mid-month
  the spend window shifts by the offset, which is acceptable for a personal
  ledger (same rule as the rest of the app).