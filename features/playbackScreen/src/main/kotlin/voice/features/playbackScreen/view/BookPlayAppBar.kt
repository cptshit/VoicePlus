package voice.features.playbackScreen.view

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.BedtimeOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.core.data.PlaybackToolbarAction
import voice.core.strings.R
import voice.features.playbackScreen.BookPlayViewState

@Composable
internal fun BookPlayAppBar(
  viewState: BookPlayViewState,
  onSleepTimerClick: () -> Unit,
  onSpeedChangeClick: () -> Unit,
  onSkipSilenceClick: () -> Unit,
  onVolumeBoostClick: () -> Unit,
  onCustomizeToolbarClick: () -> Unit,
  onCloseClick: () -> Unit,
  useLandscapeLayout: Boolean,
  onEditChapterNamesClick: (() -> Unit)? = null,
) {
  val (pinnedActions, overflowActions) = partitionToolbarActions(
    pinnedActions = viewState.toolbarActions,
    chapterFixAvailable = onEditChapterNamesClick != null,
  )
  val onActionClick: (PlaybackToolbarAction) -> Unit = { action ->
    when (action) {
      PlaybackToolbarAction.PLAYBACK_SPEED -> onSpeedChangeClick()
      PlaybackToolbarAction.CHAPTER_FIX -> onEditChapterNamesClick?.invoke()
    }
  }

  val appBarActions: @Composable RowScope.() -> Unit = {
    IconButton(onClick = onSleepTimerClick) {
      Icon(
        imageVector = if (viewState.sleepTimerState is BookPlayViewState.SleepTimerViewState.Disabled) {
          Icons.Outlined.Bedtime
        } else {
          Icons.Outlined.BedtimeOff
        },
        contentDescription = stringResource(id = R.string.action_sleep),
      )
    }
    pinnedActions.forEach { action ->
      IconButton(onClick = { onActionClick(action) }) {
        Icon(
          imageVector = action.icon,
          contentDescription = stringResource(id = action.labelRes),
        )
      }
    }
    OverflowMenu(
      skipSilence = viewState.skipSilence,
      onSkipSilenceClick = onSkipSilenceClick,
      onVolumeBoostClick = onVolumeBoostClick,
      overflowActions = overflowActions,
      onActionClick = onActionClick,
      onCustomizeToolbarClick = onCustomizeToolbarClick,
    )
  }
  if (useLandscapeLayout) {
    TopAppBar(
      navigationIcon = {
        CloseIcon(onCloseClick)
      },
      actions = appBarActions,
      title = {
        AppBarTitle(viewState.title)
      },
    )
  } else {
    LargeTopAppBar(
      navigationIcon = {
        CloseIcon(onCloseClick)
      },
      actions = appBarActions,
      title = {
        AppBarTitle(viewState.title)
      },
    )
  }
}
