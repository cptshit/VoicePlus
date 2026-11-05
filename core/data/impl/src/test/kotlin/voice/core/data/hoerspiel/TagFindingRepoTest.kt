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
class TagFindingRepoTest {

  private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HoerspielDb::class.java)
    .allowMainThreadQueries()
    .build()
  private val repo = TagFindingRepoImpl(db.tagFindingDao())
  private val book = BookId(Uri.parse("file:///GabrielBurns%20-%20013%20-%20Die%20Komission"))
  private val otherBook = BookId(Uri.parse("file:///other"))

  private fun finding(
    other: String,
    bookId: BookId = book,
  ) = TagFinding(
    bookId = bookId.value,
    field = TagFindingField.EpisodeTitle,
    kind = TagFindingKind.Different,
    chosenValue = "Die Komission",
    chosenSource = TagFindingSource.FolderName,
    otherValue = other,
    otherSource = TagFindingSource.FileName,
  )

  @After
  fun tearDown() = db.close()

  @Test
  fun replaceKeepsDismissedStateOfUnchangedFindings() = runTest {
    repo.replaceForBook(book, listOf(finding("Die Kommission [Teil 2]"), finding("Etwas anderes")))
    val stored = repo.flow(book).first()
    repo.setDismissed(stored.first { it.otherValue == "Die Kommission [Teil 2]" }.id, dismissed = true)

    // rescan: same finding again plus a changed one
    repo.replaceForBook(book, listOf(finding("Die Kommission [Teil 2]"), finding("Neu")))

    repo.flow(book).first().map { it.otherValue to it.dismissed } shouldBe listOf(
      "Die Kommission [Teil 2]" to true,
      "Neu" to false,
    )
  }

  @Test
  fun replaceOnlyTouchesItsBook() = runTest {
    repo.replaceForBook(otherBook, listOf(finding("X", otherBook)))
    repo.replaceForBook(book, listOf(finding("Y")))
    repo.replaceForBook(book, emptyList())

    repo.flow().first().map { it.otherValue } shouldBe listOf("X")
  }

  @Test
  fun bookIdOfInputIsIgnored() = runTest {
    repo.replaceForBook(book, listOf(finding("Y", bookId = otherBook)))
    repo.flow(book).first().map { it.otherValue } shouldBe listOf("Y")
  }
}
