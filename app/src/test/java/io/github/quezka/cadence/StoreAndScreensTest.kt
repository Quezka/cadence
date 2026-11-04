package io.github.quezka.cadence

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.quezka.cadence.data.GymStore
import io.github.quezka.cadence.domain.Gym
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

    private fun shoot(name: String) {
        if (System.getenv("SCREENSHOTS") == null) return
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        File("build/screenshots").apply { mkdirs() }.resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
