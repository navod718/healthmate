# HealthMate demonstration recording guide — Navod

Use your own voice. Practise the steps before recording and adapt these suggested words to your own understanding. Do not describe a step as successful unless it works in your recording.

## 1. Prepare on your Mac

Download `releases/HealthMate-0.1.0-release.apk` and drag it onto the running Android Studio emulator. If Android reports a signature conflict with a debug installation, preserve any records you need before uninstalling the old app: uninstalling removes its local data. Use the installed signed release for the demonstration.

Open HealthMate, close distracting windows and notifications, and choose a readable emulator size. Press Shift–Command–5, select a recording area around the emulator, and use Options to select your microphone. Make a short test recording and listen to it before recording the full demonstration. Stop using the menu-bar recording button.

## 2. Suggested demonstration and narration

### Introduction — about 20 seconds

“My name is Navod Daham, Kingston ID K2635839. This is HealthMate, an Android health and wellness tracker built with Kotlin, Jetpack Compose and Room. It lets users manually record steps, calories burned and water intake. Records stay on the device.”

Show the dashboard. Explain that totals refer to today's date and progress compares the totals with editable goals. These are manual entries, not measurements taken from a step sensor.

### Validation and adding records — about 50 seconds

Tap Add record. Try saving empty fields, then enter zero in all three fields and save. Show the feedback. Enter 1200 steps, 60 calories and 500 ml for today, then save.

“The form validates the date and whole-number fields. At least one activity value must be greater than zero. After saving a valid record, the dashboard updates from the stored data.”

Add another record for today with 300 steps, 20 calories and 250 ml. Show totals of 1500 steps, 80 kcal and 750 ml. If the app already contains other records, use your actual totals instead of claiming these exact numbers.

### History, editing and deletion — about 60 seconds

Open Records. Choose today's date to filter the list. Edit the second entry, changing water from 250 to 500 ml. Save and show the update. Return to the dashboard: with only those two records, water now totals 1000 ml.

“History supports date filtering and editing. Updating a stored entry also updates the daily totals.”

Request deletion of the second entry. Show the confirmation, cancel once to demonstrate that the record remains, then confirm deletion. The remaining totals should match the first entry.

“Deletion requires confirmation. Cancelling keeps the record. Confirming removes it and refreshes the totals.”

### Goals and persistence — about 40 seconds

Open daily goals and change the water goal to 2500 ml. Save and show the progress update. Close and reopen the app to show that the saved record and goal remain.

“Goals can be changed without changing the activity records. Room stores records and goals in a local SQLite database, so saved data remains after the app is reopened.”

### Architecture and closing — about 30 seconds

“The Compose screens render state from HealthViewModel. The ViewModel calls HealthRepository, which validates writes and accesses the Room DAO. Room queries provide reactive updates to the screens. The project includes JVM validation and state tests, plus Android database and interface tests. The source and report identify AI assistance used during development.”

Finish by briefly showing the dashboard and stating one real limitation you understand, such as manual activity entry or the absence of cloud backup. This script is guidance; explain the implementation in words you can defend in the viva.

## 3. Size, upload and link check

The brief limits the video to 25 MB and requires your own narration. Aim for a focused recording of around 3–4 minutes; the brief does not specify that duration. Check the actual file size and listen to the final audio.

If needed, compress a four-minute recording using HandBrake with an MP4 container, H.264 video, average video bitrate around 600 kbps and AAC audio around 64 kbps. Check the final result: file size depends on duration and overhead. Increase text visibility by recording a tightly cropped emulator area rather than the whole desktop.

Upload the final video and signed APK to Google Drive. For each file choose Share → General access → Anyone with the link → Viewer → Copy link. Check each link in a private/incognito browser window while signed out. Paste the video link into Appendix A and the APK link into Appendix B of the report.
