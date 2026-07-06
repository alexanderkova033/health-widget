# Project status

_Last updated: 2026-07-06_

This file is a snapshot for picking the project back up — it will go stale; check `git log`
and the repo itself for anything that matters more than this doc.

## What's built

All v1 milestones are implemented and on `main` (pushed to
[github.com/alexanderkova033/health-widget](https://github.com/alexanderkova033/health-widget)):

1. `chore:` Gradle Kotlin DSL skeleton, version catalog, CI, ktlint
2. `feat:` `:core` module — `TipEngine` + full unit tests
3. `feat:` DataStore-backed settings + last-tip persistence
4. `feat:` WorkManager nudge + sleep-alert notifications, quiet hours
5. `feat:` Glance home-screen widget, refresh scheduling, boot rescheduling
6. `feat:` Compose settings screen wired to DataStore
7. `docs:` README, PRIVACY.md, LICENSE, CONTRIBUTING.md

Two follow-up passes after the initial build-out:

- **`fix:` correct Glance API usage and ktlint/lint violations found by a real build** — set
  up a JDK + scratch Android SDK to actually compile/test/lint instead of relying on manual
  review, and fixed real bugs that review alone had missed (wrong `GlanceTheme` import,
  missing `updateAll` import, wrong `actionStartActivity` signature, a lint
  `MissingPermission` false-negative from a wrapper function).
- **`refactor:` reorganize into screaming + clean architecture** — `:core` split into
  `tips/`, `settings/`, `scheduling/` domain packages with repository *interfaces*
  (`SettingsRepository`, `TipHistoryRepository`) and a new `AdvanceTipUseCase` that
  de-duplicated tip-picking logic that had been hand-copied across three call sites.
  `:app` reorganized by feature (`settings/`, `tips/`, `notifications/`, `widget/`, `boot/`)
  instead of by technical layer; `MainActivity` renamed `SettingsActivity`.

## Verified for real (not just reviewed)

- `:core` — 35 unit tests pass (JVM, no Android needed).
- `:app` — 7 unit tests pass, debug and release variants.
- `ktlintCheck`, `lint` (0 errors), full `build` including `assembleRelease` with R8
  minification — all green.
- **CI is live and green**: GitHub Actions ran on the latest push (`b192ea8`) and passed —
  see the badge in README.md or the [Actions tab](https://github.com/alexanderkova033/health-widget/actions).
- The app has **never been run on an actual device or emulator** — everything above is
  compile/lint/unit-test level, not "I saw it work."

## Release readiness

- **Signing**: `keystore/healthwidget-release.jks` exists locally (gitignored — not in the
  repo). `app/build.gradle.kts` applies it to the `release` build type when
  `keystore/keystore.properties` is present; falls back to unsigned when absent (so CI
  isn't affected). **The passwords in `keystore/keystore.properties` are only on this
  machine — back them up externally (password manager) before this machine's disk is the
  only copy.** See `keystore/README.md`.
- **Store icon**: `store-assets/play-store-icon-512.png` — flattened 512×512 PNG for the
  Play Console listing.
- **Not yet done**:
  - No Play Console developer account yet ($25, identity verification).
  - `PRIVACY.md` isn't hosted at a public URL yet (Play Console requires one — GitHub Pages
    on this repo is the easy option).
  - No screenshots (needs the app running on a device).
  - No signed `.aab` has been uploaded anywhere yet — `bundleRelease` produces
    `app/build/outputs/bundle/release/app-release.aab` locally, gitignored, not committed.

## In progress right now

Testing on a **physical Android device via USB** (the user's choice over emulator /
Android Studio / Firebase Test Lab, made explicitly). Waiting on:

1. Developer Options + USB debugging enabled on the phone.
2. Phone connected via USB, "Allow USB debugging" accepted.

Once connected: `adb devices` to confirm, `adb install app/build/outputs/apk/debug/app-debug.apk`,
then walk the golden path (widget placement, notification permission prompt, settings
screen, quiet hours dialog) while watching `adb logcat`.

## Local environment notes

This machine has **no JDK or Android SDK installed by default**. To verify builds in this
session, a JDK 17 (Temurin) was downloaded to `%TEMP%\claude\jdk17` and a minimal Android
SDK (platform-tools, build-tools 34, platform 35) to `%TEMP%\claude\android-sdk-empty`, both
outside the repo and both ephemeral (not guaranteed to survive a reboot or session end). A
future session verifying a build from scratch will need to either redo this or use a real
Android Studio / SDK installation.

Note from that setup: the first `unzip -q` extraction of the JDK zip silently dropped most
files (only 24 of ~126 expected) with no error — if a from-scratch JDK extraction ever looks
suspiciously small, re-extract without `-q` and check the file count before trusting it.

## Assumptions and deliberate decisions worth knowing

- Fixed nudge times per frequency level (10:00/14:00/18:00 etc.), not user-configurable
  exact times — see `NudgeScheduler.kt`; product spec left this unspecified.
- Sleep-alert messages are exempt from the anti-repeat rule by design (one fixed message
  per window, not a pool) — see `TipEngine.kt`.
- No ViewModel, no DI framework (`AppContainer` is hand-written) — deliberate, the app is
  too small to justify either; see README "Notable design decisions."
- AGP kept at 8.7.3 / `compileSdk 35`. Larger dependency bumps (activity-compose 1.13,
  core-ktx 1.19, etc.) were tried and reverted after they cascaded into requiring
  `compileSdk 36+`/AGP 9.x, which felt out of scope for a first verification pass.
