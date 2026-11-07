package voice.core.scanner.hoerspiel

import voice.core.data.hoerspiel.TagFindingField
import voice.core.data.hoerspiel.TagFindingKind
import voice.core.data.hoerspiel.TagFindingSource
import voice.core.scanner.Metadata

/** What the normalizer gets to see of one book. */
internal data class EpisodeSource(
  /** Name of the book folder, `null` for single-file books. */
  val folderName: String?,
  /** Paths of the audio files relative to the book folder, without extension, in playback order. */
  val fileNames: List<String>,
  /** Tags of the first audio file, `null` when tags are ignored or unreadable. */
  val tags: Metadata?,
)

internal data class Candidate(
  val value: String,
  val source: TagFindingSource,
)

/**
 * A disagreement between two sources. The normalizer never resolves these silently: [chosen]
 * is what it picked, [other] what it dropped, so a "check tags" screen can show both.
 */
internal data class EpisodeFinding(
  val field: TagFindingField,
  val kind: TagFindingKind,
  val chosen: Candidate,
  val other: Candidate,
)

internal data class NormalizedEpisode(
  val series: Candidate,
  /** [comparisonKey] of the series name, used to group episodes of one series. */
  val seriesKey: String,
  val episodeNumber: Int,
  val episodeNumberSource: TagFindingSource,
  val episodeTitle: Candidate?,
  /** Part of a multi-part story (`Teil 2`), cut off the end of the title. */
  val titlePart: Int? = null,
  val findings: List<EpisodeFinding>,
)

/**
 * Derives series, episode number and episode title of an audio drama episode from its tags,
 * folder name and file names. See docs/hoerspiel-tagging.md for the patterns this is built on.
 *
 * Returns `null` unless both a series and an episode number are found, so regular audiobooks
 * are left alone.
 *
 * Priorities:
 * - series: series tag (MVNM/SERIES) > folder > album > file names.
 *   The display name comes from the artist/album artist tag if it has the same [comparisonKey].
 * - episode number: part tag (MVIN/PART) > folder > album > file names.
 * - title: folder > album > file names. If another source spells the same title with umlauts
 *   where the chosen one uses `ue`/`ss`, the umlaut spelling wins.
 *   A trailing `Teil 2` / `[Teil 2]` is no part of the title but of a multi-part story: it is cut
 *   off every candidate and becomes [NormalizedEpisode.titlePart], taken from the chosen title or,
 *   if that has none, from the first source that has one.
 */
internal object HoerspielTagNormalizer {

  fun normalize(source: EpisodeSource): NormalizedEpisode? {
    val tags = source.tags
    val folder = source.folderName?.let(EpisodeNameParser::parse)
    val album = tags?.album?.takeIf { it.isNotBlank() }?.let(EpisodeNameParser::parse)
    val files = FileNameAnalysis.analyze(source.fileNames)

    val seriesCandidates = listOfNotNull(
      tags?.series?.trim()?.takeIf { it.isNotEmpty() }?.let { Candidate(it, TagFindingSource.SeriesTag) },
      folder?.series?.let { Candidate(it, TagFindingSource.FolderName) },
      album?.series?.let { Candidate(it, TagFindingSource.AlbumTag) },
      files.series?.let { Candidate(it, TagFindingSource.FileName) },
    )
    val numberCandidates = listOfNotNull(
      tags?.part?.trim()?.toIntOrNull()?.let { it to TagFindingSource.PartTag },
      folder?.number?.let { it to TagFindingSource.FolderName },
      album?.number?.let { it to TagFindingSource.AlbumTag },
      files.number?.let { it to TagFindingSource.FileName },
    )
    val titlesWithParts = listOfNotNull(
      folder?.title?.takeIf { folder.number != null }?.let { Candidate(it, TagFindingSource.FolderName) },
      album?.title?.let { Candidate(it, TagFindingSource.AlbumTag) },
      files.title?.let { Candidate(it, TagFindingSource.FileName) },
    ).map { candidate ->
      val (title, part) = splitTitlePart(candidate.value)
      candidate.copy(value = title) to part
    }
    val titleCandidates = titlesWithParts.map { it.first }

    val chosenSeries = seriesCandidates.firstOrNull() ?: return null
    val (number, numberSource) = numberCandidates.firstOrNull() ?: return null
    val seriesKey = comparisonKey(chosenSeries.value)

    val findings = mutableListOf<EpisodeFinding>()

    val displaySeries = listOfNotNull(tags?.albumArtist, tags?.artist)
      .map { it.trim() }
      .firstOrNull { comparisonKey(it) == seriesKey }
      ?.let { Candidate(it, TagFindingSource.ArtistTag) }
      ?: chosenSeries
    seriesCandidates.forEach { other ->
      if (comparisonKey(other.value) != seriesKey) {
        findings += EpisodeFinding(TagFindingField.Series, TagFindingKind.Different, displaySeries, other)
      }
    }

    numberCandidates.drop(1).forEach { (otherNumber, otherSource) ->
      if (otherNumber != number) {
        findings += EpisodeFinding(
          field = TagFindingField.EpisodeNumber,
          kind = TagFindingKind.Different,
          chosen = Candidate(number.toString(), numberSource),
          other = Candidate(otherNumber.toString(), otherSource),
        )
      }
    }

    val title = chooseTitle(titleCandidates)
    val titlePart = titlesWithParts.firstOrNull { it.first.source == title?.source }?.second
      ?: titlesWithParts.firstNotNullOfOrNull { it.second }
    if (title != null) {
      titleCandidates.forEach { other ->
        if (other.value != title.value) {
          findings += EpisodeFinding(TagFindingField.EpisodeTitle, classify(title.value, other.value), title, other)
        }
      }
    }

    return NormalizedEpisode(
      series = displaySeries,
      seriesKey = seriesKey,
      episodeNumber = number,
      episodeNumberSource = numberSource,
      episodeTitle = title,
      titlePart = titlePart,
      findings = findings.distinct(),
    )
  }

  // "Teil 1", "[Teil 2]", "(Teil 2)", "- Teil 2", "Teil 2 von 2"
  private val TRAILING_TITLE_PART = Regex(
    """\s*(?:[-–,]\s*)?[\[(]?\s*\bTeil\s+(\d{1,2})(?:\s+von\s+\d{1,2})?\s*[])]?$""",
    RegexOption.IGNORE_CASE,
  )

  /** Splits `Ohne Bewusstsein Teil 1` into `Ohne Bewusstsein` and 1. A title that is only `Teil 1` stays. */
  internal fun splitTitlePart(title: String): Pair<String, Int?> {
    val match = TRAILING_TITLE_PART.find(title) ?: return title to null
    val rest = title.substring(0, match.range.first).trim()
    if (rest.isEmpty()) return title to null
    return rest to match.groupValues[1].toInt()
  }

  private fun chooseTitle(candidates: List<Candidate>): Candidate? {
    val first = candidates.firstOrNull() ?: return null
    if (first.value.hasGermanSpecialCharacters()) return first
    val key = comparisonKey(first.value)
    return candidates.firstOrNull { comparisonKey(it.value) == key && it.value.hasGermanSpecialCharacters() } ?: first
  }

  private fun classify(
    chosen: String,
    other: String,
  ): TagFindingKind {
    val a = comparisonKey(chosen)
    val b = comparisonKey(other)
    return when {
      a == b -> TagFindingKind.Formatting
      a.startsWith(b) || b.startsWith(a) -> TagFindingKind.Extended
      else -> TagFindingKind.Different
    }
  }
}
