package voice.core.data.hoerspiel

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import voice.core.data.BookId

@ContributesBinding(AppScope::class)
public class EpisodePartRepoImpl(private val dao: EpisodePartDao) : EpisodePartRepo {

  override fun flow(): Flow<Map<BookId, EpisodePart>> = dao.flow()
    .map { parts -> parts.associateBy { BookId(it.bookId) } }
    .distinctUntilChanged()

  override suspend fun all(): List<EpisodePart> = dao.all()

  override suspend fun setPart(
    bookId: BookId,
    part: Int?,
  ) {
    dao.setPart(bookId.value, part)
  }

  override suspend fun setPartCounts(counts: Map<BookId, Int>) {
    dao.setPartCounts(counts.mapKeys { it.key.value })
  }
}
