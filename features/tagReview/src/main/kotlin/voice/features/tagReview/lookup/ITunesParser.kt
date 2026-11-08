package voice.features.tagReview.lookup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ITunesSearch(val results: List<ITunesResult> = emptyList())

@Serializable
private data class ITunesResult(
  val wrapperType: String? = null,
  val artistName: String? = null,
  val collectionName: String? = null,
)

private val json = Json { ignoreUnknownKeys = true }

private val advertising = Regex(
  "\\s*[\\(\\[](?:ungekürzt|gekürzt|hörspiel|hörbuch|audiobook|unabridged)[\\)\\]]|\\s*[-–]\\s*(?:jetzt\\s+reinhören|neu!?|hörspiel)\\s*$",
  RegexOption.IGNORE_CASE,
)

private val unit = "(?:Band|Folge|Teil|Episode|Tome|Volume|Vol\\.)"
private val patterns = listOf(
  // "Series, Band 3: Title"
  Regex("^(?<series>.+?),\\s*$unit\\s+(?<number>\\d+)\\s*[:\\-–]\\s*(?<title>.+)$", RegexOption.IGNORE_CASE),
  // "Series - Folge 3: Title"
  Regex("^(?<series>.+?)\\s+[-–]\\s*$unit\\s+(?<number>\\d+)\\s*[:\\-–]\\s*(?<title>.+)$", RegexOption.IGNORE_CASE),
  // "Series Tome 3: Title"
  Regex("^(?<series>.+?)\\s+$unit\\s+(?<number>\\d+)\\s*[:\\-–]\\s*(?<title>.+)$", RegexOption.IGNORE_CASE),
  // "Title (Series 3)"
  Regex("^(?<title>.+?)\\s*\\((?<series>.+?)\\s+(?<number>\\d+)\\)$"),
)

/** Removes advertising additions such as "(Ungekürzt)" or "- Jetzt reinhören". */
internal fun cleanITunesTitle(raw: String): String = raw.replace(advertising, "").trim()

/**
 * Reads an iTunes Search answer for audiobooks. Series and number are taken from patterns like
 * "Series, Band 3: Title", "Title (Series 3)" or "Series Tome 3: Title". A result counts only when
 * its series is exactly [series] and the author is [series] or one of [authors].
 */
fun parseITunes(
  body: String,
  series: String,
  authors: Set<String>,
): List<CatalogEpisode> {
  val results = runCatching { json.decodeFromString<ITunesSearch>(body) }.getOrNull()?.results.orEmpty()
  val allowed = (authors + series).map { it.trim().lowercase() }.toSet()
  return results.mapNotNull { result ->
    if (result.wrapperType != "audiobook") return@mapNotNull null
    if (result.artistName?.trim()?.lowercase() !in allowed) return@mapNotNull null
    val name = cleanITunesTitle(result.collectionName ?: return@mapNotNull null)
    patterns.firstNotNullOfOrNull { pattern ->
      val match = pattern.matchEntire(name) ?: return@firstNotNullOfOrNull null
      if (!match.groups["series"]!!.value.trim().equals(series.trim(), ignoreCase = true)) return@firstNotNullOfOrNull null
      val title = match.groups["title"]!!.value.withoutLeadingEllipsis().trim(' ', ':', '-', '–').takeIf(String::isNotEmpty)
      val number = match.groups["number"]!!.value.toIntOrNull()
      if (title == null || number == null) null else CatalogEpisode(number, title)
    }
  }.distinctBy { it.number }.sortedBy { it.number }
}
