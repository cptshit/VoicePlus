package voice.core.data.hoerspiel

import voice.core.data.BookId

public interface EpisodeNumberRepo {

  /** The number chosen by the user for [bookId], or `null` if the scanned number applies. */
  public suspend fun get(bookId: BookId): Int?

  /** Stores the [number] of [bookId], or removes the override when `null`. */
  public suspend fun set(
    bookId: BookId,
    number: Int?,
  )
}
