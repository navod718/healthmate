# HealthMate Android release APK

This folder contains the signed release APK for HealthMate 0.1.1 (version code 2).

- Package: `com.navod718.healthmate`
- Android requirement: Android 8.0 / API 26 or newer
- Application source: `f1b85f7`
- Build: `assembleRelease`, then Android SDK `zipalign` and `apksigner`
- The release signing key and passwords are private and are not stored in this repository.

Download the APK to an Android device or drag it onto an Android Studio emulator. If an existing debug installation has a different signature, Android will reject an in-place upgrade. Back up any records you need before uninstalling that debug app; uninstalling deletes its local data.

This APK does not run on iPhone. For the coursework, upload the APK to Google Drive, set the link to Anyone with the link / Viewer, and add that link to Appendix B of the report.

Version 0.1.1 adds subtle interface polish to the dashboard, records, input fields and goals dialog. It uses the same signing certificate as 0.1.0 and can update that signed installation while retaining local data. The previous 0.1.0 APK is retained for traceability.
