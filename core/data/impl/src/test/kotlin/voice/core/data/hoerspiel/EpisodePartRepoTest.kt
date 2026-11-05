package voice.core.data.hoerspiel

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import voice.core.data.BookId

@RunWith(AndroidJUnit4::class)
class EpisodePartRepoTest {

  private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HoerspielDb::class.java)
    .allowMainThreadQueries()
    .build()
  private val repo = EpisodePartRepoImpl(db.episodePartDao())
  private val first = BookId(Uri.parse("file:///15"))
  private val second = BookId(Uri.parse("file:///16"))

  @After
  fun tearDown() = db.close()

  @Test
  fun countsSurviveARescanWithTheSamePart() = runTest {
    repo.setPart(first, 1)
    repo.setPart(second, 2)
    repo.setPartCounts(mapOf(first to 2, second to 2))

    repo.setPart(first, 1)

    repo.flow().first() shouldBe mapOf(
      first to EpisodePart(first.value, 1, 2),
      second to EpisodePart(second.value, 2, 2),
    )
  }

  @Test
  fun changedPartDropsTheCountAndNullRemoves() = runTest {
    repo.setPart(first, 1)
    repo.setPart(second, 2)
    repo.setPartCounts(mapOf(first to 2, second to 2))

    repo.setPart(first, 3)
    repo.setPart(second, null)

    repo.all() shouldBe listOf(EpisodePart(first.value, 3, null))
  }

  @Test
  fun booksWithoutCountAreReset() = runTest {
    repo.setPart(first, 1)
    repo.setPartCounts(mapOf(first to 2))
    repo.setPartCounts(emptyMap())

    repo.all() shouldBe listOf(EpisodePart(first.value, 1, null))
  }
}
