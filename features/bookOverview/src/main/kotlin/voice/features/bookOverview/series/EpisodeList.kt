package voice.features.bookOverview.series

import voice.features.bookOverview.overview.BookOverviewItemViewState

internal enum class EpisodeSection { Running, Next, Heard }

internal sealed interface EpisodeListItem {

  val key: String

  data class Header(
    val section: EpisodeSection,
    val count: Int,
  ) : EpisodeListItem {
    override val key: String get() = "header:$section"
  }

  data class Episode(
    val book: BookOverviewItemViewState,
    /** Episode number as tagged ("12"), null if the book has none. */
    val number: String?,
    val title: String,
  ) : EpisodeListItem {
    override val key: String get() = "episode:${book.id.value}"
  }

  /** Episodes [from]..[to] of the series are not in the library; shown above the episode that follows them. */
  data class Gap(
    val from: Int,
    val to: Int,
  ) : EpisodeListItem {
    override val key: String get() = "gap:$from-$to"
  }
}

internal fun BookOverviewItemViewState.section(): EpisodeSection = when {
  finished -> EpisodeSection.Heard
  progress > 0f -> EpisodeSection.Running
  else -> EpisodeSection.Next
}

/**
 * Orders the episodes of a series like a concert list: running, up next, heard. The order of
 * [books] is kept inside a section. Holes in the episode numbers become [EpisodeListItem.Gap] rows
 * right above the episode that follows the hole; nothing is guessed before the first or after the
 * last known episode.
 */
internal fun episodeList(
  books: List<BookOverviewItemViewState>,
  seriesName: String,
): List<EpisodeListItem> {
  val episodes = books.map { book ->
    val label = seriesBookLabel(book, seriesName)
    EpisodeListItem.Episode(book, label.part, label.title)
  }
  val known = episodes.mapNotNull { it.number?.trim()?.toIntOrNull() }.distinct().sorted()
  val gapBefore = known.zipWithNext().filter { (a, b) -> b - a > 1 }.associate { (a, b) -> b to EpisodeListItem.Gap(a + 1, b - 1) }
  val shownGaps = mutableSetOf<Int>()
  return EpisodeSection.entries.flatMap { section ->
    val inSection = episodes.filter { it.book.section() == section }
    if (inSection.isEmpty()) {
      emptyList()
    } else {
      buildList {
        add(EpisodeListItem.Header(section, inSection.size))
        inSection.forEach { episode ->
          val number = episode.number?.trim()?.toIntOrNull()
          if (number != null && shownGaps.add(number)) gapBefore[number]?.let(::add)
          add(episode)
        }
      }
    }
  }
}
