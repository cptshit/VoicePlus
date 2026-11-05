package voice.core.data.hoerspiel

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An episode that is one part of a multi-part story, e.g. "Ohne Bewusstsein" as part 1 of 2.
 * The scanner cuts "Teil 1" off the title and stores it here.
 */
@Entity(tableName = "episode_parts")
public data class EpisodePart(
  @PrimaryKey val bookId: String,
  val part: Int,
  /** Number of parts of the story, `null` while the other parts are unknown. */
  val partCount: Int? = null,
)
