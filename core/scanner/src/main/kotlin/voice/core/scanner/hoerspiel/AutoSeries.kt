package voice.core.scanner.hoerspiel

import voice.core.common.comparator.NaturalOrderComparator
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.LibraryOrganisation
import voice.core.data.LibraryPlacement
import voice.core.data.LibraryShelf

/** A call to `LibraryOrganisationRepo.saveSeries`: [books] become the whole series, in this order. */
internal data class SeriesSave(
  /** Existing library series to extend, `null` to create one. */
  val seriesId: String?,
  val name: String,
  val books: List<BookId>,
  val shelfId: String,
)

internal data class AutoSeriesPlan(
  val saves: List<SeriesSave>,
  /** Books that must never be grouped automatically again. */
  val handled: Set<BookId>,
)

/**
 * Puts books with a recognized series into a library series stack, so a scanned audio drama
 * series shows up as one stack instead of loose books under "Other books".
 *
 * The library's stacks are its own organisation data, independent of [BookContent.series], and
 * the user can edit them. So this only ever touches each book once:
 * - a book already in a stack (put there by hand) is left there,
 * - a book that was handled before is never touched again, so taking it out of a stack sticks,
 * - a lone episode of a series waits until a second one shows up.
 *
 * New episodes join an existing stack of the same series (matched by [comparisonKey]) at the
 * position of their episode number, without reordering the books already in it. A new stack is
 * sorted by episode number (0, 1, 2 … 45), then by name.
 *
 * @param handled books considered in an earlier scan
 */
internal fun planAutoSeries(
  books: List<BookContent>,
  organisation: LibraryOrganisation,
  handled: Set<BookId>,
): AutoSeriesPlan {
  val placements = organisation.placements.associateBy { it.bookId }
  val newlyHandled = mutableSetOf<BookId>()
  val candidates = books.filter { book ->
    if (!book.isActive || book.series.isNullOrBlank() || book.id in handled) return@filter false
    val placement = placements[book.id.value] ?: return@filter false
    if (placement.seriesId != null) {
      newlyHandled += book.id
      return@filter false
    }
    true
  }
  val booksById = books.associateBy { it.id.value }
  val saves = candidates.groupBy { comparisonKey(it.series!!) }.mapNotNull { (key, newBooks) ->
    val existing = organisation.series.firstOrNull { series ->
      comparisonKey(series.name) == key && organisation.placements.any { it.seriesId == series.id }
    }
    if (existing != null) {
      val members = organisation.placements
        .filter { it.seriesId == existing.id }
        .sortedBy { it.seriesPosition }
        .map { BookId(it.bookId) }
      newlyHandled += newBooks.map { it.id }
      SeriesSave(existing.id, existing.name, insertByEpisode(members, newBooks, booksById), shelfId = shelfOf(members, placements))
    } else {
      // A series of one would just be a book with extra chrome.
      if (newBooks.size < 2) return@mapNotNull null
      // A stack lives on one shelf: take the shelf most of the episodes are on.
      val shelf = newBooks.groupingBy { placements.getValue(it.id.value).shelfId }.eachCount()
        .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).first().key
      val onShelf = newBooks.filter { placements.getValue(it.id.value).shelfId == shelf }
      if (onShelf.size < 2) return@mapNotNull null
      newlyHandled += onShelf.map { it.id }
      SeriesSave(null, seriesName(onShelf), onShelf.sortedWith(episodeOrder).map { it.id }, shelf)
    }
  }
  return AutoSeriesPlan(saves, newlyHandled)
}

private val episodeOrder: Comparator<BookContent> =
  compareBy<BookContent, Int?>(nullsLast()) { it.part?.trim()?.toIntOrNull() }
    .thenBy(NaturalOrderComparator.stringComparator) { it.name }
    .thenBy { it.id.value }

private fun episodeNumber(book: BookContent?): Int? = book?.part?.trim()?.toIntOrNull()

/**
 * Inserts each new book right after the last book with a lower episode number (or first, if all
 * are higher; last, without a number). The books already in the stack keep their order.
 */
private fun insertByEpisode(
  members: List<BookId>,
  newBooks: List<BookContent>,
  booksById: Map<String, BookContent>,
): List<BookId> {
  val result = members.toMutableList()
  newBooks.sortedWith(episodeOrder).forEach { book ->
    val number = episodeNumber(book)
    val numbers = result.map { episodeNumber(booksById[it.value]) }
    val index = when {
      number == null -> result.size
      else -> numbers.indexOfLast { it != null && it < number } + 1
    }
    result.add(index, book.id)
  }
  return result
}

private fun shelfOf(
  members: List<BookId>,
  placements: Map<String, LibraryPlacement>,
): String = members.firstNotNullOfOrNull { placements[it.value]?.shelfId } ?: LibraryShelf.OTHER

/** The most frequent spelling, ties alphabetically, so the stack name is stable between scans. */
private fun seriesName(books: List<BookContent>): String {
  return books.map { it.series!!.trim() }
    .groupingBy { it }
    .eachCount()
    .entries
    .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
    .first()
    .key
}
