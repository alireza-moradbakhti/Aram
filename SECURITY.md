# Security policy

## Supported versions

Aram is pre-1.0. Security fixes are applied to the latest commit on `main` and the most recent tagged release.

## Reporting a vulnerability

Please **do not open a public issue** for a security problem.

Report it privately through GitHub's [security advisory form](https://github.com/alireza-moradbakhti/Aram/security/advisories/new). Include:

- affected version or commit,
- a description of the issue and its impact,
- steps to reproduce, and
- any suggested fix.

You can expect an acknowledgement within a few days. This is a solo-maintained project, so timelines are best-effort.

## Scope and design guarantees

- The MVP has **no `INTERNET` permission** and no analytics SDK; user financial data stays in app-private storage.
- There are no accounts, passwords, or server-side components in the MVP.
- The release APK published by CI is debug-signed and intended for preview only.
