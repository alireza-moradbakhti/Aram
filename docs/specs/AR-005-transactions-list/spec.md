# AR-005 · Transactions list with month headers and day groups

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/6
- Phase / Sprint: Phase 1 · Transactions core / Sprint 1
- Estimate: 5 pts · P0
- Branch: feat/AR-005-transactions-list (stacked on AR-004)
- Status: Shipped

## Problem

The Activity tab still renders a hardcoded demo ledger. Saved transactions are
invisible there, and there is no real month navigation, grouping, or empty
state. The ledger must be readable at a glance from Room.

## Goals / Non-goals

- Goals: observe the selected month from Room, group rows by day (newest
  first), month navigation with real totals, All/Expenses/Income scope,
  loading and empty states with an add CTA, smooth scrolling for 500+ rows.
- Non-goals: search/filter sheets, editing rows (AR-006), pagination beyond a
  month, multi-currency conversion, insights engine.

## Acceptance criteria

- [x] Ordering is newest first within correct day groups.
- [x] Empty state offers a clear call to action.
- [x] List scrolls smoothly with at least 500 rows.

## Technical design

- `TransactionsViewModel` (`@HiltViewModel`) combines the selected month and
  scope with `TransactionRepository.observeTransactions(from, to)` for the
  month and the previous month, plus `LedgerDirectory` for labels/icons.
- A month is `[first day 00:00, first day of next month 00:00)` in the device
  zone, passed to the repository as epoch millis.
- Rows are sorted by `occurredAt` descending and grouped by `LocalDate` in the
  device zone. Each group carries its outflow total (or income when the day
  has no outflow), matching the prototype's signed day totals.
- Display currency is the most frequent currency among the month's rows,
  falling back to the first account, else USD. Totals sum only that currency.
- Spent/income cards use real sums; the spent delta is the percentage against
  the previous month ("9.2% under Sep") or "No spend last month".
- The screen is a stateless `LazyColumn` with a sticky month header, a day card
  per group, and keys so 500 rows recycle cleanly. Empty and loading layouts
  are part of the stateless screen.
- `MoneyLabel`/formatting gain minor-unit aware helpers
  (`formatMinor`/`formatSignedMinor`, currency fraction digits); USD shows
  cents, IRR/JYP render integers as the prototype does.
- Error state: the repository contract already folds failures into an empty
  emission, so the screen has no user-visible error branch; the decision is
  recorded here instead of adding unreachable UI.

## Data and migrations

None. Reads the AR-002 Room tables through the existing repository.

## Test plan

- Unit: `TransactionsViewModelTest` with fakes — day grouping and ordering,
  month range boundaries, scope filtering, totals and currency selection,
  empty state, status-row counts, month change reloads.
- Manual: dark/light, empty month, month navigation, scope switch, rotation,
  save from the FAB appears in the current month without a relaunch,
  scrolling a seeded 500-row month.

## Risks and open questions

- Archived categories/accounts fall back to type labels and a default icon
  until an archive-aware lookup exists.
- Transfers are displayed as outflows; AR-009 revisits the pair rendering.
- Multi-currency months show totals in the dominant currency only.
