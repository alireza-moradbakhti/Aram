# AR-045 · Entry flow — splash, welcome, onboarding, and create account

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/53
- Phase / Sprint: Phase 1 · Transactions core / Sprint 1
- Estimate: 5 pts · P0
- Branch: `feat/AR-045-entry-flow`
- Status: Shipped

## Problem

The design source (`Designs/Aram`) now defines a four-part entry flow — branded splash,
marketing welcome, a three-step onboarding carousel, and a create-account screen —
but the app still shows a single welcome screen that collects a display name. The
copy, artwork, progress, and account chrome are missing, so first launch does not
match the prototype. The Home FAB also sends users to Activity instead of opening
the add-expense sheet.

## Goals

- Splash while the session loads, matching `.splash-screen`.
- Welcome screen with the prototype's exact copy, preview card, points, and CTAs.
- Three-step onboarding with progress bars, dots, Skip, Back, Continue.
- Create-account step with the prototype's auth chrome (name, email, password with
  strength meter and show/hide, terms checkbox, loading CTA).
- Home FAB opens the add-expense sheet in one tap.
- Dark and light themes both match; loading, empty, and error session states remain.

## Non-goals

- Real authentication. AR-040 owns online sessions; until then only the display name
  is persisted locally. Email and password are validated in memory and never stored
  or logged.
- Locale switching. The language pill is rendered; English is active and fa/fr are
  marked "soon" until the locale task.
- Sign-in and reset-password screens (fold into AR-040 scope).
- Goals, notifications, profile, and settings parity (existing issues).

## Acceptance criteria

- [x] Splash, welcome, and onboarding render the design layout and copy at 390 dp with custom components, not Material defaults.
- [x] Onboarding requires three steps, supports Back and Skip, and shows position in both the progress bars and the dots.
- [x] Create account validates name, email, password strength, and terms, then creates the local profile and enters the app.
- [x] Home FAB opens the add-expense sheet directly; Activity keeps its own FAB.
- [x] Loading, empty, and error states of the session flow still work; dark and light themes both match.
- [x] Device pass on Galaxy A53 (Android 16) in dark and light.

## Technical design

### Modules

- `feature/onboarding`
  - `EntryFlow.kt` — stage state machine (`Welcome → Onboarding → CreateAccount`),
    splash gate, language pill + menu shared by all stages.
  - `WelcomeScreen.kt` — stateless, prototype copy and preview card.
  - `OnboardingScreen.kt` — three pages, progress bars and dots.
  - `CreateAccountScreen.kt` — stateless form + `CreateAccountRoute` backed by
    `WelcomeViewModel`.
  - `WelcomeViewModel` gains email, password, terms, validation flags, and a
    `passwordScore` helper.
- `app/navigation/AramApp.kt`
  - `UiState.Loading` renders `AramSplashScreen` with a short minimum display.
  - `UiState.Empty` renders `EntryRoute`.
  - The Home FAB sets a one-shot `openAddRequest` flag and switches to Activity;
    `TransactionsRoute` consumes it and opens the sheet, so the sheet state stays
    owned by the transactions screen.
- `core/designsystem/icon/AramIcons.kt` — globe, bar chart, clock, user, mail, eye,
  eye-off glyphs used by the entry flow.

### Data

No schema change. `AuthRepository.createLocalProfile(displayName)` is unchanged.
Email and password are transient UI state only; they are not written to Room,
DataStore, or logs. AR-040 replaces this step with the online flow.

## Test plan

- Unit: password score / label / hint (0–3), email validation, create-account
  validation (name, email, password, terms), submit success and failure.
- UI sanity: previews for each stage in dark and light.
- Device: cold start over the splash, welcome → onboarding (Back/Skip on every
  step) → create account → dashboard; Home FAB opens the sheet; rotation keeps
  the current stage and form input.

## Risks and open questions

- The language pill lists fa/fr as "soon"; swapping locales is a separate task.
- Local profile creation still needs a display name; email is captured for parity
  with the prototype but is not persisted until AR-040.
