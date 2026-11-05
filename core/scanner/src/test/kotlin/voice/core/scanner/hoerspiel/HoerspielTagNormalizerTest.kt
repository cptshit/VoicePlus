package voice.core.scanner.hoerspiel

import io.kotest.matchers.collections.shouldBeEmpty
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
}
