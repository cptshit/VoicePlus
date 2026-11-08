package voice.features.bookOverview.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.features.bookOverview.shelves.ShelfFilter
import voice.core.strings.R as StringsR

/** Pill-shaped filter row above the library; the chosen filter is filled with the accent colour. */
@Composable
internal fun LibraryFilterChips(
  filter: ShelfFilter,
  onFilterChange: (ShelfFilter) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyRow(
    modifier = modifier,
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(ShelfFilter.entries, key = { it.name }) { entry ->
      FilterChip(
        selected = filter == entry,
        onClick = { onFilterChange(entry) },
        label = { Text(stringResource(entry.label)) },
        shape = CircleShape,
        modifier = Modifier.heightIn(min = 44.dp),
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primary,
          selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        border = null,
      )
    }
  }
}

private val ShelfFilter.label: Int
  get() = when (this) {
    ShelfFilter.ALL -> StringsR.string.library_all
    ShelfFilter.IN_PROGRESS -> StringsR.string.library_in_progress
    ShelfFilter.NOT_STARTED -> StringsR.string.library_not_started
    ShelfFilter.COMPLETED -> StringsR.string.book_header_completed
  }
