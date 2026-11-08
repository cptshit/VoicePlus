package voice.core.data.hoerspiel

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HoerspielDbMigrationTest {

  @Rule
  @JvmField
  val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    HoerspielDb::class.java,
  )

  @Test
  fun version1To2KeepsFindingsAndAddsEpisodeParts() {
    helper.createDatabase(NAME, 1).use { db ->
      db.execSQL(
        """INSERT INTO tag_findings (bookId, field, kind, chosenValue, chosenSource, otherValue, otherSource, dismissed)
        VALUES ('book', 'EpisodeTitle', 'Different', 'Die Komission', 'FolderName', 'Die Kommission', 'FileName', 1)""",
      )
    }

    helper.runMigrationsAndValidate(NAME, 2, true).use { db ->
      db.query("SELECT otherValue, dismissed FROM tag_findings").use { cursor ->
        cursor.moveToFirst() shouldBe true
        cursor.getString(0) shouldBe "Die Kommission"
        cursor.getInt(1) shouldBe 1
      }
      db.execSQL("INSERT INTO episode_parts (bookId, part, partCount) VALUES ('book', 1, 2)")
    }
  }

  @Test
  fun version2To3AddsAutoGroupingMarks() {
    helper.createDatabase(NAME, 2).use { db ->
      db.execSQL("INSERT INTO episode_parts (bookId, part, partCount) VALUES ('book', 1, 2)")
    }

    helper.runMigrationsAndValidate(NAME, 3, true).use { db ->
      db.query("SELECT part, partCount FROM episode_parts").use { cursor ->
        cursor.moveToFirst() shouldBe true
        cursor.getInt(0) shouldBe 1
        cursor.getInt(1) shouldBe 2
      }
      db.execSQL("INSERT INTO series_auto_grouped (bookId) VALUES ('book')")
    }
  }

  @Test
  fun version4To5AddsTheCatalogCache() {
    helper.createDatabase(NAME, 4).use { db ->
      db.execSQL("INSERT INTO episode_number_overrides (bookId, number) VALUES ('book', 7)")
    }

    helper.runMigrationsAndValidate(NAME, 5, true).use { db ->
      db.query("SELECT number FROM episode_number_overrides").use { cursor ->
        cursor.moveToFirst() shouldBe true
        cursor.getInt(0) shouldBe 7
      }
      db.execSQL("INSERT INTO catalog_cache (series, source, json, fetchedAtEpochMillis) VALUES ('gabriel burns', 'MusicBrainz', '[]', 1)")
    }
  }

  private companion object {
    const val NAME = "hoerspiel-migration-test"
  }
}
