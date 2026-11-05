package voice.core.data.hoerspiel

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.Flow
import voice.core.data.BookId

@ContributesBinding(AppScope::class)
public class TagFindingRepoImpl(private val dao: TagFindingDao) : TagFindingRepo {

  override fun flow(): Flow<List<TagFinding>> = dao.all()

  override fun flow(bookId: BookId): Flow<List<TagFinding>> = dao.forBook(bookId.value)

  override suspend fun replaceForBook(
    bookId: BookId,
    findings: List<TagFinding>,
  ) {
    dao.replaceForBook(bookId.value, findings)
  }

  override suspend fun setDismissed(
    id: Long,
    dismissed: Boolean,
  ) {
    dao.setDismissed(id, dismissed)
  }
}
