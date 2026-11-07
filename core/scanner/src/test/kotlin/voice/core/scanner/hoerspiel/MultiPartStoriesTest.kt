package voice.core.scanner.hoerspiel

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.ChapterId
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class MultiPartStoriesTest {

  private fun episode(
    number: Int,
    series: String = "Gabriel Burns",
    active: Boolean = true,
  ): BookContent {
    val chapter = ChapterId(Uri.parse("file:///$series/$number/1.mp3"))
    return BookContent(
      id = BookId(Uri.parse("file:///${Uri.encode(series)}/$number/${if (active) "a" else "i"}")),
      playbackSpeed = 1F,
      skipSilence = false,
      isActive = active,
      lastPlayedAt = Instant.EPOCH,
      author = null,
      name = "$number",
      addedAt = Instant.EPOCH,
      chapters = listOf(chapter),
      cover = null,
      currentChapter = chapter,
      positionInChapter = 0,
      gain = 0F,
      genre = null,
      narrator = null,
      series = series,
      part = number.toString(),
    )
  }

  @Test
  fun gabrielBurnsTwoParters() {
    val books = (9..17).map { episode(it) }
    val parts = mapOf(10 to 1, 11 to 2, 12 to 1, 13 to 2, 15 to 1, 16 to 2)
      .mapKeys { (number, _) -> books.single { it.part == "$number" }.id }

    multiPartCounts(books, parts) shouldBe parts.mapValues { 2 }
  }

  @Test
  fun threeParterAndSeriesSpellings() {
    val books = listOf(episode(4, "GabrielBurns"), episode(5, "Gabriel Burns"), episode(6, "gabriel burns"))
    val parts = books.mapIndexed { index, book -> book.id to index + 1 }.toMap()

    multiPartCounts(books, parts) shouldBe books.associate { it.id to 3 }
  }

  @Test
  fun incompleteStoriesGetNoCount() {
    // 20 lone first part, 22 second part without first, 24/26 not consecutive
    val books = listOf(20, 22, 24, 26).map { episode(it) }
    val parts = mapOf(20 to 1, 22 to 2, 24 to 1, 26 to 2)
      .mapKeys { (number, _) -> books.single { it.part == "$number" }.id }

    multiPartCounts(books, parts).shouldBeEmpty()
  }

  @Test
  fun otherSeriesAndInactiveBooksDoNotJoin() {
    val first = episode(1)
    val otherSeries = episode(2, series = "Kommissar Falk")
    val inactive = episode(2, active = false)

    multiPartCounts(listOf(first, otherSeries, inactive), mapOf(first.id to 1, otherSeries.id to 2, inactive.id to 2))
      .shouldBeEmpty()
  }
}
