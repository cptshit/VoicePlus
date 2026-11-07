package voice.core.data.hoerspiel

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
public interface SeriesAutoGroupingDao {

  @Query("SELECT * FROM series_auto_grouped")
  public suspend fun all(): List<SeriesAutoGrouped>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  public suspend fun insert(rows: List<SeriesAutoGrouped>)
}
