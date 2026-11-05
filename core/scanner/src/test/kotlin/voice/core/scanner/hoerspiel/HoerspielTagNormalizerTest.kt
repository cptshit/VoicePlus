package voice.core.scanner.hoerspiel

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assume.assumeTrue
import org.junit.Test
import voice.core.data.hoerspiel.TagFindingField
import java.io.File

class HoerspielTagNormalizerTest {

  private fun resource(name: String): String = javaClass.classLoader!!.getResource(name)!!.readText()

  /**
   * One synthetic episode per pattern found in the real Gabriel Burns dump. Expected values live
   * next to the fixture so they can be reviewed as data.
   */
  @Test
  fun syntheticFixtures() {
    val sources = TagDump.parse(resource("hoerspiel/synthetic.jsonl")).associateBy { it.folderName }
    val expected = resource("hoerspiel/synthetic.expected.jsonl").lines()
      .filter { it.isNotBlank() }
      .map { Json.parseToJsonElement(it).jsonObject }

    expected.map { it.folder() } shouldContainExactly sources.keys.toList()
    expected.forEach { expectation ->
      val actual = HoerspielTagNormalizer.normalize(sources.getValue(expectation.folder()))
      actual.toJson(expectation.folder()) shouldBe expectation
    }
  }

  /**
   * Runs over the private dumps in fixtures/tags (not committed, see .gitignore) when present.
   * Every folder there is an episode, so every one must be recognized.
   */
  @Test
  fun localTagDumps() {
    val dumps = File("../../fixtures/tags").listFiles { file -> file.extension == "jsonl" }.orEmpty()
    assumeTrue("no local tag dumps", dumps.isNotEmpty())
    dumps.forEach { dump ->
      val results = TagDump.parse(dump.readText()).map { it to HoerspielTagNormalizer.normalize(it) }
      println("== ${dump.name}")
      results.sortedBy { it.second?.episodeNumber }.forEach { (source, episode) ->
        println(
          if (episode == null) {
            "  NOT RECOGNIZED  ${source.folderName}"
          } else {
            "  ${episode.series.value} #${episode.episodeNumber} ${episode.episodeTitle?.value}" +
              episode.findings.joinToString("") { "\n      ${it.field} ${it.kind}: ${it.other.value} (${it.other.source})" }
          },
        )
      }
      results.filter { it.second == null }.map { it.first.folderName }.shouldBeEmpty()
    }
  }

  /**
   * Henrik's decisions for the Gabriel Burns episodes whose sources disagree on the title,
   * checked against MusicBrainz (release group titles "Gabriel Burns, Episode N: Title").
   *
   * The normalizer keeps the folder name and records the other spelling as a finding; the
   * decided title is then set by hand via "Edit book". So for every decision the normalizer
   * must either pick it or offer it in a finding (a trailing " [Teil N]" is never wanted).
   */
  @Test
  fun localGabrielBurnsTitleDecisions() {
    val dump = File("../../fixtures/tags/gabriel-burns.jsonl")
    assumeTrue("no local Gabriel Burns dump", dump.exists())
    val episodes = TagDump.parse(dump.readText())
      .mapNotNull(HoerspielTagNormalizer::normalize)
      .associateBy { it.episodeNumber }

    data class Decision(
      val chosenByNormalizer: String,
      val decided: String,
    )
    val decisions = mapOf(
      10 to Decision("Diesseits der Kuppeln", decided = "Diesseits der Kuppeln"),
      11 to Decision("Welt der Dämmerung", decided = "Welt der Dämmerung"),
      12 to Decision("Die erste Erinnerung", decided = "Die erste Erinnerung"),
      13 to Decision("Die Komission", decided = "Die Kommission"),
      15 to Decision("Ohne Bewusstsein Teil 1", decided = "Ohne Bewusstsein Teil 1"),
      16 to Decision("Infektiös Teil 2", decided = "Infektiös Teil 2"),
      24 to Decision("Der erste der zehn", decided = "Der erste der zehn"),
      25 to Decision("Dem Winter folgt der Herbst", decided = "Dem Winter folgte der Herbst"),
      34 to Decision("Ich weiss was Angst ist", decided = "Ich weiss, was Angst ist"),
    )
    decisions.forEach { (number, decision) ->
      val episode = episodes.getValue(number)
      episode.episodeTitle?.value shouldBe decision.chosenByNormalizer
      val offered = listOfNotNull(episode.episodeTitle) + episode.findings
        .filter { it.field == TagFindingField.EpisodeTitle }
        .map { it.other }
      offered.map { it.value.replace(TRAILING_PART, "") } shouldContain decision.decided
    }
  }

  private fun JsonObject.folder(): String = getValue("folder").jsonPrimitive.content

  private fun NormalizedEpisode?.toJson(folder: String): JsonObject = buildJsonObject {
    put("folder", folder)
    if (this@toJson == null) {
      put("normalized", JsonNull)
      return@buildJsonObject
    }
    put("series", series.value)
    put("seriesSource", series.source.name)
    put("number", episodeNumber)
    put("numberSource", episodeNumberSource.name)
    put("title", episodeTitle?.value)
    put("titleSource", episodeTitle?.source?.name)
    put(
      "findings",
      buildJsonArray {
        findings.forEach { finding ->
          add(
            buildJsonObject {
              put("field", finding.field.name)
              put("kind", finding.kind.name)
              put("other", finding.other.value)
              put("otherSource", finding.other.source.name)
            },
          )
        }
      },
    )
  }

  private companion object {
    val TRAILING_PART = Regex("""\s*\[Teil \d+]$""")
  }
}
