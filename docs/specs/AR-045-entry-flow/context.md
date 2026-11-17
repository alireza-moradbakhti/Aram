# Context — AR-045 · Entry flow

## Starting point

- Base: `dev` @ `400e1a5` (AR-038 merged).
- Design reference: `C:\Users\AshenOne\Desktop\Project\Designs\Aram` (updated
  2026-11-16 18:59, after the AR-037 fidelity pass).
- Prototype phones: `#splashPhone`, `#welcomePhone`, `#obPhone`, sign-up card in
  `#cat-auth`, dashboard `#dashboardPhone`, activity `#activityPhone`.

## Relevant code

- `app/src/main/java/com/aram/app/navigation/AramApp.kt` — session switch, tab
  scaffold, Home FAB (currently navigates to Activity without opening the sheet).
- `feature/onboarding/.../WelcomeScreen.kt` — single form with name + create profile.
- `feature/onboarding/.../WelcomeViewModel.kt` — validates display name only.
- `feature/onboarding/src/main/res/values/strings.xml` — welcome copy.
- `core/designsystem/.../component/Controls.kt` — primary/glass/ghost buttons,
  icon button, chips, bars, rings.
- `core/designsystem/.../component/Navigation.kt` — `AramBackground`, bottom bar,
  FAB, `AramMark`.
- `core/designsystem/.../icon/AramIcons.kt` — line icon set.
- `feature/transactions/.../TransactionsScreen.kt` — already owns the add-expense
  sheet and its FAB; only the entry point from Home needs wiring.

## Conventions

- Stateless screens with callbacks; `Route` composables own ViewModels.
- Money is `Long`; copy comes from string resources.
- No `!!` in `src/main`; no logging of amounts, notes, emails, or passwords.
- Custom components for anything the prototype draws; Material defaults only for
  platform chrome (bottom sheet scrim, text fields remain basic `BasicTextField`).
- Dark and light themes both come from `AramTheme`; avoid hard-coded surfaces.

## Dependencies

- AR-040 (online auth) will replace the transient email/password with a real
  session and add log in / reset screens.
- Locale task (to be filed) will activate fa/fr in the language pill.

## Decisions log

- Password scoring mirrors the prototype exactly: 0 empty, 1 non-empty, 2 length
  ≥ 6, 3 length ≥ 8 with a digit and a letter; labels `""/Weak/Fair/Strong`.
- Only the display name is persisted today; email and password stay in memory and
  are never logged.
- Entry stage is `rememberSaveable`, so rotation keeps the current step and form
  text (form text lives in the ViewModel; stage in saveable UI state).
- Splash shows for at least 650 ms so it is visible on fast session reads, then
  yields to the session state.
- Inactive onboarding progress bars and dots use `borderStrong`, not `surface2`:
  the light `surface2` token is white-on-white and the track disappears.

## Verification log

- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261117` — BUILD SUCCESSFUL
  (500 tasks, onboarding unit tests included).
- Device: SM-A536E (Galaxy A53 5G), Android 16, 411 dp / dark and light.
  - Splash → welcome → onboarding step 1–3 (Continue, Back, Skip) → create
    account → dashboard; empty, invalid email/weak password, unchecked terms,
    and strong-password paths all render as designed.
  - Home FAB opens the add-expense sheet with Activity behind the scrim.
  - Fixed during the pass: aapt trimming the trailing space in the welcome
    headline (glued `spend,every` and forced an early wrap), lead/body columns
    set to the prototype's 31/32 ch width, onboarding orb clipped to a circle,
    tile shadow artifact replaced with a drawn glow, dashboard Income/Goal tiles
    switched to the prototype's wallet/target glyphs.
