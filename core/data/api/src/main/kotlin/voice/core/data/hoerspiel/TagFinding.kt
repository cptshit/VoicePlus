package voice.core.data.hoerspiel

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Which part of an episode a [TagFinding] is about. */
public enum class TagFindingField { Series, EpisodeNumber, EpisodeTitle }

/** Where a value came from. */
public enum class TagFindingSource {
  SeriesTag,
  PartTag,
  FolderName,
  AlbumTag,
  ArtistTag,
  FileName,

  /** The value the library holds now, when the other value comes from an online catalogue. */
  Local,

  /** Suggestions from the optional online comparison; they are kept when a book is re-scanned. */
  MusicBrainz,
  ITunes,
  ;

  public val isOnline: Boolean get() = this == MusicBrainz || this == ITunes
}

public enum class TagFindingKind {
  /** Same text apart from case, punctuation, spacing or umlaut spelling. */
  Formatting,

  /** One value extends the other, e.g. by "[Teil 2]" or "Directors Cut". */
  Extended,

  /** The values really differ. */
  Different,
}

/**
 * A disagreement between two sources of an audio drama episode's metadata, found while scanning.
 * The scanner used [chosenValue]; [otherValue] is what it dropped. Meant for a "check tags" screen
 * where the user can fix the book or mark the finding as fine ([dismissed]).
 *
 * Stored in its own database (not AppDb), so this fork's schema never collides with upstream
 * migrations.
 */
@Entity(tableName = "tag_findings", indices = [Index("bookId")])
public data class TagFinding(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  /** BookId.value */
  val bookId: String,
  val field: TagFindingField,
  val kind: TagFindingKind,
  val chosenValue: String,
  val chosenSource: TagFindingSource,
  val otherValue: String,
  val otherSource: TagFindingSource,
  val dismissed: Boolean = false,
) {

  /** Identity of a finding across scans, independent of [id] and [dismissed]. */
  public fun sameFindingAs(other: TagFinding): Boolean {
    return bookId == other.bookId &&
      field == other.field &&
      chosenValue == other.chosenValue &&
      chosenSource == other.chosenSource &&
      otherValue == other.otherValue &&
      otherSource == other.otherSource
  }
}
