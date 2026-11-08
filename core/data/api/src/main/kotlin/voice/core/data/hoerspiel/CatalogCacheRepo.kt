package voice.core.data.hoerspiel

public interface CatalogCacheRepo {

  public suspend fun get(series: String): CatalogCache?

  public suspend fun put(cache: CatalogCache)
}
