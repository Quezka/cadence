package io.github.quezka.cadence.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.quezka.cadence.R
import io.github.quezka.cadence.data.GymStore
import io.github.quezka.cadence.domain.Exercise
import io.github.quezka.cadence.domain.Gym
import io.github.quezka.cadence.domain.Muscle
import io.github.quezka.cadence.domain.Routine
import io.github.quezka.cadence.domain.RoutineItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class Tab { TRAIN, HISTORY, EXERCISES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadenceRoot(store: GymStore) {
    val version by store.version.collectAsState()
    var tab by remember { mutableStateOf(Tab.TRAIN) }
    var openWorkout by remember { mutableStateOf<Long?>(store.active()?.id) }
    var detail by remember { mutableStateOf<Long?>(null) }
    var pastWorkout by remember { mutableStateOf<Long?>(null) }
    var editingRoutine by remember { mutableStateOf<Routine?>(null) }
    var newRoutine by remember { mutableStateOf(false) }

    val exercises = remember(version) { store.exercises() }
    openWorkout?.let { id ->
        WorkoutScreen(store, id, exercises, version) { openWorkout = null }
        return
    }
    detail?.let { id ->
        BackHandler { detail = null }
        ExerciseScreen(store, exercises.first { it.id == id }, version) { detail = null }
        return
    }
    pastWorkout?.let { id ->
        BackHandler { pastWorkout = null }
        PastWorkoutScreen(store, id, exercises, version) { pastWorkout = null }
        return
    }
    if (newRoutine || editingRoutine != null) {
        val close = { newRoutine = false; editingRoutine = null }
        BackHandler { close() }
        RoutineScreen(store, editingRoutine, exercises, close)
        return
    }
    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(tab == Tab.TRAIN, { tab = Tab.TRAIN }, { Icon(Icons.Filled.FitnessCenter, null) },
                label = { Text(stringResource(R.string.tab_train)) })
            NavigationBarItem(tab == Tab.HISTORY, { tab = Tab.HISTORY }, { Icon(Icons.Filled.History, null) },
                label = { Text(stringResource(R.string.tab_history)) })
            NavigationBarItem(tab == Tab.EXERCISES, { tab = Tab.EXERCISES }, { Icon(Icons.Filled.ShowChart, null) },
                label = { Text(stringResource(R.string.tab_exercises)) })
        }
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.TRAIN -> TrainTab(store, version, { openWorkout = it }, { newRoutine = true }, { editingRoutine = it })
                Tab.HISTORY -> HistoryTab(store, exercises, version) { pastWorkout = it }
                Tab.EXERCISES -> ExercisesTab(store, exercises) { detail = it }
            }
        }
    }
}

@Composable
private fun TrainTab(store: GymStore, version: Int, open: (Long) -> Unit, addRoutine: () -> Unit, editRoutine: (Routine) -> Unit) {
    val routines = remember(version) { store.routines() }
    val history = remember(version) { store.history(120) }
    val today = LocalDate.now()
    val thisWeek = history.count { Gym.weekOf(it.day) == Gym.weekOf(today) }
    val streak = Gym.weekStreak(history.map { it.day }, today)
    val freeName = stringResource(R.string.free_workout)
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Hint(listOfNotNull(pluralStringResource(R.plurals.workouts_this_week, thisWeek, thisWeek),
                if (streak >= 2) stringResource(R.string.weeks_in_a_row, streak) else null).joinToString(" · "))
        }
        item {
            Button(onClick = { open(store.start(freeName)) }, Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.start_free), style = MaterialTheme.typography.titleMedium)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.routines), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = addRoutine) { Text(stringResource(R.string.new_routine)) }
            }
        }
        if (routines.isEmpty()) item { Hint(stringResource(R.string.no_routines)) }
        items(routines, key = { it.id }) { r ->
            Panel {
                Text(r.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Hint(pluralStringResource(R.plurals.exercises_count, r.items.size, r.items.size))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { open(store.start(r.name, r)) }) { Text(stringResource(R.string.start)) }
                    OutlinedButton(onClick = { editRoutine(r) }) { Text(stringResource(R.string.edit)) }
                }
            }
        }
    }
}

// ---- history ----

private fun volumeText(grams: Long) = "${(grams / 1000)} kg"

@Composable
private fun HistoryTab(store: GymStore, exercises: List<Exercise>, version: Int, open: (Long) -> Unit) {
    val context = LocalContext.current
    val history = remember(version) { store.history() }
    val format = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val names = exercises.associate { it.id to it.title(context) }
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(Gym.csv(store.history(100_000).reversed()) { id -> names[id].orEmpty() }.toByteArray())
            }
        }
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tab_history), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (history.isNotEmpty()) TextButton(onClick = { save.launch("cadence.csv") }) { Text(stringResource(R.string.export_csv)) }
            }
        }
        if (history.isEmpty()) item { Hint(stringResource(R.string.no_history)) }
        items(history, key = { it.id }) { w ->
            Panel(modifier = Modifier.clickable { open(w.id) }) {
                Text(w.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Hint(listOf(w.day.format(format), pluralStringResource(R.plurals.sets_count, w.sets.size, w.sets.size),
                    volumeText(Gym.volume(w.sets))).joinToString(" · "))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastWorkoutScreen(store: GymStore, id: Long, exercises: List<Exercise>, version: Int, back: () -> Unit) {
    val context = LocalContext.current
    val workout = remember(version) { store.workout(id) } ?: return
    var confirming by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(workout.name) }, navigationIcon = {
            IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
        }, actions = { IconButton(onClick = { confirming = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) } })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            workout.finished?.let { end -> Hint(stringResource(R.string.lasted, clock(end - workout.started)) + " · " + volumeText(Gym.volume(workout.sets))) }
            for ((exerciseId, sets) in workout.sets.groupBy { it.exerciseId }) {
                val exercise = exercises.firstOrNull { it.id == exerciseId } ?: continue
                Panel(exercise.title(context)) {
                    for (s in sets) Text(stringResource(R.string.set_line, s.position + 1, Gym.showKg(s.grams), s.reps), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
    if (confirming) AlertDialog(onDismissRequest = { confirming = false },
        title = { Text(stringResource(R.string.delete_workout)) },
        confirmButton = { TextButton(onClick = { store.deleteWorkout(id); back() }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.cancel)) } })
}

// ---- exercises ----

@Composable
private fun ExercisesTab(store: GymStore, exercises: List<Exercise>, open: (Long) -> Unit) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    val shown = exercises.map { it to it.title(context) }.filter { it.second.contains(search.trim(), ignoreCase = true) }
        .sortedBy { it.second.lowercase() }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tab_exercises), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = { adding = true }) { Text(stringResource(R.string.new_exercise)) }
            }
            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth().padding(bottom = 8.dp), singleLine = true,
                placeholder = { Text(stringResource(R.string.search)) })
        }
        items(shown, key = { it.first.id }) { (e, name) ->
            Column(Modifier.fillMaxWidth().clickable { open(e.id) }.padding(vertical = 10.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Hint(stringResource(muscleName(e.muscle)))
            }
        }
    }
    if (adding) NewExerciseDialog({ adding = false }) { name, muscle -> store.addExercise(name, muscle); adding = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewExerciseDialog(close: () -> Unit, save: (String, Muscle) -> Unit) {
    var name by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf(Muscle.OTHER) }
    var open by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = close, title = { Text(stringResource(R.string.new_exercise)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true)
                ExposedDropdownMenuBox(open, { open = it }) {
                    OutlinedTextField(stringResource(muscleName(muscle)), {}, readOnly = true, label = { Text(stringResource(R.string.muscle)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable))
                    ExposedDropdownMenu(open, { open = false }) {
                        for (m in Muscle.entries) DropdownMenuItem(text = { Text(stringResource(muscleName(m))) }, onClick = { muscle = m; open = false })
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { save(name, muscle) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.cancel)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseScreen(store: GymStore, exercise: Exercise, version: Int, back: () -> Unit) {
    val context = LocalContext.current
    val progress = remember(version) { store.progress(exercise.id) }
    val last = remember(version) { store.lastSets(exercise.id) }
    val best = progress.maxOfOrNull { it.second }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(exercise.title(context)) }, navigationIcon = {
            IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
        })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(stringResource(R.string.best_estimate)) {
                if (best == null) Hint(stringResource(R.string.no_sets_yet)) else {
                    Text("${Gym.showKg(best)} kg", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Hint(stringResource(R.string.best_estimate_hint))
                    if (best > 20000) {
                        val (plates, left) = Gym.platesPerSide(best)
                        Hint(stringResource(R.string.plates_per_side, plates.joinToString(" + ") { Gym.showKg(it) }.ifEmpty { "—" }) +
                            if (left > 0) " (+${Gym.showKg(left)})" else "")
                    }
                }
            }
            if (progress.size >= 2) Panel(stringResource(R.string.progress)) { ProgressChart(progress) }
            if (last.isNotEmpty()) Panel(stringResource(R.string.last_time)) {
                for (s in last) Text(stringResource(R.string.set_line, s.position + 1, Gym.showKg(s.grams), s.reps), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun ProgressChart(points: List<Pair<LocalDate, Int>>) {
    val color = MaterialTheme.colorScheme.primary
    val low = points.minOf { it.second }
    val high = points.maxOf { it.second }
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val span = (high - low).coerceAtLeast(1)
        val at = points.mapIndexed { i, (_, v) ->
            Offset(size.width * i / (points.size - 1), size.height - 8f - (size.height - 16f) * (v - low) / span)
        }
        at.zipWithNext { a, b -> drawLine(color, a, b, strokeWidth = 6f, cap = StrokeCap.Round) }
        at.forEach { drawCircle(color, 8f, it) }
    }
    Hint("${Gym.showKg(low)} – ${Gym.showKg(high)} kg")
}

// ---- routines ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineScreen(store: GymStore, existing: Routine?, exercises: List<Exercise>, close: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    val items = remember { mutableStateListOf<RoutineItem>().apply { addAll(existing?.items.orEmpty()) } }
    var picking by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().imePadding()) {
        TopAppBar(title = { Text(stringResource(if (existing == null) R.string.new_routine else R.string.edit_routine)) }, navigationIcon = {
            IconButton(onClick = close) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
        }, actions = {
            if (existing != null) IconButton(onClick = { store.deleteRoutine(existing.id); close() }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
            TextButton(enabled = name.isNotBlank() && items.isNotEmpty(), onClick = { store.saveRoutine(existing?.id, name, items.toList()); close() }) { Text(stringResource(R.string.save)) }
        })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.name)) },
                placeholder = { Text(stringResource(R.string.routine_hint)) }, singleLine = true)
            items.forEachIndexed { i, item ->
                val exercise = exercises.firstOrNull { it.id == item.exerciseId }
                Panel(exercise?.title(context).orEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberBox(item.sets, stringResource(R.string.sets), Modifier.weight(1f)) { items[i] = item.copy(sets = it) }
                        NumberBox(item.reps, stringResource(R.string.reps), Modifier.weight(1f)) { items[i] = item.copy(reps = it) }
                        IconButton(onClick = { items.removeAt(i) }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                    }
                }
            }
            FilledTonalButton(onClick = { picking = true }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_exercise)) }
        }
    }
    if (picking) ExercisePicker(exercises, { picking = false }) { items += RoutineItem(it, 3, 8); picking = false }
}

@Composable
private fun NumberBox(value: Int, label: String, modifier: Modifier, change: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(text, { t -> text = t.filter(Char::isDigit).take(3); text.toIntOrNull()?.takeIf { it > 0 }?.let(change) },
        modifier, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
}

@Composable
fun ExercisePicker(exercises: List<Exercise>, close: () -> Unit, pick: (Long) -> Unit) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    val shown = exercises.map { it to it.title(context) }.filter { it.second.contains(search.trim(), ignoreCase = true) }.sortedBy { it.second.lowercase() }
    AlertDialog(onDismissRequest = close, title = { Text(stringResource(R.string.add_exercise)) },
        text = {
            Column {
                OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth().padding(bottom = 8.dp), singleLine = true,
                    placeholder = { Text(stringResource(R.string.search)) })
                LazyColumn(Modifier.height(320.dp)) {
                    items(shown, key = { it.first.id }) { (e, name) ->
                        Text(name, Modifier.fillMaxWidth().clickable { pick(e.id) }.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {}, dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.cancel)) } })
}

