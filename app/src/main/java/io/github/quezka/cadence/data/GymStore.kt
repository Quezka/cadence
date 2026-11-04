package io.github.quezka.cadence.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.github.quezka.cadence.domain.BUILT_IN
import io.github.quezka.cadence.domain.Exercise
import io.github.quezka.cadence.domain.Gym
import io.github.quezka.cadence.domain.Muscle
import io.github.quezka.cadence.domain.Routine
import io.github.quezka.cadence.domain.RoutineItem
import io.github.quezka.cadence.domain.Workout
import io.github.quezka.cadence.domain.WorkoutSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

private class Helper(context: Context, name: String?) : SQLiteOpenHelper(context, name, null, 1) {
    override fun onConfigure(db: SQLiteDatabase) = db.setForeignKeyConstraintsEnabled(true)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE exercises (id INTEGER PRIMARY KEY, key TEXT UNIQUE, name TEXT NOT NULL, muscle TEXT NOT NULL)")
        db.execSQL("CREATE TABLE routines (id INTEGER PRIMARY KEY, name TEXT NOT NULL)")
        db.execSQL("""CREATE TABLE routine_items (routine_id INTEGER NOT NULL REFERENCES routines(id) ON DELETE CASCADE,
            position INTEGER NOT NULL, exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
            sets INTEGER NOT NULL, reps INTEGER NOT NULL)""")
        db.execSQL("CREATE TABLE workouts (id INTEGER PRIMARY KEY, name TEXT NOT NULL, started INTEGER NOT NULL, finished INTEGER, day TEXT NOT NULL)")
        db.execSQL("""CREATE TABLE sets (id INTEGER PRIMARY KEY, workout_id INTEGER NOT NULL REFERENCES workouts(id) ON DELETE CASCADE,
            exercise_id INTEGER NOT NULL REFERENCES exercises(id), position INTEGER NOT NULL, grams INTEGER NOT NULL,
            reps INTEGER NOT NULL, done INTEGER NOT NULL)""")
        db.execSQL("CREATE INDEX idx_sets_exercise ON sets(exercise_id)")
        db.execSQL("CREATE INDEX idx_sets_workout ON sets(workout_id)")
        for ((key, muscle) in BUILT_IN) db.insert("exercises", null, ContentValues().apply {
            put("key", key); put("name", ""); put("muscle", muscle.name)
        })
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}

/** Everything is stored as soon as it happens: a workout survives the phone dying halfway through it. */
class GymStore(context: Context, fileName: String? = "cadence.db", private val now: () -> Long = { System.currentTimeMillis() / 1000 },
               private val today: () -> LocalDate = LocalDate::now) {
    private val db = Helper(context, fileName).writableDatabase
    private val _version = MutableStateFlow(0)
    /** Goes up on every change, so screens know to look again. */
    val version: StateFlow<Int> = _version
    private fun changed() { _version.value++ }

    private inline fun <T> Cursor.all(row: Cursor.() -> T): List<T> = use { c ->
        buildList { while (c.moveToNext()) add(c.row()) }
    }
    private fun Cursor.long(n: String) = getLong(getColumnIndexOrThrow(n))
    private fun Cursor.int(n: String) = getInt(getColumnIndexOrThrow(n))
    private fun Cursor.str(n: String): String = getString(getColumnIndexOrThrow(n)) ?: ""

    // ---- exercises ----

    fun exercises(): List<Exercise> = db.rawQuery("SELECT * FROM exercises ORDER BY id", null).all {
        Exercise(long("id"), str("key").ifEmpty { null }, str("name"),
            runCatching { Muscle.valueOf(str("muscle")) }.getOrDefault(Muscle.OTHER))
    }

    fun addExercise(name: String, muscle: Muscle): Long {
        val id = db.insertOrThrow("exercises", null, ContentValues().apply {
            putNull("key"); put("name", name.trim()); put("muscle", muscle.name)
        })
        changed()
        return id
    }

    // ---- routines ----

    fun routines(): List<Routine> = db.rawQuery("SELECT * FROM routines ORDER BY name COLLATE NOCASE", null)
        .all { long("id") to str("name") }.map { (id, name) ->
            Routine(id, name, db.rawQuery("SELECT * FROM routine_items WHERE routine_id = ? ORDER BY position", arrayOf(id.toString()))
                .all { RoutineItem(long("exercise_id"), int("sets"), int("reps")) })
        }

    fun saveRoutine(id: Long?, name: String, items: List<RoutineItem>): Long {
        var saved = id ?: -1
        db.beginTransaction()
        try {
            val values = ContentValues().apply { put("name", name.trim()) }
            if (id == null) saved = db.insertOrThrow("routines", null, values)
            else {
                db.update("routines", values, "id = ?", arrayOf(id.toString()))
                db.delete("routine_items", "routine_id = ?", arrayOf(id.toString()))
            }
            items.forEachIndexed { i, item ->
                db.insertOrThrow("routine_items", null, ContentValues().apply {
                    put("routine_id", saved); put("position", i); put("exercise_id", item.exerciseId)
                    put("sets", item.sets.coerceIn(1, 20)); put("reps", item.reps.coerceIn(1, Gym.MAX_REPS))
                })
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        changed()
        return saved
    }

    fun deleteRoutine(id: Long) {
        db.delete("routines", "id = ?", arrayOf(id.toString()))
        changed()
    }

    // ---- workouts ----

    private fun setsOf(workoutId: Long) = db.rawQuery("SELECT * FROM sets WHERE workout_id = ? ORDER BY id", arrayOf(workoutId.toString())).all { aSet() }

    private fun Cursor.aSet() = WorkoutSet(long("id"), long("workout_id"), long("exercise_id"), int("position"), int("grams"), int("reps"), int("done") == 1)

    private fun Cursor.aWorkout() = Workout(long("id"), str("name"), long("started"),
        if (isNull(getColumnIndexOrThrow("finished"))) null else long("finished"), LocalDate.parse(str("day")))

    fun workout(id: Long): Workout? = db.rawQuery("SELECT * FROM workouts WHERE id = ?", arrayOf(id.toString())).all { aWorkout() }
        .firstOrNull()?.let { it.copy(sets = setsOf(it.id)) }

    /** The workout under way, if one was left open. */
    fun active(): Workout? = db.rawQuery("SELECT * FROM workouts WHERE finished IS NULL ORDER BY id DESC LIMIT 1", null)
        .all { aWorkout() }.firstOrNull()?.let { it.copy(sets = setsOf(it.id)) }

    /** Finished workouts, newest first. */
    fun history(limit: Int = 500): List<Workout> =
        db.rawQuery("SELECT * FROM workouts WHERE finished IS NOT NULL ORDER BY started DESC LIMIT ?", arrayOf(limit.toString()))
            .all { aWorkout() }.map { it.copy(sets = setsOf(it.id)) }

    /** Start a workout, filled in from a routine and from what you lifted last time. */
    fun start(name: String, routine: Routine? = null): Long {
        val id = db.insertOrThrow("workouts", null, ContentValues().apply {
            put("name", name.trim()); put("started", now()); put("day", today().toString())
        })
        routine?.items?.forEach { item ->
            val weight = Gym.nextWeight(lastSets(item.exerciseId), item.reps) ?: 0
            repeat(item.sets) { addSet(id, item.exerciseId, weight, item.reps) }
        }
        changed()
        return id
    }

    fun addExerciseToWorkout(workoutId: Long, exerciseId: Long) {
        val last = lastSets(exerciseId).lastOrNull()
        addSet(workoutId, exerciseId, last?.grams ?: 0, last?.reps ?: 8)
    }

    fun addSet(workoutId: Long, exerciseId: Long, grams: Int, reps: Int): Long {
        val position = db.rawQuery("SELECT COUNT(*) FROM sets WHERE workout_id = ? AND exercise_id = ?",
            arrayOf(workoutId.toString(), exerciseId.toString())).use { it.moveToFirst(); it.getInt(0) }
        val id = db.insertOrThrow("sets", null, ContentValues().apply {
            put("workout_id", workoutId); put("exercise_id", exerciseId); put("position", position)
            put("grams", grams); put("reps", reps); put("done", 0)
        })
        changed()
        return id
    }

    fun updateSet(id: Long, grams: Int, reps: Int, done: Boolean) {
        db.update("sets", ContentValues().apply {
            put("grams", grams.coerceIn(0, Gym.MAX_GRAMS)); put("reps", reps.coerceIn(0, Gym.MAX_REPS)); put("done", if (done) 1 else 0)
        }, "id = ?", arrayOf(id.toString()))
        changed()
    }

    fun deleteSet(id: Long) {
        db.delete("sets", "id = ?", arrayOf(id.toString()))
        changed()
    }

    /** Finish it: sets left unticked weren't done, so they go. A workout with nothing done goes altogether. */
    fun finish(id: Long): Boolean {
        db.delete("sets", "workout_id = ? AND done = 0", arrayOf(id.toString()))
        val kept = setsOf(id).isNotEmpty()
        if (kept) db.update("workouts", ContentValues().apply { put("finished", now()) }, "id = ?", arrayOf(id.toString()))
        else db.delete("workouts", "id = ?", arrayOf(id.toString()))
        changed()
        return kept
    }

    fun deleteWorkout(id: Long) {
        db.delete("workouts", "id = ?", arrayOf(id.toString()))
        changed()
    }

    /** The sets of the last finished workout that had this exercise (what "last time" means). */
    fun lastSets(exerciseId: Long): List<WorkoutSet> {
        val workout = db.rawQuery("""SELECT w.id FROM workouts w JOIN sets s ON s.workout_id = w.id
            WHERE s.exercise_id = ? AND w.finished IS NOT NULL ORDER BY w.started DESC, w.id DESC LIMIT 1""", arrayOf(exerciseId.toString()))
            .all { long("id") }.firstOrNull() ?: return emptyList()
        return setsOf(workout).filter { it.exerciseId == exerciseId }
    }

    /** Best estimated single per finished workout, oldest first: the progress line. */
    fun progress(exerciseId: Long): List<Pair<LocalDate, Int>> =
        db.rawQuery("""SELECT w.day AS day, s.grams AS grams, s.reps AS reps FROM sets s JOIN workouts w ON w.id = s.workout_id
            WHERE s.exercise_id = ? AND s.done = 1 AND w.finished IS NOT NULL ORDER BY w.started""", arrayOf(exerciseId.toString()))
            .all { Triple(LocalDate.parse(str("day")), int("grams"), int("reps")) }
            .groupBy { it.first }.map { (day, sets) -> day to sets.maxOf { Gym.oneRepMax(it.second, it.third) } }

    /** The best estimate before this workout, to tell a new record. */
    fun bestBefore(exerciseId: Long, workoutId: Long): Int =
        db.rawQuery("""SELECT s.grams AS grams, s.reps AS reps FROM sets s JOIN workouts w ON w.id = s.workout_id
            WHERE s.exercise_id = ? AND s.done = 1 AND w.finished IS NOT NULL AND w.id != ?""", arrayOf(exerciseId.toString(), workoutId.toString()))
            .all { Gym.oneRepMax(int("grams"), int("reps")) }.maxOrNull() ?: 0

    fun close() = db.close()
}
