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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import io.github.quezka.cadence.domain.Gym
import io.github.quezka.cadence.domain.WorkoutSet
import kotlinx.coroutines.delay

private const val DEFAULT_REST = 90L

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(store: GymStore, workoutId: Long, exercises: List<Exercise>, version: Int, close: () -> Unit) {
    val context = LocalContext.current
    val workout = remember(version) { store.workout(workoutId) }
    if (workout == null) { LaunchedEffect(Unit) { close() }; return }
    var picking by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }
    var discarding by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<List<Pair<Long, WorkoutSet>>?>(null) }
    var restUntil by remember { mutableLongStateOf(0L) }
    var restLength by remember { mutableLongStateOf(DEFAULT_REST) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }

    LaunchedEffect(Unit) {
        while (true) {
            val next = System.currentTimeMillis() / 1000
            if (restUntil in (now + 1)..next) restOver(context)
            now = next
            delay(1000)
        }
    }
    // The screen stays on between sets: nobody wants to unlock with chalk on their hands.
    DisposableEffect(Unit) {
        val window = (context as? ComponentActivity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    BackHandler { close() }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopAppBar(title = {
            Column {
                Text(workout.name, maxLines = 1)
                Text(clock((now - workout.started).coerceAtLeast(0)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }, navigationIcon = {
            IconButton(onClick = close) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
        }, actions = {
            TextButton(onClick = { discarding = true }) { Text(stringResource(R.string.discard)) }
            Button(onClick = { finishing = true }, Modifier.padding(end = 8.dp)) { Text(stringResource(R.string.finish)) }
        })
        val left = restUntil - now
        if (left > 0) RestBar(left, restLength, { restUntil += 30; restLength += 30 }, { restUntil = 0 })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (workout.sets.isEmpty()) Hint(stringResource(R.string.workout_empty))
            for ((exerciseId, sets) in workout.sets.groupBy { it.exerciseId }) {
                val exercise = exercises.firstOrNull { it.id == exerciseId } ?: continue
                ExerciseCard(store, workoutId, exercise, sets) {
                    restLength = DEFAULT_REST
                    restUntil = System.currentTimeMillis() / 1000 + DEFAULT_REST
                }
            }
            FilledTonalButton(onClick = { picking = true }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_exercise)) }
        }
    }
    if (picking) ExercisePicker(exercises, { picking = false }) { store.addExerciseToWorkout(workoutId, it); picking = false }
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
private fun RestBar(left: Long, total: Long, more: () -> Unit, stop: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.rest) + "  " + clock(left), Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = more) { Text("+30 s") }
            IconButton(onClick = stop) { Icon(Icons.Filled.Close, stringResource(R.string.stop_rest)) }
        }
        LinearProgressIndicator(progress = { (left.toFloat() / total).coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ExerciseCard(store: GymStore, workoutId: Long, exercise: Exercise, sets: List<WorkoutSet>, rest: () -> Unit) {
    val context = LocalContext.current
    val last = remember(exercise.id) { store.lastSets(exercise.id) }
    Panel(exercise.title(context)) {
        if (last.isNotEmpty()) Hint(stringResource(R.string.last_time_short, last.joinToString(", ") { "${Gym.showKg(it.grams)}×${it.reps}" }))
        sets.sortedBy { it.position }.forEachIndexed { i, s -> SetRow(store, i + 1, s, rest) }
        TextButton(onClick = {
            val previous = sets.maxByOrNull { it.position }
            store.addSet(workoutId, exercise.id, previous?.grams ?: 0, previous?.reps ?: 8)
        }) { Text(stringResource(R.string.add_set)) }
    }
}

@Composable
private fun SetRow(store: GymStore, number: Int, set: WorkoutSet, rest: () -> Unit) {
    var kg by remember(set.id) { mutableStateOf(if (set.grams > 0) Gym.showKg(set.grams) else "") }
    var reps by remember(set.id) { mutableStateOf(set.reps.toString()) }
    val grams = Gym.parseKg(kg)
    val count = reps.toIntOrNull()
    val valid = grams != null && count != null
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(number.toString(), Modifier.width(22.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(kg, { kg = it; val g = Gym.parseKg(it); if (g != null && count != null) store.updateSet(set.id, g, count, set.done) },
            Modifier.weight(1f), label = { Text("kg") }, singleLine = true, isError = grams == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        OutlinedTextField(reps, { t -> reps = t.filter(Char::isDigit).take(4); val r = reps.toIntOrNull(); if (r != null && grams != null) store.updateSet(set.id, grams, r, set.done) },
            Modifier.weight(1f), label = { Text(stringResource(R.string.reps)) }, singleLine = true, isError = count == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        FilledIconToggleButton(set.done, { on ->
            if (valid) {
                store.updateSet(set.id, grams!!, count!!, on)
                if (on) rest()
            }
        }, enabled = valid) { Icon(Icons.Filled.Check, stringResource(R.string.set_done)) }
        IconButton(onClick = { store.deleteSet(set.id) }) { Icon(Icons.Filled.Close, stringResource(R.string.delete)) }
    }
}
