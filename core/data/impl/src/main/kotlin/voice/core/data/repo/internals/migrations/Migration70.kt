package voice.core.data.repo.internals.migrations

import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec

/** The character list feature was removed; its table and data are dropped. */
@DeleteTable(tableName = "book_character")
internal class Migration70 : AutoMigrationSpec
