# HealthMate AI-assistance disclosure — draft for the report

> **How to use this file:** adapt this draft into whatever declaration format the module requires, keep only what is true for your submission, and make sure you can explain every listed item in the viva. Do not submit statements you do not understand.

## Statement (adapt into your own words)

I developed HealthMate as an individual project. I used an AI coding assistant (ZCode, powered by GLM) as a development tool for parts of the implementation, and I reviewed, tested and take responsibility for everything submitted.

## What the assistant did

For each feature below, the assistant proposed an implementation plan that I reviewed and approved before any code was written, then wrote the Kotlin code, tests and documentation on a feature branch that I reviewed in Android Studio on an emulator before it was merged:

1. **Insights tab (v0.2.0)** — weekly/monthly trend chart drawn with Compose Canvas, goal streak calculation, achievements grid, and a clearer "no records found" state for date filtering, derived from a per-day SQL aggregation without a schema change.
2. **Dashboard redesign (v0.2.1)** — overall-goal progress ring, gradient metric cards, count-up number animations, a goal-completion celebration state, and water quick-add buttons that write records through the existing validated repository path.
3. **Search by date (v0.3.0)** — a search screen (calendar date picker, Search records / Show all records actions, "Daily health record" result cards with edit and delete) and a dashboard day view with a "Viewing" chip.
4. **Release engineering** — built, aligned and signed the 0.3.0 release APK, generated checksums and signature verification, refreshed the report screenshot evidence, and drafted the demonstration guide and this disclosure.

The assistant also suggested which features would be worth adding, and I chose between design options (for example: drawing the chart with Canvas instead of a library, how a streak day is defined, and where search results live).

## What I did

- Decided the product direction: chose the features, their placement and behaviour from the options presented.
- Reviewed each feature on an emulator before approving the merge, and confirmed the flows work.
- Maintained the repository: merged pull requests, kept the CI workflow green, and kept signing material out of source control.
- I can explain the architecture (Compose screens → HealthViewModel → HealthRepository → Room DAO), the validation rules, the Room storage and the new features, and I will answer viva questions about any part of the code.

## Verification

- Every change was verified with JVM unit tests (validation, ViewModel state, streak and achievement rules), Android instrumentation tests (database, UI and draft-restoration), lint, and a release build; GitHub Actions runs the same checks on an emulator.
- The signed 0.3.0 APK was installed and exercised on an emulator, with checksums and `apksigner` output recorded in `releases/`.

## Notes

- The project blueprint contains an earlier AI-assistance statement; this disclosure covers the v0.2.0, v0.2.1 and v0.3.0 feature work that followed it.
- Follow the lecturer's guidance on the required format and scope of AI declarations; if in doubt, disclose more rather than less.
