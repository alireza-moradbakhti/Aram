# AR-006 · Edit and delete transactions with undo

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/7
- Phase / Sprint: Phase 1 · Transactions core / Sprint 2
- Estimate: 3 pts · P0
- Branch: feat/AR-006-edit-delete-undo
- Status: Done

## Problem

A saved transaction is currently immutable. Users cannot fix a typo, change the category, or remove a duplicate, which makes the ledger untrustworthy.

## Goals

- Tap a ledger row to reopen it in the existing transaction sheet with every field prefilled.
- Save edits through `UpdateTransaction`, keeping the row id (no duplicates) and refreshing `updatedAt`.
- Delete from the edit sheet behind a confirmation dialog; after deletion show a snackbar with **Undo**.
- Undo clears the tombstone (`deletedAt = null`), sets a fresh `updatedAt`, and queues an `UPDATE` push.

## Non-goals

- Swipe-to-delete gestures, bulk delete, category management.
- Editing transfers (transfers are not created by this form yet).

## Acceptance criteria

- [x] Editing persists all changed fields and timestamps.
- [x] Undo restores the exact row and removes the deletion state.
- [x] No duplicate rows are created by edit.

## Technical design

### Domain

- `TransactionRepository.restore(id): Outcome<Unit>` — tombstone reversal, documented alongside `softDelete`.
- `RestoreTransaction` use case delegating to the repository (mirrors `DeleteTransaction`).

### Database

- `TransactionDao.findByIdIncludingDeleted(userId, id)` — needed to build the outbox payload from a tombstone.
- `TransactionDao.restore(userId, id, updatedAt)` — `deletedAt = NULL`, `updatedAt = now`, `pendingPush = 1`, guarded by `deletedAt IS NOT NULL`.
- `LedgerWriteDao.restoreWithOutbox(...)` — atomic restore + `UPDATE` outbox insert. No schema change, so no migration.

### Data

- `TransactionRepositoryImpl.restore` reads the tombstone, enqueues an `OP_UPDATE` with the restored entity, schedules the outbox worker. Restoring a live row is a successful no-op; an unknown id is `AppError.NotFound`.

### Feature — transactions

- `AddTransactionViewModel` accepts an optional `transactionId` nav argument through `SavedStateHandle`.
  - Prefills type, amount, account, category, date, note, and the original time-of-day.
  - Saves with `UpdateTransaction` when editing, `CreateTransaction` otherwise.
  - Exposes `transactionId`, `deleted`, `deleteFailed`; `onDeleteConfirmed()` calls `DeleteTransaction`.
- `AddTransactionScreen` renders "Edit expense/income", "Save changes", and a danger "Delete transaction" action plus a confirmation dialog matching the existing logout dialog.
- `TransactionsScreen` rows are clickable and raise `onOpenTransaction(id)`.

### App

- Route `transaction/{transactionId}` reuses `AddTransactionRoute`; the bottom bar and FAB stay hidden for both sheet routes.
- `MainActivity` owns `TransactionUndoViewModel` (restore use case); after deletion `MainScaffold` shows a snackbar with **Undo** and calls it on action.
- `AramToast` gains an optional action label/callback; the existing look is unchanged when absent.

## Data and migrations

None — no column changes.

## Test plan

- Domain: `RestoreTransactionTest` delegates and propagates failures.
- Data: repository test — delete → restore clears the tombstone, bumps `updatedAt`, queues one `UPDATE`; restore of a live row queues nothing; unknown id is NotFound.
- Database: Robolectric `LedgerWriteDaoTest` — restore writes the row and one outbox entry; restoring a live row queues nothing.
- Feature: ViewModel tests for prefill, edit-save, delete, and delete-failure; screen test for row click.
- Manual: emulator, light + dark, delete-undo after process background, offline queue.

## Risks and open questions

- Undo after the delete has already been pushed: the restored row is re-upserted by `UPDATE`, which the API treats as create-or-update.
- Category or account archived between edit open and save: the form keeps the stored id; validation rejects only if the directory entry is missing.