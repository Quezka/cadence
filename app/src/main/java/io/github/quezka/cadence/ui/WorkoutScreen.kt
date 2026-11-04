package io.github.quezka.cadence.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.quezka.cadence.CadenceApp
import io.github.quezka.cadence.MainActivity
import io.github.quezka.cadence.R
import io.github.quezka.cadence.data.GymStore
import io.github.quezka.cadence.domain.Exercise
import io.github.quezka.cadence.domain.ExerciseKind
import io.github.quezka.cadence.domain.Gym
import io.github.quezka.cadence.domain.WorkoutSet
import kotlinx.coroutines.delay

/** The rest is over: a buzz, and a notification in case the phone is in a pocket. */
private fun restOver(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java).defaultVibrator
    else @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
    val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
    runCatching {
        NotificationManagerCompat.from(context).notify(1, NotificationCompat.Builder(context, CadenceApp.REST_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(context.getString(R.string.rest_over))
            .setContentText(context.getString(R.string.rest_over_text)).setContentIntent(open).setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH).build())
    }  // no permission: the buzz is enough
}

/** A workout under way or, for one already finished ([Workout.finished] set), the same page to correct it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(store: GymStore, workoutId: Long, exercises: List<Exercise>, version: Int, close: () -> Unit) {
    val context = LocalContext.current
    val workout = remember(version) { store.workout(workoutId) }
    if (workout == null) { LaunchedEffect(Unit) { close() }; return }
    val editing = workout.finished != null
    var picking by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }
    var discarding by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<List<Pair<Long, WorkoutSet>>?>(null) }
    // These survive turning the phone: the rest doesn't stop because the screen rotated.
    var restUntil by rememberSaveable { mutableLongStateOf(0L) }
    var restLength by rememberSaveable { mutableLongStateOf(90L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    val leaveEdit = { store.finish(workoutId); close() }  // a set added and left unticked isn't kept

    LaunchedEffect(Unit) {
        while (true) {
            val next = System.currentTimeMillis() / 1000
            if (restUntil in (now + 1)..next) restOver(context)
            now = next
            delay(1000)
        }
    }
    // The screen stays on between sets: nobody wants to unlock with chalk on their hands.
    if (!editing) DisposableEffect(Unit) {
        val window = (context as? ComponentActivity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    BackHandler { if (editing) leaveEdit() else close() }

    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
        TopAppBar(title = {
            Column {
                Text(workout.name, maxLines = 1)
                if (!editing) Text(clock((now - workout.started).coerceAtLeast(0)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }, navigationIcon = {
            IconButton(onClick = { if (editing) leaveEdit() else close() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
        }, actions = {
            if (editing) Button(onClick = { leaveEdit() }, Modifier.padding(end = 8.dp)) { Text(stringResource(R.string.done)) }
            else {
                TextButton(onClick = { discarding = true }) { Text(stringResource(R.string.discard)) }
                Button(onClick = { finishing = true }, Modifier.padding(end = 8.dp)) { Text(stringResource(R.string.finish)) }
            }
        })
        val left = restUntil - now
        if (left > 0) RestBar(left, restLength, { restUntil += 30; restLength += 30 }, { restUntil -= 15 }, { restUntil = 0 })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (workout.sets.isEmpty()) Hint(stringResource(R.string.workout_empty))
            for ((exerciseId, sets) in workout.sets.groupBy { it.exerciseId }) {
                val exercise = exercises.firstOrNull { it.id == exerciseId } ?: continue
                ExerciseCard(store, workoutId, exercise, sets) {
                    if (!editing) {
                        restLength = store.restSeconds().toLong()
                        restUntil = System.currentTimeMillis() / 1000 + restLength
                    }
                }
            }
            FilledTonalButton(onClick = { picking = true }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_exercise)) }
            var note by remember(workoutId) { mutableStateOf(workout.note) }
            OutlinedTextField(note, { note = it; store.setNote(workoutId, it) }, Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.note)) }, placeholder = { Text(stringResource(R.string.note_hint)) })
        }
    }
    if (picking) ExercisePicker(store, exercises, { picking = false }) { store.addExerciseToWorkout(workoutId, it); picking = false }
    if (discarding) AlertDialog(onDismissRequest = { discarding = false }, title = { Text(stringResource(R.string.discard_title)) },
        confirmButton = { TextButton(onClick = { store.deleteWorkout(workoutId); close() }) { Text(stringResource(R.string.discard)) } },
        dismissButton = { TextButton(onClick = { discarding = false }) { Text(stringResource(R.string.cancel)) } })
    if (finishing) AlertDialog(onDismissRequest = { finishing = false }, title = { Text(stringResource(R.string.finish_title)) },
        text = { Text(stringResource(R.string.finish_text)) },
        confirmButton = {
            TextButton(onClick = {
                finishing = false
                val records = workout.sets.groupBy { it.exerciseId }.flatMap { (id, sets) ->
                    Gym.records(sets, store.bestBefore(id, workoutId)).map { id to it }
                }
                if (store.finish(workoutId) && records.isNotEmpty()) summary = records else close()
            }) { Text(stringResource(R.string.finish)) }
        },
        dismissButton = { TextButton(onClick = { finishing = false }) { Text(stringResource(R.string.cancel)) } })
    summary?.let { records ->
        AlertDialog(onDismissRequest = close, title = { Text(stringResource(R.string.new_records)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for ((id, set) in records.distinctBy { it.first }) {
                        val name = exercises.firstOrNull { it.id == id }?.title(context).orEmpty()
                        Text("$name: ${Gym.showKg(set.grams)} kg × ${set.reps}", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }, confirmButton = { TextButton(onClick = close) { Text(stringResource(R.string.nice)) } })
    }
}

@Composable
private fun RestBar(left: Long, total: Long, more: () -> Unit, less: () -> Unit, stop: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.rest) + "  " + clock(left), Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = less) { Text("−15") }
            OutlinedButton(onClick = more) { Text("+30") }
            IconButton(onClick = stop) { Icon(Icons.Filled.Close, stringResource(R.string.stop_rest)) }
        }
        LinearProgressIndicator(progress = { (left.toFloat() / total).coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
    }
}

/** What last time's set was, in few characters. */
private fun shortSet(s: WorkoutSet, kind: ExerciseKind): String = when (kind) {
    ExerciseKind.WEIGHT -> if (s.grams > 0) "${Gym.showKg(s.grams)}×${s.reps}" else "×${s.reps}"
    ExerciseKind.TIME -> clock(s.seconds.toLong())
    ExerciseKind.CARDIO -> clock(s.seconds.toLong()) + if (s.meters > 0) " ${Gym.showKm(s.meters)} km" else ""
}

@Composable
private fun ExerciseCard(store: GymStore, workoutId: Long, exercise: Exercise, sets: List<WorkoutSet>, rest: () -> Unit) {
    val context = LocalContext.current
    val last = remember(exercise.id) { store.lastSets(exercise.id).filter { it.workoutId != workoutId } }
    var plates by remember { mutableStateOf(false) }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(exercise.title(context), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { store.removeExerciseFromWorkout(workoutId, exercise.id) }) {
                Icon(Icons.Filled.Delete, stringResource(R.string.remove_exercise))
            }
        }
        sets.sortedBy { it.position }.forEachIndexed { i, s -> SetRow(store, i + 1, s, exercise.kind, last.getOrNull(i), rest) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                val previous = sets.maxByOrNull { it.position }
                store.addSet(workoutId, exercise.id, previous?.grams ?: 0, previous?.reps ?: 8, previous?.seconds ?: 0, previous?.meters ?: 0)
            }) { Text(stringResource(R.string.add_set)) }
            Spacer(Modifier.weight(1f))
            if (exercise.kind == ExerciseKind.WEIGHT && sets.any { it.grams > 0 }) TextButton(onClick = { plates = true }) { Text(stringResource(R.string.plates)) }
        }
    }
    if (plates) PlatesDialog(sets.maxOf { it.grams }) { plates = false }
}

@Composable
private fun PlatesDialog(grams: Int, close: () -> Unit) {
    var bar by remember { mutableStateOf(20000) }
    val (plates, left) = Gym.platesPerSide(grams, bar)
    AlertDialog(onDismissRequest = close, title = { Text(stringResource(R.string.plates_for, Gym.showKg(grams), Gym.showKg(bar))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.bar))
                    for (b in listOf(20000, 15000, 10000)) FilterChip(bar == b, { bar = b }, { Text(Gym.showKg(b)) })
                }
                Text(stringResource(R.string.plates_side, plates.joinToString(" + ") { Gym.showKg(it) }.ifEmpty { "—" }),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (left > 0) Hint(stringResource(R.string.plates_left, Gym.showKg(left)))
            }
        }, confirmButton = { TextButton(onClick = close) { Text(stringResource(R.string.close)) } })
}

@Composable
private fun SetRow(store: GymStore, number: Int, set: WorkoutSet, kind: ExerciseKind, last: WorkoutSet?, rest: () -> Unit) {
    // A set that's done stands out, so you can see at a glance where you are.
    val tint = if (set.done) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent
    Column(Modifier.fillMaxWidth().background(tint, RoundedCornerShape(14.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
        when (kind) {
            ExerciseKind.WEIGHT -> WeightFields(store, number, set, last, rest)
            ExerciseKind.TIME -> TimedFields(store, number, set, last, false, rest)
            ExerciseKind.CARDIO -> TimedFields(store, number, set, last, true, rest)
        }
    }
}

@Composable
private fun WeightFields(store: GymStore, number: Int, set: WorkoutSet, last: WorkoutSet?, rest: () -> Unit) {
    var kg by remember(set.id) { mutableStateOf(if (set.grams > 0) Gym.showKg(set.grams) else "") }
    var reps by remember(set.id) { mutableStateOf(set.reps.toString()) }
    val grams = Gym.parseKg(kg)
    val count = reps.toIntOrNull()
    val valid = grams != null && count != null && count > 0
    fun step(by: Int) {
        val next = ((grams ?: 0) + by).coerceIn(0, Gym.MAX_GRAMS)
        kg = if (next > 0) Gym.showKg(next) else ""
        if (count != null) store.updateSet(set.id, next, count, set.done)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(number.toString(), Modifier.width(20.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(kg, { kg = it; val g = Gym.parseKg(it); if (g != null && count != null) store.updateSet(set.id, g, count, set.done) },
            Modifier.weight(1f), label = { Text("kg") }, singleLine = true, isError = grams == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        OutlinedTextField(reps, { t -> reps = t.filter(Char::isDigit).take(4); val r = reps.toIntOrNull(); if (r != null && grams != null) store.updateSet(set.id, grams, r, set.done) },
            Modifier.weight(1f), label = { Text(stringResource(R.string.reps)) }, singleLine = true, isError = count == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        DoneButton(set.done, valid) { on ->
            store.updateSet(set.id, grams!!, count!!, on)
            if (on) rest()
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { step(-Gym.STEP_GRAMS) }) { Text("−2.5") }
        TextButton(onClick = { step(Gym.STEP_GRAMS) }) { Text("+2.5") }
        Spacer(Modifier.weight(1f))
        last?.let { Hint(stringResource(R.string.last_short, shortSet(it, ExerciseKind.WEIGHT))) }
        RemoveSet(store, set)
    }
}

@Composable
private fun TimedFields(store: GymStore, number: Int, set: WorkoutSet, last: WorkoutSet?, cardio: Boolean, rest: () -> Unit) {
    // Cardio is counted in minutes and km, something held in seconds.
    val unit = if (cardio) 60 else 1
    var time by remember(set.id) { mutableStateOf(if (set.seconds > 0) (set.seconds / unit).toString() else "") }
    var km by remember(set.id) { mutableStateOf(if (set.meters > 0) Gym.showKm(set.meters) else "") }
    val seconds = time.toIntOrNull()?.times(unit)
    val meters = Gym.parseKm(km)
    val valid = seconds != null && seconds > 0 && meters != null
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(number.toString(), Modifier.width(20.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(time, { t -> time = t.filter(Char::isDigit).take(5); val s = time.toIntOrNull()?.times(unit); if (s != null && meters != null) store.updateTimedSet(set.id, s, meters, set.done) },
            Modifier.weight(1f), label = { Text(stringResource(if (cardio) R.string.minutes else R.string.seconds)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        if (cardio) OutlinedTextField(km, { km = it; val m = Gym.parseKm(it); if (m != null && seconds != null) store.updateTimedSet(set.id, seconds, m, set.done) },
            Modifier.weight(1f), label = { Text(stringResource(R.string.km)) }, singleLine = true, isError = meters == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        DoneButton(set.done, valid) { on ->
            store.updateTimedSet(set.id, seconds!!, meters!!, on)
            if (on) rest()
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        last?.let { Hint(stringResource(R.string.last_short, shortSet(it, if (cardio) ExerciseKind.CARDIO else ExerciseKind.TIME))) }
        RemoveSet(store, set)
    }
}

@Composable
private fun DoneButton(done: Boolean, valid: Boolean, change: (Boolean) -> Unit) {
    FilledIconToggleButton(done, { if (valid) change(it) }, enabled = valid) { Icon(Icons.Filled.Check, stringResource(R.string.set_done)) }
}

@Composable
private fun RemoveSet(store: GymStore, set: WorkoutSet) {
    IconButton(onClick = { store.deleteSet(set.id) }) { Icon(Icons.Filled.Close, stringResource(R.string.delete)) }
}
