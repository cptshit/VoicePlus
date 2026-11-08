package voice.features.tagReview.lookup

import android.net.Uri
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import voice.core.data.BookId
import voice.core.data.hoerspiel.TagFindingField
import voice.core.data.hoerspiel.TagFindingKind
import voice.core.data.hoerspiel.TagFindingSource

@RunWith(RobolectricTestRunner::class)
class SuggestionsTest {

  private fun local(
    name: String,
    number: Int?,
  ) = LocalEpisode(BookId(Uri.parse("file:///GB/$name")), name, number)

  private val catalog = listOf(
    CatalogEpisode(1, "Der Flüsterer"),
    CatalogEpisode(25, "dem Winter folgte der Herbst"),
    CatalogEpisode(26, "R."),
    CatalogEpisode(34, "Ich weiss, was Angst ist"),
    CatalogEpisode(13, "Die Kommission"),
  )

  @Test
  fun suggestsTheCatalogueSpellingWhereLettersDiffer() {
    val findings = suggestFromCatalog(listOf(local("Der Fluesterer", 1), local("Die Komission", 13)), catalog, CatalogSource.MusicBrainz)

    findings.map { Triple(it.chosenValue, it.otherValue, it.kind) } shouldBe listOf(
      Triple("Der Fluesterer", "Der Flüsterer", TagFindingKind.Formatting),
      Triple("Die Komission", "Die Kommission", TagFindingKind.Different),
    )
    findings.first().let {
      it.field shouldBe TagFindingField.EpisodeTitle
      it.chosenSource shouldBe TagFindingSource.Local
      it.otherSource shouldBe TagFindingSource.MusicBrainz
    }
  }

  @Test
  fun caseAndPunctuationAloneAreNoSuggestion() {
    suggestFromCatalog(
      listOf(local("Dem Winter folgte der Herbst", 25), local("R", 26), local("Ich weiss was Angst ist", 34)),
      catalog,
      CatalogSource.MusicBrainz,
    ) shouldBe emptyList()
  }

  @Test
  fun anEpisodeTheCatalogueDoesNotListIsLeftAlone() {
    suggestFromCatalog(listOf(local("Etwas Neues", 99)), catalog, CatalogSource.MusicBrainz) shouldBe emptyList()
  }

  @Test
  fun aNumberListedTwiceIsAmbiguousAndSkipped() {
    suggestFromCatalog(listOf(local("A", 3)), listOf(CatalogEpisode(3, "Eins"), CatalogEpisode(3, "Zwei")), CatalogSource.ITunes) shouldBe
      emptyList()
  }

  @Test
  fun aBookWithoutNumberGetsOneOnlyFromAnExactTitle() {
    val findings = suggestFromCatalog(listOf(local("Die Kommission", null), local("Unbekannt", null)), catalog, CatalogSource.ITunes)

    findings.map { it.field to it.otherValue } shouldBe listOf(TagFindingField.EpisodeNumber to "13")
    findings.single().otherSource shouldBe TagFindingSource.ITunes
  }
}
