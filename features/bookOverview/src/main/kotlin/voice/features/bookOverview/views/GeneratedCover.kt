package voice.features.bookOverview.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import voice.core.ui.BarlowCondensed
import voice.features.bookOverview.overview.BookOverviewItemViewState

private val coverColors = listOf(
  Color(0xFF7A2E35),
  Color(0xFF2F5A36),
  Color(0xFF3B4A6B),
  Color(0xFF6B4E2E),
  Color(0xFF55407A),
  Color(0xFF2E5C63),
)

private val leadingArticles = setOf("der", "die", "das", "ein", "eine", "the", "a", "an")

/** Short mark of a series for its generated cover: "Gabriel Burns" → "GB", "TKKG" → "TKKG". */
internal fun seriesAbbreviation(name: String): String {
  val words = name.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
  val withoutArticle = if (words.size > 2 && words.first().lowercase() in leadingArticles) words.drop(1) else words
  return when {
    withoutArticle.isEmpty() -> ""
    withoutArticle.size == 1 -> withoutArticle.single().let { word ->
      if (word.length <= 4 && word.none(Char::isLowerCase)) word else word.take(3).uppercase()
    }
    else -> withoutArticle.take(3).joinToString("") { it.first().uppercase() }
  }
}

/** The same series always gets the same colour. */
internal fun seriesCoverColor(name: String): Color = coverColors[(name.lowercase().hashCode() and Int.MAX_VALUE) % coverColors.size]

/** The series this book belongs to, preferring the one the user chose. */
internal val BookOverviewItemViewState.seriesLabel: String?
  get() = (seriesGroup ?: importedSeries)?.trim()?.takeIf(String::isNotEmpty)

/**
 * Stand-in for a missing cover: series mark on top, the episode number big in the middle.
 * Draws nothing for a book that belongs to no series, so the caller keeps its icon.
 */
@Composable
internal fun GeneratedCover(
  book: BookOverviewItemViewState,
  modifier: Modifier = Modifier,
) {
  val series = book.seriesLabel ?: return
  val number = book.seriesPart?.trim()?.takeIf(String::isNotEmpty)
  BoxWithConstraints(modifier.fillMaxSize().background(seriesCoverColor(series)), contentAlignment = Alignment.Center) {
    val width = maxWidth
    with(LocalDensity.current) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          seriesAbbreviation(series),
          color = Color.White.copy(alpha = 0.9f),
          fontFamily = BarlowCondensed,
          fontSize = (width * 0.2f).toSp(),
          maxLines = 1,
        )
        if (number != null) {
          Text(
            number,
            color = Color.White,
            fontFamily = BarlowCondensed,
            fontSize = (width * 0.5f).toSp(),
            textAlign = TextAlign.Center,
            maxLines = 1,
          )
        }
      }
    }
  }
}
