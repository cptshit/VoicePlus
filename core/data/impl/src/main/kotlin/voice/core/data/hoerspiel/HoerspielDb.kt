package voice.core.data.hoerspiel

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Database for data that only exists in this fork. Kept apart from AppDb so upstream can keep
 * bumping AppDb versions without colliding with fork-only tables.
 */
@Database(
  entities = [TagFinding::class, EpisodePart::class, SeriesAutoGrouped::class, EpisodeNumberOverride::class, CatalogCache::class],
  version = HoerspielDb.VERSION,
  autoMigrations = [
    AutoMigration(from = 1, to = 2),
    AutoMigration(from = 2, to = 3),
    AutoMigration(from = 3, to = 4),
    AutoMigration(from = 4, to = 5),
  ],
)
public abstract class HoerspielDb : RoomDatabase() {

  public abstract fun tagFindingDao(): TagFindingDao

  public abstract fun episodePartDao(): EpisodePartDao

  public abstract fun seriesAutoGroupingDao(): SeriesAutoGroupingDao

  public abstract fun episodeNumberOverrideDao(): EpisodeNumberOverrideDao

  public abstract fun catalogCacheDao(): CatalogCacheDao

  public companion object {
    public const val VERSION: Int = 5
    public const val DATABASE_NAME: String = "hoerspiel.db"
  }
}
