package voice.core.scanner.hoerspiel

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class ChapterNameNormalizerTest {

  private fun normalize(vararg names: String?) = ChapterNameNormalizer.normalize(names.toList()) { "Kapitel $it" }

  @Test
  fun genericNamesAreNumbered() {
    normalize("Track 01", "Track 02") shouldContainExactly listOf("Kapitel 1", "Kapitel 2")
    normalize("AudioTrack 01", "AudioTrack 02") shouldContainExactly listOf("Kapitel 1", "Kapitel 2")
    normalize("Kapitel 01", "Kapitel 02") shouldContainExactly listOf("Kapitel 1", "Kapitel 2")
    normalize("Angst-Kapitel 01", "Angst-Kapitel 02") shouldContainExactly listOf("Kapitel 1", "Kapitel 2")
  }

  @Test
  fun numbersInGenericNamesAreKept() {
    normalize("Chapter 1", "Chapter 2", "Chapter 20", "Chapter 30") shouldContainExactly
      listOf("Kapitel 1", "Kapitel 2", "Kapitel 20", "Kapitel 30")
  }

  @Test
  fun repeatedNumbersAreNumberedByPosition() {
    // every disc starts at "Track 01"
    normalize("Track 01", "Track 02", "Track 01", "Track 02") shouldContainExactly
      listOf("Kapitel 1", "Kapitel 2", "Kapitel 3", "Kapitel 4")
  }

  @Test
  fun hugeNumbersDoNotCrash() {
    normalize("Track 99999999999", "Track 1") shouldContainExactly listOf("Kapitel 1", "Kapitel 2")
  }

  @Test
  fun repeatedEpisodeTitleIsNumbered() {
    normalize("01 - Die Brut", "02 - Die Brut") shouldContainExactly listOf("Kapitel 1", "Kapitel 2")
    normalize("Am Ende aller Tage", "Am Ende aller Tage") shouldContainExactly listOf("Kapitel 1", "Kapitel 2")
    normalize("Gabriel Burns - 40", "Gabriel Burns - 40") shouldContainExactly listOf("Kapitel 1", "Kapitel 2")
    normalize("027 - Zwiespalt - 01", "027 - Zwiespalt - 02") shouldContainExactly listOf("Kapitel 1", "Kapitel 2")
  }

  @Test
  fun meaningfulNamesAreKept() {
    val names = listOf("Einleitung", "Prolog", "Kapitel 01", "Kapitel 04a", "Kapitel 04b", "Epilog")
    ChapterNameNormalizer.normalize(names) { "Kapitel $it" } shouldContainExactly names
  }

  @Test
  fun discSplitSuffixIsNotGeneric() {
    normalize("Kapitel 03", "Kapitel 04a", "Kapitel 04b") shouldContainExactly listOf("Kapitel 03", "Kapitel 04a", "Kapitel 04b")
  }

  @Test
  fun singleChapterIsKept() {
    normalize("Track 01") shouldContainExactly listOf("Track 01")
  }

  @Test
  fun idempotent() {
    val once = normalize("Track 01", "Track 02", "Track 03")
    ChapterNameNormalizer.normalize(once) { "Kapitel $it" } shouldBe once
    val english = ChapterNameNormalizer.normalize(listOf("Track 01", "Track 02")) { "Chapter $it" }
    ChapterNameNormalizer.normalize(english) { "Chapter $it" } shouldBe english
  }

  @Test
  fun syntheticFixtures() {
    val books = TagDump.parseBooks(javaClass.classLoader!!.getResource("hoerspiel/synthetic.jsonl")!!.readText())
      .associateBy { it.source.folderName }
    fun namesOf(folder: String) = ChapterNameNormalizer.normalize(books.getValue(folder).tracks.map { it.chapterName }) { "Kapitel $it" }

    val numbered = listOf("Kapitel 1", "Kapitel 2", "Kapitel 3")
    namesOf("KommissarFalk - 002 - Die Brücke") shouldContainExactly numbered
    namesOf("KommissarFalk - 012 - Am Hafen") shouldContainExactly numbered
    namesOf("KommissarFalk - 013 - Ich weiss wer du bist") shouldContainExactly numbered
    namesOf("KommissarFalk - 014 - Kaltes Feuer") shouldContainExactly numbered
    namesOf("KommissarFalk - 016 - Livrare") shouldContainExactly numbered
    namesOf("KommissarFalk - 018 - Die Tiefe") shouldContainExactly numbered
    namesOf("KommissarFalk - 017 - Ehrfurcht") shouldContainExactly listOf("Einleitung", "Kapitel 01a", "Kapitel 01b", "Epilog")
  }

  /** On the private dumps in fixtures/tags (when present): no book may end up with repeated names. */
  @Test
  fun localTagDumps() {
    val dumps = File("../../fixtures/tags").listFiles { file -> file.extension == "jsonl" }.orEmpty()
    assumeTrue("no local tag dumps", dumps.isNotEmpty())
    dumps.forEach { dump ->
      val duplicates = TagDump.parseBooks(dump.readText()).mapNotNull { book ->
        val before = book.tracks.map { it.chapterName }
        val after = ChapterNameNormalizer.normalize(before) { "Kapitel $it" }
        println("${book.source.folderName}: ${before.first()} … ${before.last()}  ->  ${after.first()} … ${after.last()}")
        book.source.folderName.takeIf { after.toSet().size != after.size }
      }
      duplicates.shouldBeEmpty()
    }
  }
}
