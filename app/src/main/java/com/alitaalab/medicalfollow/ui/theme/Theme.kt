package com.alitaalab.medicalfollow.ui.theme

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
    primary = Color(0xFF00696E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF0F5),
    onPrimaryContainer = Color(0xFF002022),
    secondary = Color(0xFF4A6365),
    secondaryContainer = Color(0xFFCCE8E9),
    tertiary = Color(0xFF4F5F7D),
    tertiaryContainer = Color(0xFFD7E3FF),
    background = Color(0xFFF7FAFA),
    surface = Color(0xFFF7FAFA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D4D9),
    onPrimary = Color(0xFF003739),
    primaryContainer = Color(0xFF004F53),
    onPrimaryContainer = Color(0xFF9CF0F5),
    secondary = Color(0xFFB0CCCD),
    secondaryContainer = Color(0xFF324B4D),
    tertiary = Color(0xFFB7C7EA),
    tertiaryContainer = Color(0xFF374764),
)

/** Status colours used for vaccine chips. */
object StatusColors {
    val taken = Color(0xFF2E7D32)
    val overdue = Color(0xFFC62828)
    val dueSoon = Color(0xFFEF6C00)
    val upcoming = Color(0xFF546E7A)
}

@Composable
fun MedicalFollowTheme(content: @Composable () -> Unit) {
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
