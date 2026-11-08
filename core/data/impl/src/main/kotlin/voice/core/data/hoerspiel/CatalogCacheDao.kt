package voice.core.data.hoerspiel

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
public interface CatalogCacheDao {

  @Query("SELECT * FROM catalog_cache WHERE series = :series")
  public suspend fun get(series: String): CatalogCache?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public suspend fun put(cache: CatalogCache)
}
