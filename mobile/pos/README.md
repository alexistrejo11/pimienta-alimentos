# Pimienta POS

Native Android point of sale for Pimienta Alimentos school cafeterias. The
application is designed for local-first sales and resilient synchronization
with the central API.

## Status

Version `2.2.0` with Android `versionCode` 3. The production delivery path is
prepared: a signed release APK is published to S3 by GitHub Actions after a
successful merge to `main`.

## Features

- Offline-first cafeteria sales
- Local persistence for operational data
- Operator access and manager controls
- Device enrollment and credential refresh
- Background synchronization with the central API
- Manual in-app APK update from Manager → Estado (installs over the existing app)
- POS telemetry and recovery from stale synchronization cursors
- Android hardware integration foundation

## Technology Stack

- Kotlin
- Jetpack Compose
- Android SDK 37
- Minimum Android API 26
- Room
- WorkManager
- Retrofit and Kotlin serialization
- Gradle

## Requirements

- JDK 17 for CI-compatible builds
- Android SDK with the required platform and build tools
- An Android device or emulator for manual validation

## Build and Test

```bash
./gradlew :app:assembleDebug
./gradlew :app:test
```

The release build requires the signing environment variables used by CI:
`POS_STORE_FILE`, `POS_STORE_PASSWORD`, `POS_KEY_ALIAS`, and
`POS_KEY_PASSWORD`.

## Release Delivery

Version source of truth is `app/build.gradle.kts` (`versionName` + `versionCode`).
Bump **`versionCode`** (required) and adjust **`versionName`** (semver) before
merging a POS change that should ship to devices. CI refuses to overwrite an
already-published `versionCode` object in S3.

On a successful push to `main`, the POS workflow:

1. Runs JVM unit tests and builds the signed release APK.
2. Uploads an immutable object:
   `pimienta/releases/pos/android/{versionCode}/pimienta-pos-{versionName}.apk`
3. Overwrites the alias `pimienta/releases/pos/android/latest.apk`.
4. Writes `pimienta/releases/pos/android/current.json` with
   `versionName`, `versionCode`, `s3Key` (the versioned key), and `uploadedAt`.

The public API `GET /api/v1/pos/releases/android/latest` reads `current.json`
and returns a time-limited pre-signed download URL for that `s3Key`. The web
home CTA and (later) in-app updates must use this endpoint — do not hardcode
Spring Boot or web versions to match the APK; the match is
**Gradle → APK BuildConfig + current.json → API**.

The installed app shows the build version on the mode banner and in
Manager → Estado.

## Documentation

POS product, architecture, workflow, integration, and implementation
navigation is organized under `docs/`.

## License

Apache License 2.0.
