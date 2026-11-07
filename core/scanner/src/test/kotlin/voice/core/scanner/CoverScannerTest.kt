package voice.core.scanner

import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.featureflag.MemoryFeatureFlag
import java.io.File
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class CoverScannerTest {

  @get:Rule val temporaryFolder = TemporaryFolder()

  /** Which image ended up as the cover, per book. */
  private val covers = mutableMapOf<BookId, String>()
  private lateinit var library: File

  private val coverSaver = mockk<CoverSaver> {
    coEvery { newBookCoverFile() } answers { File.createTempFile("cover", ".jpg", temporaryFolder.root) }
    coEvery { setBookCoverIfMissing(any(), any()) } answers {
      covers[secondArg()] = firstArg<File>().readText()
      true
    }
  }
  private val scanner = CoverScanner(
    context = ApplicationProvider.getApplicationContext(),
    coverSaver = coverSaver,
    coverExtractor = mockk { coEvery { extractCover(any(), any()) } returns false },
    ignoreFileTagsStore = mockk { every { data } returns MutableStateFlow(false) },
    hoerspielNormalizer = MemoryFeatureFlag(true),
  )

  @Before
  fun setUp() {
    // Robolectric's MimeTypeMap is empty, and DocumentFile.fromFile derives the type from it.
    @Suppress("DEPRECATION")
    val mimeTypes = shadowOf(MimeTypeMap.getSingleton())
    mimeTypes.apply {
      addExtensionMimeTypeMapping("jpg", "image/jpeg")
      addExtensionMimeTypeMapping("png", "image/png")
      addExtensionMimeTypeMapping("mp3", "audio/mpeg")
    }
    library = temporaryFolder.newFolder("Hörspiele")
  }

  private fun episode(
    folder: String,
    series: String? = "Gabriel Burns",
    vararg images: String,
  ): Book {
    val dir = File(library, folder).also { it.mkdirs() }
    val track = File(dir, "01 - Kapitel 01.mp3").also { it.writeText("audio") }
    images.forEach { File(dir, it).writeText("$folder/$it") }
    val chapter = Chapter(ChapterId(track.toUri()), "Kapitel 01", 1000L, Instant.EPOCH, emptyList())
    val content = BookContent(
      id = BookId(dir.toUri()),
      playbackSpeed = 1F,
      skipSilence = false,
      isActive = true,
      lastPlayedAt = Instant.EPOCH,
      author = null,
      name = folder,
      addedAt = Instant.EPOCH,
      chapters = listOf(chapter.id),
      currentChapter = chapter.id,
      positionInChapter = 0,
      cover = null,
      gain = 0F,
      genre = null,
      narrator = null,
      series = series,
      part = null,
    )
    return Book(content, listOf(chapter))
  }

  @Test
  fun episodeWithoutCoverTakesTheOneOneFolderUp() = runTest {
    File(library, "Cover.jpg").writeText("library/Cover.jpg")
    val first = episode("Gabriel.Burns.01.-.Der.Fluesterer")
    val second = episode("GabrielBurns - 002 - Die Brut", images = arrayOf("Cover.jpg"))

    scanner.scan(listOf(first, second))

    covers shouldBe mapOf(
      first.id to "library/Cover.jpg",
      second.id to "GabrielBurns - 002 - Die Brut/Cover.jpg",
    )
  }

  @Test
  fun booksWithoutSeriesDoNotTakeTheParentImage() = runTest {
    File(library, "Cover.jpg").writeText("library/Cover.jpg")
    val book = episode("Der Zauberberg", series = null)

    scanner.scan(listOf(book))

    covers shouldBe emptyMap()
  }

  @Test
  fun coverNamedImageWinsOverOtherImages() = runTest {
    val book = episode("GabrielBurns - 003 - Experiment Stille", images = arrayOf("back.jpg", "booklet.png", "Folder.jpg"))

    scanner.scan(listOf(book))

    covers shouldBe mapOf(book.id to "GabrielBurns - 003 - Experiment Stille/Folder.jpg")
  }
}
