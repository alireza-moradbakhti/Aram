# Contributing to Aram

Thanks for taking a look. Aram is a portfolio project, but it is run like a real product: small, reviewed changes, tests for business logic, and a green CI build before anything lands on `main`.

## Before you start

- Read [docs/SPEC.md](docs/SPEC.md) for the product and architecture rules.
- Check the [project board](https://github.com/alireza-moradbakhti/Aram/projects) for the current sprint.
- Open an issue before starting significant work so the approach can be agreed on first.

## Setup

```bash
git clone https://github.com/alireza-moradbakhti/Aram.git
cd Aram
./gradlew :app:assembleAlphaDebug
./gradlew test
```

Requirements: JDK 21, Android SDK Platform 37, Android Studio latest stable.

## Workflow

1. Create a branch from `main`:
   - `feat/transactions-add` — user-facing functionality
   - `fix/budget-progress` — bug fix
   - `chore/ci-cache` — tooling or maintenance
   - `docs/roadmap` — documentation
2. Keep commits small and use [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat(transactions): add account picker`
   - `fix(money): round half up on major-to-minor conversion`
   - `test(budgets): cover month boundary`
   - `docs: document sync-ready schema`
3. Run the checks locally:
   ```bash
   ./gradlew :app:assembleAlphaDebug test
   ```
4. Open a pull request using the template. Keep it focused; unrelated refactors belong in their own PR.

## Code conventions

- Kotlin official style; no wildcard imports.
- One responsibility per package: `presentation`, `repository`, `mapper`, `defaults`, `di`, `entity`, `dao`.
- Screens stay stateless. ViewModels expose immutable `UiState` via `StateFlow`.
- Features must not depend on `:core:data` or `:core:database`; go through a `:core:domain` interface.
- Never use `Double` or `Float` for money. Use `Money` / `Long` minor units.
- No analytics, tracking, or network dependencies in the MVP.
- Add a comment only when the *why* is non-obvious.

## Build and release conventions

- Develop, preview, and test everything on `alphaDebug` — it is the only variant CI builds and the APK that goes on a real phone.
- Cut `beta`/`prod` builds only when alpha has fully passed on a device and a release is being prepared. `prodRelease` is then verified once more before tagging.
- Bump `aram.versionName` and `aram.versionCode` in `gradle.properties` before tagging a release.
- Never commit a keystore, `.env`, or signing passwords. Release signing values belong in `.env` or environment variables.
- Keep the R8 mapping file (`app/build/outputs/mapping/prodRelease/mapping.txt`) with the release it belongs to; the release workflow archives it as an artifact.
- APKs are named `aram-<stage>-<buildType>-<versionCode>-<yyyyMMdd>.apk`; do not rename build outputs by hand.

## Definition of done

- [ ] Acceptance criteria in the linked issue are met
- [ ] New business logic has unit tests
- [ ] `./gradlew :app:assembleAlphaDebug test` passes
- [ ] CI is green
- [ ] README/spec updated if behavior or architecture changed

## Reporting bugs

Use the bug report template. Include the app version, Android version/device, and reproducible steps. Do not include personal financial data in screenshots.
