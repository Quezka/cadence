# Changelog

## [0.5.0] - 2026-11-09
- **Liquid glass done properly, the Apple way.** Backgrounds are plain and cards solid again (the colour wash is gone).
  The top bar of each tab, the workout's bar (with the rest timer) and the bottom bar are glass: what scrolls under
  them shows through, blurred (Android 13 and newer; older phones get a see-through tint).

## [0.4.0] - 2026-11-08
- **Brighter colours and the glass look**, as across the suite: screens sit on a soft wash of colour, with see-through
  cards and bottom bar. Sheets and dialogs stay solid, so what's on them is easy to read.

## [0.3.2] - 2026-11-04

The same indigo and colours as the rest of the suite, computer included.

## [0.3.1] - 2026-11-04

The app icon has the same indigo as Quire's and Vesper's.

## [0.3.0] - 2026-11-04

### Changed
- **Cadence now looks like Quire and Vesper.** The same indigo palette on every phone instead of the wallpaper's colours, the same top bars, cards with a stripe of colour (one per muscle), summary tiles and round + button, and the same motion between pages.
- A routine starts from its play button; tapping it opens it to edit.

## [0.2.0] - 2026-11-04

### Fixed
- **Pages going white**: routines, workouts and exercises showed the white window behind them, even in the dark theme.
- **A workout left with Back couldn't be found again.** The first page now offers to resume it, and only one runs at a time.
- Turning the phone no longer loses the page you're on or the rest timer.
- Bodyweight exercises no longer show a best of "0 kg": they go by reps.

### Added
- **Timed and cardio exercises**: a plank is in seconds, a run in minutes and km, and your own exercises can be either.
- **Stats**: volume over the last 8 weeks, sets per muscle this week, best lifts, and your body weight with its chart.
- **In a workout**: ±2.5 kg buttons, what you did last time beside each set, finished sets highlighted, a plate calculator, notes, removing an exercise, and -15 and +30 on the rest timer.
- **History**: correct a finished workout, do it again, or save it as a routine.
- **Routines**: reorder and duplicate.
- **Exercises**: 29 more, a filter by muscle, make one while picking, delete your own if unused.
- **Settings**: how long to rest, and saving or restoring a backup of everything.

## [0.1.1] - 2026-11-04

Nothing changes in the app: this is the first version built and signed by the release pipeline. It installs over 0.1.0 and keeps your workouts.

## [0.1.0] - 2026-11-04

First release: a gym log for your phone, with no account and no network.

- Log workouts set by set; a set counts once you tick it, and a workout survives the phone dying.
- Built-in exercises, in English, Italian and Russian.
- Material 3, with the Manrope typeface.

Install: download `Cadence-0.1.0-android.apk` on your phone and open it (allow installing from your browser if asked). If you had a test build of Cadence, uninstall it first: this one is signed with a different key, and uninstalling removes what you logged in it.
