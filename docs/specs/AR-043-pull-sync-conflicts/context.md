# Context — AR-043 · Pull sync, conflicts, and tombstones

## Starting point

- Base: `feat/AR-042-outbox-sync-worker` after the AR-042 commit (PR #68).
- AR-042 delivered the outbox (`OutboxSyncImpl`, `SyncOutboxWorker`) and the
  push endpoint; `LedgerSyncRepositoryImpl` still pulled one month only.

## Relevant code

- `core/datastore/SyncMetadataStore.kt` — added `cursor` to `SyncMetadata`.
- `core/datastore/SyncMetadataDataStore.kt` — `sync_cursor` preference.
- `core/domain/repository/LedgerSync.kt` — `syncNow(): Boolean`.
- `core/data/repository/LedgerSyncRepositoryImpl.kt` — `runSync`, delta pull,
  `upsertPreservingLocal`, `CURSOR_PULL_CAP`.
- `core/data/sync/LedgerSyncWorker.kt`, `core/data/sync/SyncScheduler.kt`.
- `app/AramApplication.kt` — periodic + one-shot scheduling on start.
- `feature/settings/.../SettingsViewModel.kt`, `SettingsRoute.kt`,
  `SettingsScreen.kt` — live status + "Sync now".
- `core/network/mock/MockApiStore.kt` — `syncPull(since)` filters by
  `clientUpdatedAt`; `syncPush` and `deleteTransaction` used by the
  two-device test.

## Conventions

- Reads stay on Room; repository only decides when to pull.
- Money is minor units (`Long`); no amounts in logs.
- Stateless screens + a Hilt ViewModel per feature; `collectAsStateWithLifecycle`.
- No analytics, no `!!` in `src/main` (`verifyNoNotNullAssertions`).

## Dependencies

- `feature:settings` now depends on `:core:domain`, `:core:model`,
  lifecycle, coroutines, Hilt (KSP).
- `androidx.hilt:hilt-work`/`hilt-compiler` and WorkManager already in the app.

## Decisions log

- 2026-11-22 — Cursor held on a 500-row delta because the backend has no
  paging; re-pulling is correct but wasteful until paging lands.
- 2026-11-22 — Push-first inside the same mutex as the pull so a refresh cannot
  observe half-pushed state; `refresh()` and `syncNow()` share `runSync`.
- 2026-11-22 — Settings shows status in the existing profile header line
  instead of adding a new group, preserving the design source.
- 2026-11-22 — Relative time ("Synced 5m ago") computed in the ViewModel from
  the injected clock at each emission.

## Verification log

- `:core:data:testDebugUnitTest --tests LedgerSyncRepositoryImplTest` — pass
  (new ordering/cursor/convergence tests included).
- `:feature:settings:testDebugUnitTest :feature:transactions:testDebugUnitTest`
  — pass.
- Full CI-equivalent run recorded after the final `assembleAlphaDebug test`.
