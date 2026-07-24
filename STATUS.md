# Project status

_Last updated: 2026-07-24 (later same day)_

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
- **`docs:` scientifically ground and expand tip content (2026-07-24)** — reviewed every
  existing tip against primary research and tightened the handful that overreached or were
  imprecise (e.g. the afternoon "post-lunch dip" now correctly framed as partly circadian
  rather than just food; the cold-water-splash tip now names the actual dive-reflex
  mechanism; the alcohol tip now notes REM suppression instead of just "reduced quality").
  Added ~33 new research-grounded tips (`general.txt` 30→40, `morning.txt` 15→23,
  `afternoon.txt` 15→22, `evening.txt` 15→23). Added [TIP_SOURCES.md](TIP_SOURCES.md), which
  cites a primary source (PubMed/PNAS/Annals of Internal Medicine/etc.) for every
  non-obvious claim across all four pools, organized by theme. New/edited tip lengths were
  kept within the existing catalog's envelope (~50-115 chars) since the widget caps tip text
  at `maxLines = 5` in a bold, centered 16sp font (`TipWidget.kt`) that also has to fit the
  smallest home-screen widget size.
- **`feat:` settings screen redesign + widget styles + wider anti-repeat window
  (2026-07-24, later same day)** — by request: "more visual, less text, more freedom, more
  settings" for the settings screen, plus the same tip shouldn't repeat within a 30-tip span
  instead of just not back-to-back. Concretely:
  - Every settings section now has a leading icon (`material-icons-extended`, justified in
    a build.gradle.kts comment since it's beyond the originally-fixed stack) and shorter
    body text; the app's own launcher mark now sits next to the screen title.
  - New **Widget** section: a live preview (rendered from the actual widget background
    drawable, not a hand-picked brush) plus four selectable background styles
    (Forest/Ocean/Sunset/Midnight — `WidgetStyle` enum in `:core`, actual drawables/colors
    in `:app`), wired into `AppSettings`/`SettingsRepository` alongside the existing
    fields. Changing the style persists it and calls `WidgetScheduler.refreshNow()`
    (runs the update inside a WorkManager job so it isn't tied to this screen's
    lifecycle). The refresh cadence is a fixed 2h default in `WidgetScheduler` now —
    the user-facing "Refresh every" control was removed (2026-07-24, later still) along
    with `WidgetRefreshInterval` and its DataStore key.
  - Settings sections are now grouped into `Card`s (`SectionCard`) instead of flat
    `HorizontalDivider`-separated blocks, and the widget style swatches/preview render the
    real layered gradient drawables via `AndroidView`/`ImageView` interop (Compose's
    `painterResource` doesn't support `<layer-list>`/`<shape>` resources).
  - Nudge frequency ceiling raised from 3/day to 6/day (`AppSettings.MAX_NOTIFICATION_FREQUENCY`),
    with new default nudge times for levels 4-6 in `NudgeScheduler.NUDGE_TIMES_BY_FREQUENCY`,
    and the frequency label switched from three hardcoded strings to a proper `<plurals>`
    resource so it scales to any count.
  - Anti-repeat generalized from "exclude the single last tip" to "exclude the last
    `TipHistoryRepository.MAX_RECENT_TIPS` (30) tips": the repository interface changed from
    `lastTip: Flow<String?>` / `setLastTip()` to `recentTips: Flow<List<String>>` /
    `recordTip()`, `TipEngine.messageFor` now takes `recentTips: List<String>` instead of a
    single nullable, and `DataStoreTipHistoryRepository` stores the list as a
    newline-joined string (tips are always single-line, so no JSON dependency needed) and
    trims to the 30 most recent on every write.
- **`feat:` tip source citations + settings screen polish (2026-07-24, later still)** —
  every tip now carries a citation, and a real bug in the widget-style picker got fixed:
  - New `Tip` data class (`text`, `sourceLabel`, `sourceUrl`) replaces the raw `String` that
    `TipCatalog`/`TipEngine`/`AdvanceTipUseCase` used to pass around. Each pool `.txt` file
    now has a companion `<name>_sources.txt` (`Label<TAB>URL` per line, same order),
    zipped in by `TipCatalog.loadDefault()`; `TipCatalogTest`'s "every tip has a real
    citation" test fails the build if a label or URL is blank. `TipEngine.findByText`
    resolves the persisted (plain-text) history back to a full `Tip` for display.
  - Settings screen gained a "Why this tip?" card (`TipSourceSection`) showing the current
    tip's citation with a button that opens `sourceUrl` in the system browser
    (`Intent.ACTION_VIEW`) — no `INTERNET` permission needed for that, since the browser
    process makes the request, not this app.
  - **Real bug found and fixed**: changing the widget style worked the *first* time, then
    silently stopped working until the app was force-stopped and relaunched. Root cause:
    the style-change handler called `TipWidget().updateAll(context)` directly inside the
    settings screen's own `rememberCoroutineScope()`-backed coroutine, which is cancelled
    if the user navigates away before the Glance composition finishes — apparently leaving
    that widget ID's Glance session stuck until the process died. Fixed by routing through
    `WidgetScheduler.refreshNow()` (a WorkManager one-time job) instead, mirroring how
    `WidgetRefreshWorker` already reliably drives the periodic refresh. Confirmed via
    `adb`/screenshots before the fix (reproduced exactly as described) — not yet
    re-confirmed on-device after the fix (pending device reconnect).
  - The user-configurable widget refresh interval (1h/2h/4h, `WidgetRefreshInterval`) added
    earlier the same day was removed again: FR1 only requires "at least every 2 hours," and
    the setting wasn't earning its keep. `WidgetScheduler` is back to a hardcoded 2h.
  - Settings sections are now grouped into `Card`s (`SectionCard`) instead of flat
    `HorizontalDivider`-separated blocks, and the widget preview/style swatches render the
    *actual* layered gradient drawables (radial glow + decorative sparkle/star/ember accents
    added to each `widget_background_*.xml`, not flat two-stop gradients) via
    `AndroidView`/`ImageView` interop, since Compose's `painterResource` doesn't support
    `<layer-list>`/`<shape>` resources — `WidgetStyle.backgroundDrawableRes()` (in
    `TipWidget.kt`, now `internal`) is shared between the real widget and this preview so
    they can't drift apart.

## Verified for real (not just reviewed)

- `:core` — 38 unit tests pass (JVM, no Android needed) — includes two new
  `TipCatalogTest` cases for the citation requirement.
- `:app` — 10 unit tests pass, debug and release variants.
- `ktlintCheck`, `lint` (0 errors), full `build` including `assembleRelease` with R8
  minification — all green, after fixing the `Tip`-typed API in every test that still
  expected raw `String`s (`TipEngineTest`, `AdvanceTipUseCaseTest`, `TipCatalogTest`).
- **CI is live and green**: GitHub Actions ran on push `b192ea8` and passed — see the badge
  in README.md or the [Actions tab](https://github.com/alexanderkova033/health-widget/actions).
  Everything described in this file is **local and uncommitted as of this writing** — CI
  hasn't seen any of the settings-redesign/citation/anti-repeat-window work yet.
- On-device: the widget-style picker bug (see above) was reproduced and confirmed exactly
  as reported, then fixed and rebuilt. The fix itself has **not yet been re-verified
  on-screen** — pending the phone reconnecting/re-authorizing over `adb` again. Before that
  bug was found, this same build's settings screen (icons, trimmed text, Card sections,
  live widget preview, style swatches, frequency slider to 6, the "100% offline" string)
  was confirmed rendering correctly via `adb` screenshots, and the notification-permission
  flow, WorkManager job scheduling, and settings persistence-through-reinstall all checked
  out via `adb`/`dumpsys` in an earlier pass this session.

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
Android Studio / Firebase Test Lab, made explicitly) — a Samsung Galaxy A34 (`SM_A346E`),
using a USB-A-to-C cable (not the USB-C-to-C the user would've preferred, but confirmed
working fine — an early "is this a bad cable" theory turned out to be wrong; Windows showed
a perfectly healthy, driver-bound ADB interface throughout). The device keeps needing
re-authorization (`adb devices -l` shows `unauthorized`) each time the connection drops and
comes back, which has happened a few times this session — that's normal for a "new" USB
session from Android's perspective and just needs "Allow" tapped again on the phone each
time, not a sign of a deeper problem.

Next step once reconnected/authorized: `adb install -r app/build/outputs/apk/debug/app-debug.apk`,
relaunch `SettingsActivity`, and specifically re-verify the widget-style picker — tap through
**multiple different styles in a row** without restarting the app (that's exactly the
sequence that exposed the now-fixed bug), and confirm the home-screen widget's background
actually updates each time, plus the new "Why this tip?" citation card.

One methodological note for next time: while both this session and the user were sending
input to the same phone at once (adb `input tap` here, real touches from the user), the two
interleaved and produced confusing, hard-to-interpret results (a style selection appeared to
"jump back" to an earlier choice). Prefer read-only observation (screenshots, logcat,
`dumpsys`) plus asking the user to perform the interaction, over sending synthetic input to a
device the user is also actively holding.

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

- Fixed nudge times per frequency level 0-6 (10:00/14:00/18:00 etc.), not user-configurable
  exact times — see `NudgeScheduler.kt`; product spec left this unspecified.
- The 30-tip anti-repeat window is a single constant, `TipHistoryRepository.MAX_RECENT_TIPS`,
  not tied to any particular pool size — it happens to comfortably fit under every current
  pool's size (~64-65 unique tips per day-part after the 2026-07-24 content expansion), so
  the fallback-to-repeat path is rare in practice, not the common case.
- Sleep-alert messages are exempt from the anti-repeat rule by design (one fixed message
  per window, not a pool) — see `TipEngine.kt`.
- No ViewModel, no DI framework (`AppContainer` is hand-written) — deliberate, the app is
  too small to justify either; see README "Notable design decisions."
- AGP kept at 8.7.3 / `compileSdk 35`. Larger dependency bumps (activity-compose 1.13,
  core-ktx 1.19, etc.) were tried and reverted after they cascaded into requiring
  `compileSdk 36+`/AGP 9.x, which felt out of scope for a first verification pass.
