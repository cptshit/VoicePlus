package voice.core.data.hoerspiel

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import voice.core.data.BookId

@ContributesBinding(AppScope::class)
public class SeriesAutoGroupingRepoImpl(private val dao: SeriesAutoGroupingDao) : SeriesAutoGroupingRepo {

  override suspend fun handled(): Set<BookId> = dao.all().map { BookId(it.bookId) }.toSet()

  override suspend fun markHandled(books: Set<BookId>) {
    if (books.isNotEmpty()) dao.insert(books.map { SeriesAutoGrouped(it.value) })
  }
}
