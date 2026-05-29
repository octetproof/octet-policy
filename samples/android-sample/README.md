# Android sample — run on your Android phone

A real Android app (Jetpack Compose + Material 3) that starts the
OctetSDK and evaluates every v1 predicate against your phone's **live
location**. It shows a map of where you are (OpenStreetMap / osmdroid),
a **Generate proof & evaluate** button (each tap mints a fresh
location proof and re-runs the predicates), and a country picker that
asks the SDK `loc.isWithin(country)` for any ISO country.

## Prerequisites

- **Android Studio** Ladybug (2024.2) or newer with the Android SDK
  installed. `ANDROID_HOME` set (or `ANDROID_SDK_ROOT`).
- **Android phone** running **Android 11 (API 30) or newer** — the
  OctetSDK requires `minSdk 30`. **USB Debugging** enabled in the
  phone's developer options.
- **An OctetSDK license key.** Get one from
  [sdk.octetproof.com/signup](https://sdk.octetproof.com/signup) — a
  free trial key works for evaluation.

The Gradle wrapper (`gradlew`) is committed, so you don't need Gradle
installed system-wide.

For deeper SDK documentation see
[docs.octetproof.com](https://docs.octetproof.com).

## 1. Add your license key

Create a **gitignored** `samples/android-sample/local.properties`
file with your key:

```
octet.licenseKey=octet_live_xxxxxxxxxxxxxxxxxxxx
```

It's read into `BuildConfig.OCTET_LICENSE_KEY` at build time. Without
it the app launches but shows a "no license key" message.

The key must be unquoted, no spaces around `=`, on a single line. If
the `local.properties` file already exists with other entries
(e.g. `sdk.dir`), just add the `octet.licenseKey` line alongside.

## 2. Build and run

### Option A — Android Studio (IDE)

1. **File → Open** → select `samples/android-sample/` → let Gradle
   sync.
2. Connect your phone via USB; accept the **Allow USB debugging?**
   prompt on the device.
3. Pick your device in the device dropdown at the top of Android
   Studio.
4. **Shift+F10** (Run). On first launch, grant the **Location**
   permission when prompted. The **Activity Recognition** permission
   is optional; granting it gives the predicates fuller motion
   context.

### Option B — Command line

From the repository root:

```bash
cd samples/android-sample
./gradlew :app:installDebug          # build + install on the connected device
adb shell am start -n com.octetproof.octetpolicysample/.MainActivity
```

Or just build an APK without installing:

```bash
cd samples/android-sample
./gradlew :app:assembleDebug
# APK lands at app/build/outputs/apk/debug/app-debug.apk
```

## What you should see

- A **map** centered on your current location (osmdroid + the OS
  fused-location provider — the OctetSDK is a containment oracle and
  exposes no raw coordinates).
- A **Generate proof & evaluate** button. Tap it (give the sensors a
  few seconds to warm up first) — it mints a fresh location proof and
  runs `isSingapore`, `isUS`, `isUSState([CA, NY, TX])`, and
  `isOfacComprehensive` against your live location, rendering each
  `PolicyResult` (match, reason, country/state/confidence).
- A **country picker** that calls `loc.isWithin(country)` and shows
  inside / outside / can't-tell.

A cold start before the sensors warm up reads back
`VERDICT_INDETERMINATE` — the honest "can't tell", not a false "no".
Tap again after a moment.

## How this sample is wired up

- Jetpack Compose. AGP 8.5.0, Kotlin 2.1.20, JVM 17, `minSdk 30`.
- Single artifact dependency: `com.octetproof:octetpolicy` (the
  umbrella). It pulls in every predicate, `PolicyResult`, and —
  transitively via `api(...)` — the OctetSDK API (`Octet`, `OctetLoc`,
  `OctetRegion`, `OctetVerdict`).
- `settings.gradle.kts` resolves `octetpolicy` from
  [`https://raw.githubusercontent.com/octetproof/octet-policy/mvn-repo`](https://raw.githubusercontent.com/octetproof/octet-policy/mvn-repo)
  and the SDK AAR from
  [`https://raw.githubusercontent.com/octetproof/octet-sdk-android/mvn-repo`](https://raw.githubusercontent.com/octetproof/octet-sdk-android/mvn-repo).
  A local `file://` fallback also exists for maintainers hacking on
  unpublished versions — see `settings.gradle.kts`.

## Troubleshooting

**"Could not find com.octetproof:octetpolicy:…"** or **"Could not
find com.octetproof:sdk:…"** — Check internet access; both artifacts
are fetched from public `mvn-repo` branches on GitHub.

**"license key is malformed"** at runtime — Check `local.properties`:
the key must be unquoted, no spaces around `=`, on a single line.

**"SDK refused to start: license expired"** — Trial keys from
[sdk.octetproof.com/signup](https://sdk.octetproof.com/signup) are
valid for 90 days plus a 15-day grace period. Request a new one.

**Compose Compiler mismatch** — The Compose plugin tracks the Kotlin
version; both are `2.1.20` here. If you bump one, bump the other.
