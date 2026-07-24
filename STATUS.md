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
  - **Real bug found and fixed, in two attempts**: changing the widget style worked the
    *first* time, then silently stopped working until the app was force-stopped and
    relaunched. Root cause: the style-change handler called `TipWidget().updateAll(context)`
    directly inside the settings screen's own `rememberCoroutineScope()`-backed coroutine,
    which is cancelled if the user navigates away before the Glance composition finishes —
    apparently leaving that widget ID's Glance session stuck until the process died.
    - First attempt: routed the call through `WidgetScheduler.refreshNow()` (a one-time
      `WorkManager` job), mirroring how `WidgetRefreshWorker` drives the periodic refresh.
      This fixed the "stuck after one change" symptom (confirmed on-device), but introduced
      a new problem — the user reported a multi-second **lag** before the background
      actually changed, and sometimes it didn't visibly change at all within the time they
      were watching. `WorkManager`'s scheduler doesn't guarantee near-instant execution for
      a plain (non-expedited) one-time request, which is fine for a background periodic
      refresh but not for something tapped expecting to see change immediately.
    - Second attempt (current): call `TipWidget().updateAll()` directly again, but inside
      `HealthWidgetApp.applicationScope` (a process-lifetime `CoroutineScope`, now
      `internal` instead of `private`) rather than the screen's own scope. This keeps the
      original problem fixed (the scope outlives the screen) while being as fast as the
      original direct call (no `WorkManager` queueing). `setExpedited()` on the
      `WorkManager` request was considered and rejected: it requires overriding
      `getForegroundInfo()` (i.e. showing a system notification) on API < 31 or the app
      crashes there, which is a non-starter for a purely cosmetic background refresh on an
      app whose whole philosophy is minimal, unobtrusive notifications.
    - Not yet re-confirmed on-device after this second fix — reinstalled, awaiting the
      user's repeat of the "tap through several styles in a row" test.
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
- **`feat:` notifications master switch, manual tip refresh, settings restructure
  (2026-07-24, later still)** — by request:
  - New `AppSettings.notificationsEnabled` (default `true`) is a true kill-switch, not just
    "frequency = 0": `NudgeScheduler.apply()` and `SleepAlertWorker` both gate on it ahead of
    the existing frequency/sleep-alert checks, so turning it back on restores whatever
    cadence was already dialed in rather than losing it. New DataStore key
    (`notifications_enabled`) + repository method + test.
  - Frequency/Sleep alert/Quiet hours collapsed from three separate `SectionCard`s into one
    **Notifications** card (`NotificationsSection`) with the master switch in its header row;
    the sub-controls drop to a plain `SubsectionTitle` (no icon) instead of a full
    `SectionTitle` each, and the whole card reduces to a one-line "off" hint when the switch
    is off. Sections reordered: Notifications → Widget → current-tip card → About (functional
    controls first, boilerplate last).
  - "Why this tip?" gained a refresh `IconButton` (`Icons.Filled.Refresh`) that calls the
    same `AdvanceTipUseCase` the periodic worker uses, then `TipWidget().updateAll()` via
    `HealthWidgetApp.applicationScope` — picks a new tip out of turn and pushes it to the
    home-screen widget immediately, same instant-update reasoning as the widget-style fix.
  - About is now collapsed by default (tap the row to expand/collapse; a plain `if`, no
    `AnimatedVisibility`, to avoid pulling in `androidx.compose.animation` on a fixed stack),
    and its copy was rewritten from a "no X, no Y, no Z, not even W" listy paragraph to two
    short clauses ("Everything stays on your phone — tips, settings, history. No account, no
    tracking, no ads.").
  - The four widget background drawables had drifted toward looking like the same template
    recolored: Forest's end color was blue-gray (`#3A5F72`), landing in the same cool-blue
    family as Ocean and Midnight, and all four used an identical 135° base-gradient angle.
    Forest's palette is now genuinely green (`widget_gradient_start/end` renamed to
    `widget_forest_start/end`, end color to a near-black forest green `#0B2E1F`, its "light"
    accent from icy mint to warm dappled gold), and each theme got a distinct base angle
    (Forest 135°, Ocean 90°, Sunset 225°, Midnight 315°) so they read as different scenes,
    not just different color stops on the same template.

## Verified for real (not just reviewed)

- The notifications-master-switch/tip-refresh/settings-restructure pass above is verified via
  `compileDebugKotlin`, `testDebugUnitTest` (including a new `setNotificationsEnabled`
  DataStore test), and `ktlintCheck` — all green, and the build has since been installed on
  the device. **Not yet confirmed on-screen**: the collapsible About row, the Notifications
  card's collapse-on-disable, the refresh-tip button's visible effect on the widget, and the
  four re-angled/recolored backgrounds have only been checked as compiled resources, not as
  rendered pixels — pending the user's next pass through the app.
- `:core` — 37 unit tests pass (JVM, no Android needed) — includes a new `TipCatalogTest`
  case for the citation requirement.
- `:app` — 10 unit tests pass, debug and release variants.
- `ktlintCheck`, `lint` (0 errors), full `build` including `assembleRelease` with R8
  minification — all green, after fixing the `Tip`-typed API in every test that still
  expected raw `String`s (`TipEngineTest`, `AdvanceTipUseCaseTest`, `TipCatalogTest`).
- **CI is live and green**: GitHub Actions ran on push `b192ea8` and passed — see the badge
  in README.md or the [Actions tab](https://github.com/alexanderkova033/health-widget/actions).
  Everything described in this file is **local and uncommitted as of this writing** — CI
  hasn't seen any of the settings-redesign/citation/anti-repeat-window work yet.
- On-device: the widget-style picker bug (see above) was reproduced and confirmed exactly
  as reported (both the original "stuck after one change" version and the follow-up "lags,
  doesn't change" regression from the first fix attempt). The current (second) fix is
  installed on the device but **not yet re-verified on-screen** — see "In progress right
  now." Before either bug was found, this same build's settings screen (icons, trimmed
  text, Card sections, live widget preview, style swatches, frequency slider to 6, the
  "100% offline" string) was confirmed rendering correctly via `adb` screenshots, and the
  notification-permission flow, WorkManager job scheduling, and settings
  persistence-through-reinstall all checked out via `adb`/`dumpsys` in an earlier pass this
  session.

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
a perfectly healthy, driver-bound ADB interface throughout). The device has repeatedly
dropped to `unauthorized` in `adb devices -l` over the course of this session and needed
"Allow" tapped again on the phone each time — that's normal for a "new" USB session from
Android's perspective, not a sign of a deeper problem, but expect to hit it again next time.

The build with the second (current) widget-refresh fix — `applicationScope` instead of
`WorkManager` — has been installed on the device. **Awaiting the user's re-test**: tap
through several different widget styles in a row without restarting the app (the exact
sequence that exposed both the original stuck-session bug and the WorkManager-lag
regression) and confirm the home-screen widget's background changes instantly each time,
with no lag and no "only works once" behavior.

Two methodological notes for next time:
- While both this session and the user were sending input to the same phone at once (adb
  `input tap` here, real touches from the user), the two interleaved and produced confusing,
  hard-to-interpret results (a style selection appeared to "jump back" to an earlier
  choice). Prefer read-only observation (screenshots, logcat, `dumpsys`) plus asking the
  user to perform the interaction, over sending synthetic input to a device the user is
  also actively holding.
- A fix that passes `ktlintCheck`/`test`/`lint`/`adb`-observed-once isn't necessarily the
  final fix — the WorkManager-based widget-refresh fix looked completely correct and was
  confirmed working via screenshots, but the user caught a UX regression (lag) that no
  automated check here would have surfaced. Real user re-testing after "it's fixed" claims
  matters even when the automated signals are all green.

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
