package voice.features.tagReview.lookup

import io.kotest.matchers.shouldBe
import org.junit.Test
import java.io.File

class CatalogParsingTest {

  private val musicBrainz = File("../../fixtures/musicbrainz/gabriel-burns-release-groups.json").readText()

  @Test
  fun readsEveryEpisodeOfTheRealMusicBrainzAnswerAndDropsTheSoundtrack() {
    val episodes = parseMusicBrainz(musicBrainz, "Gabriel Burns")

    episodes.size shouldBe 45
    episodes.first() shouldBe CatalogEpisode(0, "Die grauen Engel")
    episodes.first { it.number == 1 }.title shouldBe "Der Flüsterer"
    episodes.first { it.number == 25 }.title shouldBe "dem Winter folgte der Herbst"
    episodes.first { it.number == 26 }.title shouldBe "R."
    // MusicBrainz lists "Episode 40" only as a soundtrack, which must not count.
    episodes.map { it.number } shouldBe ((0..45) - 40)
  }

  @Test
  fun artistMustMatchTheSeriesExactly() {
    parseMusicBrainz(musicBrainz, "Gabriel") shouldBe emptyList()
    parseMusicBrainz(musicBrainz, "gabriel burns").size shouldBe 45
  }

  @Test
  fun garbageGivesNothing() {
    parseMusicBrainz("not json", "X") shouldBe emptyList()
    parseMusicBrainz("""{"error":"busy"}""", "X") shouldBe emptyList()
  }

  private fun itunes(vararg names: Pair<String, String>) =
    names.joinToString(prefix = """{"resultCount":${names.size},"results":[""", postfix = "]}") { (artist, name) ->
      """{"wrapperType":"audiobook","artistName":"$artist","collectionName":"$name"}"""
    }

  @Test
  fun readsSeriesAndNumberFromTheCommonITunesPatterns() {
    val body = itunes(
      "Gabriel Burns" to "Gabriel Burns, Band 12: Die erste Erinnerung (Ungekürzt)",
      "Gabriel Burns" to "Der Atem der Fahlen (Gabriel Burns 37)",
      "Gabriel Burns" to "Gabriel Burns - Folge 5: Nachtkathedrale - Jetzt reinhören",
      "Gabriel Burns" to "Gabriel Burns Tome 7: Die Fänge des Windes",
    )

    parseITunes(body, "Gabriel Burns", emptySet()) shouldBe listOf(
      CatalogEpisode(5, "Nachtkathedrale"),
      CatalogEpisode(7, "Die Fänge des Windes"),
      CatalogEpisode(12, "Die erste Erinnerung"),
      CatalogEpisode(37, "Der Atem der Fahlen"),
    )
  }

  @Test
  fun iTunesResultsOfOtherAuthorsOrSeriesAreIgnored() {
    val body = itunes(
      "Someone Else" to "Gabriel Burns, Band 1: Fremd",
      "Gabriel Burns" to "Anderes Hörspiel, Band 2: Falsche Serie",
      "Gabriel Burns" to "Gabriel Burns: Das große Hörbuch",
    )
    parseITunes(body, "Gabriel Burns", emptySet()) shouldBe emptyList()
    parseITunes(body, "Gabriel Burns", setOf("Someone Else")) shouldBe listOf(CatalogEpisode(1, "Fremd"))
  }

  @Test
  fun cleansAdvertisingAdditions() {
    cleanITunesTitle("Der Titel (Ungekürzt)") shouldBe "Der Titel"
    cleanITunesTitle("Der Titel [Hörspiel]") shouldBe "Der Titel"
    cleanITunesTitle("Der Titel - Jetzt reinhören") shouldBe "Der Titel"
  }
}
