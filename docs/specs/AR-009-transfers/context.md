# Context — AR-009 · Transfer money between accounts atomically

## Starting point

- Base branch / commit: `origin/dev` @ `58277b5c3866391a75a558f6be02857f5589a93a`
- Working branch: `feat/AR-009-transfers`
- Queue note: AR-008 (#9) skipped — its "row → edit" acceptance needs AR-006's edit sheet (PR #76).

## Relevant code

- `core/domain/.../transaction/TransactionDraft.kt`, `TransactionValidation.kt`,
  `TransactionValidator.kt` — draft/validation patterns to mirror; the validator
  already rejects a category on `TRANSFER`.
- `core/domain/.../usecase/CreateTransaction.kt` — lookup → validate → write flow.
- `core/domain/.../repository/TransactionRepository.kt` — repository contract.
- `core/data/.../repository/TransactionRepositoryImpl.kt` — session scoping,
  `safeOutcome`, outbox enqueue, `softDelete`.
- `core/database/.../dao/LedgerWriteDao.kt` — atomic write + outbox pattern.
- `core/database/.../dao/TransactionDao.kt` — `softDeleteMissing` /
  `softDeleteAllInRange` month sweeps spare only `pendingPush` rows.
- `core/database/.../entity/TransactionEntity.kt` — `transferGroupId`,
  `pendingPush`.
- `feature/transactions/.../presentation/AddTransactionViewModel.kt` — ignores
  `TRANSFER` today (`onTypeChange` returns early).
- `feature/transactions/.../presentation/TransactionsViewModel.kt` —
  `buildState` totals sum every non-INCOME row.
- AR-007 (`feat/AR-007-account-balances`, 5202e0c) — balance is
  `opening + income − expense + signed transfer`; legs must be signed.
- Design: `Designs/Aram/index.html` lines 352–391 (segmented
  `Expense | Income | Transfer`), 1785–1796 (transfer row in the ledger,
  day total includes the outflow).

## Conventions

- Money as `Long` minor units; never mix currencies without a rate.
- Room writes that must be atomic go through `LedgerWriteDao`.
- DAO queries scope `userId` and exclude tombstones; parameters only.
- No logging of amounts, notes, or account names; no analytics.
- Screens stateless; ViewModel exposes one `StateFlow`.

## Dependencies

- AR-007 defines the signed-leg contract; not merged yet, so the balance AC is
  proven at the data level (legs are exact negatives and the derivation is
  pure arithmetic).
- AR-006 provides the delete UI; this task still makes `softDelete(id)` cascade
  so the group rule holds when that lands.

## Decisions log

- 2026-11-28 — Legs are signed (`source = −amount`, `destination = +amount`)
  per AR-007; a single row cannot see its pair, so sign is the direction.
- 2026-11-28 — Legs are **not** pushed: the backend accepts only
  `expense|income`, so a sync push would corrupt server totals. The monthly
  sweep gains `AND transferGroupId IS NULL` so pulled months never tombstone
  local-only legs.
- 2026-11-28 — Group delete is a single `UPDATE ... WHERE transferGroupId = :g`
  and queues no server deletes (the server never saw the rows).
- 2026-11-28 — The Activity list collapses each group to its source leg; the
  detail names the destination account (`Transfer · 10:00 · Card`), matching
  the prototype's row. Day totals keep the outflow; SPENT/INCOME and their
  deltas exclude transfers.
- 2026-11-28 — Type selection reuses the existing segmented control with a
  third option rather than a separate transfer screen; the destination picker
  reuses `AccountPicker`.
- 2026-11-28 — Currency mismatch disables the CTA and shows an inline hint;
  the alpha mock gives all accounts one currency, so that state is covered by
  unit/screen tests and the preview rather than the emulator.

## Verification log

- Focused tests (all pass):
  - `:core:domain:test` — TransferValidatorTest 9/9, CreateTransferTest 6/6
    (new); TransactionValidatorTest 14/14, CreateTransactionTest 5/5,
    Update/Delete 4/4 unchanged.
  - `:core:database:testDebugUnitTest` — LedgerWriteDaoTest 9/9 (4 new:
    pair written together, rollback via SQLite trigger, group delete,
    sweep spares legs), SyncOutboxDaoTest 10/10, migration 2/2.
  - `:core:data:testDebugUnitTest` — TransactionRepositoryImplTest 17/17
    (3 new: signed pair + no outbox/schedule, unauthorized, group delete);
    outbox 9/9, sync 16/16, auth 10/10.
  - `:feature:transactions:testDebugUnitTest` — AddTransactionViewModelTest
    14/14 (5 new), TransactionsViewModelTest 16/16 (2 new),
    TransactionsScreenTest 3/3 unchanged.
- CI-equivalent: `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261128`
  — BUILD SUCCESSFUL.
- Device: emulator-5554 (Pixel 9 Pro, Android 16), light and dark.
  - Add sheet shows Expense | Income | Transfer; Transfer switches the title to
    "Transfer money", hides categories, shows From/To and a "Transfer" CTA.
  - Cash → Card, 500,000 IRR, note "Move to card" → saved; Activity shows one
    outflow row `Move to card · Transfer · 05:56 · Card`; the day total
    (−555,000) includes the outflow, SPENT (18,644,000) and INCOME are
    unchanged; Expenses scope includes the row, Income scope does not.
  - Destination picker excludes the source account; changing the source to the
    current destination clears To ("Choose account") and disables the CTA.
  - Relaunch after `am force-stop`: the pair is still one row with the same
    totals.
  - Delete via repository is covered by DAO/repository tests (rows have no
    tap-to-delete UI on `dev` until AR-006).
  - logcat: no crashes; no amount/note text logged; WorkManager sync succeeds.