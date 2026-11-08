package voice.core.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Palette of the Hörspiel design: warm paper in light mode, near-black with an orange accent in dark
 * mode. Every text/background pair used by the app meets a contrast of 4.5:1, see `VoiceColorsTest`.
 */
internal object Paper {
  val background = Color(0xFFF4ECE0)
  val surface = Color(0xFFFBF7F0)
  val line = Color(0xFFE2D6C4)
  val text = Color(0xFF2A211B)
  val textMuted = Color(0xFF6B5D50)
  val accent = Color(0xFF9B2D35)
  val onAccent = Color(0xFFFFFFFF)
  val chip = Color(0xFFE8DCCB)
  val noteBackground = Color(0xFFF6E6C3)
  val noteBorder = Color(0xFFB7791F)
  val noteText = Color(0xFF6E470C)
  val success = Color(0xFF2F5A36)
  val successBackground = Color(0xFFDCE8D5)
  val error = Color(0xFFA3262B)
  val onError = Color(0xFFFFFFFF)
}

internal object Night {
  val background = Color(0xFF141311)
  val surface = Color(0xFF1F1D1A)
  val line = Color(0xFF2D2A26)
  val text = Color(0xFFF3EFE8)
  val textMuted = Color(0xFFA9A298)
  val accent = Color(0xFFFF6A3D)
  val onAccent = Color(0xFF1A0E08)
  val chip = Color(0xFF34302B)

  // Not part of the design sketch: derived so the note and success boxes also work in dark mode.
  val noteBackground = Color(0xFF3A2E17)
  val noteBorder = Color(0xFFE0A84A)
  val noteText = Color(0xFFF6E6C3)
  val success = Color(0xFF9BC79F)
  val successBackground = Color(0xFF1F3324)
  val error = Color(0xFFFFB4AB)
  val onError = Color(0xFF3A0A08)
}

/** Colors Material 3 has no slot for. */
@Immutable
data class VoiceExtraColors(
  val noteBackground: Color,
  val noteBorder: Color,
  val noteText: Color,
  val success: Color,
  val successBackground: Color,
)

internal val LightExtraColors = VoiceExtraColors(
  Paper.noteBackground,
  Paper.noteBorder,
  Paper.noteText,
  Paper.success,
  Paper.successBackground,
)

internal val DarkExtraColors = VoiceExtraColors(
  Night.noteBackground,
  Night.noteBorder,
  Night.noteText,
  Night.success,
  Night.successBackground,
)

internal val PaperColorScheme: ColorScheme = lightColorScheme(
  primary = Paper.accent,
  onPrimary = Paper.onAccent,
  primaryContainer = Paper.chip,
  onPrimaryContainer = Paper.text,
  secondary = Paper.accent,
  onSecondary = Paper.onAccent,
  secondaryContainer = Paper.chip,
  onSecondaryContainer = Paper.text,
  tertiary = Paper.noteBorder,
  onTertiary = Paper.onAccent,
  tertiaryContainer = Paper.noteBackground,
  onTertiaryContainer = Paper.noteText,
  background = Paper.background,
  onBackground = Paper.text,
  surface = Paper.background,
  onSurface = Paper.text,
  surfaceVariant = Paper.chip,
  onSurfaceVariant = Paper.textMuted,
  surfaceContainerLowest = Paper.surface,
  surfaceContainerLow = Paper.surface,
  surfaceContainer = Color(0xFFF0E6D6),
  surfaceContainerHigh = Paper.chip,
  surfaceContainerHighest = Paper.line,
  outline = Paper.textMuted,
  outlineVariant = Paper.line,
  error = Paper.error,
  onError = Paper.onError,
)

internal val NightColorScheme: ColorScheme = darkColorScheme(
  primary = Night.accent,
  onPrimary = Night.onAccent,
  primaryContainer = Night.chip,
  onPrimaryContainer = Night.text,
  secondary = Night.accent,
  onSecondary = Night.onAccent,
  secondaryContainer = Night.chip,
  onSecondaryContainer = Night.text,
  tertiary = Night.noteBorder,
  onTertiary = Night.onAccent,
  tertiaryContainer = Night.noteBackground,
  onTertiaryContainer = Night.noteText,
  background = Night.background,
  onBackground = Night.text,
  surface = Night.background,
  onSurface = Night.text,
  surfaceVariant = Night.chip,
  onSurfaceVariant = Night.textMuted,
  surfaceContainerLowest = Night.background,
  surfaceContainerLow = Night.surface,
  surfaceContainer = Night.surface,
  surfaceContainerHigh = Night.chip,
  surfaceContainerHighest = Night.line,
  outline = Night.textMuted,
  outlineVariant = Night.line,
  error = Night.error,
  onError = Night.onError,
)
