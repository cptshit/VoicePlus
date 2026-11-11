package voice.features.playbackScreen.view

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Test
import voice.core.data.PlaybackToolbarAction
import voice.core.data.PlaybackToolbarAction.CHAPTER_FIX
import voice.core.data.PlaybackToolbarAction.PLAYBACK_SPEED

class ToolbarActionsTest {

  @Test
  fun `defaults pin the original icons and overflow the rest`() {
    val (pinned, overflow) = partitionToolbarActions(PlaybackToolbarAction.DEFAULT, chapterFixAvailable = true)
    pinned shouldContainExactly listOf(PLAYBACK_SPEED)
    overflow shouldContainExactly listOf(CHAPTER_FIX)
  }

  @Test
  fun `display order follows the enum regardless of pin order`() {
    val (pinned, _) = partitionToolbarActions(linkedSetOf(CHAPTER_FIX, PLAYBACK_SPEED), chapterFixAvailable = true)
    pinned shouldContainExactly listOf(PLAYBACK_SPEED, CHAPTER_FIX)
  }

  @Test
  fun `chapter fix is dropped everywhere when the book has nothing to fix`() {
    val (pinned, overflow) = partitionToolbarActions(setOf(CHAPTER_FIX, PLAYBACK_SPEED), chapterFixAvailable = false)
    pinned shouldContainExactly listOf(PLAYBACK_SPEED)
    overflow shouldContainExactly emptyList()
  }

  @Test
  fun `pinning every action leaves the overflow empty`() {
    val (pinned, overflow) = partitionToolbarActions(PlaybackToolbarAction.entries.toSet(), chapterFixAvailable = true)
    pinned shouldContainExactly listOf(PLAYBACK_SPEED, CHAPTER_FIX)
    overflow shouldContainExactly emptyList()
  }
}
