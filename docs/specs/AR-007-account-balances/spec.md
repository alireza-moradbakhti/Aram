# AR-007 · Derive account balances

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/8
- Phase / Sprint: Phase 1 · Transactions core / Sprint 2
- Estimate: 3 pts · P0
- Branch: `feat/AR-007-account-balances` (from `dev` @ `58277b5`)
- Status: Done

## Problem

Accounts store only an `openingBalanceMinor`. Nothing derives the current
balance, so no screen can show trustworthy numbers. Storing a mutable balance
column would drift from the ledger on every edit, delete, restore and sync
merge.

## Goals / non-goals

Goals

- Derive a balance per account from the ledger on every observation:
  `opening + Σ income − Σ expense ± transfer legs`.
- Expose a per-account `Flow<AccountBalance?>` that re-emits whenever that
  account's rows or the account itself change.
- Exclude soft-deleted rows and rows of another user.
- Refuse cross-currency arithmetic: only rows whose `currencyCode` matches the
  account's currency contribute to the sum.

Non-goals

- Any UI that displays balances (accounts screen lands with a later task).
- Creating transfers (AR-009 owns the two-row write). This task only defines
  how transfer legs are read.
- A total/net-worth rollup across accounts (needs FX conversion, out of scope).
- Storing or caching a balance value anywhere.

## Acceptance criteria

- [x] Transfers do not double-count as income or expense.
- [x] Zero-transaction accounts return the opening balance.
- [x] Unit tests cover positive, negative, and same-account cases.

## Technical design

Transfer legs are two ledger rows sharing a `transferGroupId`, one row per
account, `categoryId` null, `type = TRANSFER`, `amountMinor` always positive at
the schema level. A shared group cannot be read from a single row, so the
signed leg is the only representation that keeps balances derivable per row:
**a TRANSFER row carries a signed amount — negative when money leaves this
account, positive when it arrives.** Income/expense rows stay positive. This is
recorded as a decision for AR-009 to honor when it writes the pair, and it
generalizes the SPEC's `± transfers`.

Layers:

1. `:core:model` — `AccountBalance(accountId, currencyCode, balanceMinor)`.
2. `:core:database` — `AccountBalanceRow` projection and
   `AccountDao.observeBalance(userId, accountId): Flow<AccountBalanceRow?>`.
   One SQL query: account row + a correlated `SUM(CASE type …)` over its
   non-deleted transactions, `WHERE t.currencyCode = a.currencyCode`. Room's
   invalidation tracker observes both tables, so the Flow re-emits on any
   transaction or account change. Missing or tombstoned account → `null`.
3. `:core:domain` — `AccountBalanceRepository.observeBalance(accountId)`
   returning `Flow<AccountBalance?>`; `null` also means "no session".
4. `:core:data` — `AccountBalanceRepositoryImpl` scopes the query to the
   session user (`session.userId.flatMapLatest`), maps the row, logs and
   emits `null` on failure (same policy as the other read paths), runs on
   `dispatchers.io`. Bound in `DataModule`.

SQL sketch:

```sql
SELECT a.id AS accountId, a.currencyCode AS currencyCode,
       a.openingBalanceMinor + COALESCE((
           SELECT SUM(CASE t.type
                        WHEN 'INCOME'   THEN t.amountMinor
                        WHEN 'EXPENSE'  THEN -t.amountMinor
                        WHEN 'TRANSFER' THEN t.amountMinor
                        ELSE 0 END)
           FROM transactions t
           WHERE t.userId = a.userId AND t.accountId = a.id
             AND t.deletedAt IS NULL AND t.currencyCode = a.currencyCode
       ), 0) AS balanceMinor
FROM accounts a
WHERE a.userId = :userId AND a.id = :accountId AND a.deletedAt IS NULL
```

Archived accounts still return a balance — archiving hides an account from
pickers, it does not erase its money.

## Data and migrations

No schema change: the balance is derived from existing columns.

## Test plan

Unit (Robolectric, in-memory Room) on the DAO query:

- zero transactions → opening balance
- income adds, expense subtracts (positive and negative results)
- transfer legs move source and destination by the same amount and are not
  counted as income/expense
- same-account transfer pair nets to zero
- soft-deleted rows ignored
- mismatched-currency rows ignored
- another user's rows ignored
- unknown or tombstoned account → null

Unit (JVM) on the repository: session scoping, mapping, `null` on no session,
`null` on DAO failure.

Then the CI-equivalent `:app:assembleAlphaDebug test` and a device smoke run
(no UI consumes the flow yet, so the device check is regression-only).

## Risks and open questions

- Transfer sign convention is a cross-task contract with AR-009. If AR-009 picks
  positive-only legs, this query must switch to a pair-aware implementation.
- Pulled rows guarantee matching currency today; the guard protects against
  legacy or malformed data without failing the whole balance.