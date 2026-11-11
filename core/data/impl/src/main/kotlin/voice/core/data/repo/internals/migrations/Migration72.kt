package voice.core.data.repo.internals.migrations

import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec

/** Bookmarks were removed; their table is dropped. */
@DeleteTable(tableName = "bookmark2")
internal class Migration72 : AutoMigrationSpec
