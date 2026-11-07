package voice.core.scanner

import voice.core.data.BookId
import voice.core.data.hoerspiel.SeriesAutoGroupingRepo

internal class FakeSeriesAutoGroupingRepo : SeriesAutoGroupingRepo {

  val marked = mutableSetOf<BookId>()

  override suspend fun handled(): Set<BookId> = marked.toSet()

  override suspend fun markHandled(books: Set<BookId>) {
    marked += books
  }
}
