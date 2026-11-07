package voice.core.scanner

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.documentfile.provider.DocumentFile
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import voice.core.data.Book
import voice.core.data.store.IgnoreFileTagsStore
import voice.core.data.toUri
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.HoerspielNormalizerFeatureFlagQualifier
import voice.core.logging.api.Logger
import java.io.IOException

@Inject
internal class CoverScanner(
  private val context: Context,
  private val coverSaver: CoverSaver,
  private val coverExtractor: CoverExtractor,
  @IgnoreFileTagsStore
  private val ignoreFileTagsStore: DataStore<Boolean>,
  @HoerspielNormalizerFeatureFlagQualifier
  private val hoerspielNormalizer: FeatureFlag<Boolean>,
) {

  suspend fun scan(books: List<Book>) {
    books.forEach { findCoverForBook(it) }
  }

  private suspend fun findCoverForBook(book: Book) {
    val coverFile = book.content.cover
    if (coverFile != null && coverFile.exists()) {
      return
    }

    val bookFolder = CoverLocations.folder(context, book.id.toUri())
    if (bookFolder != null && saveFirstImage(book, bookFolder)) {
      return
    }

    if (!ignoreFileTagsStore.data.first() && scanForEmbeddedCover(book)) {
      return
    }

    // Episodes of a series sometimes share one cover one folder up, or an episode's cover ended
    // up there when its folder was copied. Only for series, so a stray image in the library
    // root does not become the cover of every book without one.
    if (bookFolder != null && !book.content.series.isNullOrBlank() && hoerspielNormalizer.get()) {
      val parent = CoverLocations.parentFolder(book.id.toUri())?.let { CoverLocations.folder(context, it) }
      if (parent != null && !saveFirstImage(book, parent)) Logger.d("No cover found for ${book.id}")
    }
  }

  private suspend fun saveFirstImage(
    book: Book,
    folder: DocumentFile,
  ): Boolean = withContext(Dispatchers.IO) {
    val images = folder.listFiles().filter { child ->
      child.isFile && child.canRead() && child.type?.startsWith("image/") == true
    }
    CoverLocations.sortImages(images) { it.name }.forEach { child ->
      val coverFile = coverSaver.newBookCoverFile()
      val worked = try {
        context.contentResolver.openInputStream(child.uri)?.use { input ->
          coverFile.outputStream().use { output ->
            input.copyTo(output)
          }
        } != null
      } catch (e: IOException) {
        Logger.w(e, "Error while copying the cover from ${child.uri}")
        false
      } catch (e: IllegalStateException) {
        // On some Samsung Devices, openInputStream throws this exception, though it should not.
        Logger.w(e, "Error while copying the cover from ${child.uri}")
        false
      }
      if (worked) {
        if (!coverSaver.setBookCoverIfMissing(coverFile, book.id)) coverFile.delete()
        return@withContext true
      }
      coverFile.delete()
    }
    false
  }

  private suspend fun scanForEmbeddedCover(book: Book): Boolean {
    val coverFile = coverSaver.newBookCoverFile()
    book.chapters
      .take(5).forEach { chapter ->
        val success = coverExtractor.extractCover(
          input = chapter.id.toUri(),
          outputFile = coverFile,
        )
        if (success && coverFile.exists() && coverFile.length() > 0) {
          if (!coverSaver.setBookCoverIfMissing(coverFile, book.id)) coverFile.delete()
          return true
        }
      }
    coverFile.delete()
    return false
  }
}
