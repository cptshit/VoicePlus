package voice.features.tagReview.lookup

import dev.zacsweers.metro.Inject
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import voice.core.data.BookContent
import voice.core.data.hoerspiel.CatalogCache
import voice.core.data.hoerspiel.CatalogCacheRepo
import voice.core.data.hoerspiel.TagFindingRepo
import voice.core.data.repo.BookContentRepo
import voice.core.logging.api.Logger
import java.io.IOException
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

sealed interface LookupResult {

  /** [suggestions] differences were stored; 0 means the library already agrees with the catalogue. */
  data class Found(
    val suggestions: Int,
    val source: CatalogSource,
  ) : LookupResult

  /** No catalogue lists this series in a form that matches (type, artist and episode numbers). */
  data object NothingSuitable : LookupResult

  /** Offline, blocked or an error answer; nothing was changed. */
  data object Failed : LookupResult
}

/** The name a book is grouped under: the user's group, else the series from its tags. */
internal val BookContent.seriesName: String? get() = (seriesGroup ?: series)?.trim()?.takeIf(String::isNotEmpty)

/**
 * Compares one series with MusicBrainz and, for audiobooks, iTunes. Runs only when the user asks. It
 * writes suggestions as findings and never changes a book.
 */
@Inject
class OnlineLookup(
  private val http: CatalogHttp,
  private val cacheRepo: CatalogCacheRepo,
  private val findingRepo: TagFindingRepo,
  private val contentRepo: BookContentRepo,
) {

  @IgnorableReturnValue
  suspend fun compare(
    series: String,
    refresh: Boolean = false,
  ): LookupResult {
    val books = contentRepo.all().filter { it.isActive && it.seriesName.equals(series, ignoreCase = true) }
    if (books.isEmpty()) return LookupResult.NothingSuitable
    val authors = books.mapNotNull { it.author?.trim()?.takeIf(String::isNotEmpty) }.toSet()
    val catalog = try {
      (if (refresh) null else cached(series)) ?: fetch(series, authors)
    } catch (e: CancellationException) {
      throw e
    } catch (e: IOException) {
      Logger.w("Online comparison failed: $e")
      return LookupResult.Failed
    } ?: return LookupResult.NothingSuitable

    val local = books.map { LocalEpisode(it.id, it.name, it.part?.trim()?.toIntOrNull()) }
    val findings = suggestFromCatalog(local, catalog.episodes, catalog.source).groupBy { it.bookId }
    books.forEach { book -> findingRepo.replaceOnlineForBook(book.id, findings[book.id.value].orEmpty()) }
    return LookupResult.Found(findings.values.sumOf { it.size }, catalog.source)
  }

  private class Catalog(
    val source: CatalogSource,
    val episodes: List<CatalogEpisode>,
  )

  private suspend fun cached(series: String): Catalog? {
    val stored = cacheRepo.get(series) ?: return null
    if (Instant.now().toEpochMilli() - stored.fetchedAtEpochMillis > CACHE_LIFETIME_MS) return null
    val source = CatalogSource.entries.firstOrNull { it.name == stored.source } ?: return null
    val episodes =
      runCatching { Json.decodeFromString(ListSerializer(CatalogEpisode.serializer()), stored.json) }.getOrNull() ?: return null
    return Catalog(source, episodes).takeIf { episodes.isNotEmpty() }
  }

  private suspend fun fetch(
    series: String,
    authors: Set<String>,
  ): Catalog? {
    val fromMusicBrainz = musicBrainz(series)
    val catalog = if (fromMusicBrainz.isNotEmpty()) {
      Catalog(CatalogSource.MusicBrainz, fromMusicBrainz)
    } else {
      val fromITunes = parseITunes(http.get(iTunesUrl(series)), series, authors)
      if (fromITunes.isEmpty()) return null
      Catalog(CatalogSource.ITunes, fromITunes)
    }
    cacheRepo.put(
      CatalogCache(
        series = series,
        source = catalog.source.name,
        json = Json.encodeToString(ListSerializer(CatalogEpisode.serializer()), catalog.episodes),
        fetchedAtEpochMillis = Instant.now().toEpochMilli(),
      ),
    )
    return catalog
  }

  private suspend fun musicBrainz(series: String): List<CatalogEpisode> {
    val episodes = mutableListOf<CatalogEpisode>()
    for (page in 0 until MAX_PAGES) {
      val body = http.get(musicBrainzUrl(series, offset = page * PAGE_SIZE))
      episodes += parseMusicBrainz(body, series)
      if (musicBrainzGroupCount(body) < PAGE_SIZE) break
    }
    return episodes.distinctBy { it.number }.sortedBy { it.number }
  }

  internal companion object {
    const val PAGE_SIZE = 100
    const val MAX_PAGES = 3
    const val CACHE_LIFETIME_MS = 30L * 24 * 60 * 60 * 1000

    fun musicBrainzUrl(
      series: String,
      offset: Int,
    ): String = "https://musicbrainz.org/ws/2/release-group".toHttpUrl().newBuilder()
      .addQueryParameter("query", "artist:\"${series.replace("\"", "")}\"")
      .addQueryParameter("limit", PAGE_SIZE.toString())
      .addQueryParameter("offset", offset.toString())
      .addQueryParameter("fmt", "json")
      .build().toString()

    fun iTunesUrl(series: String): String = "https://itunes.apple.com/search".toHttpUrl().newBuilder()
      .addQueryParameter("term", series)
      .addQueryParameter("media", "audiobook")
      .addQueryParameter("entity", "audiobook")
      .addQueryParameter("country", "de")
      .addQueryParameter("lang", "de_de")
      .addQueryParameter("limit", "200")
      .build().toString()
  }
}
