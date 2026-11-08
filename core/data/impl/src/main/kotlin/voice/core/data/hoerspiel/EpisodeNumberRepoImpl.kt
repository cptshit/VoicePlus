package voice.core.data.hoerspiel

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import voice.core.data.BookId

@ContributesBinding(AppScope::class)
public class EpisodeNumberRepoImpl(private val dao: EpisodeNumberOverrideDao) : EpisodeNumberRepo {

  override suspend fun get(bookId: BookId): Int? = dao.get(bookId.value)

  override suspend fun all(): Map<BookId, Int> = dao.all().associate { BookId(it.bookId) to it.number }

  override suspend fun set(
    bookId: BookId,
    number: Int?,
  ) {
    if (number == null) dao.delete(bookId.value) else dao.put(EpisodeNumberOverride(bookId.value, number))
  }
}
