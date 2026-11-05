package voice.core.scanner.hoerspiel

import voice.core.data.BookContent
import voice.core.data.BookId

/**
 * Finds out how many parts a multi-part story has. Episodes with consecutive numbers in one
 * series whose parts count up from 1 (`10 Teil 1`, `11 Teil 2`) form one story; every episode of
 * it gets the number of parts.
 *
 * Nothing is guessed: a story whose first part is missing, or whose parts are not on
 * consecutive episodes, gets no count, and a lone `Teil 1` does not become a two-parter.
 *
 * @param parts the part of each book, as the normalizer cut it off the title
 * @return the number of parts, per book that is part of a complete story
 */
internal fun multiPartCounts(
  books: List<BookContent>,
  parts: Map<BookId, Int>,
): Map<BookId, Int> {
  data class Episode(
    val id: BookId,
    val number: Int,
    val part: Int,
  )
  return books
    .filter { it.isActive && !it.series.isNullOrBlank() && it.id in parts }
    .mapNotNull { book ->
      val number = book.part?.trim()?.toIntOrNull() ?: return@mapNotNull null
      comparisonKey(book.series!!) to Episode(book.id, number, parts.getValue(book.id))
    }
    .groupBy({ it.first }, { it.second })
    .values
    .flatMap { series ->
      val stories = mutableListOf<MutableList<Episode>>()
      series.sortedBy { it.number }.forEach { episode ->
        val story = stories.lastOrNull()
        val previous = story?.lastOrNull()
        if (previous != null && episode.number == previous.number + 1 && episode.part == previous.part + 1) {
          story.add(episode)
        } else if (episode.part == 1) {
          stories.add(mutableListOf(episode))
        } else {
          // a later part without its predecessor: start a story that can never be complete
          stories.add(mutableListOf())
        }
      }
      stories.filter { it.size >= 2 }.flatMap { story -> story.map { it.id to story.size } }
    }
    .toMap()
}
