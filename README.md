# HealthMate - Person 1 development

Native Android health tracker built with **Kotlin, Jetpack Compose and Room**, using only [navod718/healthmate](https://github.com/navod718/healthmate).

## Implemented scope: your part

- **Add Record:** date selection, steps, calories and water inputs; field validation; asynchronous saving; useful errors.
- **Dashboard:** reactive totals for today's entries, loading/error states and zero totals for an empty day.
- **Shared UI foundation:** Material 3 theme, reusable inputs/cards, Dashboard-to-Add navigation and back handling.
- Minimum Room scaffolding for insertion and daily totals, so these features can run independently.
- Validation unit tests, Add/Dashboard instrumentation tests and a GitHub Actions build workflow.

History, date filtering, editing, deletion and demo-data management are **not implemented** here: those belong to Person 2. This is a partial app for the agreed workload, not the complete coursework submission.

Multiple entries per day are summed. Dates use the device local date. Values must be non-negative whole numbers fitting an Int, with at least one positive value. Water uses ml. Records remain on this device; uninstalling or clearing app data removes them. Backup is disabled. There is no network permission or backend service.

## Run in Android Studio

1. Open this directory in Android Studio supporting Android Gradle Plugin 8.9.1.
2. Use Gradle JDK 17 or newer (CI uses 17).
3. Install Android SDK Platform 35 and accept its licenses. Let Android Studio create the local SDK configuration.
4. Sync Gradle and run `app` on Android 8.0 / API 26 or newer.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest # requires an Android device/emulator
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. A debug APK is for development, not the final signed release deliverable.

## Architecture and integration

```text
Compose screens -> HealthViewModel -> HealthRepository -> HealthRecordDao -> Room / SQLite
```

- `core/database`: minimal Room database, version 1, exported schema; future changes require explicit migrations.
- `core/validation`: shared validation rules, reusable by Person 2's future Edit form.
- `feature/health/data`: `HealthRecord`, `DailyTotals`, insert/totals DAO and repository.
- `feature/health/presentation`: Add form, Dashboard and ViewModel/StateFlow.
- `ui/theme`: reusable Material 3 theme.

### Contract for your friend's work

Keep `health_records`: `id` (Long primary key), `date` (YYYY-MM-DD text), `steps`, `calories`, `water` (Int). Extend the existing DAO and repository with record reads, date filtering, updates, deletes and optional demo loading. Keep the existing insert and totals contracts. Room observation will update dashboard totals when future update/delete operations modify records.

Add Records navigation alongside the existing dashboard, and reuse `RecordValidator` for Edit. Coordinate changes to Gradle, the entity, database, repository and app navigation before merging. Person 2's features and tests remain their workstream.

## Verification

JVM tests cover required values, numeric boundaries, future/malformed dates and zero-only entries. Instrumentation tests cover day-specific totals, large sums and Add form validation/save-to-dashboard behavior. Device tests must not be marked passed until actually run on a device/emulator. CI runs unit tests/lint, builds a debug APK and compiles the instrumentation APK.

## Coursework context

The original draft specifies sqflite; this follows the user's reported lecturer instruction to use Kotlin, Compose and Room. Keep that clarification. Follow individual-coursework collaboration rules and accurately acknowledge assistance and AI use. Planning PDFs are in `docs/`; their full-app scope describes the eventual combined project.
