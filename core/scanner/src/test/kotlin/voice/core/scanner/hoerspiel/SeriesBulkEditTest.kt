package voice.core.scanner.hoerspiel

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import voice.core.data.BookId

@RunWith(AndroidJUnit4::class)
class SeriesBulkEditTest {

  private fun draft(
    name: String,
    part: String? = null,
  ) = EpisodeDraft(BookId(Uri.parse("file:///$name")), name, part)

  private fun names(
    episodes: List<EpisodeDraft>,
    vararg rules: BulkRule,
  ) = previewBulkEdit(episodes, rules.toList()).map { it.newName }

  @Test
  fun stripPrefixRemovesSeriesNameAndSeparator() {
    names(
      listOf(draft("Gabriel Burns - Die Brut"), draft("gabriel burns: Der Atem"), draft("Die Fänge")),
      BulkRule.StripPrefix("Gabriel Burns"),
    ) shouldBe listOf("Die Brut", "Der Atem", "Die Fänge")
  }

  @Test
  fun replaceRespectsCase() {
    val episodes = listOf(draft("Fluesterer"), draft("fluesterer"))
    names(episodes, BulkRule.Replace("Fluesterer", "Flüsterer")) shouldBe listOf("Flüsterer", "fluesterer")
    names(episodes, BulkRule.Replace("fluesterer", "Flüsterer", ignoreCase = true)) shouldBe listOf("Flüsterer", "Flüsterer")
  }

  @Test
  fun emptyFindDoesNothing() {
    names(listOf(draft("Die Brut")), BulkRule.Replace("", "x")) shouldBe listOf("Die Brut")
  }

  @Test
  fun ruleNeverProducesEmptyTitle() {
    names(listOf(draft("Gabriel Burns")), BulkRule.StripPrefix("Gabriel Burns")) shouldBe listOf("Gabriel Burns")
    names(listOf(draft("R")), BulkRule.Replace("R", "")) shouldBe listOf("R")
  }

  @Test
  fun tidyWhitespaceCollapsesRuns() {
    names(listOf(draft("  Die,   die  nicht bluten ")), BulkRule.TidyWhitespace) shouldBe listOf("Die, die nicht bluten")
  }

  @Test
  fun renumberFollowsCurrentOrder() {
    previewBulkEdit(listOf(draft("A", "7"), draft("B"), draft("C", "3")), listOf(BulkRule.Renumber(start = 10)))
      .map { it.newPart } shouldBe listOf("10", "11", "12")
  }

  @Test
  fun rulesRunInOrder() {
    names(
      listOf(draft("Gabriel Burns -  Die   Brut")),
      BulkRule.StripPrefix("Gabriel Burns"),
      BulkRule.TidyWhitespace,
    ) shouldBe listOf("Die Brut")
  }

  @Test
  fun changedIsFalseWhenNothingMatches() {
    val result = previewBulkEdit(listOf(draft("Die Brut", "2")), listOf(BulkRule.Replace("xyz", "abc")))
    result.single().changed shouldBe false
  }
}
