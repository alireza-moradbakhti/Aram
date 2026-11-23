# Context — AR-040 · Online auth flow and secure token session

## Starting point

- Base branch: `feat/AR-039-mock-api-fixtures` (AR-039 is PR #65 into `dev`; the
  auth work needs its `/api` alignment and mock engine).
- Base commit: `d0b4c91` — `feat(network): align client with Aram-Api and add alpha mock fixtures`.

## Relevant code

| Area | Path |
| --- | --- |
| Domain contract | `core/domain/src/main/java/com/aram/core/domain/repository/AuthRepository.kt` |
| Repository | `core/data/src/main/java/com/aram/core/data/repository/AuthRepositoryImpl.kt` |
| Defaults | `core/data/src/main/java/com/aram/core/data/defaults/DefaultData.kt` |
| Session id | `core/datastore/src/main/java/com/aram/core/datastore/SessionDataStore.kt` |
| HTTP client | `core/network/src/main/kotlin/com/aram/core/network/http/AramHttpClient.kt`, `AramClientPlugin.kt`, `TokenProvider.kt` |
| Calls | `core/network/src/main/kotlin/com/aram/core/network/api/SafeApiCall.kt` |
| DTOs | `core/network/src/main/kotlin/com/aram/core/network/dto/AuthDtos.kt`, `ProfileDtos.kt` |
| Mock backend | `core/network/src/main/kotlin/com/aram/core/network/mock/MockApiEngine.kt`, `MockApiStore.kt` |
| DI | `app/src/main/java/com/aram/app/di/NetworkModule.kt`, `core/data/src/main/java/com/aram/core/data/di/DataModule.kt` |
| Onboarding | `feature/onboarding/src/main/java/com/aram/feature/onboarding/presentation/` |
| Session VM | `app/src/main/java/com/aram/app/session/MainViewModel.kt` |
| Design source | `Desktop/Project/Designs/Aram/index.html` lines 1129–1245 (Sign up, Log in) |

Backend: `Desktop/Project/Root/Aram-Api/src/Aram.Api/Features/Auth/AuthEndpoints.cs`.

## Conventions

- Money is `Long` minor units; never log amounts, notes, emails or tokens.
- Feature modules depend on `:core:designsystem`, `:core:domain`, `:core:model`
  only; they never see the network layer.
- `:core:network` is Android-free (`SourceGuardrailsTest`).
- No `!!` in `src/main`, no analytics SDK.

## Dependencies

- No new external library. Keystore crypto uses the platform API.
- `:core:data` gains `:core:network` + ktor client/JSON.

## Decisions log

1. **No refresh token.** Grep of the backend shows only `register`, `login`,
   `forgot-password`, `reset-password`, `change-password`, `me`, `profile`,
   `account`. The issue's "refresh once on 401" therefore becomes: a `401` on an
   authenticated call clears the token and session (Welcome), and an expired
   stored token is dropped on startup. Documented in the spec AC.
2. **Token at rest.** `androidx.security:security-crypto` deprecates
   `EncryptedSharedPreferences`; an AES/GCM `AndroidKeyStore` key with ciphertext
   in private prefs avoids a new dependency and the deprecation.
3. **Alpha in memory.** `BuildConfig.BUILD_STAGE == "alpha"` selects
   `InMemorySessionTokenStore`; beta/prod use the Keystore store. Keeps demo
   installs deterministic and never touches the Keystore on emulators.
4. **One repository implementation.** The `HttpClient` already abstracts mock vs
   OkHttp, so a single `AuthRepositoryImpl` serves both modes; no feature or DI
   variance beyond the token store and `Clock`.
5. **Cache wipe on explicit sign-out only.** A `401` clears credentials and
   session but keeps cached rows so an expired token cannot destroy data waiting
   in the future outbox. The outbox warning is deferred to AR-042.
6. **Guest mode preserved.** `createLocalProfile` stays and produces a session
   without a token; `observeSession` only expires sessions that *have* a token.
7. **Login 401 vs register 400.** The backend returns FastEndpoints validation
   bodies (`errors: {field: [msg]}`) for register and a `Result` message for
   login; both already map through `ApiError.toAppError()`.

## Verification log

- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261121` — pass in 33s.
  - `aram-alpha-debug-1-20261121.apk` (17.5 MB) produced.
  - `:core:data` — `AuthRepositoryImplTest` 10/10.
  - `:feature:onboarding` — `WelcomeViewModelTest` 12/12.
  - `:app` — `MainViewModelTest` 4/4, `AramIconsTest`, `ExampleUnitTest`.
- Fix found during verification: `:app` did not depend on `:core:datastore`
  directly, so KSP could not resolve `SessionTokenStore` in `SessionModule`.
  Added an explicit `implementation(project(":core:datastore"))`; `:core:data`
  keeps its own dependency `implementation`, so `:app` needs its own edge.
- PR #66 (base `feat/AR-039-mock-api-fixtures`) — CI "Alpha build & test" pass.
  CI previously skipped stacked PRs because `ci.yml` only triggered for
  `main`/`dev`; the trigger now covers feature/fix/chore/docs/test bases.
