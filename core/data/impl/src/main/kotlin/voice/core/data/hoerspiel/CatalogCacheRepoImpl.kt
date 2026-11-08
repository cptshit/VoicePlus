package voice.core.data.hoerspiel

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

@ContributesBinding(AppScope::class)
public class CatalogCacheRepoImpl(private val dao: CatalogCacheDao) : CatalogCacheRepo {

  override suspend fun get(series: String): CatalogCache? = dao.get(series.trim().lowercase())

  override suspend fun put(cache: CatalogCache) {
    dao.put(cache.copy(series = cache.series.trim().lowercase()))
  }
}
