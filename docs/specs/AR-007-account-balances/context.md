# Context — AR-007 · Derive account balances

## Starting point

- Base branch / commit: `origin/dev` @ `58277b5c3866391a75a558f6be02857f5589a93a` (2026-11-24)
- Working branch: `feat/AR-007-account-balances`
- Board: issue #8 set to In Progress.

## Relevant code

- `core/database/src/main/java/com/aram/core/database/dao/AccountDao.kt` —
  existing account queries; `findById` pattern for user + tombstone scoping.
- `core/database/src/main/java/com/aram/core/database/dao/TransactionDao.kt` —
  ledger queries; `deletedAt IS NULL` convention, `occurredAt` indexes.
- `core/database/src/main/java/com/aram/core/database/entity/AccountEntity.kt` —
  `openingBalanceMinor`, `currencyCode`, `archived`.
- `core/database/src/main/java/com/aram/core/database/entity/TransactionEntity.kt` —
  `type` string (`INCOME|EXPENSE|TRANSFER`), `amountMinor`, `transferGroupId`,
  `deletedAt`, `pendingPush`.
- `core/model/src/main/java/com/aram/core/model/Account.kt` — domain account.
- `core/data/src/main/java/com/aram/core/data/repository/TransactionRepositoryImpl.kt` —
  session scoping (`session.userId.flatMapLatest`), `catch` → empty/null +
  `flowOn(dispatchers.io)` policy to mirror.
- `core/data/src/main/java/com/aram/core/data/mapper/TransactionMappers.kt` —
  entity → model mappers live here.
- `core/data/src/main/java/com/aram/core/data/di/DataModule.kt` — `@Binds`
  registry for repository interfaces.
- `docs/SPEC.md` §6 — transactions table (`amountMinor` positive,
  `transferGroupId` links the pair) and derived values
  (`balance = opening + Σ income − Σ expense ± transfers`); §8 — transfers
  write both rows atomically.
- Tests to mirror: `core/database/src/test/.../dao/LedgerWriteDaoTest.kt`
  (Robolectric + in-memory Room, `runBlocking`),
  `core/data/src/test/.../repository/TransactionRepositoryImplTest.kt`
  (fakes + `runTest`), `core/data/src/test/.../TestDoubles.kt`
  (`FakeAccountDao`).

## Conventions

- Money is `Long` minor units; never fold currencies together without a rate.
- Read paths never throw to the UI: log and emit empty/null.
- DAO queries always scope by `userId` and exclude `deletedAt IS NOT NULL`.
- Screens/ViewModels are stateless and Hilt-injected; this task adds no UI.
- No analytics, no logging of amounts or account values.

## Dependencies

- `#7` AR-006 soft delete/undo — tombstoned rows must drop out of balances
  (covers via `deletedAt IS NULL`).
- `#10` AR-009 transfers — must write signed `TRANSFER` legs (negative = out)
  for this derivation to stay row-local; noted as a contract in the spec.

## Decisions log

- 2026-11-28 — Balance derived in one Room SQL query rather than reducing
  flows in Kotlin: single source of truth, Room invalidation covers both tables,
  and the per-row `SUM(CASE …)` keeps the flow cheap.
- 2026-11-28 — `TRANSFER` rows are signed (negative leaves the account,
  positive arrives). The pair shares a `transferGroupId`, but a single row
  cannot see its counterpart, so sign is the only row-local direction. AR-009
  must follow this when writing legs; SPEC's "amountMinor always positive"
  applies to income/expense rows.
- 2026-11-28 — Cross-currency guard implemented as exclusion
  (`t.currencyCode = a.currencyCode`) instead of an error; mismatched legacy
  rows never corrupt the sum and the rest of the balance stays observable.
- 2026-11-28 — Archived accounts still report a balance; only tombstoned
  accounts return `null`.

## Verification log

- 2026-11-28 — Focused tests pass: `:core:database:testDebugUnitTest`
  `AccountBalanceDaoTest` 10/10, `:core:data:testDebugUnitTest`
  `AccountBalanceRepositoryImplTest` 4/4.
- 2026-11-28 — CI-equivalent
  `./gradlew :app:assembleAlphaDebug test -PbuildDate=$(date -u +%Y%m%d)`
  → BUILD SUCCESSFUL (574 tasks, cache reuse).
- 2026-11-28 — Device smoke on emulator-5554 (Pixel_9_Pro, Android 16):
  alpha debug installed, cold start, dashboard renders, `LedgerSyncWorker`
  and `SyncOutboxWorker` report SUCCESS, no `FATAL`/`AndroidRuntime` entries.
  No screen consumes the balance flow yet, so the device pass is
  regression-only; balance values were verified by the DAO tests.
- Fix during work: the new repository test's file-level `TestDispatchers` /
  `FakeSessionStore` names collided with the identical private doubles in
  `TransactionRepositoryImplTest.kt` (same package); renamed to
  `BalanceTestDispatchers` / `BalanceFakeSessionStore`.