package io.github.quezka.cadence.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

import io.github.quezka.cadence.R

private val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal), Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold), Font(R.font.manrope_bold, FontWeight.Bold),
)

/** Material's type scale in the suite's typeface. */
private val SuiteType = SuiteTokens.typography(Manrope)

/** The suite's palette, the same as the desktop's (quezka_ui/theme.py), Quire's and Vesper's: Cadence looks like them, not like the wallpaper. */
private val Light = SuiteTokens.Light

private val Dark = SuiteTokens.Dark

/** Content stays solid, as in Apple's glass design; only the bars over content are glass (`Glass.kt`). */
object Glass {
    @Composable
    fun surface(): Color = MaterialTheme.colorScheme.surfaceContainerLow

    @Composable
    fun edge(): androidx.compose.foundation.BorderStroke? = null
}

@Composable
fun CadenceTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, typography = SuiteType) {
        // Every screen stands on this: without it they show the window behind, which is white even in the dark theme.
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, content = content)
    }
}
