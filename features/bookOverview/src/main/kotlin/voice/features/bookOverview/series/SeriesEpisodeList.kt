package voice.features.bookOverview.series

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import voice.core.data.BookId
import voice.core.ui.VoiceTheme
import voice.features.bookOverview.overview.BookOverviewItemViewState
import voice.features.bookOverview.views.BookshelfCover
import voice.features.bookOverview.views.multiPartLabel
import voice.core.strings.R as StringsR

/**
 * The episodes of a series as a list like a concert programme: a big episode number on the left,
 * section headers (running / up next / heard) that stay on top while scrolling, and a note row
 * wherever episodes are missing.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SeriesEpisodeList(
  items: List<EpisodeListItem>,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  modifier: Modifier = Modifier,
  state: LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
) {
  LazyColumn(
    modifier = modifier.testTag("series-grid"),
    state = state,
    contentPadding = PaddingValues(bottom = 16.dp),
  ) {
    items.forEach { item ->
      when (item) {
        is EpisodeListItem.Header -> stickyHeader(key = item.key) { SectionHeader(item) }
        is EpisodeListItem.Gap -> item(key = item.key) { GapRow(item) }
        is EpisodeListItem.Episode -> item(key = item.key) {
          EpisodeRow(item, onClick = { onBookClick(item.book.id) }, onLongClick = { onBookLongClick(item.book.id) })
        }
      }
    }
  }
}

@Composable
private fun SectionHeader(header: EpisodeListItem.Header) {
  val name = stringResource(
    when (header.section) {
      EpisodeSection.Running -> StringsR.string.series_section_running
      EpisodeSection.Next -> StringsR.string.series_section_next
      EpisodeSection.Heard -> StringsR.string.series_section_heard
    },
  )
  Row(
    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 16.dp, vertical = 8.dp)
      .semantics { heading() },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      name.uppercase(),
      style = VoiceTheme.extraTextStyles.sectionHeader,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.weight(1f),
    )
    Text(
      header.count.toString(),
      style = VoiceTheme.extraTextStyles.sectionHeader,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun GapRow(gap: EpisodeListItem.Gap) {
  val text = if (gap.from == gap.to) {
    stringResource(StringsR.string.series_gap_one, gap.from)
  } else {
    stringResource(StringsR.string.series_gap_range, gap.from, gap.to)
  }
  Row(
    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).height(IntrinsicSize.Min)
      .background(VoiceTheme.extraColors.noteBackground, MaterialTheme.shapes.small),
  ) {
    Box(Modifier.width(4.dp).fillMaxHeight().background(VoiceTheme.extraColors.noteBorder))
    Text(
      text,
      style = MaterialTheme.typography.bodyMedium,
      color = VoiceTheme.extraColors.noteText,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeRow(
  episode: EpisodeListItem.Episode,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
) {
  val book = episode.book
  Row(
    Modifier.fillMaxWidth().heightIn(min = 72.dp)
      .combinedClickable(onClick = onClick, onLongClick = onLongClick)
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .testTag("series-book:${book.id.value}"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      if (episode.number != null) {
        Text(
          stringResource(StringsR.string.series_episode_label).uppercase(),
          style = VoiceTheme.extraTextStyles.episodeLabel,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          episode.number,
          style = VoiceTheme.extraTextStyles.episodeNumber,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
          maxLines = 1,
        )
      }
    }
    Box(Modifier.width(44.dp)) {
      BookshelfCover(book, modifier = Modifier.testTag("book-cover:${book.id.value}"), compact = true)
    }
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(episode.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
      val detail = episodeDetail(book)
      if (detail.isNotEmpty()) {
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      if (!book.finished && book.progress > 0f) {
        LinearProgressIndicator(progress = { book.progress }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
      }
    }
  }
}

@Composable
private fun episodeDetail(book: BookOverviewItemViewState): String {
  val parts = listOfNotNull(
    book.multiPartLabel(),
    when {
      book.finished -> book.finishedOn
      else -> book.remainingTime.takeIf(String::isNotBlank)
    },
  )
  return parts.joinToString(" · ")
}
