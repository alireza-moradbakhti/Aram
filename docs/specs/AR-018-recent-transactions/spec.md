# AR-018 · Recent transactions on the dashboard

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/19
- Phase / Sprint: Phase 2 · Dashboard & insight / Sprint 4
- Estimate: 2 pts · P1
- Branch: `feat/AR-018-recent-transactions` (from `dev`)
- Status: In review

## Problem

The dashboard's Recent card is static example data. Users need a quick path back
into the ledger: the five newest transactions with icon, title, context and
signed amount, and a way to open the full list or add the first entry.

## Goals / non-goals

Goals

- Five most recent live transactions, in ledger order, from Room only.
- Each row: category icon, note (or category fallback), `category · date`
  meta, signed amount with currency fraction digits.
- Transfer legs labeled `Transfer · date · account`.
- Row tap and "See all" open the Activity tab; the empty state links to
  Add transaction.
- Both themes match the prototype's `.list-row` card.

Non-goals

- Real budgets/safe-to-spend math on the card above (AR-021+).
- A dedicated transaction detail route; rows open the list for now.
- Signed positive amounts for the receiving leg of a transfer (the domain
  stores one TRANSFER row per account with a positive `amountMinor`; the app's
  ledger renders both legs as outflow — the dashboard follows that convention).

## Acceptance criteria

- [x] Rows match the ledger ordering.
- [x] Transfer legs are labeled clearly.
- [x] No duplicate loading of the full transaction list.

## Technical design

`feature:dashboard`

- `DashboardViewModel` combines `TransactionRepository.observeRecent(5)`,
  `LedgerDirectory.observeCategories()` and `LedgerDirectory.observeAccounts()`
  into `DashboardUiState(isLoading, recent)`.
- `RecentTransactionUi(id, title, meta, categoryIconKey, amountMinor,
  fractionDigits, incoming)`:
  - `title` = note when present, else category name, else type label.
  - `meta` = category (omitted when it equals the title), date label, and the
    account name for `TRANSFER` rows, joined with ` · `.
  - Date label: `HH:mm` for today, `MMM d` otherwise, in the injected `Clock`'s
    zone.
  - `amountMinor` is negated unless the row is `INCOME`; fraction digits come
    from the row's `currencyCode` (`IRR` → 0, unknown → 0).
- `DashboardScreen` renders the card from state: dividers between rows,
  `RecentRow` → `onOpenLedger`; empty state (`No transactions yet`, copy from
  the prototype, `Add expense` primary button) → `onAddTransaction`.
- `AramApp` wires `onOpenLedger = navigate(MainTab.Activity)` and
  `onAddTransaction = navigate(ADD_TRANSACTION_ROUTE)`.

`feature:dashboard/build.gradle.kts` gains `:core:domain` and `:core:money`.

## Data and migrations

No schema change. Reuses `TransactionDao.observeRecent(userId, limit)` with
`deletedAt IS NULL ORDER BY occurredAt DESC LIMIT 5`.

## Test plan

- Unit (`DashboardViewModelTest`, 6): loading → loaded; ledger order and meta
  formatting (today time vs older date, transfer account suffix); limit is 5
  and `observeTransactions` is never called; empty ledger; per-row currency
  fraction digits.
- Robolectric (`DashboardRecentTest`, 4): rows as supplied by state; row tap and
  "See all" callbacks; empty-state copy and `Add expense` callback.
- Device (emulator-5554): real ledger rows light/dark, row tap and See all →
  Activity, injected transfer pair for labeling, empty state via soft-deleted
  transactions, `Add expense` → Add transaction sheet, logcat crash scan.

## Risks and open questions

- Transfers are rendered as outflow on both legs (see non-goals); revisit when
  the ledger gains transfer direction.
- A transfer whose account was removed by sync shows no account segment; the
  row stays readable (`Transfer · 16:07`).