# InstaLock

**Take a break from Instagram — one you can't undo.**

InstaLock is a minimal Android focus app. Pick a duration (30 minutes to 30 days),
hold to confirm, and Instagram stays blocked until the timer runs out — the app,
`instagram.com` in browsers, and even the settings screens you'd use to cheat.

> ⚠️ The lock **cannot be stopped or shortened once started — not even by you.**
> Only start a block you're sure about.

---

## Download & install

Grab the APK from the [**Releases**](../../releases) page
(`InstaLock.apk`), then sideload it on your phone:

1. Copy the APK to your phone (or download it directly on the phone).
2. Open it and allow **Install unknown apps** when asked.
3. Requires **Android 7.0 (API 24) or newer**.

> 📝 This APK is signed with a **debug key**, which is fine for sideloading but
> means a future update signed with a different key will require uninstalling
> first. If a timer is running, let it finish before uninstalling.

---

## How to use

1. Open InstaLock. The **Setup** card lists four items:
   - **01 · Accessibility** — *required.* This is how InstaLock detects
     Instagram. Without it, the start button stays disabled.
   - **02 · Device admin** — makes uninstalling take extra steps.
   - **03 · Battery** — exempts InstaLock from battery optimization so the
     watcher keeps running in the background.
   - **04 · Autostart** — opens the autostart page on Xiaomi, Oppo, Vivo,
     Realme, Huawei, etc.
   - On Android 13+, if the Accessibility toggle is greyed out: open
     **App info → ⋮ → Allow restricted settings** first.
2. Pick a length: **30 min, 1 hour, 3 hours, 1 / 3 / 7 days**, or a
   **Custom** duration (days + hours + minutes, 1 minute – 30 days).
3. Tap the start button. A confirmation sheet spells out exactly what happens.
   Tick **"I understand this can't be undone"**, then **press and hold** the
   button (~1.6 s) to start.
4. While locked, the app shows a live countdown ring plus start/end times.
   When the timer ends, Instagram unlocks by itself.

---

## How it works

Everything is plain Android SDK, zero dependencies, no AndroidX — the entire UI
is built programmatically in Java (no layout XML).

### Starting a block — `MainActivity.java` + `HoldButton.java`

- Duration chips write to a `selected` value; the custom picker combines
  day/hour/minute `NumberPicker`s with 1-minute minimum and 30-day maximum.
- The consent sheet lists the four lock rules, warns if device-admin or
  battery setup is off, and requires two deliberate actions: ticking the
  checkbox (which enables the button) and holding `HoldButton` for 1600 ms.
  Releasing early animates the fill back to zero; nothing fires.
- On completion, `LockState.start(duration)` writes `start` and `until`
  (wall-clock) timestamps to `SharedPreferences` and anchors the monotonic
  clock (see below). There is deliberately **no stop/shorten API**.

### The lock record — `LockState.java`

- `isLocked()` is a single comparison: `until > now`. Every screen and the
  background service reads the same record, so the state is always consistent.
- `progress()` gives the 0–1 elapsed fraction that drives the countdown rings.
- **Clock-change protection:** `start()` also stores a pairing of wall-clock
  time and `SystemClock.elapsedRealtime()` (which the user can't change).
  When the system clock is edited, `onClockChanged()` compares the new
  wall-clock against the expected wall-clock derived from the monotonic
  anchor; if they differ by more than 5 s while a lock is still active in
  real time, `until` (and `start`) are shifted by the same jump — moving the
  clock forward buys nothing, and moving it back doesn't extend anything
  beyond real elapsed time.

### The watcher — `BlockerService.java` (AccessibilityService)

While a timer is active, the service reacts to window events:

| Trigger | What happens |
|---|---|
| Foreground app is `com.instagram.android` or `com.instagram.lite` | Blocked immediately |
| A browser's address-bar view contains `instagram.com` (matched via URL-bar view IDs like `url`, `omnibox`, `address`) | Blocked |
| A system screen (Settings, package installer, permission controller, Play Store, OEM security apps…) whose visible text mentions InstaLock | Blocked — this closes App-info / Uninstall / Accessibility / Device-admin pages opened to tamper with the lock |

Blocking = press Back, go Home, then open `BlockedActivity` with a message
("Instagram can wait.", "Not in the browser either.", "These settings are
locked."). Scans run on every window-state change plus throttled (400 ms)
content-change checks, with 800 ms block coalescing and a depth-30 tree-walk
cap so it stays cheap. The service ignores its own package.

The service declaration lives in `res/xml/accessibility_config.xml`
(window-state + content-change events, window content retrieval, view IDs).

### The blocked screen — `BlockedActivity.java`

A full-screen notice with the reason, a live countdown ring (`TimerFace` +
`RingView` — watch-face styling with 60 ticks and a progress arc), and the
human-readable end date ("It ends Friday, 6:00 PM."). Back press just returns
to the home screen. `singleTask` + excluded from recents.

### Surviving reboots & tampering — `SystemReceiver.java` + `AdminReceiver.java`

- `BOOT_COMPLETED` → re-anchors the monotonic clock pairing so later
  clock-change math stays correct. (The lock itself persists because it's a
  wall-clock timestamp in preferences.)
- `TIME_SET` → runs the clock-shift compensation described above.
- `AdminReceiver` (device admin, `res/xml/device_admin.xml`) shows a warning
  when disabling admin is requested, adding friction to uninstalls.
- `RECEIVE_BOOT_COMPLETED` and `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` are the
  only install-time permissions; backup is disabled (`allowBackup=false`) so
  the lock record can't be restored away.

### Design tokens — `Ui.java`

One accent (`#FF5A36` ember orange on near-black `#0D0D0C`), warm neutrals,
light/medium system sans, pill status chips ("Ready" / "Setup needed" /
"Active"), bottom-sheet dialogs, haptics on taps.

---

## Project structure

```
instalock/
├── app/
│   ├── build.gradle                 # com.focus.instalock, minSdk 24, targetSdk 34
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/focus/instalock/
│       │   ├── MainActivity.java     # setup UI, duration picker, consent, locked view
│       │   ├── LockState.java       # lock record + clock-tamper protection
│       │   ├── BlockerService.java  # accessibility watcher
│       │   ├── BlockedActivity.java # "Instagram can wait." screen
│       │   ├── SystemReceiver.java  # boot + clock-change receiver
│       │   ├── AdminReceiver.java   # device-admin receiver
│       │   ├── HoldButton.java      # press-and-hold confirm button
│       │   ├── TimerFace.java       # countdown ring + time text
│       │   ├── RingView.java        # watch-face progress ring
│       │   └── Ui.java              # colors, type, view helpers
│       └── res/
│           ├── drawable/ic_launcher.xml
│           ├── values/strings.xml
│           └── xml/
│               ├── accessibility_config.xml
│               └── device_admin.xml
├── build.gradle                     # AGP 8.5.2
├── settings.gradle
└── gradle.properties                # plain SDK build, no AndroidX
```

## Build from source

Prerequisites: JDK 17, Android SDK with API 34 + build-tools, Gradle 8.7+.

```bash
# point Gradle at your SDK (or set ANDROID_HOME / ANDROID_SDK_ROOT)
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

gradle :app:assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

Or just open the folder in Android Studio and press **Run** — the release
build type is preconfigured to sign with the debug key so the APK installs
directly (`app/build.gradle`).

## Limitations & honesty

- Sideload-only, debug-signed (see install note above).
- The watcher needs Accessibility + background execution; aggressive OEM task
  killers can still starve it — that's what steps 03/04 of setup mitigate.
- Blocking `instagram.com` relies on recognizing browser address-bar views;
  unusual browsers may slip through the URL check while the native apps are
  still always caught.
- Device admin adds uninstall friction, not impossibility: a determined user
  with the phone in hand can eventually remove any app. InstaLock is a
  commitment device, not DRM.

## Privacy

No accounts, no network calls, no analytics. The accessibility service reads
window content **on-device only** to detect Instagram and tamper attempts —
nothing is collected or sent anywhere.

## License

MIT — see [LICENSE](LICENSE).
