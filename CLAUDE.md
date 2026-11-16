# Cadence

Android gym log (Kotlin + Compose, minSdk 26), sibling of Quire's phone app: same toolchain and look, no account; the network is used only for over-the-air updates (`data/ReleaseFeed.kt`, `data/Updater.kt`, Settings → Updates, as in Vesper). Started 2026-11-04.

- Layers: `domain/Gym.kt` (pure rules, whole grams), `data/GymStore.kt` (SQLite; every change is stored at once, so a workout survives the phone dying), `ui/`.
- Test: `JAVA_HOME=~/.local/share/android-dev/jdk17 ANDROID_HOME=~/.local/share/android-dev/sdk ./gradlew testDebugUnitTest --offline`; add `SCREENSHOTS=1` to render screens to `app/build/screenshots`. Tests use Robolectric and an in-memory store.
- UI text: `res/values*/strings.xml` in English, Italian and Russian. Built-in exercises are stored by key, their names come from `ex_<key>`, mapped in `ui/Common.kt`.
- A set counts only once ticked; `finish` drops unticked sets, and a workout with nothing done is removed.
- Public on GitHub (Quezka/cadence), with a backup on git.lan.
- Releases: bump `versionName` and `versionCode`, add a CHANGELOG section, tag `vX.Y.Z`, push. The key is in `~/.local/share/cadence-signing`; without it an installed copy can't be updated. If the repo has the secrets `CADENCE_KEYSTORE_B64`/`CADENCE_KEYSTORE_PASSWORD`, CI publishes `Cadence-<v>-android.apk` by itself. If not, copy the key's `keystore.properties` to the repo root (git-ignored; delete it afterwards), run `assembleRelease`, and `gh release create` with the APK under that name.
- Every screen stands on the `Surface` in `CadenceTheme`: without it a page without a Scaffold shows the white window behind.
- Schema changes: bump `Helper.VERSION`, add a step in `onUpgrade`, cover it in `StoreAndScreensTest`. `builtIns()` adds new exercises to existing databases; a new one also needs `ex_<key>` strings and a line in `Common.nameRes`.
- A set is measured by its exercise's kind: `grams`+`reps`, or `seconds`, or `seconds`+`meters` (`RoutineItem.reps` is then seconds or minutes). A backup is the database file itself.
- The look is Quire's on purpose: Material 3 tonal, NO glass since 0.8.0 (`Glass.kt` keeps the `GlassScreen` layout with solid bars, `LocalBottomInset` pads lists and buttons; plain backgrounds, never a gradient wash; no wallpaper colours), `Fields.kt` and `Motion.kt` copied from it, the same cards, titles, tiles and floating button (`Common.kt`). A new screen should look like one of Quire's.

## 0.9 (2026-11-16)
rest timer = `rest/RestAlarm.kt` (exact alarm -> `RestReceiver` buzz + countdown notif via chronometer); screen loop no longer buzzes
undo: `GymStore.deleteSet/removeExerciseFromWorkout` return removed sets -> `restoreSets`; snackbar in `WorkoutScreen`
hint "try X kg" = `Gym.nextWeight` per exercise card; Train "Repeat last workout" card (`history.first()` -> `routineFrom`)
