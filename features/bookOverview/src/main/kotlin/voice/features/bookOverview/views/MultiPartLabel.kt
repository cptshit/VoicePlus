package voice.features.bookOverview.views

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import voice.features.bookOverview.overview.BookOverviewItemViewState
import voice.core.strings.R as StringsR

/** "Zweiteiler · Teil 1 von 2", or just "Teil 1" while the other parts are unknown. */
internal fun multiPartLabel(
  resources: Resources,
  part: Int?,
  count: Int?,
): String? {
  if (part == null) return null
  if (count == null || count < 2) return resources.getString(StringsR.string.multipart_part, part)
  val name = resources.getString(
    when (count) {
      2 -> StringsR.string.multipart_2
      3 -> StringsR.string.multipart_3
      4 -> StringsR.string.multipart_4
      else -> StringsR.string.multipart_n
    },
  )
  return resources.getString(StringsR.string.multipart_part_of, name, part, count)
}

@Composable
internal fun BookOverviewItemViewState.multiPartLabel(): String? {
  return multiPartLabel(LocalContext.current.resources, titlePart, titlePartCount)
}
