package voice.core.scanner

import android.content.Context
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.media3.common.C
import androidx.media3.common.FileTypes
import androidx.media3.common.MediaItem
import androidx.media3.container.MdtaMetadataEntry
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.id3.ChapterFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import androidx.media3.inspector.MetadataRetriever
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.guava.await
import voice.core.data.MarkData
import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.nameWithoutExtension
import voice.core.logging.api.Logger
import voice.core.scanner.matroska.MatroskaMetaDataExtractor
import voice.core.scanner.matroska.MatroskaParseException
import voice.core.scanner.mp4.Mp4ChapterExtractor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

@Inject
internal class MediaAnalyzer(
  private val context: Context,
  private val mp4ChapterExtractor: Mp4ChapterExtractor,
  private val matroskaExtractorFactory: MatroskaMetaDataExtractor.Factory,
) {

  // we use a custom MediaSourceFactory because the default one for the
  // retriever also extracts the covers
  private val mediaSourceFactory = DefaultMediaSourceFactory(
    context,
    DefaultExtractorsFactory(),
  )

  suspend fun analyze(file: CachedDocumentFile): Metadata? {
    val builder = Metadata.Builder(file.nameWithoutExtension())
    val (trackGroups, duration) = retrieveMetadataAndDuration(file.uri)
      ?: return null
    if (duration <= Duration.ZERO) {
      Logger.w("Duration is zero or negative for file: ${file.uri}")
      return null
    }

    repeat(trackGroups.length) { trackGroupsIndex ->
      val trackGroup = trackGroups[trackGroupsIndex]
      if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
        repeat(trackGroup.length) { formatIndex ->
          val format = trackGroup.getFormat(formatIndex)
          format.metadata?.let { metadata ->
            repeat(metadata.length()) { metadataIndex ->
              when (val entry = metadata.get(metadataIndex)) {
                is TextInformationFrame -> visitText(entry, builder)
                is BinaryFrame -> visitBinary(entry, builder)
                is ChapterFrame -> visitChapter(entry, builder)
                is VorbisComment -> visitVorbis(entry, builder)
                is MdtaMetadataEntry -> visitMdta(entry, builder)
                else -> Logger.d("Unknown metadata entry: $entry")
              }
            }
          }
        }
      }
    }

    val fileType = FileTypes.inferFileTypeFromUri(file.uri)
    val extension = (file.name ?: "").substringAfterLast(delimiter = ".", missingDelimiterValue = "").lowercase()
    if (fileType == FileTypes.MP4 || extension == "mp4" || extension == "m4a" || extension == "m4b") {
      parseMp4Chapters(file, builder)
    }
    if (fileType == FileTypes.MATROSKA || extension == "mka" || extension == "mkv") {
      parseMatroskaMetaData(file, builder)
    }

    return builder.build(duration)
  }

  private fun parseMatroskaMetaData(
    file: CachedDocumentFile,
    builder: Metadata.Builder,
  ) {
    try {
      matroskaExtractorFactory.create(file.uri).use { extractor ->
        val mediaInfo = extractor.readMediaInfo()
        builder.chapters.addAll(mediaInfo.chapters)
        builder.artist = builder.artist ?: mediaInfo.artist
        builder.album = builder.album ?: mediaInfo.album
        builder.title = builder.title ?: mediaInfo.title
      }
    } catch (e: MatroskaParseException) {
      Logger.e(e, "Error parsing Matroska metadata")
    }
  }

  private suspend fun parseMp4Chapters(
    file: CachedDocumentFile,
    builder: Metadata.Builder,
  ) {
    val chapters = mp4ChapterExtractor.extractChapters(file.uri)
    builder.chapters += chapters
  }

  private fun visitMdta(
    entry: MdtaMetadataEntry,
    builder: Metadata.Builder,
  ) {
    when (entry.key) {
      "com.apple.quicktime.title" -> {
        builder.title = entry.value.toString(Charsets.UTF_8)
      }
      "com.apple.quicktime.artist" -> {
        builder.artist = entry.value.toString(Charsets.UTF_8)
      }
      "com.apple.quicktime.album" -> {
        builder.album = entry.value.toString(Charsets.UTF_8)
      }
    }
  }

  private fun visitVorbis(
    entry: VorbisComment,
    builder: Metadata.Builder,
  ) {
    val key = entry.key
    val value = entry.value
    when {
      key == "ARTIST" -> builder.artist = value
      key == "ALBUM" -> builder.album = value
      key == "TITLE" -> builder.title = value
      key == "ALBUMARTIST" || key == "ALBUM ARTIST" -> builder.albumArtist = value
      key == "TRACKNUMBER" -> builder.setTrack(value)
      key == "DISCNUMBER" -> builder.setDisc(value)
      key == "DISCTOTAL" || key == "TOTALDISCS" -> builder.discCount = value.trim().toIntOrNull() ?: builder.discCount
      key == "GROUPING" -> builder.grouping = value
      key == "DATE" || key == "YEAR" -> builder.setYear(value)
      key == "SERIES" -> builder.series = value
      key.startsWith("CHAPTER") -> {
        val withoutPrefix = key.removePrefix("CHAPTER")
        val isName = withoutPrefix.endsWith("NAME")
        if (isName) {
          val index = withoutPrefix.removeSuffix("NAME").toIntOrNull()
          if (index != null) {
            builder.vorbisChapterNames[index] = value
          }
        } else {
          val index = withoutPrefix.toIntOrNull()
          if (index != null) {
            val duration = parseVorbisDuration(value)
            if (duration != null) {
              builder.vorbisChapterStarts[index] = duration.inWholeMilliseconds
            }
          }
        }
      }
      else -> Logger.d("Unknown comment name: ${entry.key}, value: $value")
    }
  }

  private fun visitChapter(
    entry: ChapterFrame,
    builder: Metadata.Builder,
  ) {
    repeat(entry.subFrameCount) { subFrameIndex ->
      val subFrame = entry.getSubFrame(subFrameIndex)
      if (subFrame is TextInformationFrame) {
        builder.chapters.add(MarkData(startMs = entry.startTimeMs.toLong(), name = subFrame.values.first()))
      }
    }
  }

  // Media3 only decodes frames starting with "T" as text. GRP1, MVNM and MVIN (iTunes, ID3v2.3/2.4)
  // share the text frame layout but arrive as BinaryFrame.
  private fun visitBinary(
    entry: BinaryFrame,
    builder: Metadata.Builder,
  ) {
    if (entry.id !in BINARY_TEXT_FRAME_IDS) return
    val value = decodeId3Text(entry.data) ?: return
    visitText(TextInformationFrame(entry.id, null, listOf(value)), builder)
  }

  @VisibleForTesting
  internal fun visitText(
    entry: TextInformationFrame,
    builder: Metadata.Builder,
  ) {
    val value = entry.values.first()
    when (entry.id) {
      "TIT2" -> builder.title = value
      "TPE1" -> builder.artist = value
      "TALB" -> builder.album = value
      "TCON" -> builder.genre = value
      "TCOM" -> builder.narrator = value
      "TPE2" -> builder.albumArtist = value
      "TRCK" -> builder.setTrack(value)
      "TPOS" -> builder.setDisc(value)
      // GRP1 is the iTunes grouping frame; older taggers (and MP4 ©grp via Media3) use TIT1.
      "GRP1" -> builder.grouping = value
      "TIT1" -> if (builder.grouping.isNullOrBlank()) builder.grouping = value
      "TYER", "TDRC", "TDRL" -> if (builder.year == null) builder.setYear(value)
      // ID3v2.4 movement frames and MP4 ©mvn/©mvi arrive with these ids, not as TXXX.
      "MVNM" -> builder.series = value
      "MVIN" -> builder.part = if (builder.part.isNullOrBlank()) value else builder.part
      "TXXX" -> when (entry.description) {
        "MVNM" -> builder.series = value
        "MVIN" -> builder.part = if (builder.part.isNullOrBlank()) value else builder.part
        "PART", "TXXX:PART" -> builder.part = value
        "NARRATOR", "TXXX:NARRATOR" -> builder.narrator = if (builder.narrator.isNullOrBlank()) value else builder.narrator
        else -> Logger.v("Unknown TXXX frame description:  ${entry.description}, value: $value")
      }
      "TSSE" -> {}
      else -> Logger.v("Unknown frame ID: ${entry.id}, value: $value")
    }
  }

  private suspend fun retrieveMetadataAndDuration(uri: Uri): Pair<TrackGroupArray, Duration>? {
    return try {
      MetadataRetriever.Builder(context, MediaItem.fromUri(uri))
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
        .use { retriever ->
          val trackGroups = retriever.retrieveTrackGroups().await()
          val duration = retriever.retrieveDurationUs().await().microseconds
          trackGroups to duration
        }
    } catch (e: Exception) {
      if (e is CancellationException) currentCoroutineContext().ensureActive()
      Logger.w(e, "Error retrieving metadata")
      null
    }
  }

  private suspend fun retrieveMetadata(uri: Uri): TrackGroupArray? {
    return try {
      MetadataRetriever.Builder(context, MediaItem.fromUri(uri))
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
        .use {
          it.retrieveTrackGroups().await()
        }
    } catch (e: Exception) {
      if (e is CancellationException) currentCoroutineContext().ensureActive()
      Logger.w(e, "Error retrieving metadata")
      null
    }
  }

  private suspend fun retrieveDuration(uri: Uri): Duration? {
    return try {
      MetadataRetriever.Builder(context, MediaItem.fromUri(uri))
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
        .use {
          it.retrieveDurationUs().await().microseconds
        }
    } catch (e: Exception) {
      if (e is CancellationException) currentCoroutineContext().ensureActive()
      Logger.w(e, "Error retrieving metadata")
      null
    }
  }
}

private val BINARY_TEXT_FRAME_IDS = setOf("GRP1", "MVNM", "MVIN")

/** Decodes the payload of an ID3v2 text frame (encoding byte + text) and returns its first value. */
internal fun decodeId3Text(data: ByteArray): String? {
  if (data.isEmpty()) return null
  val charset = when (data[0].toInt()) {
    0 -> Charsets.ISO_8859_1
    1 -> Charsets.UTF_16
    2 -> Charsets.UTF_16BE
    3 -> Charsets.UTF_8
    else -> return null
  }
  return String(data, 1, data.size - 1, charset)
    .substringBefore('\u0000')
    .takeIf { it.isNotBlank() }
}
