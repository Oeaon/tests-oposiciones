package com.testsoposiciones.app.ui.theme

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

private val LightColors = lightColorScheme(
    primary = Color(0xFF00695C),
    secondary = Color(0xFF5E5C71),
    tertiary = Color(0xFF7B5266),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80CBC4),
    secondary = Color(0xFFC8C4DC),
    tertiary = Color(0xFFEDB8CF),
)

@Composable
fun TestsTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}

/** Verde para acierto y rojo para fallo, legibles en claro y oscuro. */
object AnswerColors {
    val correct = Color(0xFF2E7D32)
    val wrong = Color(0xFFC62828)
}
