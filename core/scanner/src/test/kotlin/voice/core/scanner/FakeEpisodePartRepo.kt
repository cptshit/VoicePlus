package voice.core.scanner

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import voice.core.data.BookId
import voice.core.data.hoerspiel.EpisodePart
import voice.core.data.hoerspiel.EpisodePartRepo

internal class FakeEpisodePartRepo : EpisodePartRepo {

  val parts = MutableStateFlow<Map<BookId, EpisodePart>>(emptyMap())

  override fun flow(): Flow<Map<BookId, EpisodePart>> = parts

  override suspend fun all(): List<EpisodePart> = parts.value.values.toList()

  override suspend fun setPart(
    bookId: BookId,
    part: Int?,
  ) {
    val existing = parts.value[bookId]
    parts.value = when {
      part == null -> parts.value - bookId
      existing?.part == part -> parts.value
      else -> parts.value + (bookId to EpisodePart(bookId.value, part))
    }
  }

  override suspend fun setPartCounts(counts: Map<BookId, Int>) {
    parts.value = parts.value.mapValues { (id, part) -> part.copy(partCount = counts[id]) }
  }
}
