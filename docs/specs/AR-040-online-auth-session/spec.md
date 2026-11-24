# AR-040 · Online auth flow and secure token session

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/43
- Phase / Sprint: Phase 1 · Transactions core / Sprint 2
- Estimate: 5 pts · P0
- Branch: `feat/AR-040-online-auth-session` (stacked on `feat/AR-039-mock-api-fixtures`, PR #65)
- Status: Review (device checks batched)

## Problem

The app only has a name-only local profile (`createLocalProfile`). The backend is
live and issues a 24-hour JWT from `POST /api/auth/register` and
`POST /api/auth/login`, but the app cannot create an account, sign in, keep the
token out of plain storage, or end a session.

## Goals / Non-goals

**Goals**

- Real email/password session that behaves identically on the alpha mock engine
  and on the real backend.
- Token persisted through an interface: AndroidKeyStore-encrypted storage for
  stage/production builds, in-memory for the alpha flavor.
- Welcome screen provides guest mode (unchanged), Log in and Create account
  flows with typed errors and a debug-only demo shortcut.
- Sign-out clears the token, the session id and the user-scoped Room cache.

**Non-goals**

- Refresh-token rotation: the Aram API has no refresh endpoint (see Decisions).
- Password reset / change UI (backend endpoints exist; tracked separately).
- Sync outbox warning on sign-out (AR-042 introduces the outbox; the warning
  lands with it).
- Biometric unlock (design shows it; Android BiometricPrompt UI is a follow-up).

## Acceptance criteria

- [x] Sign up, sign in, refresh, sign out, and failed-refresh flows are covered by tests with the mock engine.
  - The backend has no refresh endpoint, so "refresh" is replaced by the
    documented expiry rule: an expired or rejected token signs the session out
    (covered by `expired token at startup logs out` and `401 signs out`).
- [x] Onboarding session states are Loading → LoggedOut → Ready; splash never hangs on a failed call.
- [x] Tokens never appear in logs, crash reports, or plain DataStore.
- [x] Sign-out clears cached finance data and returns to Welcome.
- [x] UI modules depend only on `AuthRepository`; swapping mock → remote needs no feature changes.

## Definition of done

- [x] Acceptance criteria met
- [x] Tests added or updated for the new logic
- [x] `./gradlew :app:assembleAlphaDebug test` passes locally
- [x] CI green on the pull request

## Technical design

### Contracts (`:core:domain`)

```kotlin
interface AuthRepository {
    fun observeSession(): Flow<Outcome<UserProfile?>>
    suspend fun createLocalProfile(displayName: String): Outcome<UserProfile>   // guest, offline
    suspend fun register(displayName: String, email: String, password: String): Outcome<UserProfile>
    suspend fun signIn(email: String, password: String): Outcome<UserProfile>
    suspend fun updateProfile(displayName: String): Outcome<UserProfile>
    suspend fun signOut(): Outcome<Unit>
}
```

### Token storage (`:core:datastore`)

- `SessionToken(value, expiresAtEpochMillis)` + `SessionTokenStore { read, write, clear }`.
- `InMemorySessionTokenStore` — alpha flavor.
- `KeystoreSessionTokenStore` — AES/GCM key in `AndroidKeyStore`, ciphertext in a
  private prefs file. No new dependency (avoids the deprecated
  `EncryptedSharedPreferences` and keeps the offline Gradle cache valid).
- `SessionStore` interface extracted from `SessionDataStore` so the repository is
  testable without Android DataStore.

### Session (`:core:data`)

`AuthRepositoryImpl` now owns network + local state:

1. `register`/`signIn` → `POST /api/auth/register|login` via `safeApiCall`,
   persist `AuthResponseDto.token` + `expiresAtUtc`, upsert the profile and seed
   default categories/Cash account when the user cache is empty, then set the
   session id.
2. `observeSession` drops a stored session when its token is present but expired;
   a guest session (no token) is kept.
3. `updateProfile` → `PUT /api/auth/profile`, persist the returned profile.
4. `signOut` clears token → session id → user-scoped rows (transactions, budgets,
   goals, contributions, accounts, categories, recurring rules, user).
5. `SessionTokenProvider : TokenProvider` supplies the bearer token and, on a
   `401`, clears token + session id (client plugin already guards against loops).
   Cache is intentionally kept on a `401` so nothing queued for sync is lost
   before AR-042 ships the outbox.

### DI

- `SessionTokenStore` chosen in `:app` by `BuildConfig.BUILD_STAGE`
  (`alpha` → in-memory, otherwise Keystore).
- `Clock` provided once in `:app`; repositories take it for expiry math.
- `TokenProvider` bound to `SessionTokenProvider` and handed to the one
  `HttpClient`, so mock and remote share the whole auth path.

### UI (`:feature:onboarding`)

`WelcomeUiState` gains `screen` (`Welcome | SignIn | SignUp`), email, password,
password visibility, terms, `fieldErrors`. One composable renders the Log in and
Create account layouts from the design (back chevron, labels, icon fields,
show/hide password, strength bar for sign-up, inline alert for typed errors,
switch link). Welcome keeps the guest name field and routes the two secondary
actions. The alpha demo shortcut fills the mock credentials. Typed error copy:
validation (per field) / unauthorized / offline / timeout / rate limited /
conflict / server.

## Data and migrations

No schema change. Existing DAOs gain `DELETE ... WHERE userId = :userId` queries
only (no Room version bump).

## Test plan

- `:core:data` — repository tests against `MockApiEngine` (zero latency) with fake
  DAOs/stores: register success + seeding, validation failure, duplicate email,
  wrong password, sign-in, expired token at startup, 401 auto sign-out,
  updateProfile, signOut wipe.
- `:feature:onboarding` — ViewModel tests: screen switching, validation, typed
  error → `SubmitState.Failed`, demo fill, double-tap guard, success stays
  submitting.
- `:app` — `MainViewModelTest` fake updated for the extended interface.
- Manual: alpha mock + real backend device script (batched).

## Risks and open questions

- AndroidKeyStore can fail on some OEM devices; a decrypt failure clears the
  token and returns to Welcome rather than crashing.
- `POST /api/auth/login` returns `409` when the identity has no Aram profile;
  mapped to a conflict message.
- Sign-out wipe is destructive; the pending-outbox warning must ship with AR-042.
