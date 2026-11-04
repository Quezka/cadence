package io.github.quezka.cadence.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Light = lightColorScheme(primary = Color(0xFF4B4BC8), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDDCFB), onPrimaryContainer = Color(0xFF23236E),
    background = Color(0xFFF6F5FC), surface = Color(0xFFF6F5FC), onSurface = Color(0xFF0E0F13), onSurfaceVariant = Color(0xFF3F4651),
    surfaceContainerLow = Color(0xFFFFFFFF), error = Color(0xFFC62828), tertiary = Color(0xFF1B7F4F))

private val Dark = darkColorScheme(primary = Color(0xFF9A9BFF), onPrimary = Color(0xFF0E0F11),
    primaryContainer = Color(0xFF35366B), onPrimaryContainer = Color(0xFFE2E2FF),
    background = Color(0xFF111119), surface = Color(0xFF111119), onSurface = Color(0xFFF4F4F8), onSurfaceVariant = Color(0xFFC3C8D1),
    surfaceContainerLow = Color(0xFF1B1B25), error = Color(0xFFFF6369), tertiary = Color(0xFF4CC38A))

/** Material You: the wallpaper's colours where the phone offers them, the suite's indigo elsewhere. */
@Composable
fun CadenceTheme(dynamic: Boolean = true, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        dynamic && Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = colors, content = content)
}
