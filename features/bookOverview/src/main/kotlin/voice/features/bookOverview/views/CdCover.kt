package voice.features.bookOverview.views

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import voice.core.ui.sharedBookCover
import voice.features.bookOverview.overview.BookOverviewItemViewState

/**
 * A square, uncropped cover in a subtle jewel case: a dark hinge on the left with a few ridges,
 * a hairline frame, and a faint diagonal reflection over the clear front. The artwork is fitted,
 * never cropped, so series logos at the edge stay visible.
 */
@Composable
internal fun CdCover(
  book: BookOverviewItemViewState,
  modifier: Modifier = Modifier,
  sharedTransitionScope: SharedTransitionScope? = null,
  onCoverReady: () -> Unit = {},
  compact: Boolean = false,
) {
  val caseShape = RoundedCornerShape(3.dp)
  val colors = MaterialTheme.colorScheme
  val dark = colors.surface.luminance() < 0.5f
  val caseColor = lerp(colors.surfaceContainerHighest, Color.Black, if (dark) 0.35f else 0.55f)
  val hinge = if (compact) 5.dp else 7.dp
  val frame = 1.5.dp
  Box(
    modifier = modifier
      .fillMaxWidth()
      .aspectRatio(1f)
      .testTag("cd-cover")
      .dropShadow(
        caseShape,
        Shadow(radius = 5.dp, color = Color.Black.copy(alpha = if (dark) 0.4f else 0.2f), offset = DpOffset(1.dp, 3.dp)),
      )
      .clip(caseShape)
      .background(caseColor),
  ) {
    // Tray behind the artwork, visible where a non-square cover leaves room.
    Box(
      Modifier
        .matchParentSize()
        .absolutePadding(left = hinge, top = frame, right = frame, bottom = frame)
        .background(colors.surfaceContainerHighest),
    ) {
      Icon(
        imageVector = Icons.Outlined.Album,
        contentDescription = null,
        tint = colors.onSurfaceVariant,
        modifier = Modifier.align(Alignment.Center).size(48.dp),
      )
      AsyncImage(
        model = book.cover?.file,
        contentDescription = null,
        onSuccess = { onCoverReady() },
        onError = { onCoverReady() },
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize().sharedBookCover(book.id, sharedTransitionScope),
      )
    }
    // Hinge ridges and the reflection of the clear lid, drawn over everything.
    Box(
      Modifier.matchParentSize().drawWithCache {
        val hingeWidth = hinge.toPx()
        val ridge = Color.White.copy(alpha = if (dark) 0.10f else 0.16f)
        val hingeShade = Brush.horizontalGradient(
          0f to Color.Black.copy(alpha = 0.35f),
          0.5f to Color.Transparent,
          1f to Color.Black.copy(alpha = 0.25f),
          endX = hingeWidth,
        )
        val reflection = Brush.linearGradient(
          0f to Color.White.copy(alpha = 0.12f),
          0.35f to Color.White.copy(alpha = 0.03f),
          0.36f to Color.Transparent,
          1f to Color.White.copy(alpha = 0.05f),
          start = Offset.Zero,
          end = Offset(size.width, size.height),
        )
        onDrawBehind {
          drawRect(hingeShade, size = Size(hingeWidth, size.height))
          listOf(0.3f, 0.55f, 0.8f).forEach { x ->
            drawLine(ridge, Offset(hingeWidth * x, 0f), Offset(hingeWidth * x, size.height), strokeWidth = 0.6.dp.toPx())
          }
          drawRect(reflection, topLeft = Offset(hingeWidth, 0f), size = Size(size.width - hingeWidth, size.height))
        }
      },
    )
  }
}
