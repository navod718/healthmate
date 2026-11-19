# HealthMate Android release APK

This folder contains the signed release APK for HealthMate 0.3.0 (version code 5).

- Package: `com.navod718.healthmate`
- Android requirement: Android 8.0 / API 26 or newer
- Build: `assembleRelease`, then Android SDK `zipalign` and `apksigner`
- The release signing key and passwords are private and are not stored in this repository.

Download the APK to an Android device or drag it onto an Android Studio emulator. If an existing installation has a different signature, Android will reject an in-place upgrade. Back up any records you need before uninstalling that other app; uninstalling deletes its local data.

This APK does not run on iPhone. For the coursework, upload the APK to Google Drive, set the link to Anyone with the link / Viewer, and add that link to Appendix B of the report.

Version 0.3.0 adds the Insights tab (trend chart, goal streak, achievements), the redesigned dashboard (overall goal ring, gradient metric cards, count-up numbers, water quick-add) and a search screen with a dashboard day view. The original release keystore was lost, so 0.3.0 is signed with a new key (same subject `CN=Navod Daham, OU=HealthMate`); it cannot update an installed 0.1.x release in place — uninstall the older app first, which deletes its local data. The previous 0.1.0 and 0.1.1 APKs are retained for traceability.
