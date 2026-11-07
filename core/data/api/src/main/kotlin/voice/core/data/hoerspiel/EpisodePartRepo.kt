package voice.core.data.hoerspiel

import kotlinx.coroutines.flow.Flow
import voice.core.data.BookId

public interface EpisodePartRepo {

  public fun flow(): Flow<Map<BookId, EpisodePart>>

  public suspend fun all(): List<EpisodePart>

  /** Stores the [part] of [bookId], or removes it when `null`. Keeps a known part count if the part is unchanged. */
  public suspend fun setPart(
    bookId: BookId,
    part: Int?,
  )

  /** Sets the part counts; books missing from [counts] get `null`. */
  public suspend fun setPartCounts(counts: Map<BookId, Int>)
}
