package io.github.quezka.cadence.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.github.quezka.cadence.domain.BUILT_IN
import io.github.quezka.cadence.domain.Exercise
import io.github.quezka.cadence.domain.ExerciseKind
import io.github.quezka.cadence.domain.builtInKind
import io.github.quezka.cadence.domain.Gym
import io.github.quezka.cadence.domain.Muscle
import io.github.quezka.cadence.domain.Routine
import io.github.quezka.cadence.domain.RoutineItem
import io.github.quezka.cadence.domain.Workout
import io.github.quezka.cadence.domain.WorkoutSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate

private class Helper(context: Context, name: String?) : SQLiteOpenHelper(context, name, null, VERSION) {
    override fun onConfigure(db: SQLiteDatabase) = db.setForeignKeyConstraintsEnabled(true)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE exercises (id INTEGER PRIMARY KEY, key TEXT UNIQUE, name TEXT NOT NULL, muscle TEXT NOT NULL, kind TEXT NOT NULL DEFAULT 'WEIGHT')")
        db.execSQL("CREATE TABLE routines (id INTEGER PRIMARY KEY, name TEXT NOT NULL)")
        db.execSQL("""CREATE TABLE routine_items (routine_id INTEGER NOT NULL REFERENCES routines(id) ON DELETE CASCADE,
            position INTEGER NOT NULL, exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
            sets INTEGER NOT NULL, reps INTEGER NOT NULL)""")
        db.execSQL("CREATE TABLE workouts (id INTEGER PRIMARY KEY, name TEXT NOT NULL, started INTEGER NOT NULL, finished INTEGER, day TEXT NOT NULL, note TEXT NOT NULL DEFAULT '')")
        db.execSQL("""CREATE TABLE sets (id INTEGER PRIMARY KEY, workout_id INTEGER NOT NULL REFERENCES workouts(id) ON DELETE CASCADE,
            exercise_id INTEGER NOT NULL REFERENCES exercises(id), position INTEGER NOT NULL, grams INTEGER NOT NULL,
            reps INTEGER NOT NULL, done INTEGER NOT NULL, seconds INTEGER NOT NULL DEFAULT 0, meters INTEGER NOT NULL DEFAULT 0)""")
        db.execSQL("CREATE INDEX idx_sets_exercise ON sets(exercise_id)")
        db.execSQL("CREATE INDEX idx_sets_workout ON sets(workout_id)")
        newTables(db)
        builtIns(db)
    }

    private fun newTables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE body (day TEXT PRIMARY KEY, grams INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
    }

    /** Adds the exercises that aren't there yet (so an update can bring new ones) and says how each is measured. */
    private fun builtIns(db: SQLiteDatabase) {
        for ((key, muscle) in BUILT_IN) {
            db.insertWithOnConflict("exercises", null, ContentValues().apply {
                put("key", key); put("name", ""); put("muscle", muscle.name)
            }, SQLiteDatabase.CONFLICT_IGNORE)
            db.update("exercises", ContentValues().apply { put("kind", builtInKind(key).name) }, "key = ?", arrayOf(key))
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN kind TEXT NOT NULL DEFAULT 'WEIGHT'")
            db.execSQL("ALTER TABLE workouts ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE sets ADD COLUMN seconds INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE sets ADD COLUMN meters INTEGER NOT NULL DEFAULT 0")
            newTables(db)
            builtIns(db)
        }
    }

    companion object { const val VERSION = 2 }
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
            runCatching { Muscle.valueOf(str("muscle")) }.getOrDefault(Muscle.OTHER),
            runCatching { ExerciseKind.valueOf(str("kind")) }.getOrDefault(ExerciseKind.WEIGHT))
    }

    fun addExercise(name: String, muscle: Muscle, kind: ExerciseKind = ExerciseKind.WEIGHT): Long {
        val id = db.insertOrThrow("exercises", null, ContentValues().apply {
            putNull("key"); put("name", name.trim()); put("muscle", muscle.name); put("kind", kind.name)
        })
        changed()
        return id
    }

    /** Only one of your own, and only if no workout used it: history must keep its names. False if it stays. */
    fun deleteExercise(id: Long): Boolean {
        val used = db.rawQuery("SELECT COUNT(*) FROM sets WHERE exercise_id = ?", arrayOf(id.toString())).use { it.moveToFirst(); it.getInt(0) } > 0
        if (used || db.delete("exercises", "id = ? AND key IS NULL", arrayOf(id.toString())) == 0) return false
        changed()
        return true
    }

    private fun kindOf(exerciseId: Long): ExerciseKind =
        db.rawQuery("SELECT kind FROM exercises WHERE id = ?", arrayOf(exerciseId.toString())).all { str("kind") }.firstOrNull()
            ?.let { runCatching { ExerciseKind.valueOf(it) }.getOrNull() } ?: ExerciseKind.WEIGHT

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

    fun duplicateRoutine(routine: Routine, name: String): Long = saveRoutine(null, name, routine.items)

    /** A routine out of a workout you did: same exercises, same number of sets. */
    fun routineFrom(workout: Workout): List<RoutineItem> = workout.sets.groupBy { it.exerciseId }.map { (id, sets) ->
        val target = when (kindOf(id)) {
            ExerciseKind.WEIGHT -> sets.maxOf { it.reps }
            ExerciseKind.TIME -> sets.maxOf { it.seconds }
            ExerciseKind.CARDIO -> sets.maxOf { it.seconds } / 60
        }
        RoutineItem(id, sets.size, target.coerceAtLeast(1))
    }

    fun deleteRoutine(id: Long) {
        db.delete("routines", "id = ?", arrayOf(id.toString()))
        changed()
    }

    // ---- workouts ----

    private fun setsOf(workoutId: Long) = db.rawQuery("SELECT * FROM sets WHERE workout_id = ? ORDER BY id", arrayOf(workoutId.toString())).all { aSet() }

    private fun Cursor.aSet() = WorkoutSet(long("id"), long("workout_id"), long("exercise_id"), int("position"), int("grams"), int("reps"), int("done") == 1, int("seconds"), int("meters"))

    private fun Cursor.aWorkout() = Workout(long("id"), str("name"), long("started"),
        if (isNull(getColumnIndexOrThrow("finished"))) null else long("finished"), LocalDate.parse(str("day")), note = str("note"))

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
            when (kindOf(item.exerciseId)) {
                ExerciseKind.WEIGHT -> {
                    val weight = Gym.nextWeight(lastSets(item.exerciseId), item.reps) ?: 0
                    repeat(item.sets) { addSet(id, item.exerciseId, weight, item.reps) }
                }
                ExerciseKind.TIME -> repeat(item.sets) { addSet(id, item.exerciseId, 0, 0, item.reps) }
                ExerciseKind.CARDIO -> {
                    val meters = lastSets(item.exerciseId).lastOrNull()?.meters ?: 0
                    repeat(item.sets) { addSet(id, item.exerciseId, 0, 0, item.reps * 60, meters) }
                }
            }
        }
        changed()
        return id
    }

    fun addExerciseToWorkout(workoutId: Long, exerciseId: Long) {
        val last = lastSets(exerciseId).lastOrNull()
        val kind = kindOf(exerciseId)
        addSet(workoutId, exerciseId, last?.grams ?: 0, if (kind == ExerciseKind.WEIGHT) last?.reps ?: 8 else 0,
            last?.seconds ?: if (kind == ExerciseKind.TIME) 60 else if (kind == ExerciseKind.CARDIO) 1800 else 0, last?.meters ?: 0)
    }

    fun removeExerciseFromWorkout(workoutId: Long, exerciseId: Long) {
        db.delete("sets", "workout_id = ? AND exercise_id = ?", arrayOf(workoutId.toString(), exerciseId.toString()))
        changed()
    }

    fun setNote(workoutId: Long, note: String) {
        db.update("workouts", ContentValues().apply { put("note", note) }, "id = ?", arrayOf(workoutId.toString()))
        changed()
    }

    fun addSet(workoutId: Long, exerciseId: Long, grams: Int, reps: Int, seconds: Int = 0, meters: Int = 0): Long {
        val position = db.rawQuery("SELECT COUNT(*) FROM sets WHERE workout_id = ? AND exercise_id = ?",
            arrayOf(workoutId.toString(), exerciseId.toString())).use { it.moveToFirst(); it.getInt(0) }
        val id = db.insertOrThrow("sets", null, ContentValues().apply {
            put("workout_id", workoutId); put("exercise_id", exerciseId); put("position", position)
            put("grams", grams); put("reps", reps); put("done", 0); put("seconds", seconds); put("meters", meters)
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

    fun updateTimedSet(id: Long, seconds: Int, meters: Int, done: Boolean) {
        db.update("sets", ContentValues().apply {
            put("seconds", seconds.coerceIn(0, Gym.MAX_SECONDS)); put("meters", meters.coerceIn(0, Gym.MAX_METERS)); put("done", if (done) 1 else 0)
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
        // Editing one finished long ago keeps its own time.
        if (kept) db.update("workouts", ContentValues().apply { put("finished", now()) }, "id = ? AND finished IS NULL", arrayOf(id.toString()))
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

    /** The best of each finished workout, oldest first: the progress line. [score] is what "best" means (the estimated single, unless told otherwise). */
    fun progress(exerciseId: Long, score: (WorkoutSet) -> Int = { Gym.oneRepMax(it.grams, it.reps) }): List<Pair<LocalDate, Int>> =
        db.rawQuery("""SELECT w.day AS day, s.* FROM sets s JOIN workouts w ON w.id = s.workout_id
            WHERE s.exercise_id = ? AND s.done = 1 AND w.finished IS NOT NULL ORDER BY w.started""", arrayOf(exerciseId.toString()))
            .all { LocalDate.parse(str("day")) to aSet() }
            .groupBy { it.first }.map { (day, sets) -> day to sets.maxOf { score(it.second) } }

    // ---- body weight ----

    fun logBody(day: LocalDate, grams: Int) {
        db.insertWithOnConflict("body", null, ContentValues().apply { put("day", day.toString()); put("grams", grams) }, SQLiteDatabase.CONFLICT_REPLACE)
        changed()
    }

    fun deleteBody(day: LocalDate) {
        db.delete("body", "day = ?", arrayOf(day.toString()))
        changed()
    }

    /** Oldest first. */
    fun bodyLog(): List<Pair<LocalDate, Int>> = db.rawQuery("SELECT * FROM body ORDER BY day", null).all { LocalDate.parse(str("day")) to int("grams") }

    // ---- settings ----

    fun restSeconds(): Int = setting("rest")?.toIntOrNull() ?: 90

    fun setRestSeconds(seconds: Int) {
        db.insertWithOnConflict("settings", null, ContentValues().apply { put("key", "rest"); put("value", seconds.toString()) }, SQLiteDatabase.CONFLICT_REPLACE)
        changed()
    }

    private fun setting(key: String): String? = db.rawQuery("SELECT value FROM settings WHERE key = ?", arrayOf(key)).all { str("value") }.firstOrNull()

    // ---- backup: the database file itself, so nothing is left out ----

    fun export(out: OutputStream) {
        val file = File(db.path)
        file.inputStream().use { it.copyTo(out) }
    }

    /** Replaces everything with the backup in [input]. False, and nothing touched, if it isn't one. The app must restart afterwards. */
    fun restore(context: Context, input: InputStream): Boolean {
        val target = File(db.path)
        val incoming = File.createTempFile("restore", ".db", context.cacheDir)
        try {
            incoming.outputStream().use { input.copyTo(it) }
            if (!isBackup(incoming)) return false
            db.close()
            incoming.copyTo(target, overwrite = true)
            return true
        } finally {
            incoming.delete()
        }
    }

    private fun isBackup(file: File): Boolean = runCatching {
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { d ->
            d.rawQuery("SELECT COUNT(*) FROM workouts", null).use { it.moveToFirst() } &&
                d.rawQuery("SELECT COUNT(*) FROM exercises", null).use { it.moveToFirst() }
        }
    }.getOrDefault(false)

    /** The best estimate before this workout, to tell a new record. */
    fun bestBefore(exerciseId: Long, workoutId: Long): Int =
        db.rawQuery("""SELECT s.grams AS grams, s.reps AS reps FROM sets s JOIN workouts w ON w.id = s.workout_id
            WHERE s.exercise_id = ? AND s.done = 1 AND w.finished IS NOT NULL AND w.id != ?""", arrayOf(exerciseId.toString(), workoutId.toString()))
            .all { Gym.oneRepMax(int("grams"), int("reps")) }.maxOrNull() ?: 0

    fun close() = db.close()
}
