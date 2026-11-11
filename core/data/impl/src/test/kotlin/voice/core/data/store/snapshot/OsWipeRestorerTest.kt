package voice.core.data.store.snapshot

import android.content.Context
import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.MediaScanWaiter
import voice.core.data.repo.BookContentRepoImpl
import voice.core.data.repo.BookRepositoryImpl
import voice.core.data.repo.ChapterRepoImpl
import voice.core.data.repo.internals.AppDb
import voice.core.data.repo.internals.MemoryDataStore
import voice.core.data.store.snapshot.identity.DeviceRelativePath
import java.time.Instant

/**
 * End-to-end OS-wipe restore: the snapshot is keyed to dead pre-wipe URIs, the (simulated) re-scan produces
 * the same books under brand-new post-re-grant URIs, and the restorer must re-attach the user's data onto the
 * new books — proving books resolve again with their old position, never cross-attaching.
 */
@RunWith(RobolectricTestRunner::class)
class OsWipeRestorerTest {

  private val auth = "com.android.externalstorage.documents"
  private lateinit var db: AppDb
  private lateinit var contentRepo: BookContentRepoImpl
  private val excluded = MemoryDataStore<Set<String>>(emptySet())

  // Fake scan: invoking scanAndAwait runs [onScan], which inserts the freshly-scanned (new-URI) library.
  private var onScan: suspend () -> Unit = {}
  private val scanWaiter = object : MediaScanWaiter {
    override suspend fun scanAndAwait() = onScan()
  }

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDb::class.java).allowMainThreadQueries().build()
    contentRepo = BookContentRepoImpl(db.bookContentDao())
  }

  @After
  fun teardown() = db.close()

  private val episodeNumbers = FakeEpisodeNumberRepo()

  private fun restorer() = OsWipeRestorer(
    organisationDao = db.libraryOrganisationDao(),
    scanWaiter = scanWaiter,
    contentRepo = contentRepo,
    bookContentDao = db.bookContentDao(),
    chapterDao = db.chapterDao(),
    bookmarkDao = db.bookmarkDao(),
    chapterNameOverrideDao = db.chapterNameOverrideDao(),
    excludedBooksStore = excluded,
    appDb = db,
    restoreGate = RestoreGate(),
    episodeNumberRepo = episodeNumbers,
  )

  // --- URI schemes: same documentId tail under different tree-grant prefixes (= the OS-wipe re-grant) ---

  private fun docUri(
    tree: String,
    documentId: String,
  ): String {
    val enc = java.net.URLEncoder.encode(documentId, "UTF-8").replace("+", "%20")
    return "content://$auth/tree/$tree/document/$enc"
  }

  private fun oldUri(documentId: String) = docUri("OLD-TREE", documentId)
  private fun newUri(documentId: String) = docUri("NEW-TREE", documentId)

  private fun chapterRow(
    uri: String,
    duration: Long = 1_000,
  ) = Chapter(ChapterId(uri), name = "ch", duration = duration, fileLastModified = Instant.EPOCH, markData = emptyList())

  private fun bookRow(
    uri: String,
    chapterUris: List<String>,
    position: Long,
    lastPlayed: Long,
    active: Boolean = true,
  ) = BookContent(
    id = BookId(uri), playbackSpeed = 1.5f, skipSilence = true, isActive = active,
    lastPlayedAt = Instant.ofEpochMilli(lastPlayed), author = "A", name = "Book", addedAt = Instant.ofEpochMilli(10),
    chapters = chapterUris.map { ChapterId(it) }, currentChapter = ChapterId(chapterUris.first()),
    positionInChapter = position, cover = null, gain = 2f, genre = null, narrator = null, series = null, part = null,
  )

  /** Build a single-book snapshot fragment (book dto + chapter dtos) keyed to OLD URIs. */
  private fun snapshotBookOf(
    relPath: String,
    chapterRelNames: List<String>,
    currentRel: String,
    position: Long,
    lastPlayed: Long,
  ): Pair<BookContentDto, List<ChapterDto>> {
    val chapterUris = chapterRelNames.map { oldUri("$relPath/$it") }
    val book = bookRow(oldUri(relPath), chapterUris, position, lastPlayed)
      .copy(currentChapter = ChapterId(oldUri("$relPath/$currentRel")))
    val chapters = chapterUris.map { chapterRow(it) }
    // v3 stores no identity stamp; the restorer derives it from these URIs.
    val dto = book.toDto()
    val chapterDtos = chapters.map { ch ->
      ch.toDto(relName = DeviceRelativePath.relName(ch.id.value.toUri(), relPath))
    }
    return dto to chapterDtos
  }

  /** Simulate the re-scan inserting [relPath]'s book + chapters under NEW URIs (active, no progress). */
  private suspend fun scanInBook(
    relPath: String,
    chapterRelNames: List<String>,
    durations: List<Long> = chapterRelNames.map { 1_000L },
    lastPlayed: Long = 0,
    position: Long = 0,
  ) {
    val chapterUris = chapterRelNames.map { newUri("$relPath/$it") }
    chapterRelNames.forEachIndexed { i, rel -> db.chapterDao().insert(chapterRow(newUri("$relPath/$rel"), durations[i])) }
    contentRepo.put(bookRow(newUri(relPath), chapterUris, position, lastPlayed))
  }

  private fun snapshotOf(
    books: List<BookContentDto>,
    chapters: List<ChapterDto>,
    bookmarks: List<BookmarkDto> = emptyList(),
    hiddenBooks: Set<String> = emptySet(),
  ) = LibrarySnapshot(
    schemaVersion = LibrarySnapshot.SCHEMA_VERSION, dbVersion = AppDb.VERSION, sequence = 1, savedAtEpochMillis = 0,
    totalCount = books.size, activeCount = books.size, books = books, bookmarks = bookmarks,
    chapterNameOverrides = emptyList(), chapters = chapters,
    hiddenBooks = hiddenBooks,
  )

  @Test
  fun `re-keys a wiped book onto its new URIs, restoring position and bookmark, and never cross-attaches`() = runTest {
    val (dune, duneChapters) = snapshotBookOf(
      "primary:Books/Dune",
      listOf("01.mp3", "02.mp3"),
      "01.mp3",
      position = 400,
      lastPlayed = 5_000,
    )
    val (ghost, ghostChapters) = snapshotBookOf("primary:Books/Ghost", listOf("a.mp3"), "a.mp3", position = 10, lastPlayed = 5_000)
    val bookmark = BookmarkDto(
      bookId = oldUri("primary:Books/Dune"),
      chapterId = oldUri("primary:Books/Dune/02.mp3"),
      title = "bm",
      time = 50,
      addedAtEpochMillis = 1,
      setBySleepTimer = false,
      id = "00000000-0000-0000-0000-000000000009",
    )
    val snapshot = snapshotOf(listOf(dune, ghost), duneChapters + ghostChapters, bookmarks = listOf(bookmark))

    onScan = {
      scanInBook("primary:Books/Dune", listOf("01.mp3", "02.mp3")) // Dune re-appears under new URIs
      scanInBook("primary:Books/Other", listOf("x.mp3")) // an unrelated book with NO snapshot counterpart
    }

    val result = restorer().run(snapshot)

    // Dune matched; Ghost (not in scan) surfaced; Other (no snapshot) not touched.
    result.matched.map { it.content.id.value } shouldBe listOf(newUri("primary:Books/Dune"))
    result.unmatched.map { it.reason.name } shouldBe listOf("NO_PATH_MATCH")
    result.unmatched.single().relPath shouldBe "primary:Books/Ghost"

    val duneId = BookId(newUri("primary:Books/Dune"))
    val restored = db.bookContentDao().all().single { it.id == duneId }
    restored.positionInChapter shouldBe 400L
    restored.currentChapter shouldBe ChapterId(newUri("primary:Books/Dune/01.mp3"))

    // chapters2 exist under the new ids -> the book actually resolves (is visible).
    val bookRepo = BookRepositoryImpl(ChapterRepoImpl(db.chapterDao()), contentRepo)
    bookRepo.get(duneId).shouldNotBeNull()

    // the bookmark followed relName 02.mp3 onto the new chapter id, under the new book id.
    val bm = db.bookmarkDao().all().single()
    bm.chapterId shouldBe ChapterId(newUri("primary:Books/Dune/02.mp3"))
    bm.bookId shouldBe duneId

    // the unrelated scanned book was never given Dune's data.
    val other = db.bookContentDao().all().single { it.id == BookId(newUri("primary:Books/Other")) }
    other.positionInChapter shouldBe 0L
    db.bookmarkDao().all().none { it.bookId == other.id } shouldBe true
    // Ghost was never inserted under any URI.
    db.bookContentDao().all().none { it.id.value.contains("Ghost") } shouldBe true
  }

  @Test
  fun `an empty (no-permission) scan writes nothing and surfaces everything for retry`() = runTest {
    val (dune, duneChapters) = snapshotBookOf("primary:Books/Dune", listOf("01.mp3"), "01.mp3", position = 400, lastPlayed = 5_000)
    onScan = {} // re-grant not done yet -> scan finds nothing

    val result = restorer().run(snapshotOf(listOf(dune), duneChapters))

    result.matched.shouldBe(emptyList())
    result.unmatched.single().reason.name shouldBe "NO_PATH_MATCH"
    db.bookContentDao().all().shouldBe(emptyList())
  }

  @Test
  fun `keeps a fresher live position instead of the snapshot's`() = runTest {
    val (dune, duneChapters) = snapshotBookOf("primary:Books/Dune", listOf("01.mp3"), "01.mp3", position = 400, lastPlayed = 5_000)
    // The freshly-scanned row already has newer progress (e.g. the user listened before restoring).
    onScan = { scanInBook("primary:Books/Dune", listOf("01.mp3"), lastPlayed = 9_999, position = 800) }

    restorer().run(snapshotOf(listOf(dune), duneChapters))

    db.bookContentDao().all().single().positionInChapter shouldBe 800L
  }

  @Test
  fun `a book restored without its chapter rows would be invisible`() = runTest {
    // Guards the chapters2 requirement: withhold the scanned chapter rows -> book() must NOT resolve.
    val (dune, duneChapters) = snapshotBookOf("primary:Books/Dune", listOf("01.mp3"), "01.mp3", position = 400, lastPlayed = 5_000)
    onScan = {
      // insert the book but NOT its chapter rows
      contentRepo.put(bookRow(newUri("primary:Books/Dune"), listOf(newUri("primary:Books/Dune/01.mp3")), position = 0, lastPlayed = 0))
    }

    restorer().run(snapshotOf(listOf(dune), duneChapters))

    val bookRepo = BookRepositoryImpl(ChapterRepoImpl(db.chapterDao()), contentRepo)
    bookRepo.get(BookId(newUri("primary:Books/Dune"))).shouldBeNull()
  }

  @Test
  fun `corrected episode numbers follow the book to its new id`() = runTest {
    val (dune, duneChapters) = snapshotBookOf("primary:Books/Dune", listOf("01.mp3"), "01.mp3", position = 400, lastPlayed = 5_000)
    onScan = { scanInBook("primary:Books/Dune", listOf("01.mp3")) }

    restorer().run(snapshotOf(listOf(dune), duneChapters).copy(episodeNumbers = mapOf(oldUri("primary:Books/Dune") to 7)))

    episodeNumbers.numbers shouldBe mapOf(BookId(newUri("primary:Books/Dune")) to 7)
  }

  @Test
  fun `hidden books are re-keyed with their data and stay hidden under their new ids`() = runTest {
    val (dune, duneChapters) = snapshotBookOf("primary:Books/Dune", listOf("01.mp3"), "01.mp3", position = 400, lastPlayed = 5_000)
    val hiddenOldId = oldUri("primary:Books/Dune")
    // Same-URI exclusion also present (the repo pre-unions on some paths) — must not block participation.
    excluded.updateData { setOf(hiddenOldId) }
    onScan = { scanInBook("primary:Books/Dune", listOf("01.mp3")) }

    val result = restorer().run(snapshotOf(listOf(dune), duneChapters, hiddenBooks = setOf(hiddenOldId)))

    result.matched.map { it.sourceId } shouldContainExactlyInAnyOrder listOf(hiddenOldId)
    val newId = newUri("primary:Books/Dune")
    // The book's data was restored under the new id...
    db.bookContentDao().all().single { it.id.value == newId }.let {
      it.positionInChapter shouldBe 400L
      it.isActive shouldBe false
    }
    // ...and the hidden set now covers the NEW id, so it does not resurface visible.
    excluded.data.first().contains(newId) shouldBe true
  }

  @Test
  fun `the restore keeps the cover the scan just extracted instead of clobbering it`() = runTest {
    val (dune, duneChapters) = snapshotBookOf("primary:Books/Dune", listOf("01.mp3"), "01.mp3", position = 400, lastPlayed = 5_000)
    val scannedCover = java.io.File("/data/fresh-install/bookCovers/dune.png")
    onScan = {
      scanInBook("primary:Books/Dune", listOf("01.mp3"))
      val scanned = db.bookContentDao().all().single()
      contentRepo.put(scanned.copy(cover = scannedCover))
    }

    restorer().run(snapshotOf(listOf(dune), duneChapters))

    db.bookContentDao().all().single().cover shouldBe scannedCover
  }

  @Test
  fun `portable custom cover and title follow a matched book onto its new URI`() = runTest {
    val (book, chapters) = snapshotBookOf("primary:Books/Dune", listOf("01.mp3"), "01.mp3", position = 400, lastPlayed = 5_000)
    val image = java.io.File("restored-custom.png").absoluteFile
    onScan = { scanInBook("primary:Books/Dune", listOf("01.mp3")) }
    restorer().run(snapshotOf(listOf(book.copy(name = "My title")), chapters), mapOf(book.id to image))
    val restored = db.bookContentDao().all().single()
    restored.id.value shouldBe newUri("primary:Books/Dune")
    restored.name shouldBe "My title"
    restored.cover shouldBe image
    restored.positionInChapter shouldBe 400
  }

  @Test
  fun `portable metadata wins while newer scanned playback survives`() = runTest {
    val (book, chapters) = snapshotBookOf(
      "primary:Books/Dune",
      listOf("01.mp3"),
      "01.mp3",
      position = 400,
      lastPlayed = 5_000,
    )
    val image = java.io.File("restored-custom.png").absoluteFile
    onScan = {
      scanInBook("primary:Books/Dune", listOf("01.mp3"), lastPlayed = 9_999, position = 700)
    }
    restorer().run(
      snapshotOf(listOf(book.copy(name = "Restored title")), chapters),
      mapOf(book.id to image),
    )
    val restored = db.bookContentDao().all().single()
    restored.name shouldBe "Restored title"
    restored.cover shouldBe image
    restored.positionInChapter shouldBe 700
    restored.lastPlayedAt shouldBe Instant.ofEpochMilli(9_999)
  }
}
