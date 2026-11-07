package voice.core.data.hoerspiel

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A book the scanner already considered for automatic series grouping. It is never grouped
 * automatically again, so taking a book out of a series by hand sticks.
 */
@Entity(tableName = "series_auto_grouped")
public data class SeriesAutoGrouped(@PrimaryKey val bookId: String)
