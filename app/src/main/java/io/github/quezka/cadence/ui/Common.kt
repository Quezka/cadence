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
    "decline_press" -> R.string.ex_decline_press
    "cable_crossover" -> R.string.ex_cable_crossover
    "machine_chest_press" -> R.string.ex_machine_chest_press
    "chin_up" -> R.string.ex_chin_up
    "t_bar_row" -> R.string.ex_t_bar_row
    "dumbbell_row" -> R.string.ex_dumbbell_row
    "back_extension" -> R.string.ex_back_extension
    "shrug" -> R.string.ex_shrug
    "front_squat" -> R.string.ex_front_squat
    "hack_squat" -> R.string.ex_hack_squat
    "bulgarian_split_squat" -> R.string.ex_bulgarian_split_squat
    "hip_thrust" -> R.string.ex_hip_thrust
    "goblet_squat" -> R.string.ex_goblet_squat
    "dumbbell_shoulder_press" -> R.string.ex_dumbbell_shoulder_press
    "front_raise" -> R.string.ex_front_raise
    "rear_delt_fly" -> R.string.ex_rear_delt_fly
    "dumbbell_curl" -> R.string.ex_dumbbell_curl
    "preacher_curl" -> R.string.ex_preacher_curl
    "overhead_triceps" -> R.string.ex_overhead_triceps
    "close_grip_bench" -> R.string.ex_close_grip_bench
    "side_plank" -> R.string.ex_side_plank
    "russian_twist" -> R.string.ex_russian_twist
    "hanging_knee_raise" -> R.string.ex_hanging_knee_raise
    "ab_wheel" -> R.string.ex_ab_wheel
    "elliptical" -> R.string.ex_elliptical
    "stair_climber" -> R.string.ex_stair_climber
    "walking" -> R.string.ex_walking
    "swimming" -> R.string.ex_swimming
    "jump_rope" -> R.string.ex_jump_rope
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

fun clock(seconds: Long): String = if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
    else "%d:%02d".format(seconds / 60, seconds % 60)
