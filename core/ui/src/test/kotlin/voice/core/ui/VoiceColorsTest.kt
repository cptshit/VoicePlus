package voice.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import org.junit.Test

class VoiceColorsTest {

  private fun contrast(
    a: Color,
    b: Color,
  ): Double {
    val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
    return (light + 0.05) / (dark + 0.05)
  }

  private fun check(
    name: String,
    text: Color,
    background: Color,
  ) {
    withClue(name) { contrast(text, background) shouldBeGreaterThanOrEqual 4.5 }
  }

  @Test
  fun paperPairsMeetContrastMinimum() {
    check("text on background", Paper.text, Paper.background)
    check("muted on background", Paper.textMuted, Paper.background)
    check("muted on surface", Paper.textMuted, Paper.surface)
    check("on accent", Paper.onAccent, Paper.accent)
    check("accent on background", Paper.accent, Paper.background)
    check("text on chip", Paper.text, Paper.chip)
    check("note", Paper.noteText, Paper.noteBackground)
    check("success", Paper.success, Paper.successBackground)
    check("error", Paper.error, Paper.background)
    check("on error", Paper.onError, Paper.error)
  }

  @Test
  fun nightPairsMeetContrastMinimum() {
    check("text on background", Night.text, Night.background)
    check("muted on background", Night.textMuted, Night.background)
    check("muted on surface", Night.textMuted, Night.surface)
    check("muted on chip", Night.textMuted, Night.chip)
    check("on accent", Night.onAccent, Night.accent)
    check("accent on background", Night.accent, Night.background)
    check("text on chip", Night.text, Night.chip)
    check("note", Night.noteText, Night.noteBackground)
    check("success", Night.success, Night.successBackground)
    check("error", Night.error, Night.background)
    check("on error", Night.onError, Night.error)
  }
}
