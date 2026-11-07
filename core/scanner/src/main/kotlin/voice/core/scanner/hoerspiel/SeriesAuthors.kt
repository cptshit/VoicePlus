package voice.core.scanner.hoerspiel

import voice.core.data.BookContent
import voice.core.data.BookId

/**
 * Untagged episodes have no author, tagged episodes of the same series usually do. So that a
 * series does not fall apart into "with author" and "without author", books without an author
 * get the most frequent author of the other books in their series. Nothing is invented: a series
 * where no book has an author stays without one, and existing authors are never replaced.
 *
 * Series are matched by [comparisonKey], so `GabrielBurns` and `Gabriel Burns` are one series.
 * Ties are broken alphabetically to keep the result stable between scans.
 *
 * @return the author to set, per book that currently has none
 */
internal fun missingSeriesAuthors(books: List<BookContent>): Map<BookId, String> {
  return books
    .filter { it.isActive && !it.series.isNullOrBlank() }
    .groupBy { comparisonKey(it.series!!) }
    .flatMap { (_, series) ->
      val author = series
        .mapNotNull { it.author?.trim()?.takeIf(String::isNotEmpty) }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .firstOrNull()
        ?.key
        ?: return@flatMap emptyList()
      series.filter { it.author.isNullOrBlank() }.map { it.id to author }
    }
    .toMap()
}
