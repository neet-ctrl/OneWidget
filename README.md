# Sakura Clock Weather Widget

This repository is a standalone native Android home-screen widget. It contains
no web app and no JavaScript runtime.

## What it does

- Uses the supplied Sakura widget artwork as the visual base.
- Updates the time every minute when Android allows the scheduled refresh.
- Displays the current day and date in the widget.
- Starts with the reference weather value of `34° / Cloudy`.
- Refreshes weather from the no-key Open-Meteo endpoint roughly every 15 minutes.
- Uses Delhi coordinates by default. Change `LATITUDE` and `LONGITUDE` in
  `WeatherRepository.kt` for another city.
- Can be resized horizontally and vertically by the launcher.

## Build with GitHub Actions

Push this repository to GitHub. The workflow in
`.github/workflows/build-android.yml` builds the debug APK and publishes it as a
workflow artifact named `sakura-clock-weather-debug`.

## Local build

With Java 17 and Gradle 8.9 installed:

```bash
gradle assembleDebug
```

Install the generated `app/build/outputs/apk/debug/app-debug.apk`, then add
**Sakura clock and weather** from the Android launcher widget picker.