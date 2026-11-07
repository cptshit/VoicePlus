package voice.core.data.hoerspiel

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import voice.core.data.BookId

@RunWith(AndroidJUnit4::class)
class SeriesAutoGroupingRepoTest {

  private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HoerspielDb::class.java)
    .allowMainThreadQueries()
    .build()
  private val repo = SeriesAutoGroupingRepoImpl(db.seriesAutoGroupingDao())

  @After
  fun tearDown() = db.close()

  @Test
  fun marksAccumulateAndRepeatsAreIgnored() = runTest {
    val first = BookId(Uri.parse("file:///1"))
    val second = BookId(Uri.parse("file:///2"))
    repo.markHandled(setOf(first))
    repo.markHandled(setOf(first, second))
    repo.markHandled(emptySet())

    repo.handled() shouldBe setOf(first, second)
  }
}
