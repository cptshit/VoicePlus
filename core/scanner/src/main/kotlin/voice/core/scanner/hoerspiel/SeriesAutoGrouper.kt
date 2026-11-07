package voice.core.scanner.hoerspiel

import dev.zacsweers.metro.Inject
import voice.core.data.LibraryEditException
import voice.core.data.hoerspiel.SeriesAutoGroupingRepo
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.LibraryOrganisationRepo
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.HoerspielNormalizerFeatureFlagQualifier
import voice.core.logging.api.Logger

/** Applies [planAutoSeries] after a scan, when all episodes of a series are known. */
@Inject
internal class SeriesAutoGrouper(
  private val contentRepo: BookContentRepo,
  private val organisationRepo: LibraryOrganisationRepo,
  private val autoGroupingRepo: SeriesAutoGroupingRepo,
  @HoerspielNormalizerFeatureFlagQualifier
  private val hoerspielNormalizer: FeatureFlag<Boolean>,
) {

  suspend fun groupNewEpisodes() {
    if (!hoerspielNormalizer.get()) return
    val plan = planAutoSeries(contentRepo.all(), organisationRepo.get(), autoGroupingRepo.handled())
    val failed = plan.saves.flatMap { save ->
      try {
        organisationRepo.saveSeries(save.seriesId, save.name, save.books, save.shelfId)
        emptyList()
      } catch (e: LibraryEditException) {
        // e.g. the user created a series with this name in the meantime: try again next scan
        Logger.w(e, "Could not group ${save.name} automatically")
        save.books
      }
    }
    autoGroupingRepo.markHandled(plan.handled - failed.toSet())
  }
}
