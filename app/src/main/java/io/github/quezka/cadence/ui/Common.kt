package io.github.quezka.cadence.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun Hint(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(top = 12.dp, bottom = 2.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}

/** A big number with a label, for summaries. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AnimatedContent(value, transitionSpec = {
                (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
            }, label = "number") {
                Text(it, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** The row of a list: a card with a stripe of colour, like the agenda in Quire. */
@Composable
fun ListCard(title: String, meta: String, stripe: Color, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    val source = remember { MutableInteractionSource() }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().pressScale(source).then(
            if (onClick != null) Modifier.clickable(source, null, onClick = onClick) else Modifier)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(40.dp).background(stripe, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            trailing()
        }
    }
}

/** Each muscle's colour, from the same family as the course colours in Quire. */
fun muscleColor(m: Muscle?): Color = Color(when (m) {
    Muscle.CHEST -> 0xFFE5484D
    Muscle.BACK -> 0xFF4466EE
    Muscle.LEGS -> 0xFF12A594
    Muscle.SHOULDERS -> 0xFFF5A524
    Muscle.ARMS -> 0xFF8E4EC6
    Muscle.CORE -> 0xFF0091FF
    Muscle.CARDIO -> 0xFF30A46C
    else -> 0xFF8D8D94
})

fun clock(seconds: Long): String = if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
    else "%d:%02d".format(seconds / 60, seconds % 60)
