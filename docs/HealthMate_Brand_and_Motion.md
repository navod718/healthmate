# HealthMate brand, motion and state

## Brand system

The HealthMate mark uses two rounded uprights and a rising crossbar. The uprights keep the H legible in the toolbar and launcher. The rising mint stroke suggests steady daily progress. The vector uses two opaque paths so Android can use the same silhouette for its themed icon.

| Role | Colour |
| --- | --- |
| Launcher tile and dashboard header | Indigo `#263B68` |
| Mark crossbar and dashboard date | Mint `#8EE5D3` |
| Mark uprights and light app background | Off-white `#F5FAF9` |
| Light primary action | Teal `#006872` with white text |
| Dark primary action | Light teal `#85DAD8` with `#00363B` text |
| Dark app background | `#101C20` |

The launcher foreground and background occupy the standard 108 dp layer. The visible strokes are about 55 × 51 dp and are centred inside the 66 dp safe zone. API 26–32 uses the adaptive colour layers. API 33+ adds a monochrome layer for launchers that support themed icons. The toolbar uses the same glyph and indigo tile next to the HealthMate wordmark.

The manifest registers the adaptive launcher icon and starting theme. API 26–30 uses the branded window background. API 31+ uses the platform splash with the same vector mark and matching light or dark background. The API 27 theme override applies `windowLightNavigationBar` only on supported versions. The base theme remains valid on API 26.

The dashboard places the date and title in an indigo header. Activity values share a high-contrast type hierarchy, with colour reserved for metric badges and progress. History uses the same card radius and surface roles, plus an explicit empty-state card. The editor keeps its existing field order and keyboard actions and adds a visible date label. Layouts remain scrollable and content is centred with a 640 dp width cap.

## Motion

| Interaction | Implementation |
| --- | --- |
| App entry | Android 12+ displays the branded system splash. On fresh activity creation, its exit fades over 180 ms once the activity draws. Android 8–11 uses a branded starting window. Matching light and dark window and Compose backgrounds keep the handoff consistent. |
| Toolbar wordmark | 420 ms fade and scale from 96% on fresh activity creation. It runs alongside app entry, with no timed loading gate or separate splash activity. The wordmark does not replay during tab changes or activity restoration. |
| Dashboard ↔ Records | 220 ms short horizontal arrival with RTL-aware direction and a 180 ms fade. |
| Enter and leave editor | 240 ms rise on entry and 200 ms reverse arrival of the previous screen. Only the active destination has interactive content or accessibility nodes during the transition. |
| Record changes | Stable record IDs drive 160 ms insert fade, 140 ms removal fade and 200 ms placement. A changed date filter gets a fresh list scope so unrelated result sets do not animate as edits. |
| Goal progress | A 360 ms tween interpolates the displayed bar when its target changes. Numeric totals and percentages update immediately. |
| Dialog and save feedback | Material dialogs, button ripples and snackbars provide feedback. The native date picker has composition-scoped cleanup and restores visibility with the draft during activity recreation. |

Compose uses the system animation scale. The platform splash exit removes the splash immediately when Android animators are disabled. The dashboard renders normal Room loading and error states as soon as Compose is ready. Date refresh is scoped to the resumed lifecycle and runs immediately on resume, then at most once a minute while resumed.

## Architecture refinements

- **HM-AUD-006** preserves validation errors for fields the user has not changed. A changed field with visible feedback is revalidated, and the all-zero message clears when corrected. The repository still validates every write.
- **HM-AUD-007** makes the goals observer restartable from the dashboard and dialog. A failed read keeps the last loaded or successfully saved targets. A save after a read failure restarts observation, while a healthy save continues to use Room's existing invalidation stream.
- History and dashboard render the date carried in their own loading, result and error state. A delayed result cannot be displayed under a different date label.
- The editor rejects repeated Add actions while open. Database entities, schema files, queries and migrations are unchanged.

## Verification

An AI-assisted engineering review ran the checks below on the project's behalf. The results record the scope and limits of that verification.

| Check | Observed result |
| --- | --- |
| Builds after the API 27 theme correction | `assembleDebug`, `assembleDebugAndroidTest` and lint-gated `assembleRelease` passed on JBR 21. `lintDebug` reported 0 errors and 4 warnings. Local dependency resolution used a temporary repository containing the checksum-verified Google Maven lint compiler. |
| JVM tests after the correction | 19 passed in each of `testDebugUnitTest` and `testReleaseUnitTest`. |
| API 37 instrumentation before the correction | 16 passed. Earlier attempts were blocked by emulator startup timeouts. Espresso 3.7.0 resolved a separate API 37 compatibility issue in 3.6.1. [AndroidX Test release notes](https://developer.android.com/jetpack/androidx/releases/test) |
| QA release smoke before the correction | Nine checks passed on the installed release variant, covering validation, CRUD, filtering, totals, goals, persistence, draft restoration and reduced-motion navigation. Visual checks included the launcher, splash, light and dark screens and 1.3× text. |
| Device reruns after the correction | The QA release installed and eventually displayed `MainActivity`. The smoke runner timed out before its first assertion on the overloaded emulator. The debug APK installation failed during the instrumentation rerun. |

For an already installed debug and instrumentation APK pair, replace `<device-serial>` with an ID from `adb devices`.

```sh
adb -s <device-serial> shell am instrument -w com.navod718.healthmate.test/androidx.test.runner.AndroidJUnitRunner
```

Calculated contrast includes 11.01:1 for white on indigo and 6.52:1 for the light primary action. At 1.3× text, the floating Add button can partially cover lower content until it is scrolled clear. The emulator recording showed stutter. Physical-device performance, a full TalkBack pass, the Android 8–11 starting window and launcher-applied monochrome tint remain unverified.

## Local evidence

Raw evidence remains in the ignored `captures/` and build directories.

| Local path | Evidence |
| --- | --- |
| `captures/release/lint-gated-release-build.txt` | Passing build after the API 27 correction. `lint-before-api27-fix.txt` records the earlier lint failure. |
| `app/build/test-results/testDebugUnitTest/` and `app/build/test-results/testReleaseUnitTest/` | JVM results for each variant. |
| `captures/release/android-instrumentation-before-api27-fix.txt` | Passing 16-test device run. The startup-timeout logs record separate failed attempts. |
| `captures/release/release-smoke-before-api27-fix.txt` | Passing nine-check release smoke run. `release-smoke.txt`, `release-smoke-system-ui-timeout.txt`, `release-final-smoke.txt` and `post-fix-emulator-diagnostics.txt` record failed or incomplete attempts. |
| `captures/release/` screenshots and `release-motion.mp4` | Visual evidence from the pre-fix QA release. The recording has no narration. |
| `app/build/outputs/apk/release/app-release-unsigned.apk` | Unsigned release output for signing with a private release key. |
| `captures/release/healthmate-release-qa.apk` and `apk-verification.txt` | Debug-key-signed release variant for QA, with package, signature and hash evidence. The coursework-signed APK remains outstanding. |

## Changed files

| Path | Change |
| --- | --- |
| `app/build.gradle.kts` | Espresso 3.7.0 for Android instrumentation |
| `app/src/main/AndroidManifest.xml` | Launcher icon and starting theme registration |
| `app/src/main/java/com/navod718/healthmate/MainActivity.kt` | Normal theme handoff and native splash exit |
| `app/src/main/java/com/navod718/healthmate/ui/theme/Theme.kt` | Light/dark tokens, typography and toolbar mark |
| `app/src/main/java/com/navod718/healthmate/core/validation/RecordValidator.kt` | Shared all-zero message for live validation |
| `app/src/main/java/com/navod718/healthmate/feature/health/presentation/HealthViewModel.kt` | Field errors, goals recovery, query dates and repeated Add guard |
| `app/src/main/java/com/navod718/healthmate/feature/health/presentation/HealthMateApp.kt` | Wordmark, scaffold, destination motion and resumed date refresh |
| `app/src/main/java/com/navod718/healthmate/feature/health/presentation/Screens.kt` | Dashboard and editor styling, goal animation and date picker lifecycle |
| `app/src/main/java/com/navod718/healthmate/feature/health/presentation/RecordsScreen.kt` | History styling and stable-key list motion |
| `app/src/main/java/com/navod718/healthmate/feature/health/presentation/GoalsDialog.kt` | Goals read recovery action |
| `app/src/main/res/drawable/healthmate_foreground.xml` | Original vector glyph, used by launcher, monochrome, splash and toolbar |
| `app/src/main/res/drawable/healthmate_starting_window.xml` | Starting window for Android 8–11 |
| `app/src/main/res/mipmap-anydpi-v26/ic_healthmate.xml` | Adaptive icon colour layers |
| `app/src/main/res/mipmap-anydpi-v33/ic_healthmate.xml` | Themed adaptive icon layer |
| `app/src/main/res/values/colors.xml` | Light launch window and launcher background |
| `app/src/main/res/values-night/colors.xml` | Dark launch window colours and system bar appearance |
| `app/src/main/res/values/styles.xml` | Base and legacy starting themes |
| `app/src/main/res/values-night/styles.xml` | Native dark window and dialog theme |
| `app/src/main/res/values-v27/styles.xml` | Navigation-bar appearance override on API 27+ |
| `app/src/main/res/values-v31/styles.xml` | Android 12+ platform splash styling |
| `app/src/test/java/com/navod718/healthmate/RecordsViewModelTest.kt` | Four state and recovery regressions |
| `app/src/androidTest/java/com/navod718/healthmate/HealthMateUiTest.kt` | Field-level error retention in the UI |
| `app/src/androidTest/java/com/navod718/healthmate/DraftRecreationTest.kt` | Date-picker and draft recreation regression |
| `docs/HealthMate_Brand_and_Motion.md` | Design and verification record |
