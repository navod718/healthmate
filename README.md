# HealthMate

HealthMate is a native Android health and wellness tracker. It stores daily steps, calories burned and water intake in millilitres on the device using Room and SQLite.

## Features

- View today's activity totals and progress toward editable daily goals.
- Add, edit and delete records, and filter history by date. Changed drafts prompt for confirmation before dismissal.
- Load four optional sample records into an empty history for a demonstration.

## Architecture

The app uses Kotlin, Jetpack Compose and Material 3. Its MVVM structure separates the Compose screens from UI state and storage. `HealthViewModel` exposes `StateFlow` for lifecycle-aware collection and uses `SavedStateHandle` to restore the editor draft, selected tab and date filter. `HealthRepository` validates writes and delegates database operations to `HealthRecordDao`.

```text
Compose screens → HealthViewModel → HealthRepository → HealthRecordDao → Room / SQLite
```

Room schema version 2 contains `health_records` and `daily_goals`. The explicit migration from version 1 adds goals while preserving existing health records. Exported schemas are in [app/schemas](app/schemas).

## Build and test

Open the project in Android Studio with support for Android Gradle Plugin 8.9.1. Install Android SDK Platform 35 and Build Tools 35.0.0. The app supports Android 8.0 (API 26) and later.

Use JDK 17 for Gradle to match CI. Local builds were also verified with JBR 21. On Windows, replace `./gradlew` with `.\gradlew.bat`.

```sh
./gradlew testDebugUnitTest testReleaseUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
./gradlew assembleRelease
```

The instrumentation command requires a connected device or emulator. The [CI workflow](.github/workflows/android.yml) uses an API 29 emulator. The debug APK is written to `app/build/outputs/apk/debug/`. Without a release signing configuration, `assembleRelease` produces `app/build/outputs/apk/release/app-release-unsigned.apk`. Use a private release key for the coursework APK and keep signing credentials outside source control.

## Documentation

| Document | Purpose |
| --- | --- |
| [Brand, motion and state](docs/HealthMate_Brand_and_Motion.md) | Current implementation, verification results and local evidence index |
| [Project blueprint](docs/HealthMate_Project_Blueprint.pdf) | Historical design and submission plan, with its AI-assistance disclosure |
| [Work allocation](docs/HealthMate_Work_Allocation.pdf) | Earlier proposal to split UI and data work |
| [Development workload](docs/HealthMate_Development_Workload.pdf) | Later proposal to split development work by feature |

The allocation documents record proposed work. Actual contributions should be recorded separately. The coursework brief describes individual assessment, so follow the lecturer's collaboration guidance and accurately acknowledge assistance and AI use. HealthMate implements SQLite through Room. The brief names `sqflite`, and lecturer guidance accepting Room was reported. Retain any written clarification with the coursework records.
