package voice.core.data.store.snapshot

import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.room.RoomDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.MediaScanWaiter
import voice.core.data.hoerspiel.EpisodeNumberRepo
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.internals.dao.BookContentDao
import voice.core.data.repo.internals.dao.ChapterDao
import voice.core.data.repo.internals.dao.ChapterNameOverrideDao
import voice.core.data.repo.internals.dao.LibraryOrganisationDao
import voice.core.data.repo.internals.transaction
import voice.core.data.store.ExcludedBooksStore
import voice.core.data.store.snapshot.identity.DeviceRelativePath
import voice.core.data.store.snapshot.identity.IdentityStampBuilder
import voice.core.data.store.snapshot.rekey.BookIdentityStamp
import voice.core.data.store.snapshot.rekey.ReKeyResult
import voice.core.data.store.snapshot.rekey.RestoreReKeyer
import voice.core.data.store.snapshot.rekey.ScannedBook
import voice.core.data.store.snapshot.rekey.ScannedChapter
import voice.core.data.store.snapshot.rekey.SnapChapter
import voice.core.data.store.snapshot.rekey.SnapshotBook
import voice.core.logging.api.Logger
import java.time.Instant

/**
 * Restores an external backup bundle after an OS-level wipe (uninstall/reinstall → SAF re-grant changes every
 * `content://` URI). Unlike the on-device [BackupRestorer] (same URIs, stored-id), this path must:
 *  1. trigger a scan and wait for it to finish, so the library exists under the NEW post-re-grant URIs and
 *     the scan's active-book reconcile has run;
 *  2. re-key the snapshot (keyed to dead URIs) onto those freshly-scanned books by volume-namespaced relPath,
 *     via the strict, pure [RestoreReKeyer];
 *  3. write the matched, NEW-id-keyed rows additively, never clobbering a fresher live position.
 *
 * Books that can't be safely matched are returned in [ReKeyResult.unmatched] for the UI to surface — never
 * silently dropped, never guessed onto a neighbouring book.
 */
@SingleIn(AppScope::class)
@Inject
internal class OsWipeRestorer(
  private val scanWaiter: MediaScanWaiter,
  private val contentRepo: BookContentRepo,
  private val bookContentDao: BookContentDao,
  private val chapterDao: ChapterDao,
  private val chapterNameOverrideDao: ChapterNameOverrideDao,
  @ExcludedBooksStore private val excludedBooksStore: DataStore<Set<String>>,
  private val appDb: RoomDatabase,
  private val restoreGate: RestoreGate,
  private val organisationDao: LibraryOrganisationDao,
  private val episodeNumberRepo: EpisodeNumberRepo,
) {

  @IgnorableReturnValue
  suspend fun run(
    snapshot: LibrarySnapshot,
    covers: Map<String, java.io.File> = emptyMap(),
  ): ReKeyResult {
    val outcome = doRestore(snapshot, covers)
    // A clean, complete restore: flush the re-keyed state to the ring + external bundle now (the gate has
    // cleared) rather than waiting on the debounce. On a PARTIAL restore (some books surfaced as unmatched)
    // we deliberately do NOT flush — the external bundle still holds those books' data for a re-grant-and-retry,
    // and an export would overwrite it.
    if (outcome.matched.isNotEmpty() && outcome.unmatched.isEmpty()) {
      restoreGate.requestFlush()
    }
    return outcome
  }

  private suspend fun doRestore(
    snapshot: LibrarySnapshot,
    covers: Map<String, java.io.File>,
  ): ReKeyResult = restoreGate.withRestoreActive {
    // 1. Make the freshly-scanned, new-URI books exist AND the scan's setAllInactiveExcept reconcile complete
    // before we read them. scanAndAwait joins the actual scan job (not the racy scannerActive flag).
    scanWaiter.scanAndAwait()

    // 2. Scanned side: build a stamp + chapter anchors for each freshly-scanned active book.
    val liveBooks = bookContentDao.all()
    val chapterById = chapterDao.all().associateBy { it.id.value }
    val scannedBooks = liveBooks.filter { it.isActive }.map { book ->
      toScannedBook(book, book.chapters.mapNotNull { chapterById[it.value] })
    }

    // 3. Snapshot side: filter user-deleted books, then attach each book's stamp + chapters + user data.
    val excludedIds = excludedBooksStore.data.first()
    val snapChapterById = snapshot.chapters.associateBy { it.id }
    val overridesByBook = snapshot.chapterNameOverrides.groupBy { it.bookId }
    val snapshotBooks = snapshot.books
      // Hidden books DO participate: the scan has already put their files back on disk under new
      // ids, so skipping them would leave them visible and dataless. They are re-keyed with their
      // data and their NEW ids are added to the hidden set below, so they stay hidden.
      .filter { it.id !in excludedIds || it.id in snapshot.hiddenBooks }
      .map { dto ->
        toSnapshotBook(dto, snapChapterById, overridesByBook)
      }

    // 4. Pure, deterministic, never-cross-attach re-key.
    val result = RestoreReKeyer.reKey(snapshotBooks, scannedBooks)

    // 5. Persist the matched books, keyed entirely to the new ids. Atomic; additive; freshness-aware.
    val liveById = liveBooks.associateBy { it.id.value }
    appDb.transaction {
      organisationDao.restore(snapshot, result.matched.associate { it.sourceId to it.content.id.value })
      result.matched.forEach { matched ->
        val live = liveById[matched.content.id.value]
        // Portable artwork wins; legacy JSON bundles retain the freshly scanned cover. Newer
        // playback progress is merged into the restored metadata instead of blocking it.
        val content = matched.content
          .copy(
            cover = covers[matched.sourceId] ?: live?.cover,
            isActive = matched.sourceId !in snapshot.hiddenBooks,
          )
          .preserveNewerPlaybackFrom(live)
        bookContentDao.insert(content)
        // chapters2 rows already exist from the scan (matched.content.chapters are the scanned ids), so
        // BookRepository.book() resolves without re-inserting anything.
        matched.overrides.forEach { chapterNameOverrideDao.insert(it) }
      }
    }

    // Translate the hidden set onto the new ids: the snapshot's hidden ids died with the wipe, and
    // without this the freshly-scanned copies of hidden books would resurface visible.
    val hiddenNewIds = result.matched
      .filter { it.sourceId in snapshot.hiddenBooks }
      .map { it.content.id.value }
    if (hiddenNewIds.isNotEmpty()) {
      excludedBooksStore.updateData { it + hiddenNewIds }
    }

    // The corrected episode numbers are keyed by book id too, so they follow the books to their new ids.
    result.matched.forEach { matched ->
      snapshot.episodeNumbers[matched.sourceId]?.let { episodeNumberRepo.set(matched.content.id, it) }
    }

    // 6. Refresh the fill-once content cache so the restored books render this session, not after a restart.
    contentRepo.invalidateCache()
    deleteReplacedCovers(
      oldCovers = result.matched
        .filter { it.sourceId in covers }
        .map { liveById[it.content.id.value]?.cover },
      importedCovers = covers.values,
      referencedCovers = bookContentDao.all().map { it.cover },
    )
    Logger.i("OS-wipe restore: ${result.matched.size} re-keyed, ${result.unmatched.size} surfaced")
    result
  }

  private fun toScannedBook(
    book: BookContent,
    chapters: List<Chapter>,
  ): ScannedBook {
    val relPath = DeviceRelativePath.documentId(book.id.value.toUri())
    return ScannedBook(
      newBookId = book.id,
      stamp = IdentityStampBuilder.build(book, chapters),
      chapters = chapters.map {
        ScannedChapter(
          newId = it.id,
          relName = DeviceRelativePath.relName(it.id.value.toUri(), relPath),
          duration = it.duration,
        )
      },
    )
  }

  private fun toSnapshotBook(
    dto: BookContentDto,
    snapChapterById: Map<String, ChapterDto>,
    overridesByBook: Map<String, List<ChapterNameOverrideDto>>,
  ): SnapshotBook {
    val bookRelPath = DeviceRelativePath.documentId(dto.id.toUri())
    val bookChapters = dto.chapters.mapNotNull { snapChapterById[it] }
    val stamp = reconstructStamp(dto, bookChapters, bookRelPath)
    val snapChapters = bookChapters.map { chapter ->
      // Prefer the stored relName; fall back to deriving it (legacy bundles written before relNames existed).
      val relName = chapter.relName.ifEmpty { DeviceRelativePath.relName(chapter.id.toUri(), bookRelPath) }
      SnapChapter(oldId = ChapterId(chapter.id), relName = relName, duration = chapter.duration)
    }
    return SnapshotBook(
      stamp = stamp,
      content = dto,
      chapters = snapChapters,
      overrides = overridesByBook[dto.id].orEmpty(),
    )
  }

  /** Build the snapshot side's stamp from its stored URIs — the URIs in the bundle ARE the identity. */
  private fun reconstructStamp(
    dto: BookContentDto,
    chapters: List<ChapterDto>,
    bookRelPath: String,
  ): BookIdentityStamp {
    val children = chapters
      .map { DeviceRelativePath.relName(it.id.toUri(), bookRelPath) }
      .sorted()
    return BookIdentityStamp(
      authority = DeviceRelativePath.authority(dto.id.toUri()),
      isSingleFile = children.size == 1 && children.single().isEmpty(),
      relPath = bookRelPath,
      folderName = bookRelPath.substringAfterLast('/'),
      children = children,
    )
  }
}
