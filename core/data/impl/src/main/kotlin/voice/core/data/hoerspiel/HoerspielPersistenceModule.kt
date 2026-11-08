package voice.core.data.hoerspiel

import android.content.Context
import androidx.room.Room
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
public interface HoerspielPersistenceModule {

  // Deliberately not bound as RoomDatabase and not fed the AppDb migration set.
  @Provides
  @SingleIn(AppScope::class)
  private fun hoerspielDb(context: Context): HoerspielDb {
    return Room.databaseBuilder(context, HoerspielDb::class.java, HoerspielDb.DATABASE_NAME).build()
  }

  @Provides
  private fun tagFindingDao(db: HoerspielDb): TagFindingDao = db.tagFindingDao()

  @Provides
  private fun episodePartDao(db: HoerspielDb): EpisodePartDao = db.episodePartDao()

  @Provides
  private fun seriesAutoGroupingDao(db: HoerspielDb): SeriesAutoGroupingDao = db.seriesAutoGroupingDao()

  @Provides
  private fun episodeNumberOverrideDao(db: HoerspielDb): EpisodeNumberOverrideDao = db.episodeNumberOverrideDao()
}
