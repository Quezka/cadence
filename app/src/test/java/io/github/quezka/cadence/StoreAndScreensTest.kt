package io.github.quezka.cadence

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.quezka.cadence.data.GymStore
import io.github.quezka.cadence.domain.BUILT_IN
import io.github.quezka.cadence.domain.ExerciseKind
import io.github.quezka.cadence.domain.Gym
import io.github.quezka.cadence.domain.Muscle
import io.github.quezka.cadence.domain.RoutineItem
import io.github.quezka.cadence.ui.CadenceRoot
import io.github.quezka.cadence.ui.CadenceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xxhdpi", application = android.app.Application::class)
class StoreAndScreensTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var clock = 1_000_000L
    private var day = LocalDate.of(2026, 11, 4)

    private fun store() = GymStore(compose.activity, null, { clock }, { day })

    private fun bench(s: GymStore) = s.exercises().first { it.key == "bench_press" }.id

    @Test fun aRoutineStartsFromLastTimeAndStepsUpWhenEverythingWasHit() {
        val s = store()
        val routine = s.routines().let { s.saveRoutine(null, "Push", listOf(RoutineItem(bench(s), 2, 8))); s.routines().single() }
        val first = s.start("Push", routine)
        assertEquals(listOf(0, 0), s.workout(first)!!.sets.map { it.grams })
        s.workout(first)!!.sets.forEach { s.updateSet(it.id, 60000, 8, true) }
        assertTrue(s.finish(first))
        clock += 86400
        val second = s.start("Push", routine)
        assertEquals(listOf(62500, 62500), s.workout(second)!!.sets.map { it.grams })
        assertNotNull(s.active())
    }

    @Test fun finishingDropsUntickedSetsAndAnEmptyWorkoutVanishes() {
        val s = store()
        val w = s.start("Free")
        s.addExerciseToWorkout(w, bench(s))
        val kept = s.addSet(w, bench(s), 50000, 5)
        s.updateSet(kept, 50000, 5, true)
        assertTrue(s.finish(w))
        assertEquals(1, s.workout(w)!!.sets.size)
        assertNull(s.active())
        val empty = s.start("Nothing")
        assertFalse(s.finish(empty))
        assertNull(s.workout(empty))
    }

    @Test fun progressAndRecordsLookAtFinishedWorkoutsOnly() {
        val s = store()
        val id = bench(s)
        val a = s.start("A"); s.updateSet(s.addSet(a, id, 100000, 1), 100000, 1, true); s.finish(a)
        day = day.plusDays(1); clock += 86400
        val b = s.start("B"); s.updateSet(s.addSet(b, id, 90000, 5), 90000, 5, true); s.finish(b)
        assertEquals(listOf(100000, 105000), s.progress(id).map { it.second })
        val open = s.start("C"); s.updateSet(s.addSet(open, id, 200000, 1), 200000, 1, true)
        assertEquals(2, s.progress(id).size)
        assertEquals(105000, s.bestBefore(id, open))
        assertEquals(1, Gym.records(s.workout(open)!!.sets, s.bestBefore(id, open)).size)
    }

    @Test fun anOpenWorkoutComesBackAndTheScreensDraw() {
        val s = store()
        val id = bench(s)
        s.saveRoutine(null, "Push", listOf(RoutineItem(id, 3, 8), RoutineItem(s.exercises().first { it.key == "lateral_raise" }.id, 3, 12)))
        val past = s.start("Push", s.routines().single())
        s.workout(past)!!.sets.forEach { s.updateSet(it.id, 60000, 8, true) }
        s.finish(past)
        clock += 86400
        compose.setContent { CadenceTheme(dynamic = false) { CadenceRoot(s) } }
        compose.onNodeWithText("Start a workout").assertIsDisplayed()
        shoot("train")
        compose.onNodeWithText("History").performClick()
        compose.onNodeWithText("Push").assertIsDisplayed()
        shoot("history")
        compose.onNodeWithText("Train").performClick()
        compose.onNodeWithText("Start").performClick()
        compose.onNodeWithText("Bench press").assertIsDisplayed()
        shoot("workout")
        compose.onNodeWithText("Finish").assertIsDisplayed()
    }

    @Test fun anOldDatabaseGainsTheNewColumnsAndExercisesAndKeepsItsData() {
        val context = compose.activity
        val name = "old-${System.nanoTime()}.db"
        val old = object : android.database.sqlite.SQLiteOpenHelper(context, name, null, 1) {
            override fun onCreate(db: android.database.sqlite.SQLiteDatabase) {
                db.execSQL("CREATE TABLE exercises (id INTEGER PRIMARY KEY, key TEXT UNIQUE, name TEXT NOT NULL, muscle TEXT NOT NULL)")
                db.execSQL("CREATE TABLE routines (id INTEGER PRIMARY KEY, name TEXT NOT NULL)")
                db.execSQL("CREATE TABLE routine_items (routine_id INTEGER NOT NULL, position INTEGER NOT NULL, exercise_id INTEGER NOT NULL, sets INTEGER NOT NULL, reps INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE workouts (id INTEGER PRIMARY KEY, name TEXT NOT NULL, started INTEGER NOT NULL, finished INTEGER, day TEXT NOT NULL)")
                db.execSQL("CREATE TABLE sets (id INTEGER PRIMARY KEY, workout_id INTEGER NOT NULL, exercise_id INTEGER NOT NULL, position INTEGER NOT NULL, grams INTEGER NOT NULL, reps INTEGER NOT NULL, done INTEGER NOT NULL)")
                db.execSQL("INSERT INTO exercises VALUES (1, 'plank', '', 'CORE'), (2, 'squat', '', 'LEGS')")
                db.execSQL("INSERT INTO workouts VALUES (1, 'Old', 100, 200, '2026-11-01')")
                db.execSQL("INSERT INTO sets VALUES (1, 1, 2, 0, 80000, 5, 1)")
            }
            override fun onUpgrade(db: android.database.sqlite.SQLiteDatabase, a: Int, b: Int) = Unit
        }
        old.writableDatabase.close()
        old.close()
        val s = GymStore(context, name, { clock }, { day })
        assertEquals(80000, s.workout(1)!!.sets.single().grams)
        assertEquals(ExerciseKind.TIME, s.exercises().first { it.key == "plank" }.kind)
        assertEquals(ExerciseKind.WEIGHT, s.exercises().first { it.key == "squat" }.kind)
        assertEquals(BUILT_IN.size, s.exercises().size)
        s.logBody(day, 70000)
        assertEquals(70000, s.bodyLog().single().second)
        s.close()
    }

    @Test fun timedAndCardioSetsAreKeptAndRoutinesFillThemIn() {
        val s = store()
        val plank = s.exercises().first { it.key == "plank" }.id
        val run = s.exercises().first { it.key == "running" }.id
        s.saveRoutine(null, "Core", listOf(RoutineItem(plank, 2, 45), RoutineItem(run, 1, 30)))
        val w = s.start("Core", s.routines().single())
        val sets = s.workout(w)!!.sets
        assertEquals(listOf(45, 45, 1800), sets.map { it.seconds })
        s.updateTimedSet(sets[0].id, 50, 0, true)
        s.updateTimedSet(sets[2].id, 1500, 5000, true)
        s.finish(w)
        assertEquals(listOf(50), s.progress(plank) { it.seconds }.map { it.second })
        assertEquals(5000, s.workout(w)!!.sets.first { it.exerciseId == run }.meters)
        assertEquals(listOf(RoutineItem(plank, 1, 50), RoutineItem(run, 1, 25)), s.routineFrom(s.workout(w)!!))
    }

    @Test fun editingAFinishedWorkoutKeepsItsTimeAndABackupRoundTrips() {
        val s = store()
        val id = bench(s)
        val w = s.start("A")
        s.updateSet(s.addSet(w, id, 50000, 5), 50000, 5, true)
        s.finish(w)
        val finished = s.workout(w)!!.finished
        clock += 5000
        s.addSet(w, id, 60000, 5)  // left unticked while correcting
        assertTrue(s.finish(w))
        assertEquals(finished, s.workout(w)!!.finished)
        assertEquals(1, s.workout(w)!!.sets.size)
        s.setNote(w, "Good")
        assertEquals("Good", s.workout(w)!!.note)
        val file = "backup-${System.nanoTime()}.db"
        val real = GymStore(compose.activity, file, { clock }, { day })
        real.logBody(day, 71000)
        val copy = java.io.ByteArrayOutputStream().also(real::export).toByteArray()
        real.logBody(day, 99000)
        assertFalse(real.restore(compose.activity, "not a database".byteInputStream()))
        assertEquals(99000, real.bodyLog().single().second)  // nothing touched
        assertTrue(real.restore(compose.activity, copy.inputStream()))
        val again = GymStore(compose.activity, file, { clock }, { day })
        assertEquals(71000, again.bodyLog().single().second)
        again.close()
    }

    @Test fun statsAddUpTheWeekAndCustomExercisesGoOnlyIfUnused() {
        val s = store()
        val id = bench(s)
        val w = s.start("A")
        s.updateSet(s.addSet(w, id, 50000, 10), 50000, 10, true)
        s.finish(w)
        val history = s.history()
        assertEquals(500000L, Gym.weeklyVolume(history, day, 2).last().second)
        assertEquals(0L, Gym.weeklyVolume(history, day, 2).first().second)
        assertEquals(Muscle.CHEST to 1, Gym.setsByMuscle(history, s.exercises().associate { it.id to it.muscle }::get, Gym.weekOf(day)).single())
        assertEquals(id, Gym.bests(history).single().first)
        val mine = s.addExercise("Mine", Muscle.OTHER, ExerciseKind.TIME)
        val used = s.addExercise("Used", Muscle.OTHER)
        val other = s.start("B"); s.addSet(other, used, 0, 5)
        assertTrue(s.deleteExercise(mine))
        assertFalse(s.deleteExercise(used))
        assertFalse(s.deleteExercise(id))  // built in
    }

    @Test fun aWorkoutLeftWithBackCanBeResumed() {
        val s = store()
        s.saveRoutine(null, "Push", listOf(RoutineItem(bench(s), 1, 8)))
        compose.setContent { CadenceTheme(dynamic = false) { CadenceRoot(s) } }
        compose.onNodeWithText("Start").performClick()
        compose.onNodeWithText("Finish").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Resume").assertIsDisplayed()
        compose.onNodeWithText("Resume").performClick()
        compose.onNodeWithText("Finish").assertIsDisplayed()
    }

    @Test @Config(qualifiers = "w400dp-h860dp-night-xxhdpi") fun everyPageIsDarkInTheDarkTheme() {
        val s = store()
        val w = s.start("A"); s.updateSet(s.addSet(w, bench(s), 50000, 5), 50000, 5, true); s.finish(w)
        compose.setContent { CadenceTheme(dynamic = false) { CadenceRoot(s) } }
        fun dark(name: String) {
            compose.waitForIdle()
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            shoot(name)
            val corner = bitmap.getPixel(view.width / 2, view.height - 40)
            assertTrue("$name: #${Integer.toHexString(corner)}", android.graphics.Color.luminance(corner) < 0.2f)
        }
        compose.onNodeWithText("New routine").performClick()
        dark("routine_dark")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Start a workout").performClick()
        dark("workout_dark")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Stats").performClick()
        dark("stats_dark")
    }

    private fun shoot(name: String) {
        if (System.getenv("SCREENSHOTS") == null) return
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        File("build/screenshots").apply { mkdirs() }.resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
