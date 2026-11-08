package voice.core.data.hoerspiel

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
public interface EpisodeNumberOverrideDao {

  @Query("SELECT number FROM episode_number_overrides WHERE bookId = :bookId")
  public suspend fun get(bookId: String): Int?

  @Query("SELECT * FROM episode_number_overrides")
  public suspend fun all(): List<EpisodeNumberOverride>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public suspend fun put(override: EpisodeNumberOverride)

  @Query("DELETE FROM episode_number_overrides WHERE bookId = :bookId")
  public suspend fun delete(bookId: String)
}
