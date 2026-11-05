package voice.core.scanner

import android.net.Uri
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.hoerspiel.TagFinding
import voice.core.data.hoerspiel.TagFindingRepo
import voice.core.data.repo.BookContentRepo
import voice.core.data.store.IgnoreFileTagsStore
import voice.core.data.toUri
import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.CachedDocumentFileFactory
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.HoerspielNormalizerFeatureFlagQualifier
import voice.core.logging.api.Logger
import voice.core.scanner.hoerspiel.EpisodeFinding
import voice.core.scanner.hoerspiel.EpisodeSource
import voice.core.scanner.hoerspiel.HoerspielTagNormalizer
import voice.core.scanner.hoerspiel.NormalizedEpisode
import voice.core.scanner.hoerspiel.missingSeriesAuthors
import java.time.Instant

@Inject
internal class BookParser(
  private val contentRepo: BookContentRepo,
  private val mediaAnalyzer: MediaAnalyzer,
  private val fileFactory: CachedDocumentFileFactory,
  @IgnoreFileTagsStore
  private val ignoreFileTagsStore: DataStore<Boolean>,
  @HoerspielNormalizerFeatureFlagQualifier
  private val hoerspielNormalizer: FeatureFlag<Boolean>,
  private val tagFindingRepo: TagFindingRepo,
) {

  suspend fun parseAndStore(
    chapters: List<Chapter>,
    file: CachedDocumentFile,
    firstChapterMetadata: Metadata? = null,
    forceReParse: Boolean = false,
  ): BookContent {
    val id = BookId(file.uri)
    val existing = contentRepo.get(id)
    if (existing != null && !forceReParse) {
      return existing
    }

    val ignoreFileTags = ignoreFileTagsStore.data.first()
    val analyzed = if (ignoreFileTags) {
      null
    } else {
      firstChapterMetadata ?: run {
        val uri = chapters.first().id.toUri()
        mediaAnalyzer.analyze(fileFactory.create(uri))
      }
    }
    val episode = normalizeEpisode(chapters, analyzed, file)
    val parsed = parse(chapters, id, analyzed, file, episode)
    tagFindingRepo.replaceForBook(id, episode?.findings.orEmpty().map { it.toStored(id) })

    // When re-parsing an existing book (e.g. after toggling "ignore file tags"), only the
    // metadata derived from tags/file names is refreshed. Playback-related state such as the
    // position, speed and added/last-played timestamps is preserved.
    val content = if (existing != null) {
      contentRepo.update(id) { current ->
        current.copy(
          author = parsed.author,
          name = if (current.nameOverridden) current.name else parsed.name,
          genre = parsed.genre,
          narrator = parsed.narrator,
          series = parsed.series,
          part = parsed.part,
        )
      } ?: parsed.also { contentRepo.put(it) }
    } else {
      parsed.also { contentRepo.put(it) }
    }
    return content
  }

  /**
   * Gives books without an author the most frequent author of their series, see
   * [missingSeriesAuthors]. Runs after a scan, when all episodes of a series are known.
   */
  suspend fun fillMissingSeriesAuthors() {
    if (!hoerspielNormalizer.get()) return
    missingSeriesAuthors(contentRepo.all()).forEach { (id, author) ->
      contentRepo.update(id) { current ->
        if (current.author.isNullOrBlank()) current.copy(author = author) else current
      }
    }
  }

  fun parse(
    chapters: List<Chapter>,
    id: BookId,
    analyzed: Metadata?,
    file: CachedDocumentFile,
    episode: NormalizedEpisode? = normalizeEpisode(chapters, analyzed, file),
  ): BookContent {
    return BookContent(
      id = id,
      isActive = true,
      addedAt = Instant.now(),
      author = analyzed?.artist,
      lastPlayedAt = Instant.EPOCH,
      name = episode?.episodeTitle?.value
        ?: analyzed?.album
        ?: analyzed?.title?.takeIf { file.isFile }
        ?: file.bookName(),
      playbackSpeed = 1F,
      skipSilence = false,
      chapters = chapters.map { it.id },
      positionInChapter = 0L,
      currentChapter = chapters.first().id,
      cover = null,
      gain = 0F,
      genre = analyzed?.genre,
      narrator = analyzed?.narrator,
      series = episode?.series?.value ?: analyzed?.series,
      part = episode?.episodeNumber?.toString() ?: analyzed?.part,
    ).also {
      validateIntegrity(it, chapters)
    }
  }

  private fun normalizeEpisode(
    chapters: List<Chapter>,
    analyzed: Metadata?,
    file: CachedDocumentFile,
  ): NormalizedEpisode? {
    if (!hoerspielNormalizer.get()) return null
    val source = EpisodeSource(
      folderName = file.name?.takeUnless { file.isFile },
      fileNames = chapters.map { it.id.toUri().fileNameWithoutExtension() },
      tags = analyzed,
    )
    return HoerspielTagNormalizer.normalize(source)
  }

  private fun CachedDocumentFile.bookName(): String {
    val fileName = name
    return if (fileName == null) {
      uri.toString()
        .removePrefix("/storage/emulated/0/")
        .removePrefix("/storage/emulated/")
        .removePrefix("/storage/")
        .also {
          Logger.w("Could not parse fileName from $this. Fallback to $it")
        }
    } else {
      if (isFile) {
        fileName.substringBeforeLast(".")
      } else {
        fileName
      }
    }
  }
}

internal fun validateIntegrity(
  content: BookContent,
  chapters: List<Chapter>,
) {
  // the init block performs integrity validation
  @Suppress("RETURN_VALUE_NOT_USED")
  Book(content, chapters)
}

internal fun Uri.filePath(): String? {
  return pathSegments.lastOrNull()
    ?.dropWhile { it != ':' }
    ?.removePrefix(":")
}

internal fun Uri.fileNameWithoutExtension(): String {
  // filePath() strips the SAF volume prefix ("primary:"), but is empty for plain file URIs
  val path = filePath()?.takeIf { it.isNotEmpty() } ?: lastPathSegment ?: toString()
  return path.substringAfterLast('/').substringBeforeLast('.')
}

private fun EpisodeFinding.toStored(bookId: BookId) = TagFinding(
  bookId = bookId.value,
  field = field,
  kind = kind,
  chosenValue = chosen.value,
  chosenSource = chosen.source,
  otherValue = other.value,
  otherSource = other.source,
)
