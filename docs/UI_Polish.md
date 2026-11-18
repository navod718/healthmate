# HealthMate 0.1.1 UI polish

- Dashboard: indigo-to-teal header, calendar icon and rounded progress tracks.
- Shared components: consistent rounded Material shapes and subtle card outlines in light/dark themes.
- Record form: activity icons, softly filled outlined fields and a save checkmark.
- History: icon-labelled metric rows, more internal spacing, a divider before actions and an error-colour delete action.
- Daily goals: flag icon and fields matching the editor.

Record storage, validation, navigation, labels and features retain their existing contracts. No database migration is needed. Version 0.1.1 (code 2) uses the existing private release key, allowing an update of the signed 0.1.0 app.

The dashboard storage note is placed in the measured bottom area so it stays readable above Add record. The report screenshots and review ZIP are refreshed for this version.

# HealthMate 0.2.0 Insights

- New Insights tab: a custom Canvas trend chart (steps, calories or water over 7 or 30 days with a dashed goal line), a goal streak card and an achievements grid.
- The chart reuses the dashboard metric colours, animates in over 360 ms and exposes a content description for TalkBack.
- A streak assist chip appears on the dashboard header once a streak is running.
- A date filter with no matches now shows "No records found" with the formatted date and a Clear filter button instead of the plain empty card.

Record storage, validation and goals keep their existing contracts and no database migration is needed: trends, streaks and achievements are derived from a per-day aggregation of existing records. Version 0.2.0 (code 3) keeps the 0.1.1 labels plus the additions above.

# HealthMate 0.2.1 Dashboard redesign

- Header card: an animated overall-goal ring (mean of the three goal fractions) sits beside the title, and "✨ All goals reached!" fades in when every goal is met.
- Metric cards: bold vertical gradients (teal-indigo steps, burnt-orange calories, cyan water) with white text, white progress tracks and icons on translucent tiles.
- Count-up: metric numbers animate from the previous value over 600 ms and settle on the exact total.
- Quick-add: a "Log water" row with +250/+500/+1,000 buttons writes water-only records for today through the existing validated repository path.

Labels and captions ("750 ml", "52% of 400 kcal goal", the storage note) keep their existing contracts so tests and the report stay valid. Version 0.2.1 (code 4).
