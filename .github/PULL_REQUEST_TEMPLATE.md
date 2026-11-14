## Summary

<!-- What changed and why. Link the issue: Closes #123 -->

## Type of change

- [ ] Feature
- [ ] Bug fix
- [ ] Refactor / chore
- [ ] Documentation
- [ ] CI / tooling

## How was it tested?

<!-- Commands, device/emulator, screenshots or a short recording for UI changes. -->

- [ ] `./gradlew :app:assembleDebug testDebugUnitTest` passes
- [ ] New or changed business logic has unit tests

## Checklist

- [ ] Follows the module dependency rules in `docs/SPEC.md`
- [ ] Money is handled as `Money` / `Long` minor units (no `Double`)
- [ ] No new analytics, tracking, or network access in the MVP
- [ ] Screens are stateless; state lives in a `ViewModel` as immutable `UiState`
- [ ] README or spec updated if behavior or architecture changed
