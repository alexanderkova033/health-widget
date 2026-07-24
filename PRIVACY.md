# Privacy Policy

**Last updated:** 2026-07-24

HealthWidget is built around one rule: your data never leaves your device.

## What HealthWidget collects

Nothing. HealthWidget does not collect, store remotely, transmit, or sell any personal
data, usage data, or analytics of any kind.

## What HealthWidget stores, and where

The app stores a small amount of data **only on your device**, using Android's local
DataStore mechanism:

- Your notification frequency, sleep alert toggle, quiet-hours, widget background style, and
  widget refresh interval settings.
- The most recently shown tips (up to the last 30), so the same one doesn't come up again
  too soon.

None of this is ever transmitted anywhere. There is no server for it to go to: the app has
no network permission at all (it does not request `INTERNET`), so it is technically
incapable of sending data off the device, even if it wanted to.

## Third parties

There are none. HealthWidget contains no analytics SDK, no crash-reporting SDK, no
advertising SDK, and no other third-party tracking library of any kind.

## Accounts

There is no account system. HealthWidget does not know who you are.

## Permissions HealthWidget requests, and why

- **Notifications (`POST_NOTIFICATIONS`, Android 13+):** required to show nudge and
  sleep-alert notifications. You can decline this and still use the home-screen widget.
- **Receive boot completed:** used only to reschedule your existing notification and
  widget-refresh preferences after your device restarts.

HealthWidget does not request location, contacts, storage, camera, microphone, health, or
any other sensitive permission.

## Backups

Android's built-in backup may include the small settings values described above if you
have device backup enabled in Android system settings — the same as it would for any
app's local preferences. This is standard Android backup behavior, not something
HealthWidget does on its own, and can be turned off in your device's system settings at
any time.

## Children's privacy

Because HealthWidget collects no data at all, it does not collect data from anyone,
including children.

## Changes to this policy

If this policy ever changes, the "Last updated" date above will change too, and the new
policy will be included in the app's next release.

## Contact

For questions about this policy, open an issue on the project's repository.
