package io.github.quezka.cadence.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.floor
import kotlin.math.roundToInt

/** An exercise. A built-in one has a [key] (its name comes from the translations), yours has a [name]. */
data class Exercise(val id: Long, val key: String?, val name: String, val muscle: Muscle)

enum class Muscle { CHEST, BACK, LEGS, SHOULDERS, ARMS, CORE, CARDIO, OTHER }

data class RoutineItem(val exerciseId: Long, val sets: Int, val reps: Int)

data class Routine(val id: Long, val name: String, val items: List<RoutineItem>)

/** One set. Weights are whole grams, so sums are exact; a set with no weight is bodyweight. */
data class WorkoutSet(
    val id: Long,
    val workoutId: Long,
    val exerciseId: Long,
    val position: Int,
    val grams: Int,
    val reps: Int,
    val done: Boolean,
)

data class Workout(
    val id: Long,
    val name: String,
    val started: Long, // epoch seconds
    val finished: Long?, // null: still going
    val day: LocalDate,
    val sets: List<WorkoutSet> = emptyList(),
)

/** The rules. Pure Kotlin, no Android. */
object Gym {
    const val MAX_GRAMS = 1_000_000
    const val MAX_REPS = 1000
    const val STEP_GRAMS = 2500
    val PLATES_GRAMS = listOf(25000, 20000, 15000, 10000, 5000, 2500, 1250)

    /** "62,5" or "62.5" -> grams; empty is bodyweight; null is not a weight. */
    fun parseKg(text: String): Int? {
        val t = text.trim().replace(',', '.')
        if (t.isEmpty()) return 0
        val kg = t.toDoubleOrNull() ?: return null
        val grams = (kg * 1000).roundToInt()
        return grams.takeIf { kg >= 0 && it <= MAX_GRAMS }
    }

    fun showKg(grams: Int): String {
        if (grams % 1000 == 0) return (grams / 1000).toString()
        return (grams / 1000.0).toString().trimEnd('0').trimEnd('.')
    }

    /** Estimated best single (Epley): a set of one is what it is. */
    fun oneRepMax(grams: Int, reps: Int): Int = when {
        grams <= 0 || reps <= 0 -> 0
        reps == 1 -> grams
        else -> (grams * (1 + reps / 30.0)).roundToInt()
    }

    fun volume(sets: List<WorkoutSet>): Long = sets.filter { it.done }.sumOf { it.grams.toLong() * it.reps }

    /** What to try next time: a step up if every set reached the target, else the same again. */
    fun nextWeight(last: List<WorkoutSet>, targetReps: Int, step: Int = STEP_GRAMS): Int? {
        val done = last.filter { it.done && it.grams > 0 }
        if (done.isEmpty()) return null
        val top = done.maxOf { it.grams }
        return if (done.filter { it.grams == top }.all { it.reps >= targetReps }) top + step else top
    }

    /** The sets among [sets] that beat every estimate before them (in time order). [before] is the best so far. */
    fun records(sets: List<WorkoutSet>, before: Int): List<WorkoutSet> {
        var best = before
        val found = mutableListOf<WorkoutSet>()
        for (s in sets.filter { it.done }) {
            val estimate = oneRepMax(s.grams, s.reps)
            if (estimate > best) {
                best = estimate
                found += s
            }
        }
        return found
    }

    /** The plates for one side of the bar, biggest first, and what can't be made up with them. */
    fun platesPerSide(totalGrams: Int, barGrams: Int = 20000): Pair<List<Int>, Int> {
        var left = floor((totalGrams - barGrams).coerceAtLeast(0) / 2.0).toInt()
        val plates = mutableListOf<Int>()
        for (p in PLATES_GRAMS) while (left >= p) {
            plates += p
            left -= p
        }
        return plates to left
    }

    fun weekOf(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** Weeks in a row with a workout, this week counted once it has one (and not breaking the run before). */
    fun weekStreak(days: List<LocalDate>, today: LocalDate): Int {
        val weeks = days.map(::weekOf).toSet()
        var week = weekOf(today)
        if (week !in weeks) week = week.minusWeeks(1)
        var run = 0
        while (week in weeks) {
            run++
            week = week.minusWeeks(1)
        }
        return run
    }

    fun csv(workouts: List<Workout>, nameOf: (Long) -> String): String {
        fun cell(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
        val out = StringBuilder("date,workout,exercise,set,kg,reps\n")
        for (w in workouts) for (s in w.sets.filter { it.done }) {
            out.append(listOf(w.day.toString(), cell(w.name), cell(nameOf(s.exerciseId)), (s.position + 1).toString(),
                showKg(s.grams), s.reps.toString()).joinToString(",")).append('\n')
        }
        return out.toString()
    }
}

/** The exercises that come with the app, by translation key. */
val BUILT_IN: List<Pair<String, Muscle>> = listOf(
    "bench_press" to Muscle.CHEST, "incline_press" to Muscle.CHEST, "dumbbell_press" to Muscle.CHEST,
    "chest_fly" to Muscle.CHEST, "push_up" to Muscle.CHEST, "dip" to Muscle.CHEST,
    "deadlift" to Muscle.BACK, "pull_up" to Muscle.BACK, "lat_pulldown" to Muscle.BACK,
    "barbell_row" to Muscle.BACK, "cable_row" to Muscle.BACK,
    "squat" to Muscle.LEGS, "leg_press" to Muscle.LEGS, "lunge" to Muscle.LEGS, "romanian_deadlift" to Muscle.LEGS,
    "leg_curl" to Muscle.LEGS, "leg_extension" to Muscle.LEGS, "calf_raise" to Muscle.LEGS,
    "overhead_press" to Muscle.SHOULDERS, "lateral_raise" to Muscle.SHOULDERS, "face_pull" to Muscle.SHOULDERS,
    "barbell_curl" to Muscle.ARMS, "hammer_curl" to Muscle.ARMS, "triceps_pushdown" to Muscle.ARMS,
    "skull_crusher" to Muscle.ARMS,
    "plank" to Muscle.CORE, "crunch" to Muscle.CORE, "leg_raise" to Muscle.CORE,
    "running" to Muscle.CARDIO, "cycling" to Muscle.CARDIO, "rowing" to Muscle.CARDIO,
)
