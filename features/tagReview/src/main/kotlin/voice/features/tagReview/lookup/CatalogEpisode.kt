package voice.features.tagReview.lookup

import kotlinx.serialization.Serializable

/** One episode of a series as an online catalogue lists it. */
@Serializable
data class CatalogEpisode(
  val number: Int,
  val title: String,
)

/** Where a catalogue answer came from; the names match `TagFindingSource` and `CatalogCache.source`. */
enum class CatalogSource { MusicBrainz, ITunes }

/** Lower-case letters and digits only; "…Dem Winter folgte der Herbst" and "dem winter folgte der herbst." are alike. */
internal fun String.likeness(): String = lowercase().filter(Char::isLetterOrDigit)

/** Catalogue titles sometimes start with an ellipsis ("... dem Winter folgte der Herbst"). */
internal fun String.withoutLeadingEllipsis(): String = trim().trimStart('.', '…', ' ').trim()
