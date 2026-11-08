package voice.features.tagReview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import voice.core.common.RetainedViewModel
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.hoerspiel.EpisodeNumberRepo
import voice.core.data.hoerspiel.TagFinding
import voice.core.data.hoerspiel.TagFindingField
import voice.core.data.hoerspiel.TagFindingRepo
import voice.core.data.repo.BookContentRepo
import voice.navigation.Navigator

@Inject
class TagReviewViewModel(
  private val findingRepo: TagFindingRepo,
  private val contentRepo: BookContentRepo,
  private val episodeNumberRepo: EpisodeNumberRepo,
  private val navigator: Navigator,
) : RetainedViewModel() {

  @Composable
  fun viewState(): TagReviewViewState {
    val state by remember {
      combine(findingRepo.flow(), contentRepo.flow(), ::tagReviewViewState)
    }.collectAsState(initial = TagReviewViewState(emptyList()))
    return state
  }

  /** Uses [value] (the alternative, or text typed by the user) for the finding's field and closes the finding. */
  fun apply(
    finding: TagFindingItem,
    value: String,
  ) {
    val text = value.trim()
    if (text.isEmpty() || !finding.field.canApply) return
    scope.launch {
      val id = BookId(finding.bookId)
      when (finding.field) {
        TagFindingField.EpisodeTitle ->
          // Keep the corrected title across re-scans, like a title edited in "Edit book".
          contentRepo.update(id) { it.copy(name = text, nameOverridden = true) }
        TagFindingField.EpisodeNumber -> {
          val number = text.toIntOrNull() ?: return@launch
          contentRepo.update(id) { it.copy(part = number.toString()) }
          episodeNumberRepo.set(id, number)
        }
        TagFindingField.Series -> return@launch
      }
      findingRepo.setDismissed(finding.id, true)
    }
  }

  fun ignore(finding: TagFindingItem) {
    scope.launch { findingRepo.setDismissed(finding.id, true) }
  }

  fun onClose() {
    navigator.goBack()
  }
}

/** Series names can only be fixed in the series editor, the scanner derives them again on every scan. */
internal val TagFindingField.canApply: Boolean get() = this != TagFindingField.Series

data class TagReviewViewState(val books: List<TagReviewBook>)

data class TagReviewBook(
  val bookId: String,
  val name: String,
  val findings: List<TagFindingItem>,
)

data class TagFindingItem(
  val id: Long,
  val bookId: String,
  val field: TagFindingField,
  val chosenValue: String,
  val chosenSource: voice.core.data.hoerspiel.TagFindingSource,
  val otherValue: String,
  val otherSource: voice.core.data.hoerspiel.TagFindingSource,
)

/** Open (not dismissed) findings of books that still exist, grouped per book and sorted by title. */
internal fun tagReviewViewState(
  findings: List<TagFinding>,
  contents: List<BookContent>,
): TagReviewViewState {
  val names = contents.filter { it.isActive }.associate { it.id.value to it.name }
  return TagReviewViewState(
    books = findings.filter { !it.dismissed && it.bookId in names }
      .groupBy { it.bookId }
      .map { (bookId, items) ->
        TagReviewBook(
          bookId = bookId,
          name = names.getValue(bookId),
          findings = items.sortedBy { it.id }.map {
            TagFindingItem(it.id, it.bookId, it.field, it.chosenValue, it.chosenSource, it.otherValue, it.otherSource)
          },
        )
      }
      .sortedBy { it.name.lowercase() },
  )
}
