package voice.features.tagReview

import android.net.Uri
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.hoerspiel.EpisodeNumberRepo
import voice.core.data.hoerspiel.TagFinding
import voice.core.data.hoerspiel.TagFindingField
import voice.core.data.hoerspiel.TagFindingKind
import voice.core.data.hoerspiel.TagFindingRepo
import voice.core.data.hoerspiel.TagFindingSource
import voice.core.data.repo.BookContentRepo
import voice.navigation.Navigator
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class TagReviewViewModelTest {

  private val id = BookId(Uri.parse("file:///Hoerspiele/GB 013"))
  private val findingRepo = mockk<TagFindingRepo>(relaxed = true)
  private val contentRepo = mockk<BookContentRepo>(relaxed = true)
  private val numberRepo = mockk<EpisodeNumberRepo>(relaxed = true)
  private val model = TagReviewViewModel(findingRepo, contentRepo, numberRepo, mockk(relaxed = true))

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun content(
    name: String = "Die Komission",
    active: Boolean = true,
  ): BookContent {
    val chapter = ChapterId(Uri.parse("file:///Hoerspiele/GB 013/1.mp3"))
    return BookContent(
      id = id, playbackSpeed = 1F, skipSilence = false, isActive = active, lastPlayedAt = Instant.EPOCH,
      author = null, name = name, addedAt = Instant.EPOCH, chapters = listOf(chapter), currentChapter = chapter,
      positionInChapter = 0, cover = null, gain = 0F, genre = null, narrator = null, series = null, part = null,
    )
  }

  private fun finding(
    field: TagFindingField = TagFindingField.EpisodeTitle,
    dismissed: Boolean = false,
    nr: Long = 1,
  ) = TagFinding(
    id = nr, bookId = id.value, field = field, kind = TagFindingKind.Different,
    chosenValue = "Die Komission", chosenSource = TagFindingSource.FolderName,
    otherValue = "Die Kommission", otherSource = TagFindingSource.FileName, dismissed = dismissed,
  )

  private fun item(field: TagFindingField = TagFindingField.EpisodeTitle) =
    tagReviewViewState(listOf(finding(field)), listOf(content())).books.single().findings.single()

  @Test
  fun viewStateListsOnlyOpenFindingsOfActiveBooks() {
    val state = tagReviewViewState(
      listOf(finding(nr = 1), finding(nr = 2, dismissed = true)),
      listOf(content()),
    )
    state.books.single().findings.map { it.id } shouldBe listOf(1L)
    tagReviewViewState(listOf(finding()), listOf(content(active = false))).books shouldBe emptyList()
  }

  @Test
  fun applyingATitleKeepsItAcrossRescansAndClosesTheFinding() {
    val updates = mutableListOf<BookContent>()
    coEvery { contentRepo.update(id, any()) } answers {
      secondArg<(BookContent) -> BookContent>()(content()).also { updates += it }
    }

    model.apply(item(), "Die Kommission")

    updates.single().name shouldBe "Die Kommission"
    updates.single().nameOverridden shouldBe true
    coVerify { findingRepo.setDismissed(1, true) }
  }

  @Test
  fun applyingANumberStoresAnOverride() {
    model.apply(item(TagFindingField.EpisodeNumber), "13")

    coVerify { numberRepo.set(id, 13) }
    coVerify { findingRepo.setDismissed(1, true) }
  }

  @Test
  fun aNumberThatIsNoNumberChangesNothing() {
    model.apply(item(TagFindingField.EpisodeNumber), "dreizehn")

    coVerify(exactly = 0) { numberRepo.set(any(), any()) }
    coVerify(exactly = 0) { findingRepo.setDismissed(any(), any()) }
  }

  @Test
  fun aSeriesFindingCanOnlyBeIgnored() {
    model.apply(item(TagFindingField.Series), "Gabriel Burns")
    coVerify(exactly = 0) { findingRepo.setDismissed(any(), any()) }

    model.ignore(item(TagFindingField.Series))
    coVerify { findingRepo.setDismissed(1, true) }
  }
}
