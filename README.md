<p align="center">
  <img src="docs/images/banner.svg" alt="Aram — save smarter, spend calmer" width="100%">
</p>

<h1 align="center">Aram · آرام</h1>

<p align="center">
  <strong>An offline-first Android money companion built around one question: “Can I spend this?”</strong>
</p>

<p align="center">
  <a href="https://github.com/alireza-moradbakhti/Aram/actions/workflows/ci.yml"><img src="https://github.com/alireza-moradbakhti/Aram/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
  <a href="https://github.com/alireza-moradbakhti/Aram/actions/workflows/release.yml"><img src="https://github.com/alireza-moradbakhti/Aram/actions/workflows/release.yml/badge.svg" alt="Release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/alireza-moradbakhti/Aram" alt="License: MIT"></a>
  <img src="https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin 2.4.21">
  <img src="https://img.shields.io/badge/Compose-BOM%202026.09-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/minSdk-26-3DDC84?logo=android&logoColor=white" alt="minSdk 26">
</p>

<p align="center">
  <a href="#why-aram">Why</a> ·
  <a href="#architecture">Architecture</a> ·
  <a href="#tech-stack">Stack</a> ·
  <a href="#getting-started">Get started</a> ·
  <a href="#roadmap">Roadmap</a> ·
  <a href="docs/SPEC.md">Spec</a> ·
  <a href="docs/MASTER_PLAN.md">Plan</a>
</p>

---

Aram is a save-first personal finance app for people who abandon budgeting apps. Instead of dense tables and guilt-driven dashboards, it answers one question with one number: **how much can I safely spend today?**

It is built as a real, production-shaped Android project: multi-module Clean Architecture, MVVM with unidirectional state, an offline Room database designed for a future sync layer, and a CI/CD pipeline that builds, tests, and ships preview APKs from Git tags.

> **Status:** early development. The skeleton (M0) is complete: design system, navigation shell, onboarding, local session, Room schema v1, and CI/CD. Feature work is tracked publicly on the [project board](https://github.com/users/alireza-moradbakhti/projects/6).

## Design preview

<p align="center">
  <img src="docs/images/preview.svg" alt="Aram home dashboard and savings goals screens — design preview" width="100%">
</p>

<p align="center"><sub>A design preview of the target experience. Screens are being implemented milestone by milestone.</sub></p>

## Why Aram

| Typical budgeting app | Aram |
|---|---|
| Enter data, browse charts, feel guilty | Answer one question: **“Can I spend this?”** |
| Configuration before the first insight | Value in under 60 seconds |
| Account and cloud required | Fully offline; no account, ever — until sync is added on your terms |
| Dense dashboards | One number, one chart, one next action |
| Red-alarm notifications | Calm visuals, amber warnings, no shaming |

**Target user:** 18–32, saving toward something concrete — a trip, an emergency fund, moving out — and tired of spreadsheets disguised as apps.

## Features

### Shipping in the MVP (P0)

- [x] Local profile, default currency, Gregorian/Jalali calendar preference
- [x] Splash screen, welcome flow, session gate
- [x] Navigation shell with five destinations
- [x] Theme: Material 3, light/dark, edge-to-edge
- [ ] Accounts with opening balances (cash, bank, wallet, savings, investment)
- [ ] Income, expense, and transfer transactions
- [ ] Monthly category budgets with 80% / 100% alerts
- [ ] Savings goals with contributions, streaks, and projected completion
- [ ] Safe-to-spend dashboard
- [ ] Custom Canvas charts: category donut, 6-month trend
- [ ] Search and filter transactions
- [ ] CSV export (P1)

### Deliberately out of scope

- SMS transaction parsing (Play policy risk — CSV import instead)
- Ads, analytics, or any third-party tracking
- Investment portfolio tracking
- Cloud accounts before the local product is finished

## Architecture

Aram uses a multi-module Clean Architecture so that boundaries are enforced by the compiler, not by convention.

```mermaid
flowchart TD
    APP[":app<br/>Application · MainActivity · NavHost · session gate"]

    subgraph FEATURES[":feature:*"]
        F1[onboarding]
        F2[dashboard]
        F3[transactions]
        F4[budgets]
        F5[goals]
        F6[settings]
    end

    subgraph CORE[":core:*"]
        DS[designsystem]
        DOM[domain<br/>repository contracts]
        DATA[data<br/>implementations · mappers · DI]
        DB[database<br/>Room]
        STORE[datastore<br/>session]
        MODEL[model<br/>pure Kotlin]
        MONEY[money<br/>pure Kotlin]
        COMMON[common<br/>dispatchers]
    end

    APP --> FEATURES
    APP --> DATA
    APP --> DS
    FEATURES --> DOM
    FEATURES --> MODEL
    FEATURES --> MONEY
    FEATURES --> DS
    DATA --> DOM
    DATA --> DB
    DATA --> STORE
    DATA --> COMMON
    DATA --> MODEL
    DOM --> MODEL
    DB --> MODEL
```

**Dependency rules**

- `:feature:*` may depend on `:core:domain`, `:core:model`, `:core:money`, and `:core:designsystem` — **never** on `:core:data` or `:core:database`.
- `:core:domain` depends only on `:core:model` and is pure Kotlin.
- `:core:data` is the only module that knows about Room, DataStore, and repository implementations.
- `:core:model`, `:core:money`, and `:core:common` have no Android dependencies, so they can move to Kotlin Multiplatform later.
- Features never navigate into each other. `:app` wires navigation through lambda callbacks.

**MVVM conventions**

- Each screen is a stateless `*Screen(state, callbacks)` composable — previewable and testable.
- A `*Route` composable owns the `ViewModel` and lifecycle wiring.
- ViewModels expose an immutable `UiState` as `StateFlow`; the UI never mutates state.
- Dependencies point inward: presentation → domain ← data.

```text
*Screen (stateless)
   ↑ state / ↓ events
*Route → ViewModel → StateFlow<UiState>
              ↓
        Repository interface (:core:domain)
              ↓
        Repository impl (:core:data) → Room · DataStore
```

### Engineering decisions

| Decision | Why |
|---|---|
| **Money as `Long` minor units** | No floating-point drift. Every amount is cents/fils/rials; `BigDecimal` is used only at the input boundary with `HALF_UP` rounding. Handles 0-decimal currencies (JPY, IRR). |
| **UUID primary keys + `userId` on every row** | Rows can be created offline and pushed later without a server round-trip. |
| **Soft deletes (`deletedAt`)** | Tombstones survive until a future sync layer reconciles them. |
| **Swappable auth seam** | Features depend on `AuthRepository`; a remote implementation can replace the local one without touching UI modules. See [AuthRepository.kt](core/domain/src/main/java/com/aram/core/domain/repository/AuthRepository.kt). |
| **No INTERNET permission** | Offline privacy is a product guarantee, not an afterthought. Add it only when sync ships. |
| **Defense in depth** | No `INTERNET`, backups disabled, R8 obfuscation, stripped release logs, and signing secrets kept outside git. |
| **KMP-ready logging** | A pure-Kotlin `:core:logging` module with named events; `sendToServer` defaults to `false` and uploads stay disabled until a backend exists. |
| **Custom Canvas charts** | Full control over the calm visual language; no heavyweight chart dependency. |
| **Version catalog + CI** | One source of truth for versions, and every PR proves the build on a clean Ubuntu runner. |
| **Staged builds + R8** | `alpha`/`beta`/`prod` flavors keep previews side-by-side with prod; minified releases shrink the APK from ~14 MB to ~1.5 MB, and mapping files are archived for crash triage. |

## Tech stack

| Layer | Choice | Version |
|---|---|---|
| Language | Kotlin | 2.4.21 |
| Build | Gradle · AGP (built-in Kotlin) | 9.8.0 · 9.5.1 |
| UI | Jetpack Compose · Material 3 | BOM 2026.09.00 |
| Architecture | MVVM · Clean Architecture · multi-module | — |
| DI | Hilt | 2.60.1 |
| Persistence | Room · DataStore Preferences | 2.8.5 · 1.3.0 |
| Async | Coroutines · Flow | 1.11.0 |
| Navigation | Navigation Compose | 2.10.2 |
| Codegen | KSP | 2.3.14 |
| Testing | JUnit 4 · Turbine · Compose UI Test | — |
| CI/CD | GitHub Actions · JDK 21 · SDK 37 | — |

All versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Project structure

```text
Aram/
├── app/                          # Application, MainActivity, NavHost, session gate
├── core/
│   ├── model/                    # Pure Kotlin domain models + enums
│   ├── money/                    # Money, CurrencyCode, formatting + unit tests
│   ├── common/                   # CoroutineDispatchers abstraction
│   ├── domain/                   # Repository interfaces (AuthRepository, ...)
│   ├── data/                     # Repository impls, mappers, seed data, Hilt modules
│   ├── database/                 # Room entities, DAOs, AramDatabase, schema
│   ├── datastore/                # Session DataStore
│   └── designsystem/             # AramTheme, typography, shared components
├── feature/
│   ├── onboarding/               # Welcome screen + ViewModel
│   ├── dashboard/                # Home screen
│   ├── transactions/             # List + add/edit
│   ├── budgets/                  # Budgets + editor
│   ├── goals/                    # Savings goals
│   └── settings/                 # Profile, currency, calendar
├── docs/                         # Product spec, roadmap, images
├── gradle/libs.versions.toml     # Version catalog
└── .github/                      # CI, release workflow, templates, Dependabot
```

Package root: `com.aram` · application id: `com.aram.app`.

## Getting started

**Prerequisites**

- JDK 21
- Android Studio (latest stable) with Android SDK Platform 37
- An Android 8.0+ (API 26) device or emulator

```bash
git clone https://github.com/alireza-moradbakhti/Aram.git
cd Aram

# Alpha debug build — the test APK every change goes through
./gradlew :app:assembleAlphaDebug

# Release build — cut only after alpha has fully passed on a real device
./gradlew :app:assembleProdRelease

# Unit tests (JVM, no device needed)
./gradlew test

# Install an alpha debug build on a connected device/emulator
./gradlew :app:installAlphaDebug
```

The first sync downloads Gradle 9.8 and dependencies. `local.properties` is generated by Android Studio and is not committed.

### Build variants

The app uses one flavor dimension (`stage`) and two build types (`debug`, `release`), giving six installable variants.

| Variant | Application ID | Purpose | Minified |
|---|---|---|---|
| `alphaDebug` / `betaDebug` / `prodDebug` | `com.aram.app.<stage>.debug` | Daily development, side-by-side with previews | No |
| `alphaRelease` / `betaRelease` | `com.aram.app.<stage>` | Internal preview builds | Yes |
| `prodRelease` | `com.aram.app` | Shippable release | Yes |

- APKs are named `aram-<stage>-<buildType>-<versionCode>-<yyyyMMdd>.apk`, e.g. `aram-alpha-debug-1-20261114.apk`. Pass `-PbuildDate=20261114` for reproducible names; CI sets it automatically.
- Version name and code live in [`gradle.properties`](gradle.properties) (`aram.versionName`, `aram.versionCode`); bump both before a release.
- `alphaDebug` is the delivery gate: CI builds and uploads only this variant, and it is the APK that gets installed on a real phone. `beta` and `prod` builds are cut at release time, after alpha has passed, so a broken preview never reaches a release artifact.
- Release variants use R8 and resource shrinking. The mapping file is written to `app/build/outputs/mapping/prodRelease/mapping.txt` — the release workflow archives it for deobfuscating crash reports.
- Release signing reads `ARAM_KEYSTORE_PATH`, `ARAM_KEYSTORE_PASSWORD`, `ARAM_KEY_ALIAS`, and `ARAM_KEY_PASSWORD` from the environment or the gitignored `.env` file. Without a keystore it falls back to the debug key and logs a warning, which keeps preview releases buildable on any machine.
- `alpha` and `beta` carry an application-id suffix, so they can be installed next to each other and next to prod.

### Signing a release

Release builds are signed with your own keystore; the keystore and its passwords never enter the repository.

```bash
keytool -genkeypair -v -keystore aram-release.jks -alias aram \
  -keyalg RSA -keysize 4096 -validity 10000
```

Copy [`.env.example`](.env.example) to `.env` (gitignored) and fill in:

```bash
ARAM_KEYSTORE_PATH=C:/keys/aram-release.jks
ARAM_KEYSTORE_PASSWORD=…
ARAM_KEY_ALIAS=aram
ARAM_KEY_PASSWORD=…
```

Then `./gradlew :app:assembleProdRelease` produces a properly signed APK. Without these values the build falls back to the debug key — fine for previews, never for distribution. Back up the keystore and passwords separately: losing them means you can never update the app under the same application id.

### Branching model

- **`dev`** — integration branch and the source for every release build.
- **`main`** — stable branch; mirrors the latest tagged release.
- **Feature branches** — cut from `dev` as `feat/AR-005-<slug>`, `fix/…`, or `chore/…`; merged back into `dev` only after the CI build and a real-device pass, through a PR.
- **Pull requests** — titled `type(area): summary (AR-0NN)`, base `dev`. Merged head branches are deleted automatically; `dev` and `main` are never deleted.
- **Releases** — tag `v*` from `dev`; the release workflow builds `prodRelease`, and `main` is fast-forwarded to the tagged commit. A `dev` → `main` PR is a release action, never an automatic merge.

```bash
git checkout dev && git pull --ff-only origin dev
git checkout -b feat/12-search-and-filter
# …work, build, test on a device…
git push -u origin feat/12-search-and-filter
```

## Testing

The project follows one rule: **business math is pure Kotlin and fast to test.**

| Layer | Tool | Coverage |
|---|---|---|
| Money & calculations | JUnit 4 | Minor-unit arithmetic, 0-decimal currencies, cross-currency guards |
| Repositories / ViewModels | JUnit + Turbine | Flow emissions, session and validation states |
| DAOs | Room in-memory DB (instrumented) | Queries, soft-delete filters, migrations |
| UI | Compose UI Test | Welcome, navigation shell, transaction form |

```bash
./gradlew testDebugUnitTest
```

## CI/CD

Every push and pull request to `main` or `dev` runs on a clean Ubuntu runner:

1. Checkout, JDK 21, Android SDK 37
2. `./gradlew :app:assembleAlphaDebug test`
3. Upload the alpha test APK; on failure, the test reports

Minified `beta`/`prod` builds are **not** built in CI. They are produced by the release workflow, only when alpha has been tested on a device and a release is being cut.

Pushing a `v*` tag publishes a GitHub Release with a preview APK:

```bash
git tag v0.2.0
git push origin v0.2.0
```

Workflows: [CI](.github/workflows/ci.yml) · [Release](.github/workflows/release.yml). The release APK is debug-signed and clearly marked as a preview until a release keystore is configured.

## Roadmap

| Milestone | Scope | Status |
|---|---|---|
| **Phase 0 · Skeleton** | Multi-module Gradle, theme, splash, welcome, navigation shell, Room v1, local session, CI/CD | ✅ Complete |
| **Phase 1 · Transactions core** | Add/edit/delete, list with month headers, account balances, transfers, search & filters, tests | 🔜 Next |
| **Phase 2 · Dashboard & insight** | Safe-to-spend + explainer, month in/out, category donut, 6-month trend, recent transactions | Planned |
| **Phase 3 · Budgets & goals** | Budget CRUD and alerts, goal contributions, streaks, projection, empty states | Planned |
| **Phase 4 · Polish & release** | Recurring rules, WorkManager reminders, CSV export, biometric lock, widget, release signing | Planned |

The step-by-step backlog, sprint iterations, and acceptance criteria live on the **[GitHub Project board](https://github.com/users/alireza-moradbakhti/projects/6)**. Every task carries a stable `AR-0NN` key (`AR-001` … `AR-036` today) used in branches, commits, and comments.

- [docs/MASTER_PLAN.md](docs/MASTER_PLAN.md) — every task with points, sprint, dependencies, the release checklist, and the post-MVP Kotlin Multiplatform migration plan.
- [docs/GITHUB_SCRUM.md](docs/GITHUB_SCRUM.md) — how to read the backlog, the active sprint, story points and capacity, and how to run the sprint loop.
- [docs/ROADMAP.md](docs/ROADMAP.md) — the sprint-by-sprint narrative.

## Contributing

This is primarily a portfolio and learning project, but feedback and issues are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) for branch naming, commit conventions, and the PR checklist.

## Security & privacy

**On-device data**
- No data leaves the device; the MVP does not request `INTERNET`.
- No analytics, crash-reporting, or tracking SDKs.
- Android backup and device transfer are disabled (`android:allowBackup="false"`), so the finance database cannot be pulled through `adb backup`, cloud backup, or a transfer tool. CSV export is the deliberate, user-driven way out (Phase 4).

**Release hardening**
- `prodRelease` runs R8 with resource shrinking, so class and member names are obfuscated and unused code is removed.
- Debug and verbose `Log` calls are stripped from release builds.
- The R8 mapping file is archived privately with each release (release workflow artifact, 90 days) and never committed. Debug-signed preview APKs are clearly previews, not store builds.
- Signing uses a private keystore; its path, passwords, and aliases come from environment variables or the gitignored `.env`.

**Keys and secrets**
- No signing key, password, token, or `.env` file is tracked. Only `.env.example` is committed.
- Android Keystore (hardware-backed) is the plan for the biometric app lock and any future sync tokens (Phase 4); private keys will never sit in plain preferences.

**What "public" means**
- The repository is public by design: source, tests, CI, and the board are part of the portfolio. Nothing sensitive is published.
- Obfuscation raises the cost of reverse engineering; it does not make a client app impenetrable. Never ship API keys or credentials inside an APK — the architecture keeps them out.

To report a vulnerability, see [SECURITY.md](SECURITY.md) — please do not open a public issue.

## License

[MIT](LICENSE) © 2026 Alireza Moradbakhti

---

<p align="center">
  <sub>Built with Kotlin and a strong opinion that calm beats clutter.</sub>
</p>
