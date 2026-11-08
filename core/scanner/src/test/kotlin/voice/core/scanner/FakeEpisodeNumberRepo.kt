package voice.core.scanner

import voice.core.data.BookId
import voice.core.data.hoerspiel.EpisodeNumberRepo

internal class FakeEpisodeNumberRepo : EpisodeNumberRepo {

  val numbers = mutableMapOf<BookId, Int>()

  override suspend fun get(bookId: BookId): Int? = numbers[bookId]

  override suspend fun all(): Map<BookId, Int> = numbers.toMap()

  override suspend fun set(
    bookId: BookId,
    number: Int?,
  ) {
    if (number == null) numbers.remove(bookId) else numbers[bookId] = number
  }
}
