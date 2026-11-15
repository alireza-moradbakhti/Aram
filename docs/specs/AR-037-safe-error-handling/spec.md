# AR-037 · Safe error handling — no crashes, no freezes

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/38
- Phase / Sprint: Phase 1 · Transactions core / Sprint 1
- Estimate: 5 pts · P0 · Size L
- Branch: `chore/AR-037-safe-error-handling`
- Status: In review (device test pending)

## Problem

- Failures are untyped: `WelcomeViewModel` uses `runCatching`, which also swallows
  `CancellationException` and collapses every failure into one boolean.
- No shared success/failure type crosses the repository boundary, so AR-001+ would
  invent their own error handling per feature.
- The session gate models only Loading / LoggedOut / Ready; a database failure
  has nowhere to go.
- There is no global crash handler and no recovery path after an uncaught
  exception.
- Nothing prevents `!!` from creeping back into main sources.

## Goals / Non-goals

**Goals**

- One failure taxonomy (`AppError`) and one result type (`Outcome<T>`) in pure
  Kotlin, shared by domain, data, and UI.
- One presentation state (`UiState<T>`) and one calm `ErrorState` composable.
- Every existing repository returns `Outcome`; every existing ViewModel renders
  loading, content, empty, and error without throwing.
- `CancellationException` is always rethrown.
- A global uncaught-exception handler logs once, marks the crash, and the next
  launch shows a recovery screen.
- A static check fails the build when `!!` appears in main sources.

**Non-goals**

- No Ktor client, mock API, sync, or outbox (AR-038 … AR-044).
- No retry queue; retry is a user action.
- No analytics or crash SDK.
- No screen redesign beyond adding explicit states.
- No DI migration; Hilt stays (decision locked 2026-11-16).

## Acceptance criteria

Copied from the issue; ticked as they land.

- [x] `AppError`, `Outcome`, `UiState`, and the throwable mapper live in core modules with unit tests.
- [x] `AppError` covers every error code in `docs/API_CONTRACT.md`; 5xx carries `requestId`, 429 carries `retryAfterSeconds`.
- [x] Every repository returns typed outcomes; no raw exception escapes to a ViewModel.
- [x] Every ViewModel exposes `Loading` / `Content` / `Empty` / `Error` / `Offline` and renders them.
- [x] A shared `ErrorState` composable shows a calm message plus retry; no stack traces in UI.
- [x] No `!!` in `main` source sets; `CancellationException` is rethrown everywhere.
- [x] Uncaught exceptions are logged via `Logger` and reported once, not twice.
- [ ] Cold start, rotation, rapid tab switching, and a 10-minute monkey run produce no crash or ANR on a real device.
- [x] `./gradlew :app:assembleAlphaDebug test` and CI are green.

**Interpretation notes (confirmed in the plan review):**

- The only content ViewModel today is `MainViewModel`; it maps to
  `UiState<UserProfile?>`. Form screens (Welcome) use `SubmitState`
  (Idle / Submitting / Failed(AppError)) because a form must stay visible while
  an inline error is shown — `UiState.Error` would hide the fields.
- There is no network yet, so `Offline` is modeled as `AppError.Offline` and
  rendered by `ErrorState` with offline copy. Cached-content + offline banner
  arrives with AR-041; the device script verifies airplane mode does not break
  the app.

## Technical design

### Module placement

```
core/model      AppError.kt, Outcome.kt            (pure Kotlin, KMP-ready)
core/common     ErrorMapping.kt                    (Throwable → AppError, safeOutcome)
core/designsystem  state/UiState.kt, state/SubmitState.kt, ErrorState.kt
core/domain     AuthRepository.kt                  (returns Outcome)
core/data       AuthRepositoryImpl.kt              (+ Logger, Storage mapping)
app             CrashMarker.kt, AramCrashHandler.kt, RecoveryScreen.kt,
                MainViewModel.kt, MainActivity.kt, AramApp.kt
feature/onboarding  WelcomeViewModel/UiState/Screen (SubmitState)
feature/settings    debug-only crash trigger
root build.gradle.kts  verifyNoNotNullAssertions task
```

`core/designsystem` gains `api(project(":core:model"))` because `ErrorState`
exposes `AppError`; features already depend on the design system.

### AppError (core/model)

```kotlin
sealed interface AppError {
    data class Validation(val fields: Map<String, String> = emptyMap()) : AppError
    data object Unauthorized : AppError
    data object Forbidden : AppError
    data object NotFound : AppError
    data class Conflict(val message: String? = null) : AppError
    data class RateLimited(val retryAfterSeconds: Long? = null) : AppError
    data object Offline : AppError
    data object Timeout : AppError
    data class Server(val requestId: String? = null) : AppError
    data class Storage(val cause: Throwable? = null) : AppError
    data object Permission : AppError
    data class Unknown(val cause: Throwable? = null) : AppError
}
```

Kinds mirror `docs/API_CONTRACT.md` so AR-038 maps `ApiError` 1:1 without a
second taxonomy. KMP rule: no Android or JVM types in this file.

### Outcome (core/model)

```kotlin
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}
```

### Error mapping + logging (core/common)

- `Throwable.toAppError()` — `IllegalArgumentException` → `Validation`,
  `SecurityException` → `Permission`, everything else → `Unknown`.
- `safeOutcome(component, operation, logger, mapError = …, block)` —
  `CancellationException` rethrown, other throwables logged once
  (`logger.e(component, "…", throwable)`) and mapped. `mapError` lets
  `:core:data` add platform kinds without teaching `:core:common` about Android.
- Storage mapping stays in `:core:data`: `SQLiteException` → `AppError.Storage`
  (Android type cannot enter `:core:common`).

### UiState + ErrorState (core/designsystem)

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val error: AppError) : UiState<Nothing>
}
```

`ErrorState(error, onRetry)` maps each `AppError` kind to calm copy via string
resources (offline, timeout, auth, validation, server, unknown) and shows a
retry button when a callback is supplied. Never renders a throwable message.

### Session gate

- `AuthRepository.observeSession(): Flow<Outcome<UserProfile?>>` —
  `Success(null)` = logged out, `Success(profile)` = ready, `Failure` = error.
- `AuthRepository.createLocalProfile(...): Outcome<UserProfile>` — blank name
  returns `Failure(Validation)`; DB failures map to `Storage`.
- `MainViewModel` re-subscribes via a retry trigger and exposes
  `StateFlow<UiState<UserProfile>>` (`Empty` means logged out); `retry()`
  re-collects.
- `AramApp`: Loading → splash, Empty → Welcome, Content → main scaffold,
  Error → `ErrorState(onRetry = viewModel::retry)`.

### Crash safety net (app)

- `CrashMarker` (SharedPreferences, synchronous `commit()` in the handler):
  `mark()`, `hasPendingCrash()`, `clear()`.
- `AramCrashHandler` wraps the platform default handler: logs once
  (`sendToServer = true`, double-report guarded), marks the crash, delegates so
  the process still dies normally.
- Installed in `AramApplication.onCreate` after Hilt injection.
- `MainActivity` checks the marker before showing content; if set, it shows
  `RecoveryScreen` ("Aram closed unexpectedly. Your data is safe.") with a
  Continue action that clears the marker and recreates the activity.
- Debug-only crash trigger in Settings (`showDebugActions = BuildConfig.DEBUG`)
  so the path is testable on a device.

### Static guard

Root `build.gradle.kts` registers `verifyNoNotNullAssertions`: scans
`app/`, `core/`, `feature/` `src/main/**/*.kt`, fails with the offending
file:line list, and every `Test` task depends on it, so the CI command
(`:app:assembleAlphaDebug test`) enforces it. Zero new dependencies; it is a
text scan, so code review still applies.

## Data and migrations

None. No schema change; no new storage.

## Test plan

| Layer | Check |
| --- | --- |
| Unit (`core/common`) | mapper kinds; `safeOutcome` rethrows `CancellationException`, logs once, returns `Failure` |
| Unit (`app`) | `MainViewModel`: Loading → Empty / Content / Error, retry re-subscribes |
| Unit (`feature:onboarding`) | blank name → field error; repo `Failure` → `SubmitState.Failed`; no exception thrown |
| Static | `verifyNoNotNullAssertions` fails on a seeded `!!`, passes on the repo |
| Build | `./gradlew :app:assembleAlphaDebug test` |
| Device | crash trigger → one log event → recovery screen → Continue; rotate, rapid tabs, airplane mode |

## Risks and open questions

- Offline is `AppError.Offline` until the network layer exists; the cached-data
  banner is AR-041 scope.
- The `!!` scan is best-effort text matching (comments/strings stripped
  crudely); a proper lint rule can replace it later without changing the gate.
- Crash marker persistence uses `commit()` on the crashing thread — small
  blocking write, intentionally chosen over `apply()` so the marker survives
  process death.
- `MainViewModel` retry re-collects the DataStore flow; if the failure is
  persistent the screen stays on Error with a working retry, never a spinner.
