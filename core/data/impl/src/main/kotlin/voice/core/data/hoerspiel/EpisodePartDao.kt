package voice.core.data.hoerspiel

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
public interface EpisodePartDao {

  @Query("SELECT * FROM episode_parts")
  public fun flow(): Flow<List<EpisodePart>>

  @Query("SELECT * FROM episode_parts")
  public suspend fun all(): List<EpisodePart>

  @Query("SELECT * FROM episode_parts WHERE bookId = :bookId")
  public suspend fun get(bookId: String): EpisodePart?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public suspend fun put(parts: List<EpisodePart>)

  @Query("DELETE FROM episode_parts WHERE bookId = :bookId")
  public suspend fun delete(bookId: String)

  @Transaction
  public suspend fun setPart(
    bookId: String,
    part: Int?,
  ) {
    val existing = get(bookId)
    when {
      part == null -> delete(bookId)
      existing?.part != part -> put(listOf(EpisodePart(bookId, part)))
    }
  }

  @Transaction
  public suspend fun setPartCounts(counts: Map<String, Int>) {
    val changed = all().mapNotNull { stored ->
      val count = counts[stored.bookId]
      if (count != stored.partCount) stored.copy(partCount = count) else null
    }
    if (changed.isNotEmpty()) put(changed)
  }
}
