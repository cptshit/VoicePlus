package voice.core.data.hoerspiel

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An episode number chosen by the user. The scanner re-reads the number from tags and file names
 * on every scan; an override keeps the corrected number across re-scans.
 */
@Entity(tableName = "episode_number_overrides")
public data class EpisodeNumberOverride(
  @PrimaryKey val bookId: String,
  val number: Int,
)
