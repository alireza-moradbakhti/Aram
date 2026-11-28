# AR-009 · Transfer money between accounts atomically

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/10
- Phase / Sprint: Phase 1 · Transactions core / Sprint 2
- Estimate: 3 pts · P0
- Branch: `feat/AR-009-transfers`
- Status: Done

## Problem

Moving money between the user's own accounts must never create or destroy value. A transfer is two ledger rows (out of one account, into another) that share a `transferGroupId`, and both must land or neither may.

## Goals / non-goals

### Goals
- Write both legs inside one Room transaction; any failure rolls both back.
- Require both accounts to use the same currency and explain the limit in the UI.
- Keep transfer legs out of income/expense summaries.
- Deleting one leg soft-deletes the whole group.

### Non-goals
- Cross-currency transfers with an FX rate (backend has no rates yet).
- Pushing transfers to the API: the backend models only `expense`/`income`, so legs stay device-local for now.
- Editing a transfer pair (edit sheet arrives with AR-006).

## Acceptance criteria

- [x] A failure rolls back both rows.
- [x] Both account balances move by the same amount.
- [x] Transfer rows never appear in income/expense summaries.

## Technical design

- `TransferDraft` (from/to account, positive amount, currency, occurredAt, note).
- `TransferValidator` — amount > 0, distinct accounts, both exist and are not archived, same currency. Problems are a dedicated `TransferProblem` sealed type (`AmountNotPositive`, `SameAccount`, `AccountNotFound`, `AccountArchived`, `CurrencyMismatch`) keyed by `TransactionField`, which gains `TO_ACCOUNT`.
- `CreateTransfer` use case resolves both accounts through `LedgerLookup`, validates, then calls `TransactionRepository.createTransfer`.
- `TransactionRepositoryImpl.createTransfer` writes two signed `TRANSFER` legs in one `LedgerWriteDao` transaction:
  - source: `amountMinor = -amount`, destination: `amountMinor = +amount`, same `transferGroupId`, `categoryId = null`, `pendingPush = false`.
  - Signed legs are the AR-007 balance contract: opening + income − expense + Σ transfer.
- No outbox entry is written for either leg (the API cannot represent a transfer); the monthly server sweep skips `transferGroupId IS NOT NULL` rows so pulled months do not tombstone them.
- `softDelete(id)` cascades: a leg with a group id tombstones every non-deleted row of the group in one statement, without queueing server deletes.
- Activity list collapses a group to its source leg and shows the destination account in the row detail; day totals include the outflow, the SPENT/INCOME stats and their deltas exclude transfers.
- Add sheet gains a third segmented option (`Expense | Income | Transfer`, matching the prototype), a From/To account pair, a currency-mismatch hint, and a `Transfer` CTA.

## Data and migrations

- No schema change: `transferGroupId` and signed `amountMinor` already exist.

## Test plan

- `TransferValidatorTest` — pure rules.
- `LedgerWriteDaoTest` — both legs written together; a mid-write failure rolls both back (SQLite trigger); group delete tombstones both.
- `TransactionDaoTest`/`LedgerWriteDaoTest` — the month tombstone sweep spares transfer legs.
- `TransactionRepositoryImplTest` — signed legs, shared group, no outbox, no worker schedule, unauthorized without session, group delete.
- `AddTransactionViewModelTest` — transfer tab, destination selection, currency/same-account guards, save path.
- `TransactionsViewModelTest` — totals exclude transfers; a pair renders as one outflow row.
- Compose: transfer tab hides categories and shows the To row; mismatch hint. Covered by the `TransferScreenPreview` and device checks; no new Compose test was added for the sheet.

## Verification

- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261128` — pass.
- Device: emulator-5554 (Pixel 9 Pro, Android 16), light + dark; transfer saved,
  collapses to one row, stats exclude it, survives force-stop restart.

## Risks and open questions

- Cross-device: local-only legs do not sync. Documented limitation until the API gains a transfer kind.
- The alpha mock gives every account the user's currency, so the mismatch state is verifiable by unit/screen tests and previews, not on the emulator.