package voice.features.tagReview.lookup

import voice.core.data.BookId
import voice.core.data.hoerspiel.TagFinding
import voice.core.data.hoerspiel.TagFindingField
import voice.core.data.hoerspiel.TagFindingKind
import voice.core.data.hoerspiel.TagFindingSource

/** What the library holds for one episode of a series. [number] is `null` for books without one. */
data class LocalEpisode(
  val id: BookId,
  val name: String,
  val number: Int?,
)

/**
 * Compares the library's episodes with a catalogue and returns what differs. Nothing here changes a
 * book: the result are findings the user decides on in "Check tags".
 *
 * - A numbered episode is compared by its number; the catalogue must list that number exactly once.
 *   Titles that differ only in case, punctuation or spacing are fine; a different spelling of
 *   letters (umlauts, ß) is a suggestion.
 * - A book without a number gets one only when its title equals exactly one catalogue title.
 */
fun suggestFromCatalog(
  local: List<LocalEpisode>,
  catalog: List<CatalogEpisode>,
  source: CatalogSource,
): List<TagFinding> {
  val byNumber = catalog.groupBy { it.number }
  val byTitle = catalog.groupBy { it.title.likeness() }
  val otherSource = if (source == CatalogSource.MusicBrainz) TagFindingSource.MusicBrainz else TagFindingSource.ITunes
  return local.mapNotNull { episode ->
    if (episode.number != null) {
      val entry = byNumber[episode.number]?.singleOrNull() ?: return@mapNotNull null
      if (entry.title.likeness() == episode.name.likeness()) return@mapNotNull null
      val spellingOnly = entry.title.foldAccents().likeness() == episode.name.foldAccents().likeness()
      TagFinding(
        bookId = episode.id.value,
        field = TagFindingField.EpisodeTitle,
        kind = if (spellingOnly) TagFindingKind.Formatting else TagFindingKind.Different,
        chosenValue = episode.name,
        chosenSource = TagFindingSource.Local,
        otherValue = entry.title,
        otherSource = otherSource,
      )
    } else {
      val entry = byTitle[episode.name.likeness()]?.singleOrNull() ?: return@mapNotNull null
      TagFinding(
        bookId = episode.id.value,
        field = TagFindingField.EpisodeNumber,
        kind = TagFindingKind.Different,
        chosenValue = "–",
        chosenSource = TagFindingSource.Local,
        otherValue = entry.number.toString(),
        otherSource = otherSource,
      )
    }
  }
}

private fun String.foldAccents(): String = lowercase()
  .replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "ss")
  .replace("ae", "a").replace("oe", "o").replace("ue", "u")
