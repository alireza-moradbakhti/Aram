# Context — AR-037 · Safe error handling

## Starting point

- Base branch: `dev`
- Base commit: `285dae4` (docs PR #49: stage/prod environments, DI decision, network gate)
- Sprint 1, first task; nothing blocks it (AR-038 depends on this task).

## Relevant code

| Path | Symbol | Note |
| --- | --- | --- |
| `app/src/main/java/com/aram/app/AramApplication.kt:9` | `AramApplication` | `@HiltAndroidApp`; injects `Logger`; install the crash handler here |
| `app/src/main/java/com/aram/app/MainActivity.kt:18` | `MainActivity` | splash gate keyed on `SessionState.Loading`; add crash-marker branch |
| `app/src/main/java/com/aram/app/session/MainViewModel.kt:14` | `SessionState` | replace with `UiState<UserProfile>` + retry trigger |
| `app/src/main/java/com/aram/app/navigation/AramApp.kt:43` | `AramApp` | `when (sessionState)`; add Error branch and `showDebugActions` |
| `feature/onboarding/src/main/java/com/aram/feature/onboarding/presentation/WelcomeViewModel.kt:37` | `runCatching` | swallows `CancellationException`; replace with `Outcome` handling |
| `feature/onboarding/.../WelcomeUiState.kt:3` | `WelcomeUiState` | add `SubmitState`, drop `isSubmitting`/`showSaveError` |
| `feature/onboarding/.../WelcomeScreen.kt:124` | supporting text | map `SubmitState.Failed` to the existing save-error string |
| `feature/settings/.../SettingsScreen.kt:20` | `SettingsScreen` | add `showDebugActions` + crash trigger |
| `core/domain/src/main/java/com/aram/core/domain/repository/AuthRepository.kt:6` | `AuthRepository` | change returns to `Flow<Outcome<UserProfile?>>` / `Outcome<UserProfile>` |
| `core/data/.../repository/AuthRepositoryImpl.kt:34` | `observeSession` | wrap with `catch`; log once; map DB errors |
| `core/logging/.../Logger.kt:15` | `Logger` | injected by Hilt; `e(name, message, throwable)` |
| `core/common/.../CoroutineDispatchers.kt` | `CoroutineDispatchers` | IO dispatcher already provided |
| `root build.gradle.kts` | — | register `verifyNoNotNullAssertions`, wire to `Test` tasks |
| `docs/SPEC.md:389` | Testing strategy | add error-handling row; document uncatchable failures |
| `docs/SPEC.md:442` | Privacy & security | no sensitive data in crash logs (path only) |

Existing patterns to follow: sealed interfaces for state (`SessionState`,
`LogLevel`), constructor injection, Hilt view models, stateless `*Screen` +
`*Route`.

## Conventions

- Features depend on `:core:domain`, `:core:model`, `:core:designsystem` — never
  on `:core:data`/`:core:database`.
- KMP guardrail: no Android or `java.*` imports in `model`, `common`, `logging`,
  `domain`, `network`.
- Screens stay stateless; all state is immutable and owned by a ViewModel.
- Money stays `Long` minor units (untouched here).
- No analytics/tracking; logs carry names and messages, never amounts, emails,
  tokens, or notes.
- Branch prefix `chore/` (issue type Task); PR title
  `type(area): summary (AR-037)`.

## Dependencies

- Upstream: none — this is the first Sprint 1 task.
- Downstream: AR-038 (`ApiError` → `AppError` 1:1), AR-040 (session/401 uses
  `AppError.Unauthorized`), AR-041 (`UiState` offline/stale), AR-042 (outbox
  retry classification uses `AppError` kinds).
- No new third-party libraries planned.

## Decisions log

| Date | Decision | Why |
| --- | --- | --- |
| 2026-11-16 | `AppError` + `Outcome` in `:core:model`; `UiState` in `:core:designsystem` | pure types shared by data/domain/UI; UI contract lives with the design system |
| 2026-11-16 | Form screens use `SubmitState` instead of `UiState` | an error must not hide the form fields; `UiState` is for content surfaces |
| 2026-11-16 | `Offline` is an `AppError` kind for now | no network layer exists before AR-038; banner handling lands in AR-041 |
| 2026-11-16 | Custom Gradle scan instead of Detekt | zero new dependencies and no plugin/Kotlin-version risk; revisit if more lint rules are needed |
| 2026-11-16 | Crash marker uses `SharedPreferences.commit()` | the process is dying; async `apply()` may be lost |
| 2026-11-16 | Session state is `UiState<UserProfile>`, not `UiState<UserProfile?>` | `Empty` already means "no profile"; a nullable `Content` would force null checks in the shell |
| 2026-11-16 | `safeOutcome` takes an optional `mapError` | `:core:common` cannot see `SQLiteException`; `:core:data` supplies the storage-aware mapper |
| 2026-11-16 | `SubmitState` lives in `:core:designsystem/state` | every form feature needs it; keeping it next to `UiState` avoids per-feature copies |

## Verification log

- `./gradlew :core:common:test :app:testAlphaDebugUnitTest :feature:onboarding:testDebugUnitTest verifyNoNotNullAssertions` — pass (13 new unit tests).
- Static check seeded with `x!!.length` in `app/src/main/.../SeedNotNull.kt` — failed with `app/src/main/java/com/aram/app/SeedNotNull.kt:3`; seed removed.
- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261116` — pass in 1m21s; APK `aram-alpha-debug-1-20261115.apk` (14 MB; suffix is the UTC date).
- Device run pending.

## Post-review adjustments (2026-11-16)

- Alpha launcher label is `Aram` (was `Aram Alpha`); the alpha icon adds an `α` badge via `app/src/alpha/res/drawable/ic_launcher_foreground.xml`. Application id and the other flavors are unchanged.
- `RecoveryScreen` now renders inside a `Surface` with the theme background; previously the light window background showed through in dark mode.
- Design-fidelity pass (bundled at the user's request, scope "make everything like the design" against `Designs/Aram`):
  - Bundled Roboto (400/500/600/700) in `core/designsystem`; `LocalContentColor` provided by `AramTheme`.
  - Local stroked icon set `AramIcons` (43 icons, 24-unit viewBox, 1.75 stroke) replaces Material icons.
  - Rebuilt Welcome, Dashboard, Activity (+ add-expense sheet), Budgets, Profile, Manage-account sheet, logout dialog, FAB, and bottom bar to the prototype's tokens, spacing, and typography.
  - Add-expense sheet now has the underlined amount with focus tint, FX preview (`≈ $ · rate locked`), icon category chips, account/date meta rows, and the design note field.
  - Profile logout now uses a design dialog (tinted icon square, ghost Cancel + primary confirm) instead of Material `AlertDialog`.

## Verification log (design pass)

- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261116` — BUILD SUCCESSFUL (475 tasks).
- Device (Galaxy A53 5G, Android 16): Welcome → onboarding works; Dashboard/Activity/Budgets/Profile match the prototype in dark; light mode checked on Dashboard and the add-expense sheet; save prepends a row and updates totals; Profile → Manage account → Log out → dialog → Welcome; relaunch after Simulate crash shows the dark recovery screen and Continue returns to the shell.

## Verification log (final device run, 2026-11-16)

Galaxy A53 5G, Android 16, wireless adb; APK `aram-alpha-debug-1-20261116.apk`.

- Fresh install (`adb uninstall` + `adb install`): cold start 1.8 s, Welcome screen, no recovery screen.
- Empty-name submit: inline "Enter your name to continue", no crash.
- Name entry → Dashboard: greeting and safe-to-spend render.
- Rapid tab switching (24 taps over 4 tabs): same PID, no ANR, no fatal logs.
- Rotation: Home and Activity render in landscape without restart; FAB slightly overlaps the "22 days left" chip on Home and the INCOME card edge on Activity (cosmetic, portrait unaffected). Auto-rotate restored.
- Airplane mode skipped: adb runs over Wi-Fi and the app has no `INTERNET` permission, so connectivity cannot affect it.
- Clean relaunch (force-stop → cold start): Dashboard directly, no recovery screen.
- Session `logcat`: 0 `FATAL EXCEPTION`, 0 `ANR in com.aram`.
- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261116` — BUILD SUCCESSFUL.
