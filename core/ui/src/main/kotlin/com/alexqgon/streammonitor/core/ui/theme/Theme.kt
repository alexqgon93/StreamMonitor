package com.alexqgon.streammonitor.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object StreamSpacing {
    val Xs = 4.dp
    val Xxs = 6.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 20.dp
    val Xxl = 24.dp
    val Xxxl = 32.dp
}

private val StreamLightColorScheme = lightColorScheme(
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF18212B),
    onSurfaceVariant = Color(0xFF526172),
    outline = Color(0xFFCBD5DF),
    primary = Color(0xFF245B77),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE7F1F5),
    onPrimaryContainer = Color(0xFF245B77),
    error = Color(0xFFA12C32),
    errorContainer = Color(0xFFFCEBED),
    onErrorContainer = Color(0xFFA12C32),
)

@Composable
fun StreamMonitorTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalStreamColors provides StreamColors(
            success = Color(0xFF286147),
            successContainer = Color(0xFFE5F3EA),
            warning = Color(0xFF765319),
            warningContainer = Color(0xFFFFF2D7),
        ),
    ) {
        MaterialTheme(
            colorScheme = StreamLightColorScheme,
            typography = StreamTypography,
            content = content,
        )
    }
}
