package voice.core.data.hoerspiel

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The answer of an online catalogue for one series, kept so the same series is not requested again.
 * [json] is the parsed list of catalogue episodes, not the raw response.
 */
@Entity(tableName = "catalog_cache")
public data class CatalogCache(
  /** Lower-case series name. */
  @PrimaryKey val series: String,
  val source: String,
  val json: String,
  val fetchedAtEpochMillis: Long,
)
