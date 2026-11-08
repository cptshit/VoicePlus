package voice.features.bookOverview.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.core.strings.R as StringsR

/** Points to the "Check tags" screen when episodes were tagged inconsistently. */
@Composable
internal fun TagReviewHint(
  count: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
      .height(IntrinsicSize.Min)
      .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.shapes.small),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(Modifier.width(4.dp).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
    Column(Modifier.weight(1f).padding(start = 12.dp, top = 8.dp, bottom = 8.dp)) {
      Text(
        pluralStringResource(StringsR.plurals.tag_review_hint_count, count, count),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
    }
    TextButton(onClick = onClick) { Text(stringResource(StringsR.string.tag_review_open)) }
  }
}
