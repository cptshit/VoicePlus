package voice.core.scanner

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile

/** Where the cover search looks for image files, and in which order. */
internal object CoverLocations {

  private val PREFERRED_NAMES = setOf("cover", "folder", "front")

  /** `Cover.jpg`, `folder.png` and `front.jpg` before any other image, then by name, so the pick is stable. */
  fun <T> sortImages(
    images: List<T>,
    name: (T) -> String?,
  ): List<T> = images.sortedWith(
    compareBy<T> { image ->
      val base = name(image).orEmpty().substringBeforeLast('.').lowercase()
      if (base in PREFERRED_NAMES) 0 else 1
    }.thenBy { name(it).orEmpty().lowercase() },
  )

  /**
   * The document id of the folder containing [documentId], as long as it is still inside the
   * granted tree [treeDocumentId]. Relies on path-like ids (`primary:Hörspiele/Folge 1`) as
   * the local storage providers use them; any other id yields `null`.
   */
  fun parentDocumentId(
    treeDocumentId: String,
    documentId: String,
  ): String? {
    if (documentId == treeDocumentId) return null
    val parent = documentId.substringBeforeLast('/', missingDelimiterValue = "")
    if (parent.isEmpty()) return null
    return parent.takeIf { it == treeDocumentId || it.startsWith("${treeDocumentId.trimEnd('/')}/") }
  }

  /** The folder a book folder lives in, or `null` when that would leave the folders the user granted. */
  fun parentFolder(bookFolder: Uri): Uri? {
    return when (bookFolder.scheme) {
      "file" -> bookFolder.toFile().parentFile?.toUri()
      "content" -> {
        if (!DocumentsContract.isTreeUri(bookFolder)) return null
        val tree = DocumentsContract.getTreeDocumentId(bookFolder)
        val document = DocumentsContract.getDocumentId(bookFolder)
        parentDocumentId(tree, document)?.let { DocumentsContract.buildDocumentUriUsingTree(bookFolder, it) }
      }
      else -> null
    }
  }

  fun folder(
    context: Context,
    uri: Uri,
  ): DocumentFile? {
    return try {
      if (uri.scheme == "file") DocumentFile.fromFile(uri.toFile()) else DocumentFile.fromTreeUri(context, uri)
    } catch (_: IllegalArgumentException) {
      null
    }?.takeIf { it.isDirectory }
  }
}
