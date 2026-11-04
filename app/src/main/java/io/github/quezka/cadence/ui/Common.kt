package io.github.quezka.cadence.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.quezka.cadence.R
import io.github.quezka.cadence.domain.Exercise
import io.github.quezka.cadence.domain.Muscle

@StringRes
private fun nameRes(key: String): Int? = when (key) {
    "bench_press" -> R.string.ex_bench_press
    "incline_press" -> R.string.ex_incline_press
    "dumbbell_press" -> R.string.ex_dumbbell_press
    "chest_fly" -> R.string.ex_chest_fly
    "push_up" -> R.string.ex_push_up
    "dip" -> R.string.ex_dip
    "deadlift" -> R.string.ex_deadlift
    "pull_up" -> R.string.ex_pull_up
    "lat_pulldown" -> R.string.ex_lat_pulldown
    "barbell_row" -> R.string.ex_barbell_row
    "cable_row" -> R.string.ex_cable_row
    "squat" -> R.string.ex_squat
    "leg_press" -> R.string.ex_leg_press
    "lunge" -> R.string.ex_lunge
    "romanian_deadlift" -> R.string.ex_romanian_deadlift
    "leg_curl" -> R.string.ex_leg_curl
    "leg_extension" -> R.string.ex_leg_extension
    "calf_raise" -> R.string.ex_calf_raise
    "overhead_press" -> R.string.ex_overhead_press
    "lateral_raise" -> R.string.ex_lateral_raise
    "face_pull" -> R.string.ex_face_pull
    "barbell_curl" -> R.string.ex_barbell_curl
    "hammer_curl" -> R.string.ex_hammer_curl
    "triceps_pushdown" -> R.string.ex_triceps_pushdown
    "skull_crusher" -> R.string.ex_skull_crusher
    "plank" -> R.string.ex_plank
    "crunch" -> R.string.ex_crunch
    "leg_raise" -> R.string.ex_leg_raise
    "running" -> R.string.ex_running
    "cycling" -> R.string.ex_cycling
    "rowing" -> R.string.ex_rowing
    else -> null
}

fun Exercise.title(context: Context): String = key?.let(::nameRes)?.let(context::getString) ?: name

@StringRes
fun muscleName(m: Muscle): Int = when (m) {
    Muscle.CHEST -> R.string.muscle_chest
    Muscle.BACK -> R.string.muscle_back
    Muscle.LEGS -> R.string.muscle_legs
    Muscle.SHOULDERS -> R.string.muscle_shoulders
    Muscle.ARMS -> R.string.muscle_arms
    Muscle.CORE -> R.string.muscle_core
    Muscle.CARDIO -> R.string.muscle_cardio
    Muscle.OTHER -> R.string.muscle_other
}

@Composable
fun Panel(title: String? = null, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun Hint(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

fun clock(seconds: Long): String = "%d:%02d".format(seconds / 60, seconds % 60)
