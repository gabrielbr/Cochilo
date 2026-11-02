# The report for the doctor (0.20.0)

Settings › Data › **Report for the doctor (PDF)**: one A4 page, made on the phone and saved where
the parent chooses (then shared like any file). Nothing is sent anywhere by the app.

On the page:

- The baby's name, age and the two weeks covered ("Tue 20 Oct to Mon 2 Nov"): always absolute
  dates, never "Today", because it is read later.
- **Daily average, last 7 days** (whole days): total, night, naps, longest stretch, night wakings,
  feeds, diapers, daylight, and pumped milk by day and night.
- **What changed** and **Where naps happened**, as in Patterns ([changes.md](changes.md),
  [places.md](places.md)).
- **Sleep, last 14 days**: one bar per day, midnight to midnight, in the design system's light
  data colours, with a legend.
- The footer: "From the parent's own log in Cochilo, made Mon 2 Nov 15:00. Not a medical record."

Code: `report/Report.kt` (`build` reads the log, `draw` paints the page, `write` makes the PDF with
Android's `PdfDocument`); `ReportSnapshotTest` paints the same page. Libre Franklin, light theme.
