package voice.features.tagReview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.hoerspiel.TagFindingField
import voice.core.data.hoerspiel.TagFindingSource
import voice.features.tagReview.lookup.LookupResult
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@ContributesTo(AppScope::class)
interface TagReviewGraph {
  val tagReviewViewModel: TagReviewViewModel
}

@ContributesTo(AppScope::class)
interface TagReviewProvider {

  @Provides
  @IntoSet
  fun tagReviewNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.TagReview> { key ->
    NavEntry(key) {
      TagReviewScreen()
    }
  }
}

@Composable
fun TagReviewScreen() {
  val viewModel = retain { rootGraphAs<TagReviewGraph>().tagReviewViewModel }
  TagReviewScreen(
    viewState = viewModel.viewState(),
    onClose = viewModel::onClose,
    onApply = viewModel::apply,
    onIgnore = viewModel::ignore,
    lookups = viewModel.lookups,
    onCompareOnline = viewModel::compareOnline,
  )
}

@Composable
internal fun TagReviewScreen(
  viewState: TagReviewViewState,
  onClose: () -> Unit,
  onApply: (TagFindingItem, String) -> Unit,
  onIgnore: (TagFindingItem) -> Unit,
  modifier: Modifier = Modifier,
  lookups: Map<String, LookupStatus> = emptyMap(),
  onCompareOnline: (String) -> Unit = {},
) {
  var confirming by remember { mutableStateOf<String?>(null) }
  var editing by remember { mutableStateOf<TagFindingItem?>(null) }
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(stringResource(StringsR.string.tag_review)) },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = stringResource(StringsR.string.close))
          }
        },
      )
    },
  ) { paddingValues ->
    LazyColumn(
      contentPadding = paddingValues,
      modifier = Modifier.padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (viewState.series.isNotEmpty()) {
        item(key = "online") {
          OnlineSection(viewState.series, lookups, onCompare = { confirming = it })
        }
      }
      if (viewState.books.isEmpty()) {
        item(key = "empty") {
          Text(
            stringResource(StringsR.string.tag_review_empty),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(32.dp),
          )
        }
      }
      items(viewState.books, key = { it.bookId }) { book ->
        Card(Modifier.fillMaxWidth()) {
          Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(book.name, style = MaterialTheme.typography.titleMedium)
            book.findings.forEach { finding ->
              FindingRow(finding, onApply = {
                onApply(finding, finding.otherValue)
              }, onEdit = { editing = finding }, onIgnore = { onIgnore(finding) })
            }
          }
        }
      }
    }
  }
  confirming?.let { series ->
    AlertDialog(
      onDismissRequest = { confirming = null },
      title = { Text(stringResource(StringsR.string.tag_review_online_confirm_title)) },
      text = { Text(stringResource(StringsR.string.tag_review_online_confirm_text, series)) },
      confirmButton = {
        TextButton(
          onClick = {
            confirming = null
            onCompareOnline(series)
          },
        ) { Text(stringResource(StringsR.string.tag_review_online_send)) }
      },
      dismissButton = { TextButton(onClick = { confirming = null }) { Text(stringResource(StringsR.string.dialog_cancel)) } },
    )
  }
  editing?.let { finding ->
    var text by remember(finding.id) { mutableStateOf(finding.chosenValue) }
    AlertDialog(
      onDismissRequest = { editing = null },
      title = { Text(stringResource(finding.field.label)) },
      text = { OutlinedTextField(text, { text = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
      confirmButton = {
        TextButton(
          enabled = text.isNotBlank(),
          onClick = {
            onApply(finding, text)
            editing = null
          },
        ) { Text(stringResource(StringsR.string.tag_review_save)) }
      },
      dismissButton = { TextButton(onClick = { editing = null }) { Text(stringResource(StringsR.string.dialog_cancel)) } },
    )
  }
}

@Composable
private fun FindingRow(
  finding: TagFindingItem,
  onApply: () -> Unit,
  onEdit: () -> Unit,
  onIgnore: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      stringResource(finding.field.label),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(stringResource(StringsR.string.tag_review_used, finding.chosenValue, stringResource(finding.chosenSource.label)))
    Text(stringResource(StringsR.string.tag_review_other, finding.otherValue, stringResource(finding.otherSource.label)))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      if (finding.field.canApply) {
        Button(onClick = onApply) { Text(stringResource(StringsR.string.tag_review_apply)) }
        OutlinedButton(onClick = onEdit) { Text(stringResource(StringsR.string.tag_review_edit)) }
      }
      TextButton(onClick = onIgnore) { Text(stringResource(StringsR.string.tag_review_ignore)) }
    }
  }
}

private val TagFindingField.label: Int
  get() = when (this) {
    TagFindingField.Series -> StringsR.string.tag_review_field_series
    TagFindingField.EpisodeNumber -> StringsR.string.tag_review_field_number
    TagFindingField.EpisodeTitle -> StringsR.string.tag_review_field_title
  }

private val TagFindingSource.label: Int
  get() = when (this) {
    TagFindingSource.SeriesTag -> StringsR.string.tag_review_source_series_tag
    TagFindingSource.PartTag -> StringsR.string.tag_review_source_part_tag
    TagFindingSource.FolderName -> StringsR.string.tag_review_source_folder
    TagFindingSource.AlbumTag -> StringsR.string.tag_review_source_album
    TagFindingSource.ArtistTag -> StringsR.string.tag_review_source_artist
    TagFindingSource.FileName -> StringsR.string.tag_review_source_file
    TagFindingSource.Local -> StringsR.string.tag_review_source_local
    TagFindingSource.MusicBrainz -> StringsR.string.tag_review_source_musicbrainz
    TagFindingSource.ITunes -> StringsR.string.tag_review_source_itunes
  }

@Composable
private fun OnlineSection(
  series: List<String>,
  lookups: Map<String, LookupStatus>,
  onCompare: (String) -> Unit,
) {
  Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(stringResource(StringsR.string.tag_review_online_title), style = MaterialTheme.typography.titleMedium)
      Text(
        stringResource(StringsR.string.tag_review_online_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      series.forEach { name ->
        val status = lookups[name]
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            if (status is LookupStatus.Done) {
              Text(
                lookupMessage(status.result),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          if (status is LookupStatus.Running) {
            CircularProgressIndicator(Modifier.size(24.dp))
          } else {
            OutlinedButton(onClick = { onCompare(name) }) { Text(stringResource(StringsR.string.tag_review_online_button)) }
          }
        }
      }
    }
  }
}

@Composable
private fun lookupMessage(result: LookupResult): String = when (result) {
  is LookupResult.Found -> if (result.suggestions == 0) {
    stringResource(StringsR.string.tag_review_online_agrees)
  } else {
    pluralStringResource(StringsR.plurals.tag_review_online_found, result.suggestions, result.suggestions)
  }
  LookupResult.NothingSuitable -> stringResource(StringsR.string.tag_review_online_nothing)
  LookupResult.Failed -> stringResource(StringsR.string.tag_review_online_failed)
}
