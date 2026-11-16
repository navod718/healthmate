# Signed release verification — 16 November 2026

Exact artifact: releases/HealthMate-0.1.1-release.apk (0.1.1, version code 1).

[Successful signed APK smoke check](https://github.com/navod718/healthmate/actions/runs/43851756953), checking integration commit 581dd5b. The final artifact includes the dashboard note fix and uses the existing signing certificate.

- SHA-256 checksum matched.
- Android APK signature schemes v2 and v3 verified.
- Installation succeeded on an Android API 29 emulator.
- MainActivity opened with Status: ok on initial launch and after force-stop/relaunch.
- Both launches showed HealthMate, Steps walked, Dashboard and Records in the accessibility hierarchy.
- The crash log contained no FATAL EXCEPTION during this check.
- Screenshots, hierarchy dumps and launch/process logs were uploaded as the signed-release-smoke-evidence Actions artifact.

Scope: installation and dashboard startup/relaunch, not a complete signed-release CRUD or physical-device acceptance test. CRUD, persistence and validation are covered by the separate debug instrumentation/JVM suites; Navod should still review the signed app before recording.

Earlier smoke-check failures were overstrict dashboard assertions: the Add action was absent from the captured accessibility hierarchy. The final check verifies the documented startup scope using stable dashboard and navigation labels. It does not establish Add-action accessibility; a full TalkBack pass remains outstanding.
