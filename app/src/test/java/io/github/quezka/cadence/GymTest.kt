package io.github.quezka.cadence

import io.github.quezka.cadence.domain.Gym
import io.github.quezka.cadence.domain.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class GymTest {
    private fun set(grams: Int, reps: Int, done: Boolean = true) = WorkoutSet(0, 0, 0, 0, grams, reps, done)

    @Test fun weightsParseAndShowExactly() {
        assertEquals(62500, Gym.parseKg("62,5"))
        assertEquals(62500, Gym.parseKg("62.5"))
        assertEquals(0, Gym.parseKg(""))
        assertNull(Gym.parseKg("abc"))
        assertNull(Gym.parseKg("-5"))
        assertNull(Gym.parseKg("99999"))
        assertEquals("62.5", Gym.showKg(62500))
        assertEquals("60", Gym.showKg(60000))
        assertEquals("1.25", Gym.showKg(1250))
    }

    @Test fun theEstimateIsEpleyAndASingleIsWhatItIs() {
        assertEquals(100000, Gym.oneRepMax(100000, 1))
        assertEquals(120000, Gym.oneRepMax(100000, 6))
        assertEquals(0, Gym.oneRepMax(0, 10))
        assertEquals(0, Gym.oneRepMax(50000, 0))
    }

    @Test fun onlyTickedSetsCountAsVolume() {
        assertEquals(1_000_000L, Gym.volume(listOf(set(100000, 10), set(100000, 10, done = false))))
    }

    @Test fun nextWeightStepsUpOnlyIfEveryTopSetHitTheTarget() {
        assertEquals(62500, Gym.nextWeight(listOf(set(60000, 8), set(60000, 8)), 8))
        assertEquals(60000, Gym.nextWeight(listOf(set(60000, 8), set(60000, 6)), 8))
        assertEquals(62500, Gym.nextWeight(listOf(set(40000, 3), set(60000, 8)), 8))  // warm-ups don't hold it back
        assertNull(Gym.nextWeight(listOf(set(0, 20)), 8))  // bodyweight
        assertNull(Gym.nextWeight(emptyList(), 8))
    }

    @Test fun recordsOnlyBeatWhatCameBefore() {
        val sets = listOf(set(100000, 1), set(90000, 5), set(100000, 5), set(50000, 1, done = false))
        assertEquals(listOf(100000 to 5), Gym.records(sets, 110000).map { it.grams to it.reps })
        assertEquals(3, Gym.records(sets, 0).size)
    }

    @Test fun platesAreBiggestFirstAndSayWhatIsLeft() {
        assertEquals(listOf(25000, 20000, 2500) to 0, Gym.platesPerSide(115000))
        assertEquals(emptyList<Int>() to 0, Gym.platesPerSide(20000))
        assertEquals(listOf(1250) to 250, Gym.platesPerSide(23000))
    }

    @Test fun aWeekWithoutAWorkoutYetDoesntBreakTheRun() {
        val wed = LocalDate.of(2026, 11, 4)
        val lastWeek = wed.minusWeeks(1)
        val before = wed.minusWeeks(2)
        assertEquals(2, Gym.weekStreak(listOf(lastWeek, before), wed))
        assertEquals(3, Gym.weekStreak(listOf(wed, lastWeek, before), wed))
        assertEquals(0, Gym.weekStreak(listOf(wed.minusWeeks(3)), wed))
    }

    @Test fun csvQuotesWhatNeedsIt() {
        val w = io.github.quezka.cadence.domain.Workout(1, "A, B", 0, 1, LocalDate.of(2026, 11, 4), listOf(set(62500, 8), set(1000, 1, done = false)))
        assertEquals("date,workout,exercise,set,kg,reps\n2026-11-04,\"A, B\",Squat,1,62.5,8\n", Gym.csv(listOf(w)) { "Squat" })
    }
}
