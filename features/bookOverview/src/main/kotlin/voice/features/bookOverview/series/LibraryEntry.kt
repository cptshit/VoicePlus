package voice.features.bookOverview.series

import androidx.compose.runtime.State
import voice.core.data.hoerspiel.CoverStyle
import voice.features.bookOverview.overview.BookOverviewItemViewState
import voice.features.bookOverview.views.aspectRatio

internal data class LibraryEntry(
  val seriesName: String?,
  val books: List<State<BookOverviewItemViewState>>,
  val sectionId: String,
  val itemId: String,
  val seriesId: String? = null,
  val allBooks: List<State<BookOverviewItemViewState>> = books,
) {
  val key: String get() = "$sectionId:$itemId"
  val title: String get() = seriesName ?: books.first().value.name

  /** Width / height of the covers in this entry, see [CoverStyle.aspectRatio]. */
  val coverAspectRatio: Float get() = (books.firstOrNull() ?: allBooks.firstOrNull())?.value?.coverStyle?.aspectRatio
    ?: CoverStyle.Cd.aspectRatio
}
