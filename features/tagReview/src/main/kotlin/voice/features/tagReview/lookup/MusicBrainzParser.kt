package voice.features.tagReview.lookup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ReleaseGroupSearch(val `release-groups`: List<ReleaseGroup> = emptyList())

@Serializable
private data class ReleaseGroup(
  val title: String = "",
  val `primary-type`: String? = null,
  val `secondary-types`: List<String> = emptyList(),
  val `artist-credit`: List<ArtistCredit> = emptyList(),
)

@Serializable
private data class ArtistCredit(val name: String = "")

private val json = Json { ignoreUnknownKeys = true }

private val episodeTitle = Regex("^(.+?),\\s*Episode\\s+(\\d+)\\s*:\\s*(.+)$")

private val rejectedTypes = setOf("Soundtrack", "Musical", "Compilation", "Live", "Remix")

/**
 * Reads a MusicBrainz release-group search. A release group counts only when several features agree:
 * it is an audio drama or audiobook (no soundtrack or musical), the artist is exactly [series], and
 * the title has the form "Series, Episode N: Title" for the same series.
 */
fun parseMusicBrainz(
  body: String,
  series: String,
): List<CatalogEpisode> {
  val groups = runCatching { json.decodeFromString<ReleaseGroupSearch>(body) }.getOrNull()?.`release-groups`.orEmpty()
  return groups.mapNotNull { group ->
    val types = group.`secondary-types` + listOfNotNull(group.`primary-type`)
    val spoken = "Audio drama" in types || "Audiobook" in types
    if (!spoken || types.any { it in rejectedTypes }) return@mapNotNull null
    if (group.`artist-credit`.none { it.name.trim().equals(series.trim(), ignoreCase = true) }) return@mapNotNull null
    val match = episodeTitle.matchEntire(group.title.trim()) ?: return@mapNotNull null
    if (!match.groupValues[1].trim().equals(series.trim(), ignoreCase = true)) return@mapNotNull null
    val title = match.groupValues[3].withoutLeadingEllipsis().takeIf(String::isNotEmpty) ?: return@mapNotNull null
    CatalogEpisode(match.groupValues[2].toIntOrNull() ?: return@mapNotNull null, title)
  }.sortedBy { it.number }
}

/** How many release groups an answer holds; a full page means there may be more. */
internal fun musicBrainzGroupCount(body: String): Int =
  runCatching { json.decodeFromString<ReleaseGroupSearch>(body) }.getOrNull()?.`release-groups`?.size ?: 0
