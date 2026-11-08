package voice.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalExtraColors = staticCompositionLocalOf { LightExtraColors }

private val LocalExtraTextStyles = staticCompositionLocalOf { DefaultExtraTextStyles }

/** Colors outside Material's scheme, e.g. the beige note box: `VoiceTheme.extraColors`. */
object VoiceTheme {
  val extraColors: VoiceExtraColors
    @Composable @ReadOnlyComposable
    get() = LocalExtraColors.current
  val extraTextStyles: VoiceExtraTextStyles
    @Composable @ReadOnlyComposable
    get() = LocalExtraTextStyles.current
}

/** @param dark forces the dark or light palette; `null` follows the app setting or the system. */
@Composable
fun VoiceTheme(
  dark: Boolean? = null,
  content: @Composable () -> Unit,
) {
  val isDark = dark ?: isDarkTheme()
  CompositionLocalProvider(
    LocalExtraColors provides if (isDark) DarkExtraColors else LightExtraColors,
    LocalExtraTextStyles provides DefaultExtraTextStyles,
  ) {
    MaterialTheme(
      colorScheme = if (isDark) NightColorScheme else PaperColorScheme,
      typography = voiceTypography(),
      content = content,
    )
  }
}
