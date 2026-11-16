# HealthMate development plan

Project contact: Navod Daham (Navod)  
Kingston ID: K2635839  
Repository: https://github.com/navod718/healthmate

## Scope

Development uses Kotlin, Jetpack Compose and Room. This document organises the development scope under Navod’s name. It is a maintenance plan, not a declaration that every change was independently authored by Navod. Actual contributions, collaboration and AI assistance must be described accurately in the coursework report.

## Navod — application foundation

Maintain the Android project configuration, Material 3 theme, adaptive icon, starting screen and navigation. Keep screen state lifecycle-aware and restore unfinished editor drafts with SavedStateHandle.

## Navod — activity entry and dashboard

Maintain the dated record form, whole-number validation, save feedback and unsaved-change confirmation. Display daily steps, calories and water totals from Room, with editable daily goals and explicit loading, empty and error states.

## Navod — records management

Maintain record history, date filtering, editing, confirmed deletion and optional sample records for an empty database. Keep the dashboard and history consistent after every database change.

## Navod — persistence and integration

Maintain the health_records and daily_goals entities, DAO, repository and versioned Room schemas. Preserve existing records during migrations. Validate writes in the repository as well as displaying field feedback in the UI.

## Navod — verification and maintenance

Run JVM validation and state tests, Android Room and Compose tests, and lint/build checks after relevant code changes. Review add, view, edit, filter, delete, totals, goals, persistence and draft restoration on a device or emulator. Record actual results and keep meaningful Git history.

## Current delivery references

The application, report draft and signed APK are integrated into main. The original docs/technical-report and build/release-apk branches retain the deliverable history. The release folder contains the APK, checksum and signature verification. The private signing key must remain outside Git.

## Coursework handoff

The development plan does not replace the coursework report or final submission checklist. Review the report, complete personal reflection and declarations, record the narrated demonstration, upload the APK/video to Google Drive with Viewer link access, and follow the final ELMS instructions.
