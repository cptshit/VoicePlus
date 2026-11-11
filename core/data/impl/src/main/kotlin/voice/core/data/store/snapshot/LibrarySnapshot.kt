package voice.core.data.store.snapshot

import kotlinx.serialization.Serializable
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.ChapterNameOverride
import voice.core.data.LibraryOrganisation
import voice.core.data.MarkData
import java.io.File
import java.time.Instant
import java.util.UUID

@Serializable
internal data class LibrarySnapshot(
  val schemaVersion: Int,
  val dbVersion: Int = 0,
  val sequence: Long,
  val savedAtEpochMillis: Long,
  val totalCount: Int,
  val activeCount: Int,
  val books: List<BookContentDto>,
  val chapterNameOverrides: List<ChapterNameOverrideDto>,
  // chapters2 rows for every book. Required for the OS-wipe restore: without re-inserted Chapter rows
  // BookRepository.book() returns null and the restored book is invisible. Default-empty so legacy bundles decode.
  val chapters: List<ChapterDto> = emptyList(),
  // Book ids the user hid from the library. Restored books stay hidden instead of resurfacing.
  val hiddenBooks: Set<String> = emptySet(),
  // App settings worth carrying across a wipe, each JSON-encoded by SettingsSnapshotter.
  val settings: Map<String, String> = emptyMap(),
  val organisation: LibraryOrganisation? = null,
  // Episode numbers the user corrected (book id -> number), kept across re-scans by the scanner.
  val episodeNumbers: Map<String, Int> = emptyMap(),
) {
  fun activeIds(): Set<String> = books.filter { it.isActive }.map { it.id }.toSet()

  companion object {
    // Bump whenever a restore-affecting field is added/changed. Import/restore accepts any bundle whose
    // schemaVersion is <= this (older bundles decode via default-valued fields); a bundle from a NEWER
    // schema is refused rather than silently truncated. v2 added: listening sessions, chapters2 rows,
    // chapter relNames, character notes on the OS-wipe path. v3 added: session endReason, listening
    // events, hidden books, settings — and dropped the stored per-book identity stamp
    // (it is derived from the URIs already in the bundle; old bundles' `identity` keys are ignored).
    // v4 adds manual series groups, separate from imported series tags.
    // v5 adds the user-chosen order within a manual series group.
    // v6 adds home shelves, stable series identity and independent manual/shelf ordering.
    // v7 preserves whether the user explicitly edited a book title.
    // v8 adds episode numbers corrected by the user.
    const val SCHEMA_VERSION = 8
  }
}

/** Pre-v7 backups cannot identify edited titles, so preserve every saved title rather than lose one. */
internal fun LibrarySnapshot.withTitleOverrideCompatibility(): LibrarySnapshot {
  return if (schemaVersion < 7) {
    copy(books = books.map { it.copy(nameOverridden = true) })
  } else {
    this
  }
}

@Serializable
internal data class BookContentDto(
  val id: String,
  val playbackSpeed: Float,
  val skipSilence: Boolean,
  val isActive: Boolean,
  val lastPlayedAtEpochMillis: Long,
  val author: String?,
  val name: String,
  val addedAtEpochMillis: Long,
  val chapters: List<String>,
  val currentChapter: String,
  val positionInChapter: Long,
  val coverPath: String?,
  val gain: Float = 0f,
  val genre: String?,
  val narrator: String?,
  val series: String?,
  val part: String?,
  val chapterNameOffset: Int = 0,
  val seriesGroup: String? = null,
  val seriesOrder: Int? = null,
  val nameOverridden: Boolean = false,
)

@Serializable
internal data class ChapterNameOverrideDto(
  val chapterId: String,
  val markStartMs: Long,
  val bookId: String,
  val name: String,
)

@Serializable
internal data class ChapterDto(
  val id: String,
  val name: String?,
  val duration: Long,
  val fileLastModifiedEpochMillis: Long,
  val markData: List<MarkData>,
  // Folder-relative document-id tail (with extension), e.g. "Disc1/01 - Intro.mp3". The stable per-chapter
  // anchor for re-keying bookmarks/overrides/positions after an OS-wipe. Empty until the identity store fills it.
  val relName: String = "",
  val fileSize: Long = 0L,
)

internal fun BookContent.toDto() = BookContentDto(
  id = id.value,
  playbackSpeed = playbackSpeed,
  skipSilence = skipSilence,
  isActive = isActive,
  lastPlayedAtEpochMillis = lastPlayedAt.toEpochMilli(),
  author = author,
  name = name,
  addedAtEpochMillis = addedAt.toEpochMilli(),
  chapters = chapters.map { it.value },
  currentChapter = currentChapter.value,
  positionInChapter = positionInChapter,
  coverPath = cover?.absolutePath,
  gain = gain,
  genre = genre,
  narrator = narrator,
  series = series,
  part = part,
  chapterNameOffset = chapterNameOffset,
  seriesGroup = seriesGroup,
  seriesOrder = seriesOrder,
  nameOverridden = nameOverridden,
)

// Reconstruction runs BookContent.init{ require(...) }; wrap so one bad row is dropped, not fatal.
internal fun BookContentDto.toBookContentOrNull(): BookContent? = runCatching {
  BookContent(
    id = BookId(id),
    playbackSpeed = playbackSpeed,
    skipSilence = skipSilence,
    isActive = isActive,
    lastPlayedAt = Instant.ofEpochMilli(lastPlayedAtEpochMillis),
    author = author,
    name = name,
    addedAt = Instant.ofEpochMilli(addedAtEpochMillis),
    chapters = chapters.map { ChapterId(it) },
    currentChapter = ChapterId(currentChapter),
    positionInChapter = positionInChapter,
    cover = coverPath?.let { File(it) },
    gain = gain,
    genre = genre,
    narrator = narrator,
    series = series,
    part = part,
    chapterNameOffset = chapterNameOffset,
    seriesGroup = seriesGroup,
    seriesOrder = seriesOrder,
    nameOverridden = nameOverridden,
  )
}.getOrNull()

internal fun ChapterNameOverride.toDto() = ChapterNameOverrideDto(chapterId, markStartMs, bookId, name)

internal fun ChapterNameOverrideDto.toOverride() = ChapterNameOverride(chapterId, markStartMs, bookId, name)

internal fun Chapter.toDto(relName: String = "") = ChapterDto(
  id = id.value,
  name = name,
  duration = duration,
  fileLastModifiedEpochMillis = fileLastModified.toEpochMilli(),
  markData = markData,
  relName = relName,
  fileSize = fileSize,
)

internal fun ChapterDto.toChapter() = Chapter(
  id = ChapterId(id),
  name = name,
  duration = duration,
  fileLastModified = Instant.ofEpochMilli(fileLastModifiedEpochMillis),
  markData = markData,
  fileSize = fileSize,
)
