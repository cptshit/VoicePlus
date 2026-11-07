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
class SeriesAuthorsTest {

  private fun book(
    name: String,
    series: String?,
    author: String?,
    isActive: Boolean = true,
  ): BookContent {
    val chapter = ChapterId(Uri.parse("file:///$name/1.mp3"))
    return BookContent(
      id = BookId(Uri.parse("file:///$name")),
      playbackSpeed = 1F,
      skipSilence = false,
      isActive = isActive,
      lastPlayedAt = Instant.EPOCH,
      author = author,
      name = name,
      addedAt = Instant.EPOCH,
      chapters = listOf(chapter),
      currentChapter = chapter,
      positionInChapter = 0,
      cover = null,
      gain = 0F,
      genre = null,
      narrator = null,
      series = series,
      part = null,
    )
  }

  @Test
  fun untaggedEpisodesGetTheSeriesAuthor() {
    val untagged = book("e2", "Gabriel Burns", null)
    val result = missingSeriesAuthors(
      listOf(untagged, book("e31", "Gabriel Burns", "Gabriel Burns"), book("e32", "GabrielBurns", "Gabriel Burns")),
    )
    result shouldBe mapOf(untagged.id to "Gabriel Burns")
  }

  @Test
  fun mostFrequentAuthorWinsAndTiesAreStable() {
    val untagged = book("x", "S", null)
    missingSeriesAuthors(listOf(untagged, book("a", "S", "B"), book("b", "S", "A"), book("c", "S", "B")))
      .shouldBe(mapOf(untagged.id to "B"))
    missingSeriesAuthors(listOf(untagged, book("a", "S", "B"), book("b", "S", "A")))
      .shouldBe(mapOf(untagged.id to "A"))
  }

  @Test
  fun nothingIsInvented() {
    missingSeriesAuthors(listOf(book("a", "S", null), book("b", "S", " "))).shouldBeEmpty()
    missingSeriesAuthors(listOf(book("a", null, null), book("b", null, "X"))).shouldBeEmpty()
  }

  @Test
  fun otherSeriesAndInactiveBooksDoNotCount() {
    missingSeriesAuthors(listOf(book("a", "S", null), book("b", "T", "X"))).shouldBeEmpty()
    missingSeriesAuthors(listOf(book("a", "S", null), book("b", "S", "X", isActive = false))).shouldBeEmpty()
  }
}
