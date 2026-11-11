package voice.features.tagReview.lookup

import android.net.Uri
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.hoerspiel.CatalogCache
import voice.core.data.hoerspiel.CatalogCacheRepo
import voice.core.data.hoerspiel.TagFinding
import voice.core.data.hoerspiel.TagFindingRepo
import voice.core.data.repo.BookContentRepo
import java.io.File
import java.io.IOException
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class OnlineLookupTest {

  private val fixture = File("../../fixtures/musicbrainz/gabriel-burns-release-groups.json").readText()
  private val requests = mutableListOf<String>()
  private var answer: (String) -> String = { fixture }
  private val http = CatalogHttp { url ->
    requests += url
    answer(url)
  }
  private val cached = mutableMapOf<String, CatalogCache>()
  private val cacheRepo = object : CatalogCacheRepo {
    override suspend fun get(series: String) = cached[series.lowercase()]

    override suspend fun put(cache: CatalogCache) {
      cached[cache.series.lowercase()] = cache
    }
  }
  private val stored = mutableMapOf<BookId, List<TagFinding>>()
  private val findingRepo = mockk<TagFindingRepo>(relaxed = true)

  private fun book(
    name: String,
    part: String?,
    series: String? = "Gabriel Burns",
    active: Boolean = true,
  ): BookContent {
    val chapter = ChapterId(Uri.parse("file:///GB/$name/1.mp3"))
    return BookContent(
      id = BookId(Uri.parse("file:///GB/$name")), playbackSpeed = 1F, skipSilence = false, isActive = active,
      lastPlayedAt = Instant.EPOCH, author = "Gabriel Burns", name = name, addedAt = Instant.EPOCH,
      chapters = listOf(chapter), currentChapter = chapter, positionInChapter = 0, cover = null, gain = 0F, genre = null,
      narrator = null, series = series, part = part,
    )
  }

  private fun lookup(vararg books: BookContent): OnlineLookup {
    val repo = mockk<BookContentRepo>()
    coEvery { repo.all() } returns books.toList()
    coEvery { findingRepo.replaceOnlineForBook(any(), any()) } answers { stored[firstArg()] = secondArg() }
    return OnlineLookup(http, cacheRepo, findingRepo, repo)
  }

  @Test
  fun storesASuggestionForADifferentSpellingAndNothingForAMatch() = runTest {
    val spelled = book("Der Fluesterer", "1")
    val fine = book("Die Brut", "2")

    val result = lookup(spelled, fine).compare("Gabriel Burns")

    result shouldBe LookupResult.Found(1, CatalogSource.MusicBrainz)
    stored.getValue(spelled.id).single().otherValue shouldBe "Der Flüsterer"
    stored.getValue(fine.id) shouldBe emptyList()
  }

  @Test
  fun identifiesTheAppAndAsksOnlyForTheSeries() = runTest {
    lookup(book("Die Brut", "2")).compare("Gabriel Burns")

    requests.size shouldBe 1
    val url = requests.single().toHttpUrl()
    url.host shouldBe "musicbrainz.org"
    url.queryParameter("query") shouldBe "artist:\"Gabriel Burns\""
  }

  @Test
  fun secondRunUsesTheCacheAndSendsNothing() = runTest {
    val subject = lookup(book("Der Fluesterer", "1"))
    subject.compare("Gabriel Burns")
    requests.clear()

    subject.compare("gabriel burns") shouldBe LookupResult.Found(1, CatalogSource.MusicBrainz)
    requests shouldBe emptyList()
  }

  @Test
  fun refreshAsksTheServicesAgainDespiteTheCache() = runTest {
    val subject = lookup(book("Der Fluesterer", "1"))
    subject.compare("Gabriel Burns")
    requests.clear()

    subject.compare("Gabriel Burns", refresh = true) shouldBe LookupResult.Found(1, CatalogSource.MusicBrainz)
    requests.size shouldBe 1
  }

  @Test
  fun fallsBackToITunesWhenMusicBrainzHasNothingSuitable() = runTest {
    answer = { url ->
      if ("musicbrainz" in url) {
        """{"release-groups":[]}"""
      } else {
        """{"results":[{"wrapperType":"audiobook","artistName":"Gabriel Burns","collectionName":"Gabriel Burns, Band 1: Der Flüsterer (Ungekürzt)"}]}"""
      }
    }

    val result = lookup(book("Der Fluesterer", "1")).compare("Gabriel Burns")

    result shouldBe LookupResult.Found(1, CatalogSource.ITunes)
    requests.map { it.substringAfter("//").substringBefore("/") } shouldBe listOf("musicbrainz.org", "itunes.apple.com")
  }

  @Test
  fun nothingSuitableAnywhereIsReportedAndNotCached() = runTest {
    answer = { """{"release-groups":[],"results":[]}""" }

    lookup(book("X", "1")).compare("Gabriel Burns") shouldBe LookupResult.NothingSuitable
    cached shouldBe emptyMap()
  }

  @Test
  fun offlineChangesNothing() = runTest {
    answer = { throw IOException("offline") }

    lookup(book("X", "1")).compare("Gabriel Burns") shouldBe LookupResult.Failed
    coVerify(exactly = 0) { findingRepo.replaceOnlineForBook(any(), any()) }
  }

  @Test
  fun hiddenBooksAndOtherSeriesAreNotCompared() = runTest {
    val other = book("Etwas", "1", series = "Anderes")
    val hidden = book("Versteckt", "2", active = false)

    lookup(other, hidden).compare("Gabriel Burns") shouldBe LookupResult.NothingSuitable
    requests shouldBe emptyList()
  }

  @Test
  fun userAgentNamesAppAndVersionOnly() {
    catalogUserAgent("1.2.3", "de.henrik.voiceplus") shouldBe "VoicePlus/1.2.3 (de.henrik.voiceplus)"
    catalogUserAgent(null, "de.henrik.voiceplus") shouldBe "VoicePlus/unknown (de.henrik.voiceplus)"
  }
}
