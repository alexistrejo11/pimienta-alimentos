# Pimienta POS

Native Android point of sale for Pimienta Alimentos school cafeterias. The
application is designed for local-first sales and resilient synchronization
with the central API.

## Status

Version `2.1.0` with Android `versionCode` 2. The production delivery path is
prepared: a signed release APK is published to S3 by GitHub Actions after a
successful merge to `main`.

## Features

- Offline-first cafeteria sales
- Local persistence for operational data
- Operator access and manager controls
- Device enrollment and credential refresh
- Background synchronization with the central API
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

The POS workflow runs JVM tests, builds the signed release APK, and publishes
`latest.apk` with its `current.json` manifest under the POS release prefix in
S3. The web application obtains a time-limited download URL from the backend;
the APK is not deployed as a Docker service.

## Documentation

POS product, architecture, workflow, integration, and implementation
navigation is organized under `docs/`.

## License

Apache License 2.0.
