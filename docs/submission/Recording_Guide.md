# HealthMate demonstration recording guide — Navod

Use your own voice. Practise the steps before recording and adapt these suggested words to your own understanding. Do not describe a step as successful unless it works in your recording.

## 1. Prepare on your Mac

Download `releases/HealthMate-0.3.0-release.apk` and drag it onto the running Android Studio emulator. The 0.3.0 release is signed with a new key, so any previously installed 0.1.x release or debug build must be uninstalled first; uninstalling removes its local data. Use the installed signed release for the demonstration.

Open HealthMate, close distracting windows and notifications, and choose a readable emulator size. Press Shift–Command–5, select a recording area around the emulator, and use Options to select your microphone. Make a short test recording and listen to it before recording the full demonstration. Stop using the menu-bar recording button.

## 2. Suggested demonstration and narration

### Introduction — about 20 seconds

“My name is Navod Daham, Kingston ID K2635839. This is HealthMate, an Android health and wellness tracker built with Kotlin, Jetpack Compose and Room. It lets users manually record steps, calories burned and water intake. Records stay on the device.”

Show the dashboard. Explain that the ring shows overall progress toward the three goals and that totals refer to today's date unless another day is selected. These are manual entries, not measurements taken from a step sensor.

### Validation and adding records — about 50 seconds

Tap Add record. Try saving empty fields, then enter zero in all three fields and save. Show the feedback. Enter 1200 steps, 60 calories and 500 ml for today, then save.

“The form validates the date and whole-number fields. At least one activity value must be greater than zero. After saving a valid record, the dashboard updates from the stored data.”

Add another record for today with 300 steps, 20 calories and 250 ml. Show totals of 1500 steps, 80 kcal and 750 ml, and the metric cards animating as they update. If the app already contains other records, use your actual totals instead of claiming these exact numbers.

### Water quick-add and goals — about 40 seconds

On the dashboard, tap the +250 water button and show the total rise and the confirmation message.

“Quick-add buttons write a real water record for today through the same validated path as the full form.”

Open daily goals and change the water goal to 2500 ml. Save and show the progress update. If all three goals were met in your recording, point out the “All goals reached” message; otherwise mention it appears when every goal is met.

### Search by date — about 40 seconds

Tap the search icon in the top bar. Choose a date that has records, tap Search records, and show the “Daily health record” cards with their steps, calories and water values, plus the Daily total card when a date holds several entries. Tap Edit on a result, change a value, and save; then show Delete and its confirmation. Tap Show all records to land on the history list. Return to the dashboard, tap “View another day”, choose a past date, and show the cards switching to that day; tap the “Viewing” chip to return to today.

“The search screen reads stored records by date, and the dashboard can display any chosen day. Both reuse the same editing and deletion flows as the history list.”

### History and persistence — about 40 seconds

Open Records. Choose today's date to filter the list and show the filtered result. Close and reopen the app to show that the saved records and goals remain.

“History supports date filtering. Room stores records and goals in a local SQLite database, so saved data remains after the app is reopened.”

### Insights — about 30 seconds

Open the Insights tab. Show the trend chart, switch between steps, calories and water, and between 7 and 30 days. Show the goal streak card and the achievements grid.

“Insights are derived from a per-day aggregation of the stored records, with no extra tables. The streak counts a day only when every goal is met.”

### Architecture and closing — about 30 seconds

“The Compose screens render state from HealthViewModel. The ViewModel calls HealthRepository, which validates writes and accesses the Room DAO. Room queries provide reactive updates to the screens. The project includes JVM validation, state and insights tests, plus Android database and interface tests. The source and report identify AI assistance used during development.”

Finish by briefly showing the dashboard and stating one real limitation you understand, such as manual activity entry or the absence of cloud backup. This script is guidance; explain the implementation in words you can defend in the viva.

## 3. Size, upload and link check

The brief limits the video to 25 MB and requires your own narration. Aim for a focused recording of around 3–4 minutes; the brief does not specify that duration. Check the actual file size and listen to the final audio.

If needed, compress a four-minute recording using HandBrake with an MP4 container, H.264 video, average video bitrate around 600 kbps and AAC audio around 64 kbps. Check the final result: file size depends on duration and overhead. Increase text visibility by recording a tightly cropped emulator area rather than the whole desktop.

Upload the final video and signed APK to Google Drive. For each file choose Share → General access → Anyone with the link → Viewer → Copy link. Check each link in a private/incognito browser window while signed out. Paste the video link into Appendix A and the APK link into Appendix B of the report.
