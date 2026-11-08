package voice.core.scanner

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import voice.core.data.BookId
import voice.core.data.hoerspiel.TagFinding
import voice.core.data.hoerspiel.TagFindingRepo

internal class FakeTagFindingRepo : TagFindingRepo {

  val findings = MutableStateFlow<List<TagFinding>>(emptyList())

  override fun flow(): Flow<List<TagFinding>> = findings

  override fun flow(bookId: BookId): Flow<List<TagFinding>> = findings.map { all -> all.filter { it.bookId == bookId.value } }

  override suspend fun replaceForBook(
    bookId: BookId,
    findings: List<TagFinding>,
  ) {
    this.findings.value = this.findings.value.filter { it.bookId != bookId.value } +
      findings.map { it.copy(bookId = bookId.value) }
  }

  override suspend fun replaceOnlineForBook(
    bookId: BookId,
    findings: List<TagFinding>,
  ) {
    this.findings.value = this.findings.value.filter { it.bookId != bookId.value || !it.otherSource.isOnline } +
      findings.map { it.copy(bookId = bookId.value) }
  }

  override suspend fun setDismissed(
    id: Long,
    dismissed: Boolean,
  ) {
    findings.value = findings.value.map { if (it.id == id) it.copy(dismissed = dismissed) else it }
  }
}
