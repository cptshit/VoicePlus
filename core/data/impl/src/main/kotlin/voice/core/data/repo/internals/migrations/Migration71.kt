package voice.core.data.repo.internals.migrations

import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec

/** The listening log and statistics were removed; their session and event tables are dropped. */
@DeleteTable.Entries(
  DeleteTable(tableName = "listening_session"),
  DeleteTable(tableName = "listening_event"),
)
internal class Migration71 : AutoMigrationSpec
