package voice.features.bookOverview.views

import androidx.compose.runtime.mutableStateOf
import io.kotest.matchers.shouldBe
import org.junit.Test
import voice.core.data.BookId
import voice.core.data.hoerspiel.CoverStyle
import voice.features.bookOverview.overview.BookOverviewItemViewState
import voice.features.bookOverview.series.LibraryEntry

class CoverStyleTest {

  @Test
  fun cdsAreSquareBooksArePortrait() {
    CoverStyle.Cd.aspectRatio shouldBe 1f
    CoverStyle.Book.aspectRatio shouldBe 2f / 3f
  }

  @Test
  fun seriesStackFollowsItsBooks() {
    val book = BookOverviewItemViewState(
      name = "Die Brut",
      author = null,
      cover = null,
      progress = 0f,
      id = BookId("book"),
      remainingTime = "",
      coverStyle = CoverStyle.Book,
    )
    LibraryEntry("Gabriel Burns", listOf(mutableStateOf(book)), "other", "series:gb").coverAspectRatio shouldBe 2f / 3f
    LibraryEntry("Gabriel Burns", listOf(mutableStateOf(book.copy(coverStyle = CoverStyle.Cd))), "other", "series:gb")
      .coverAspectRatio shouldBe 1f
  }
}
