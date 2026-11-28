# Context — AR-006 · Edit and delete transactions with undo

## Starting point

- Base branch: `dev` @ `58277b5`
- Branch: `feat/AR-006-edit-delete-undo`

## Relevant code

- `core/domain/.../repository/TransactionRepository.kt` — `create`, `update`, `softDelete`; no restore yet.
- `core/domain/.../usecase/UpdateTransaction.kt`, `DeleteTransaction.kt` — validated writes.
- `core/database/.../dao/TransactionDao.kt` — `findById` filters tombstones; `softDelete` (line ~100).
- `core/database/.../dao/LedgerWriteDao.kt` — atomic row + outbox writes (`deleteWithOutbox`, line ~87).
- `core/data/.../repository/TransactionRepositoryImpl.kt` — `update` (line 132), `softDelete` (line 188), `toStorageAwareError` maps `RowNotFound` → `AppError.NotFound`.
- `core/data/.../outbox/OutboxPayloads.kt` — `outboxEntry` and `categorySlug`.
- `feature/transactions/.../AddTransactionViewModel.kt` — create-only, no SavedStateHandle yet.
- `feature/transactions/.../AddTransactionScreen.kt` — sheet UI, header hardcodes "Add …".
- `feature/transactions/.../TransactionsScreen.kt` — `TransactionRow` is not clickable.
- `app/.../navigation/AramApp.kt` — single `add-transaction` route (line 228), shared snadbar (line 157), `isAddRoute`.
- `core/designsystem/.../component/Toast.kt` — text-only toast; needs optional action.
- `feature/settings/.../SettingsScreen.kt` — `LogoutDialog` is the visual template for the delete confirmation.
- Tests: `core/data/src/test/.../TestDoubles.kt` fakes (`FakeTransactionDao` line 124, `FakeLedgerWriteDao` line 421); `core/domain/.../TransactionTestData.kt` fake repository; `AddTransactionViewModelTest` local fake.

## Conventions

- Money is `Long` minor units; prefill text via `Money.majorUnits().setScale(fractionDigits).toPlainString()`.
- No analytics, no logging of amounts/notes/emails, no `!!` in `src/main`.
- Stateless screens; ViewModels own state; Compose previews for light and dark.
- Soft delete is a tombstone for sync; never hard-delete ledger rows.

## Dependencies

- Spring/design: no new designs for edit/delete; reuse the sheet and the logout dialog styling from the prototype.
- Backend: `UPDATE` push is an upsert, so restore-after-push is safe.

## Decisions log

- Delete lives inside the edit sheet (row tap → sheet → Delete) — the prototype has no swipe gesture.
- Undo is hosted by the activity-scoped `TransactionUndoViewModel` so the snackbar survives the sheet pop.
- Edit preserves the original time-of-day; only the date can change.
- Restoring a row that is not deleted is a no-op success; unknown ids are NotFound.

## Verification log

- 2026-11-28 — focused tests `:core:domain:test :core:data:test :core:database:test :feature:transactions:test` — pass (restore use case, DAO tombstone restore, repository restore, VM prefill/edit/delete tests, row-click test).
- 2026-11-28 — CI-equivalent `:app:assembleAlphaDebug test` — pass. Fixed two findings: Hilt bindings for `UpdateTransaction`, `DeleteTransaction`, `RestoreTransaction` in `DomainModule`; redundant elvis on smart-cast `transactionId` in `AddTransactionViewModel`.
- 2026-11-28 — device pass on Pixel_9_Pro (Android 16, alpha debug, mock API signed in as demo user):
  - Row tap opens "Edit expense" prefilled (amount 1,100,000 IRR, Transport, Auto-pay, 21 Nov, note Fuel); changing the amount to 1,250,000 and saving updates the same row (no duplicate), totals refresh, time-of-day 15:47 preserved, "Changes saved" toast.
  - Delete opens the styled confirmation dialog; confirming removes the row, updates totals, and shows "Transaction deleted / Undo". Undo restores the row immediately and the outbox pushes `UPDATE` (log: `POST /api/sync/push -> 200` right after `DELETE -> 200`).
  - Server-error scenario: delete still persists locally and the banner shows "N changes waiting to sync" with Sync now; undo while failing restores the row with the "Queued" chip; switching back to Normal + Sync now drains the outbox (`DELETE -> 200`, `POST /api/sync/push -> 200`) and the banner clears.
  - Light and dark both checked for sheet, dialog, snackbar, and list. Add-expense flow still works (55,000 row saved, "Transaction saved").