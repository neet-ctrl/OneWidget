# Sakura Clock Weather Widget

This repository is a standalone native Android home-screen widget. It contains
no web app and no JavaScript runtime.

## What it does

- Uses the supplied Sakura widget artwork as the visual base.
- Redraws the clock every second using India Standard Time (`Asia/Kolkata`),
  scheduling the next second boundary with an exact alarm when the device
  allows it and an Android-safe idle-mode fallback.
- To enable the most accurate timing on Android 12+, allow **Sakura Clock
  Weather** under **Settings → Apps → Special app access → Alarms & reminders**.
- Displays the current day and date in the widget.
- Starts with the reference weather value of `34° / Cloudy`.
- Refreshes weather from the no-key Open-Meteo endpoint roughly every 15 minutes.
- Uses Delhi coordinates by default. Change `LATITUDE` and `LONGITUDE` in
  `WeatherRepository.kt` for another city.
- Can be resized horizontally and vertically by the launcher.
- Uses a transparent outside area, so the launcher wallpaper shows around the
  Sakura outline instead of a black rectangle.

## Build with GitHub Actions

Push this repository to GitHub. The workflow in
`.github/workflows/build-android.yml` builds the unsigned debug APK as a
build-check. The separate `.github/workflows/release-android.yml` workflow
builds the signed release APK and creates a GitHub Release with the APK
attached as `sakura-clock-weather.apk`.

Signed releases are created on pushes to `main` or `master`, and from a
manual workflow run. Pull requests only run the debug build-check, so signing
secrets are never made available to pull-request code.

### GitHub Actions signing secrets

Add these four repository secrets in **GitHub → Settings → Secrets and
variables → Actions**:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64 text of your `.keystore` file |
| `ANDROID_KEYSTORE_PASSWORD` | Password used for the keystore |
| `ANDROID_KEY_ALIAS` | Alias of the signing key, for example `sakura` |
| `ANDROID_KEY_PASSWORD` | Password used for that key alias |

Generate a new keystore on a secure computer and keep the original file backed
up privately:

```bash
keytool -genkeypair -v \
  -keystore sakura-release.keystore \
  -alias sakura \
  -keyalg RSA -keysize 2048 -validity 10000

base64 -w 0 sakura-release.keystore > sakura-release.keystore.base64
```

Copy the one-line contents of `sakura-release.keystore.base64` into
`ANDROID_KEYSTORE_BASE64`. Do not commit the keystore, the base64 file, or any
of the passwords to GitHub.

## Local build

With Java 17 and Gradle 8.9 installed:

```bash
gradle assembleDebug
```

Install the generated `app/build/outputs/apk/debug/app-debug.apk`, then add
**Sakura clock and weather** from the Android launcher widget picker.