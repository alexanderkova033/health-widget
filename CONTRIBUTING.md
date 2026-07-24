# Contributing

Thanks for considering a contribution to HealthWidget.

## Ground rules

These aren't up for debate on a per-PR basis — they're the whole point of the project:

1. **No `INTERNET` permission, ever.** If a change seems to need it, the change is wrong,
   not the rule.
2. **No analytics, crash-reporting, or advertising SDKs.**
3. **No new runtime dependencies** beyond AndroidX + the stack already in
   `gradle/libs.versions.toml` (Compose, Glance, WorkManager, DataStore) without a clear
   justification in the PR description.
4. **No streaks, gamification, or progress tracking.** v1 is intentionally passive.
5. Business logic (anything that isn't Android plumbing) belongs in `:core` and must have
   no `android.*` imports, so it stays unit-testable on the plain JVM.
6. All user-facing strings go in `strings.xml`, not hardcoded in Kotlin.

## Getting set up

```bash
git clone <repo-url>
cd health-widget
./gradlew build
```

Requires JDK 17. No Android device/emulator is required to build or run the unit tests.

## Before opening a PR

```bash
./gradlew ktlintCheck   # or: ./gradlew ktlintFormat to auto-fix
./gradlew lint
./gradlew test
```

CI runs the same three checks plus a full build on every push and PR.

## Commit style

This repo uses [Conventional Commits](https://www.conventionalcommits.org/)
(`feat:`, `fix:`, `chore:`, `docs:`, `test:`, ...). Keep the subject line under ~70
characters; use the body to explain *why*, not *what* (the diff already shows *what*).

## Adding a tip

Tip pools live in `core/src/main/resources/tips/*.txt`, one tip per line. Keep new tips:

- Non-numeric / non-statistical (no fabricated stats) — a real, well-established figure
  (e.g. a caffeine half-life, a recommended bedroom temperature range) is fine; an
  invented-sounding one is not.
- Phrased as a gentle suggestion ("can help", "is linked to"), never a guaranteed outcome.
- Short enough to read at a glance in a notification or a small widget — in practice, under
  ~115 characters, since the widget caps tip text at 5 lines of bold 16sp type
  (`TipWidget.kt`) and has to fit the smallest home-screen widget size too.
- Backed by real research: add or update the matching entry in
  [TIP_SOURCES.md](TIP_SOURCES.md) so every claim a tip makes stays traceable to a primary
  source.
