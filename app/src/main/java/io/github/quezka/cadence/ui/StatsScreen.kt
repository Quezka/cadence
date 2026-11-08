package io.github.quezka.cadence.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.quezka.cadence.R
import io.github.quezka.cadence.data.GymStore
import io.github.quezka.cadence.domain.Exercise
import io.github.quezka.cadence.domain.Gym
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsTab(store: GymStore, exercises: List<Exercise>, version: Int) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val history = remember(version) { store.history(100_000) }
    val muscles = exercises.associate { it.id to it.muscle }
    val names = exercises.associateBy { it.id }
    val volume = Gym.weeklyVolume(history, today)
    val byMuscle = Gym.setsByMuscle(history, muscles::get, Gym.weekOf(today))
    val bests = Gym.bests(history).take(8)
    GlassScreen(bar = {
TopAppBar(title = { Text(stringResource(R.string.tab_stats)) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = ClearBar, scrolledContainerColor = ClearBar))
}) { glass ->
    LazyColumn(contentPadding = glass.plus(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (history.isNotEmpty()) item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(stringResource(R.string.stat_volume), "${volume.last().second / 1000} kg", Modifier.weight(1f))
                StatTile(stringResource(R.string.stat_sets), byMuscle.sumOf { it.second }.toString(), Modifier.weight(1f))
            }
        }
        item { BodyPanel(store, version) }
        if (history.isEmpty()) item { Hint(stringResource(R.string.stats_empty)) } else {
            item {
                Panel(stringResource(R.string.volume_weeks)) {
                    WeekBars(volume)
                }
            }
            if (byMuscle.isNotEmpty()) item {
                Panel(stringResource(R.string.muscles_week)) {
                    val top = byMuscle.first().second.coerceAtLeast(1)
                    for ((muscle, sets) in byMuscle) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(muscleName(muscle)), Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium)
                        Box(Modifier.weight(1f).height(12.dp)) {
                            Box(Modifier.fillMaxWidth(sets.toFloat() / top).fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)))
                        }
                        Text(sets.toString(), Modifier.width(32.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (bests.isNotEmpty()) item {
                Panel(stringResource(R.string.bests)) {
                    for ((id, best) in bests) Row {
                        Text(names[id]?.title(context).orEmpty(), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text("${Gym.showEstimate(best)} kg", fontWeight = FontWeight.Bold)
                    }
                    Hint(stringResource(R.string.best_estimate_hint))
                }
            }
        }
    }
    }
}

@Composable
private fun WeekBars(weeks: List<Pair<LocalDate, Long>>) {
    val color = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val top = weeks.maxOf { it.second }.coerceAtLeast(1)
    Column {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val slot = size.width / weeks.size
            weeks.forEachIndexed { i, (_, kg) ->
                val h = (size.height * kg / top).coerceAtLeast(6f)
                drawRoundRect(if (kg > 0) color else empty, Offset(i * slot + slot * 0.18f, size.height - h),
                    Size(slot * 0.64f, h), CornerRadius(10f))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            for ((week, _) in weeks) Text(week.dayOfMonth.toString(), Modifier.weight(1f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BodyPanel(store: GymStore, version: Int) {
    val log = remember(version) { store.bodyLog() }
    val format = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    var text by remember { mutableStateOf("") }
    val grams = Gym.parseKg(text)?.takeIf { it > 0 }
    Panel(stringResource(R.string.body)) {
        log.lastOrNull()?.let { (_, now) ->
            Text("${Gym.showKg(now)} kg", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (log.size >= 2) {
                val change = now - log.first().second
                val shown = (if (change > 0) "+" else if (change < 0) "−" else "") + Gym.showKg(kotlin.math.abs(change))
                Hint(stringResource(R.string.body_change, shown, log.first().first.format(format)))
                LineChart(log)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(text, { text = it }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.body_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            FilledTonalButton(onClick = { store.logBody(LocalDate.now(), grams!!); text = "" }, enabled = grams != null) {
                Text(stringResource(R.string.add))
            }
        }
    }
}

@Composable
fun LineChart(points: List<Pair<LocalDate, Int>>) {
    val color = MaterialTheme.colorScheme.primary
    val low = points.minOf { it.second }
    val high = points.maxOf { it.second }
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val span = (high - low).coerceAtLeast(1)
        val at = points.mapIndexed { i, (_, v) ->
            Offset(size.width * i / (points.size - 1), size.height - 8f - (size.height - 16f) * (v - low) / span)
        }
        at.zipWithNext { a, b -> drawLine(color, a, b, strokeWidth = 6f, cap = StrokeCap.Round) }
        at.forEach { drawCircle(color, 8f, it) }
    }
}

// ---- settings ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(store: GymStore, version: Int, back: () -> Unit) {
    val context = LocalContext.current
    BackHandler(onBack = back)
    val rest = remember(version) { store.restSeconds() }
    var pending by remember { mutableStateOf<android.net.Uri?>(null) }
    val saved = stringResource(R.string.backup_saved)
    val bad = stringResource(R.string.restore_bad)
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) context.contentResolver.openOutputStream(uri)?.use {
            store.export(it)
            Toast.makeText(context, saved, Toast.LENGTH_SHORT).show()
        }
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { pending = it }
    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
        TopAppBar(title = { Text(stringResource(R.string.settings)) }, navigationIcon = {
            IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
        })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(stringResource(R.string.rest_length)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (s in listOf(60, 90, 120, 180, 300)) FilterChip(rest == s, { store.setRestSeconds(s) }, { Text(clock(s.toLong())) })
                }
            }
            Panel(stringResource(R.string.backup)) {
                Hint(stringResource(R.string.backup_hint))
                Button(onClick = { save.launch("cadence-${LocalDate.now()}.db") }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.backup_save)) }
                OutlinedButton(onClick = { pick.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.backup_restore)) }
            }
        }
    }
    pending?.let { uri ->
        AlertDialog(onDismissRequest = { pending = null }, title = { Text(stringResource(R.string.restore_title)) },
            text = { Text(stringResource(R.string.restore_text)) },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    val done = context.contentResolver.openInputStream(uri)?.use { store.restore(context, it) } ?: false
                    if (done) {
                        val again = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        context.startActivity(Intent.makeRestartActivityTask(again!!.component))
                        Runtime.getRuntime().exit(0)  // the database was swapped under the running app: start clean
                    } else Toast.makeText(context, bad, Toast.LENGTH_LONG).show()
                }) { Text(stringResource(R.string.restore_go)) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.cancel)) } })
    }
}
