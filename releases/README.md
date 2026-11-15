# HealthMate Android release APK

This folder contains the signed release APK for HealthMate 0.1.0 (version code 1).

- Package: `com.navod718.healthmate`
- Android requirement: Android 8.0 / API 26 or newer
- Application source: `73ceb27a3f4a974612a1df09426443880927c772`
- Build: `assembleRelease`, then Android SDK `zipalign` and `apksigner`
- The release signing key and passwords are private and are not stored in this repository.

Download the APK to an Android device or drag it onto an Android Studio emulator. If an existing debug installation has a different signature, Android will reject an in-place upgrade. Back up any records you need before uninstalling that debug app; uninstalling deletes its local data.

This APK does not run on iPhone. For the coursework, upload the APK to Google Drive, set the link to Anyone with the link / Viewer, and add that link to Appendix B of the report.
