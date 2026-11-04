package io.github.quezka.cadence.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.quezka.cadence.R
import io.github.quezka.cadence.data.GymStore
import io.github.quezka.cadence.domain.Exercise
import io.github.quezka.cadence.domain.ExerciseKind
import io.github.quezka.cadence.domain.WorkoutSet
import io.github.quezka.cadence.domain.Gym
import io.github.quezka.cadence.domain.Muscle
import io.github.quezka.cadence.domain.Routine
import io.github.quezka.cadence.domain.RoutineItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class Tab { TRAIN, HISTORY, EXERCISES, STATS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadenceRoot(store: GymStore) {
    val version by store.version.collectAsState()
    // Saved, so turning the phone doesn't send you back to the first page.
    var tab by rememberSaveable { mutableStateOf(Tab.TRAIN) }
    var openWorkout by rememberSaveable { mutableStateOf(store.active()?.id) }
    var detail by rememberSaveable { mutableStateOf<Long?>(null) }
    var pastWorkout by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingRoutine by rememberSaveable { mutableStateOf<Long?>(null) }
    var newRoutine by rememberSaveable { mutableStateOf(false) }
    var settings by rememberSaveable { mutableStateOf(false) }

    val exercises = remember(version) { store.exercises() }
    openWorkout?.let { id ->
        WorkoutScreen(store, id, exercises, version) { openWorkout = null }
        return
    }
    if (settings) {
        SettingsScreen(store, version) { settings = false }
        return
    }
    detail?.let { id ->
        BackHandler { detail = null }
        val exercise = exercises.firstOrNull { it.id == id }
        if (exercise == null) { detail = null; return }
        ExerciseScreen(store, exercise, version) { detail = null }
        return
    }
    pastWorkout?.let { id ->
        BackHandler { pastWorkout = null }
        PastWorkoutScreen(store, id, exercises, version, { pastWorkout = null }, { openWorkout = it }) { editingRoutine = it }
        return
    }
    if (newRoutine || editingRoutine != null) {
        val close = { newRoutine = false; editingRoutine = null }
        BackHandler { close() }
        RoutineScreen(store, store.routines().firstOrNull { it.id == editingRoutine }, exercises, close)
        return
    }
    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(tab == Tab.TRAIN, { tab = Tab.TRAIN }, { Icon(Icons.Filled.FitnessCenter, null) },
                label = { Text(stringResource(R.string.tab_train)) })
            NavigationBarItem(tab == Tab.HISTORY, { tab = Tab.HISTORY }, { Icon(Icons.Filled.History, null) },
                label = { Text(stringResource(R.string.tab_history)) })
            NavigationBarItem(tab == Tab.STATS, { tab = Tab.STATS }, { Icon(Icons.AutoMirrored.Filled.ShowChart, null) },
                label = { Text(stringResource(R.string.tab_stats)) })
            NavigationBarItem(tab == Tab.EXERCISES, { tab = Tab.EXERCISES }, { Icon(Icons.AutoMirrored.Filled.List, null) },
                label = { Text(stringResource(R.string.tab_exercises)) })
        }
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.TRAIN -> TrainTab(store, version, { openWorkout = it }, { newRoutine = true }, { editingRoutine = it }) { settings = true }
                Tab.HISTORY -> HistoryTab(store, exercises, version) { pastWorkout = it }
                Tab.STATS -> StatsTab(store, exercises, version)
                Tab.EXERCISES -> ExercisesTab(store, exercises) { detail = it }
            }
        }
    }
}

@Composable
private fun TrainTab(store: GymStore, version: Int, open: (Long) -> Unit, addRoutine: () -> Unit, editRoutine: (Long) -> Unit, settings: () -> Unit) {
    val routines = remember(version) { store.routines() }
    val history = remember(version) { store.history(120) }
    val active = remember(version) { store.active() }
    val today = LocalDate.now()
    val thisWeek = history.count { Gym.weekOf(it.day) == Gym.weekOf(today) }
    val streak = Gym.weekStreak(history.map { it.day }, today)
    val freeName = stringResource(R.string.free_workout)
    val copy = stringResource(R.string.copy_of, "%s")
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.app_name), Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = settings) { Icon(Icons.Filled.Settings, stringResource(R.string.settings)) }
            }
            Hint(listOfNotNull(pluralStringResource(R.plurals.workouts_this_week, thisWeek, thisWeek),
                if (streak >= 2) stringResource(R.string.weeks_in_a_row, streak) else null).joinToString(" · "))
        }
        item {
            // One at a time: you leave a workout with Back and must be able to find it again.
            if (active != null) Panel(stringResource(R.string.resume_title)) {
                Text(active.name, style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { open(active.id) }, Modifier.fillMaxWidth().height(56.dp)) {
                    Text(stringResource(R.string.resume), style = MaterialTheme.typography.titleMedium)
                }
                Hint(stringResource(R.string.finish_first))
            } else Button(onClick = { open(store.start(freeName)) }, Modifier.fillMaxWidth().height(56.dp)) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { open(store.start(r.name, r)) }, enabled = active == null) { Text(stringResource(R.string.start)) }
                    OutlinedButton(onClick = { editRoutine(r.id) }) { Text(stringResource(R.string.edit)) }
                    IconButton(onClick = { store.duplicateRoutine(r, copy.format(r.name)) }) { Icon(Icons.Filled.ContentCopy, stringResource(R.string.duplicate)) }
                }
            }
        }
    }
}

/** One set of a finished workout, as a line. */
@Composable
private fun setLine(s: WorkoutSet, number: Int, kind: ExerciseKind): String = when (kind) {
    ExerciseKind.WEIGHT -> stringResource(R.string.set_line, number, Gym.showKg(s.grams), s.reps)
    ExerciseKind.TIME -> stringResource(R.string.set_time, number, clock(s.seconds.toLong()))
    ExerciseKind.CARDIO -> stringResource(R.string.set_cardio, number, clock(s.seconds.toLong()), Gym.showKm(s.meters))
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
private fun PastWorkoutScreen(store: GymStore, id: Long, exercises: List<Exercise>, version: Int, back: () -> Unit, open: (Long) -> Unit, openRoutine: (Long) -> Unit) {
    val context = LocalContext.current
    val workout = remember(version) { store.workout(id) }
    if (workout == null) { LaunchedEffect(Unit) { back() }; return }
    val active = remember(version) { store.active() }
    var confirming by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        TopAppBar(title = { Text(workout.name) }, navigationIcon = {
            IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
        }, actions = {
            IconButton(onClick = { open(id) }, enabled = active == null) { Icon(Icons.Filled.Edit, stringResource(R.string.edit)) }
            IconButton(onClick = { confirming = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
        })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            workout.finished?.let { end -> Hint(stringResource(R.string.lasted, clock(end - workout.started)) + " · " + volumeText(Gym.volume(workout.sets))) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = active == null, onClick = {
                    val items = store.routineFrom(workout)
                    val routine = Routine(0, workout.name, items)
                    back()
                    open(store.start(workout.name, routine))
                }) { Text(stringResource(R.string.repeat)) }
                OutlinedButton(onClick = { openRoutine(store.saveRoutine(null, workout.name, store.routineFrom(workout))); back() }) {
                    Text(stringResource(R.string.save_as_routine))
                }
            }
            if (workout.note.isNotBlank()) Panel(stringResource(R.string.note)) { Text(workout.note, style = MaterialTheme.typography.bodyLarge) }
            for ((exerciseId, sets) in workout.sets.groupBy { it.exerciseId }) {
                val exercise = exercises.firstOrNull { it.id == exerciseId } ?: continue
                Panel(exercise.title(context)) {
                    sets.sortedBy { it.position }.forEachIndexed { i, s -> Text(setLine(s, i + 1, exercise.kind), style = MaterialTheme.typography.bodyLarge) }
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
    var muscle by remember { mutableStateOf<Muscle?>(null) }
    var adding by remember { mutableStateOf(false) }
    val shown = exercises.filter { muscle == null || it.muscle == muscle }.map { it to it.title(context) }
        .filter { it.second.contains(search.trim(), ignoreCase = true) }.sortedBy { it.second.lowercase() }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tab_exercises), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = { adding = true }) { Text(stringResource(R.string.new_exercise)) }
            }
            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text(stringResource(R.string.search)) })
            MuscleChips(muscle) { muscle = it }
        }
        items(shown, key = { it.first.id }) { (e, name) ->
            Column(Modifier.fillMaxWidth().clickable { open(e.id) }.padding(vertical = 10.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Hint(stringResource(muscleName(e.muscle)))
            }
        }
    }
    if (adding) NewExerciseDialog({ adding = false }) { name, m, kind -> store.addExercise(name, m, kind); adding = false }
}

@Composable
private fun MuscleChips(selected: Muscle?, pick: (Muscle?) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected == null, { pick(null) }, { Text(stringResource(R.string.all)) })
        for (m in Muscle.entries) FilterChip(selected == m, { pick(m) }, { Text(stringResource(muscleName(m))) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewExerciseDialog(close: () -> Unit, save: (String, Muscle, ExerciseKind) -> Unit) {
    var name by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf(Muscle.OTHER) }
    var open by remember { mutableStateOf(false) }
    var kind by remember { mutableStateOf(ExerciseKind.WEIGHT) }
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
                Hint(stringResource(R.string.kind))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((k, label) in listOf(ExerciseKind.WEIGHT to R.string.kind_weight, ExerciseKind.TIME to R.string.kind_time, ExerciseKind.CARDIO to R.string.kind_cardio))
                        FilterChip(kind == k, { kind = k }, { Text(stringResource(label)) })
                }
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { save(name, muscle, kind) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.cancel)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseScreen(store: GymStore, exercise: Exercise, version: Int, back: () -> Unit) {
    val context = LocalContext.current
    val last = remember(version) { store.lastSets(exercise.id) }
    // Done with no weight at all, it goes by reps: "0 kg" would say nothing about a pull-up.
    val bodyweight = remember(version) { exercise.kind == ExerciseKind.WEIGHT && store.progress(exercise.id).all { it.second == 0 } }
    val progress = remember(version) { store.progress(exercise.id) { Gym.score(it, exercise.kind, bodyweight) } }
    val best = progress.maxOfOrNull { it.second }?.takeIf { it > 0 }
    val distance = remember(version) { exercise.kind == ExerciseKind.CARDIO && store.progress(exercise.id) { it.meters }.any { it.second > 0 } }
    fun shown(value: Int) = when {
        bodyweight -> value.toString()
        exercise.kind == ExerciseKind.WEIGHT -> "${Gym.showEstimate(value)} kg"
        exercise.kind == ExerciseKind.TIME || !distance -> clock(value.toLong())
        else -> "${Gym.showKm(value)} km"
    }
    var inUse by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        TopAppBar(title = { Text(exercise.title(context)) }, navigationIcon = {
            IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
        }, actions = {
            if (exercise.key == null) IconButton(onClick = { if (store.deleteExercise(exercise.id)) back() else inUse = true }) {
                Icon(Icons.Filled.Delete, stringResource(R.string.delete_exercise))
            }
        })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (inUse) Hint(stringResource(R.string.exercise_in_use))
            val title = when {
                bodyweight -> R.string.best_reps
                exercise.kind == ExerciseKind.WEIGHT -> R.string.best_estimate
                exercise.kind == ExerciseKind.TIME || !distance -> R.string.best_time
                else -> R.string.best_distance
            }
            Panel(stringResource(title)) {
                if (best == null) Hint(stringResource(R.string.no_sets_yet)) else {
                    Text(shown(best), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    if (exercise.kind == ExerciseKind.WEIGHT && !bodyweight) {
                        Hint(stringResource(R.string.best_estimate_hint))
                        if (best > 20000) {
                            val (plates, left) = Gym.platesPerSide(best)
                            Hint(stringResource(R.string.plates_per_side, plates.joinToString(" + ") { Gym.showKg(it) }.ifEmpty { "—" }) +
                                if (left > 0) " (+${Gym.showKg(left)})" else "")
                        }
                    }
                }
            }
            if (progress.size >= 2) Panel(stringResource(R.string.progress)) {
                LineChart(progress)
                Hint("${shown(progress.minOf { it.second })} – ${shown(progress.maxOf { it.second })}")
            }
            if (last.isNotEmpty()) Panel(stringResource(R.string.last_time)) {
                last.sortedBy { it.position }.forEachIndexed { i, s -> Text(setLine(s, i + 1, exercise.kind), style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }
}

// ---- routines ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineScreen(store: GymStore, existing: Routine?, exercises: List<Exercise>, close: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    val items = remember { mutableStateListOf<RoutineItem>().apply { addAll(existing?.items.orEmpty()) } }
    var picking by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
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
                val target = when (exercise?.kind) {
                    ExerciseKind.TIME -> R.string.seconds
                    ExerciseKind.CARDIO -> R.string.minutes
                    else -> R.string.reps
                }
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(exercise?.title(context).orEmpty(), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { items.add(i - 1, items.removeAt(i)) }, enabled = i > 0) { Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.move_up)) }
                        IconButton(onClick = { items.add(i + 1, items.removeAt(i)) }, enabled = i < items.lastIndex) { Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.move_down)) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberBox(item.sets, stringResource(R.string.sets), Modifier.weight(1f)) { items[i] = items[i].copy(sets = it) }
                        NumberBox(item.reps, stringResource(target), Modifier.weight(1f)) { items[i] = items[i].copy(reps = it) }
                        IconButton(onClick = { items.removeAt(i) }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                    }
                }
            }
            FilledTonalButton(onClick = { picking = true }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_exercise)) }
        }
    }
    if (picking) ExercisePicker(store, exercises, { picking = false }) { id ->
        items += RoutineItem(id, 3, when (store.exercises().firstOrNull { it.id == id }?.kind) {
            ExerciseKind.TIME -> 60
            ExerciseKind.CARDIO -> 30
            else -> 8
        })
        picking = false
    }
}

@Composable
private fun NumberBox(value: Int, label: String, modifier: Modifier, change: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(text, { t -> text = t.filter(Char::isDigit).take(3); text.toIntOrNull()?.takeIf { it > 0 }?.let(change) },
        modifier, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
}

/** Find an exercise by name or muscle, or make it if it isn't there: no leaving the workout for that. */
@Composable
fun ExercisePicker(store: GymStore, exercises: List<Exercise>, close: () -> Unit, pick: (Long) -> Unit) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<Muscle?>(null) }
    var adding by remember { mutableStateOf(false) }
    val shown = exercises.filter { muscle == null || it.muscle == muscle }.map { it to it.title(context) }
        .filter { it.second.contains(search.trim(), ignoreCase = true) }.sortedBy { it.second.lowercase() }
    AlertDialog(onDismissRequest = close, title = { Text(stringResource(R.string.add_exercise)) },
        text = {
            Column {
                OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text(stringResource(R.string.search)) })
                MuscleChips(muscle) { muscle = it }
                LazyColumn(Modifier.height(300.dp)) {
                    items(shown, key = { it.first.id }) { (e, name) ->
                        Text(name, Modifier.fillMaxWidth().clickable { pick(e.id) }.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { adding = true }) { Text(stringResource(R.string.new_exercise)) } },
        dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.cancel)) } })
    if (adding) NewExerciseDialog({ adding = false }) { name, m, kind -> adding = false; pick(store.addExercise(name, m, kind)) }
}
