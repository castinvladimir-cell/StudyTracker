package com.kuzmin.studytracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Цвета как у eBoox, только вместо фиолетового серый. */
object Palette {
    val Bg = Color(0xFF0A0A0B)
    val Card = Color(0xFF19191B)
    val CardBorder = Color(0xFF323236)
    val Surface2 = Color(0xFF242428)
    val Sheet = Color(0xFF161618)
    val Divider = Color(0xFF26262A)

    val Text = Color(0xFFF1F1F3)
    val TextDim = Color(0xFFB4B4BC)
    val TabIdle = Color(0xFFA9A9B1)
    val Sub = Color(0xFF8E8EA4)

    val Track = Color(0xFF38383E)
    val Fill = Color(0xFFA6A6B8)
    val OnFill = Color(0xFF141416)
    val Accent = Color(0xFF5C5C64)

    val Danger = Color(0xFFD9817F)
    val Warn = Color(0xFFD8B46A)
}

@Composable
fun TrackerTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Palette.Fill,
        onPrimary = Palette.OnFill,
        primaryContainer = Palette.Accent,
        onPrimaryContainer = Palette.Text,
        inversePrimary = Palette.Accent,
        secondary = Palette.Sub,
        onSecondary = Palette.OnFill,
        secondaryContainer = Color(0xFF34343A),
        onSecondaryContainer = Palette.Text,
        tertiary = Palette.Sub,
        onTertiary = Palette.OnFill,
        tertiaryContainer = Color(0xFF34343A),
        onTertiaryContainer = Palette.Text,
        background = Palette.Bg,
        onBackground = Palette.Text,
        surface = Palette.Card,
        onSurface = Palette.Text,
        surfaceVariant = Palette.Surface2,
        onSurfaceVariant = Palette.TextDim,
        surfaceTint = Color.Transparent,
        inverseSurface = Palette.Text,
        inverseOnSurface = Palette.Bg,
        error = Palette.Danger,
        onError = Palette.OnFill,
        outline = Palette.CardBorder,
        outlineVariant = Palette.Divider,
        scrim = Color.Black,
        surfaceBright = Color(0xFF2E2E33),
        surfaceDim = Palette.Bg,
        surfaceContainer = Color(0xFF1C1C20),
        surfaceContainerHigh = Color(0xFF222226),
        surfaceContainerHighest = Color(0xFF2A2A2F),
        surfaceContainerLow = Color(0xFF18181B),
        surfaceContainerLowest = Color(0xFF111113),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
