## Summary

<!-- What changed and why, in 2–4 bullets. -->

Closes #<issue>

## Task

`AR-0NN · <issue title>` · <points> pts · <sprint> · <phase>

## Type of change

- [ ] Feature
- [ ] Bug fix
- [ ] Refactor / chore
- [ ] Documentation
- [ ] CI / tooling

## How was it tested?

<!-- Commands, device/emulator, screenshots or a short recording for UI changes. -->

- [ ] `./gradlew :app:assembleAlphaDebug test` passes
- [ ] CI is green
- [ ] Device: <model>, Android <version> — every step passes (attach screenshots or logcat on failure)

## Checklist

- [ ] One task per PR, base branch is `dev` (only `dev` merges into `main`)
- [ ] Title is `type(area): summary (AR-0NN)`
- [ ] Follows the module dependency rules in `docs/SPEC.md`
- [ ] Money is handled as `Money` / `Long` minor units (no `Double`)
- [ ] No new analytics, tracking, or network access in the MVP
- [ ] Screens are stateless; state lives in a `ViewModel` as immutable `UiState`
- [ ] Errors surface as a typed state with retry — no crash or endless spinner
- [ ] README or spec updated if behavior or architecture changed
- [ ] Branch may be deleted after merge (`dev` and `main` are permanent)
