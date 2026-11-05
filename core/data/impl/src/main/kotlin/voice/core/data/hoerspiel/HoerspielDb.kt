package voice.core.data.hoerspiel

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Database for data that only exists in this fork. Kept apart from AppDb so upstream can keep
 * bumping AppDb versions without colliding with fork-only tables.
 */
@Database(
  entities = [TagFinding::class, EpisodePart::class],
  version = HoerspielDb.VERSION,
  autoMigrations = [
    AutoMigration(from = 1, to = 2),
  ],
)
public abstract class HoerspielDb : RoomDatabase() {

  public abstract fun tagFindingDao(): TagFindingDao

  public abstract fun episodePartDao(): EpisodePartDao

  public companion object {
    public const val VERSION: Int = 2
    public const val DATABASE_NAME: String = "hoerspiel.db"
  }
}
