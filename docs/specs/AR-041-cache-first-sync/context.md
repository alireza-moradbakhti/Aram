# Context — AR-041 · Cache-first repositories with pull-to-refresh

## Starting point

- Base: `feat/AR-005-transactions-list` (stacked), which itself carries the
  ledger data layer from AR-001…AR-005; AR-040 was merged into this branch so
  auth sessions exist here.
- `dev` does not yet have the ledger DAOs, hence stacked PRs.

## Relevant code

- `core/model/.../SyncState.kt` — status enum + state data class.
- `core/domain/.../repository/LedgerSync.kt` — refresh contract.
- `core/datastore/.../SyncMetadataStore.kt`, `SyncMetadataDataStore.kt`.
- `core/database/.../AramDatabase.kt` (v2 + migration), `TransactionDao` sync
  queries, orphan sweeps in `CategoryDao` / `AccountDao`.
- `core/data/.../mapper/LedgerSyncMappers.kt` — DTO → entity, icon/tone maps.
- `core/data/.../repository/LedgerSyncRepositoryImpl.kt`.
- `feature/transactions/.../TransactionsViewModel.kt` and
  `TransactionsScreen.kt` (PullToRefreshBox + SyncBanner).

## Conventions

- Money stays `Long` IRR minor units; DTO amounts are `Double` and rounded.
- No `!!` in `src/main`; no logging of amounts, notes, emails or tokens.
- Screens stay stateless; all sync state comes from the ViewModel.

## Decisions log

- 2026-11-21 — Deletion signal: complete UTC-month page sweep, because the API
  hard-deletes. Incomplete sweeps never tombstone.
- 2026-11-21 — Store sync metadata in its own DataStore file so a cache wipe
  (sign-out) can clear it explicitly.
- 2026-11-21 — Guest sessions and 401s are silent no-ops; failures map to
  OFFLINE / STALE / FAILED for the banner.
- 2026-11-21 — `pendingPush` marks local-only writes so AR-042's outbox can
  drain them and pulls never overwrite them.

## Dependencies

- AR-040 (session/token), AR-005 (ledger DAOs), network DTOs from AR-038.

## Verification log

- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261121` — pass.
- `:core:data:test` — 30 tests pass; new `LedgerSyncRepositoryImplTest` (9
  cases).
- `:feature:transactions:test` — passes with the new LedgerSync fake.
- Device sheet on emulator-5554 (Android 16), stacked integration build:
  - Local profile dashboard rendered cached rows in IRR; pull-to-refresh and the
    settings "Sync now" action both reached `GET /api/transactions` and updated
    the list.
  - Offline start with airplane mode: cached rows stayed visible, sync banner
    shown, no crash.
- No fixes were needed in this module; the device-sheet fixes landed in AR-040
  (currency), AR-042 (outbox retry), AR-043 (post-sign-in pull) and AR-046
  (dashboard padding).
