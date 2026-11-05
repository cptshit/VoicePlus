package voice.core.scanner.hoerspiel

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.LibraryOrganisation
import voice.core.data.LibraryPlacement
import voice.core.data.LibrarySeries
import voice.core.data.LibraryShelf
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class AutoSeriesTest {

  private fun book(
    name: String,
    series: String? = "Gabriel Burns",
    part: String? = null,
    isActive: Boolean = true,
  ): BookContent {
    val chapter = ChapterId(Uri.parse("file:///$name/1.mp3"))
    return BookContent(
      id = BookId(Uri.parse("file:///${Uri.encode(name)}")),
      playbackSpeed = 1F,
      skipSilence = false,
      isActive = isActive,
      lastPlayedAt = Instant.EPOCH,
      author = null,
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
      part = part,
    )
  }

  private fun loose(
    vararg books: BookContent,
    shelfId: String = LibraryShelf.OTHER,
  ) = books.mapIndexed { index, book -> LibraryPlacement(book.id.value, shelfId, position = index) }

  @Test
  fun newSeriesIsSortedNumerically() {
    val books = listOf(
      book("Die Brut", part = "2"),
      book("Rand der Gezeiten", part = "31"),
      book("Der Fluesterer", part = "1"),
      book("Prolog", part = "0"),
      book("Nebelsee", part = "10"),
    )
    val plan = planAutoSeries(books, LibraryOrganisation(placements = loose(*books.toTypedArray())), handled = emptySet())

    plan.saves.single() shouldBe SeriesSave(
      seriesId = null,
      name = "Gabriel Burns",
      books = listOf("Prolog", "Der Fluesterer", "Die Brut", "Nebelsee", "Rand der Gezeiten").map { name ->
        books.single { it.name == name }.id
      },
      shelfId = LibraryShelf.OTHER,
    )
    plan.handled shouldBe books.map { it.id }.toSet()
  }

  @Test
  fun aLoneEpisodeWaitsForASecondOne() {
    val first = book("Der Fluesterer", part = "1")
    val plan = planAutoSeries(listOf(first), LibraryOrganisation(placements = loose(first)), emptySet())
    plan.saves.shouldBeEmpty()
    plan.handled.shouldBeEmpty()
  }

  @Test
  fun manualPlacementsAndHandledBooksAreLeftAlone() {
    val inOwnSeries = book("Die Brut", part = "2")
    val takenOut = book("Nebelsee", part = "10")
    val fresh = book("Der Fluesterer", part = "1")
    val organisation = LibraryOrganisation(
      series = listOf(LibrarySeries("mine", "Meine Lieblinge")),
      placements = listOf(
        LibraryPlacement(inOwnSeries.id.value, position = 0, seriesId = "mine", seriesPosition = 0),
        LibraryPlacement(takenOut.id.value, position = 1),
        LibraryPlacement(fresh.id.value, position = 2),
      ),
    )

    val plan = planAutoSeries(listOf(inOwnSeries, takenOut, fresh), organisation, handled = setOf(takenOut.id))

    // only one candidate left, so no stack; the manually placed book is remembered
    plan.saves.shouldBeEmpty()
    plan.handled shouldBe setOf(inOwnSeries.id)
  }

  @Test
  fun newEpisodesJoinTheExistingStackByNumberWithoutReordering() {
    // the user moved 3 before 1; that order stays
    val three = book("Experiment Stille", part = "3")
    val one = book("Der Fluesterer", part = "1")
    val five = book("Nachtkathedrale", part = "5")
    val two = book("Die Brut", series = "GabrielBurns", part = "2")
    val nine = book("Am Grenzgebiet", part = "9")
    val organisation = LibraryOrganisation(
      series = listOf(LibrarySeries("gb", "Gabriel Burns")),
      placements = listOf(
        LibraryPlacement(three.id.value, "shelf", position = 0, seriesId = "gb", seriesPosition = 0),
        LibraryPlacement(one.id.value, "shelf", position = 0, seriesId = "gb", seriesPosition = 1),
        LibraryPlacement(five.id.value, "shelf", position = 0, seriesId = "gb", seriesPosition = 2),
      ) + loose(two, nine),
      shelves = listOf(LibraryShelf.Other, LibraryShelf("shelf", "Hörspiele", 0)),
    )

    val plan = planAutoSeries(listOf(three, one, five, two, nine), organisation, handled = emptySet())

    plan.saves.single() shouldBe SeriesSave("gb", "Gabriel Burns", listOf(three, one, two, five, nine).map { it.id }, "shelf")
    plan.handled shouldBe setOf(three, one, five, two, nine).map { it.id }.toSet()
  }

  @Test
  fun booksWithoutSeriesOrInactiveAreIgnored() {
    val noSeries = book("Der Zauberberg", series = null)
    val inactive = book("Die Brut", part = "2", isActive = false)
    val active = book("Der Fluesterer", part = "1")
    val plan = planAutoSeries(
      listOf(noSeries, inactive, active),
      LibraryOrganisation(placements = loose(noSeries, inactive, active)),
      emptySet(),
    )
    plan.saves.shouldBeEmpty()
  }

  @Test
  fun stackGoesToTheShelfMostEpisodesAreOn() {
    val a = book("A", part = "1")
    val b = book("B", part = "2")
    val c = book("C", part = "3")
    val organisation = LibraryOrganisation(
      shelves = listOf(LibraryShelf.Other, LibraryShelf("shelf", "Hörspiele", 0)),
      placements = loose(a) + loose(b, c, shelfId = "shelf"),
    )
    val save = planAutoSeries(listOf(a, b, c), organisation, emptySet()).saves.single()
    save.shelfId shouldBe "shelf"
    save.books shouldContainExactly listOf(b.id, c.id)
  }
}
