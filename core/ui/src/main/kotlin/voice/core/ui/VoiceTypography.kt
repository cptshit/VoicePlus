package voice.core.ui

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun variable(
  resource: Int,
  weight: FontWeight,
) = Font(resource, weight = weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

/** Page and episode titles. */
val Literata = FontFamily(
  variable(R.font.literata_variable, FontWeight.Normal),
  variable(R.font.literata_variable, FontWeight.Medium),
  variable(R.font.literata_variable, FontWeight.SemiBold),
)

/** Running text and buttons. */
val PlexSans = FontFamily(
  variable(R.font.ibm_plex_sans_variable, FontWeight.Normal),
  variable(R.font.ibm_plex_sans_variable, FontWeight.Medium),
  variable(R.font.ibm_plex_sans_variable, FontWeight.SemiBold),
)

/** Episode numbers and section headings. */
val BarlowCondensed = FontFamily(
  Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
  Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

/** Text styles Material 3 has no slot for. */
@Immutable
data class VoiceExtraTextStyles(
  /** Big episode number in a list row, with "FOLGE" above it in [episodeLabel]. */
  val episodeNumber: TextStyle,
  val episodeLabel: TextStyle,
  /** Section heading in capitals with wide letter spacing, e.g. "ALS NÄCHSTES". Pass the text in uppercase. */
  val sectionHeader: TextStyle,
)

internal val DefaultExtraTextStyles = VoiceExtraTextStyles(
  episodeNumber = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 34.sp),
  episodeLabel = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.2.sp),
  sectionHeader = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 1.6.sp,
  ),
)

internal fun voiceTypography(): Typography {
  val base = Typography()
  fun TextStyle.serif() = copy(fontFamily = Literata, fontWeight = FontWeight.SemiBold)
  fun TextStyle.sans() = copy(fontFamily = PlexSans)
  return Typography(
    displayLarge = base.displayLarge.serif(),
    displayMedium = base.displayMedium.serif(),
    displaySmall = base.displaySmall.serif(),
    headlineLarge = base.headlineLarge.serif(),
    headlineMedium = base.headlineMedium.serif(),
    headlineSmall = base.headlineSmall.serif(),
    titleLarge = base.titleLarge.serif(),
    titleMedium = base.titleMedium.copy(fontFamily = Literata, fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.sans(),
    bodyLarge = base.bodyLarge.sans(),
    bodyMedium = base.bodyMedium.sans(),
    bodySmall = base.bodySmall.sans(),
    labelLarge = base.labelLarge.sans(),
    labelMedium = base.labelMedium.sans(),
    labelSmall = base.labelSmall.sans(),
  )
}
