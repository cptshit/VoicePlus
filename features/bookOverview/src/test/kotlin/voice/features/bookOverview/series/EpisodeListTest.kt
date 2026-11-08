package voice.features.bookOverview.series

import android.net.Uri
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import voice.core.data.BookId
import voice.features.bookOverview.overview.BookOverviewItemViewState

@RunWith(RobolectricTestRunner::class)
class EpisodeListTest {

  private fun book(
    number: Int?,
    title: String = "Titel $number",
    progress: Float = 0f,
    finished: Boolean = false,
  ) = BookOverviewItemViewState(
    name = title,
    author = null,
    cover = null,
    progress = progress,
    id = BookId(Uri.parse("file:///GB/$title")),
    remainingTime = "",
    finished = finished,
    seriesPart = number?.toString(),
  )

  private fun describe(items: List<EpisodeListItem>) = items.map {
    when (it) {
      is EpisodeListItem.Header -> "${it.section}(${it.count})"
      is EpisodeListItem.Episode -> it.number ?: "?"
      is EpisodeListItem.Gap -> "gap ${it.from}-${it.to}"
    }
  }

  @Test
  fun sectionsAreRunningNextHeardAndEmptyOnesAreLeftOut() {
    val items = episodeList(
      listOf(book(1, finished = true), book(2, progress = 0.4f), book(3), book(4)),
      "Gabriel Burns",
    )
    describe(items) shouldBe listOf("Running(1)", "2", "Next(2)", "3", "4", "Heard(1)", "1")
    describe(episodeList(listOf(book(1), book(2)), "X")) shouldBe listOf("Next(2)", "1", "2")
  }

  @Test
  fun holesBecomeGapRowsAboveTheFollowingEpisode() {
    describe(episodeList(listOf(book(7), book(11), book(12)), "X")) shouldBe
      listOf("Next(3)", "7", "gap 8-10", "11", "12")
  }

  @Test
  fun aSingleMissingEpisodeIsAGapOfOne() {
    describe(episodeList(listOf(book(1), book(3)), "X")) shouldBe listOf("Next(2)", "1", "gap 2-2", "3")
  }

  @Test
  fun gapSitsInTheSectionOfTheEpisodeThatFollowsIt() {
    describe(episodeList(listOf(book(1, finished = true), book(4)), "X")) shouldBe
      listOf("Next(1)", "gap 2-3", "4", "Heard(1)", "1")
  }

  @Test
  fun noGapBeforeTheFirstOrAfterTheLastEpisode() {
    describe(episodeList(listOf(book(5), book(6)), "X")) shouldBe listOf("Next(2)", "5", "6")
  }

  @Test
  fun booksWithoutNumbersNeverCauseGaps() {
    describe(episodeList(listOf(book(null, "Alpha"), book(null, "Beta")), "X")) shouldBe listOf("Next(2)", "?", "?")
  }

  @Test
  fun twoPartsOfOneNumberProduceNoGap() {
    describe(episodeList(listOf(book(3, "A"), book(3, "B"), book(4, "C")), "X")) shouldBe listOf("Next(3)", "3", "3", "4")
  }
}
