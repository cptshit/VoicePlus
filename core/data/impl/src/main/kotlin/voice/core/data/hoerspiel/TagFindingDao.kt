package voice.core.data.hoerspiel

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
public interface TagFindingDao {

  @Query("SELECT * FROM tag_findings ORDER BY bookId, id")
  public fun all(): Flow<List<TagFinding>>

  @Query("SELECT * FROM tag_findings WHERE bookId = :bookId ORDER BY id")
  public fun forBook(bookId: String): Flow<List<TagFinding>>

  @Query("SELECT * FROM tag_findings WHERE bookId = :bookId")
  public suspend fun forBookOnce(bookId: String): List<TagFinding>

  /** Only the scanner's findings: online suggestions survive a re-scan. */
  @Query("DELETE FROM tag_findings WHERE bookId = :bookId AND otherSource NOT IN ('MusicBrainz', 'ITunes')")
  public suspend fun deleteScannedForBook(bookId: String)

  @Query("DELETE FROM tag_findings WHERE bookId = :bookId AND otherSource IN ('MusicBrainz', 'ITunes')")
  public suspend fun deleteOnlineForBook(bookId: String)

  @Insert
  public suspend fun insert(findings: List<TagFinding>)

  @Query("UPDATE tag_findings SET dismissed = :dismissed WHERE id = :id")
  public suspend fun setDismissed(
    id: Long,
    dismissed: Boolean,
  )

  @Transaction
  public suspend fun replaceForBook(
    bookId: String,
    findings: List<TagFinding>,
  ) {
    val dismissed = forBookOnce(bookId).filter { it.dismissed && !it.otherSource.isOnline }
    deleteScannedForBook(bookId)
    insert(
      findings.map { finding ->
        val normalized = finding.copy(id = 0, bookId = bookId, dismissed = false)
        normalized.copy(dismissed = dismissed.any { it.sameFindingAs(normalized) })
      },
    )
  }

  @Transaction
  public suspend fun replaceOnlineForBook(
    bookId: String,
    findings: List<TagFinding>,
  ) {
    val dismissed = forBookOnce(bookId).filter { it.dismissed && it.otherSource.isOnline }
    deleteOnlineForBook(bookId)
    insert(
      findings.map { finding ->
        val normalized = finding.copy(id = 0, bookId = bookId, dismissed = false)
        normalized.copy(dismissed = dismissed.any { it.sameFindingAs(normalized) })
      },
    )
  }
}
