package voice.core.data.hoerspiel

import kotlinx.coroutines.flow.Flow
import voice.core.data.BookId

public interface TagFindingRepo {

  /** All findings of all books, including dismissed ones. */
  public fun flow(): Flow<List<TagFinding>>

  public fun flow(bookId: BookId): Flow<List<TagFinding>>

  /**
   * Replaces the findings of [bookId] with [findings] (their [TagFinding.id] and
   * [TagFinding.dismissed] are ignored). A finding the user already dismissed stays dismissed as
   * long as it is found again unchanged.
   */
  public suspend fun replaceForBook(
    bookId: BookId,
    findings: List<TagFinding>,
  )

  public suspend fun setDismissed(
    id: Long,
    dismissed: Boolean,
  )
}
