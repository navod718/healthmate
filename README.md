# HealthMate

Native Android personal health tracker built with **Kotlin, Jetpack Compose and Room**, using only [navod718/healthmate](https://github.com/navod718/healthmate).

## Features

- **Dashboard:** reactive totals for today's steps, calories and water, plus editable daily goals and progress bars.
- **Add Record:** date selection, numeric validation, Next/Done keyboard actions, Room saving and useful errors.
- **Records:** newest-first history, exact-date filtering, clear filter, loading/error/empty states.
- **Edit:** prefilled shared form, validation, saved edit drafts and updates that keep the original id.
- **Delete:** explicit confirmation, cancel, retry on failure and immediate dashboard refresh.
- **Sample records:** a confirmation adds four examples for today and the previous three days, only when no records exist. A Room transaction prevents concurrent loads from creating duplicates. Samples are editable/deletable like other records; they are never loaded automatically.
- **Draft protection:** Back asks before discarding changed Add/Edit forms. ViewModel/SavedStateHandle retain form values, edit identity, original values, selected tab and date filter across recreation/restoration.

Multiple entries per day are summed. Dates use the device's local date; future dates cannot be entered. Values must be non-negative whole numbers fitting an Int, with at least one positive value. Water uses ml. Progress bars stop at 100% while actual totals remain visible.

Records and goals stay on this device. Uninstalling or clearing app data removes them; backup is disabled. There is no network permission or backend service.

## Run in Android Studio

1. Open this directory in Android Studio supporting Android Gradle Plugin 8.9.1.
2. Use Gradle JDK 17 or newer (CI uses 17).
3. Install Android SDK Platform 35 and accept its licenses. Let Android Studio create the local SDK configuration.
4. Sync Gradle and run `app` on Android 8.0 / API 26 or newer.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest # requires an Android device/emulator
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. For the final submission, generate a signed release APK with your own signing key. Do not commit signing keys or passwords.

## Architecture

```text
Compose screens -> HealthViewModel -> HealthRepository -> HealthRecordDao -> Room / SQLite
```

- `core/database`: Room database version 2, exported schemas, explicit migration 1 to 2 preserving existing records while adding goals.
- `core/validation`: shared Add/Edit validation.
- `feature/health/data`: records, goals, repository and parameterized DAO operations.
- `feature/health/presentation`: Dashboard, shared Add/Edit form, Records screen, goals dialog and state management.
- `ui/theme`: shared Material 3 theme.

The `health_records` schema remains `id` (Long primary key), `date` (YYYY-MM-DD text), `steps`, `calories`, `water` (Int). History orders by date descending, then id descending. Editing changes the existing row; a missing row is reported instead of inserting a replacement. Totals update reactively, including when an edit moves a record to another date.

`daily_goals` stores one settings row. Records-management work adds no schema changes and preserves `HealthDatabase.MIGRATION_1_2`. Destructive migrations are not enabled.

## Workstreams

- Person 1: Add Record, Dashboard, shared UI/navigation, draft protection and daily goals (merged through PR #1).
- Person 2: history, filtering, editing, deletion and sample-data loading. This work starts from the merged Person 1 foundation on `feature/records-management`.

Both workstreams use the existing entity, validation, repository and ViewModel. Coordinate shared-file changes before merging.

## Verification

JVM tests cover validation, draft restoration, edit identity, missing records, confirmation/retry behavior and goal progress. Android tests cover UI flows, keyboard navigation, database CRUD/filter/order/totals, persistence, migration, activity recreation and atomic sample loading. GitHub Actions runs unit tests, lint, APK builds and Android 10 / API 29 emulator tests. Device tests must only be described as passed after execution; physical-device and additional Android-version testing remain useful.

## Coursework context

The draft's sqflite requirement is replaced here by the user's reported lecturer instruction to use Kotlin, Compose and Room. Retain that clarification. The supplied brief describes individual coursework: follow the lecturer's collaboration rules and accurately acknowledge assistance and AI use. Planning PDFs are in `docs/`.
