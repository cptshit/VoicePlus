package voice.core.data.hoerspiel

import voice.core.data.BookId

public interface SeriesAutoGroupingRepo {

  public suspend fun handled(): Set<BookId>

  public suspend fun markHandled(books: Set<BookId>)
}
