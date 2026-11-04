# Cadence

Android gym log (Kotlin + Compose, minSdk 26), sibling of Quire's phone app: same toolchain and look, no network at all, no account. Started 2026-11-04.

- Layers: `domain/Gym.kt` (pure rules, whole grams), `data/GymStore.kt` (SQLite; every change is stored at once, so a workout survives the phone dying), `ui/`.
- Test: `JAVA_HOME=~/.local/share/android-dev/jdk17 ANDROID_HOME=~/.local/share/android-dev/sdk ./gradlew testDebugUnitTest --offline`; add `SCREENSHOTS=1` to render screens to `app/build/screenshots`. Tests use Robolectric and an in-memory store.
- UI text: `res/values*/strings.xml` in English, Italian and Russian. Built-in exercises are stored by key, their names come from `ex_<key>`, mapped in `ui/Common.kt`.
- A set counts only once ticked; `finish` drops unticked sets, and a workout with nothing done is removed.
- Public on GitHub (Quezka/cadence), with a backup on git.lan. No signing key yet: CI builds a debug APK as an artifact, there are no releases.
