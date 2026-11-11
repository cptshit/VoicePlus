package voice.core.data.store

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.Test
import voice.core.data.PlaybackToolbarAction

class LenientToolbarActionsSerializerTest {

  private val json = Json

  @Test
  fun `known actions round-trip`() {
    val actions = PlaybackToolbarAction.entries.toSet()
    val encoded = json.encodeToString(LenientToolbarActionsSerializer, actions)
    json.decodeFromString(LenientToolbarActionsSerializer, encoded) shouldBe actions
  }

  @Test
  fun `names of removed actions are dropped and the rest is kept`() {
    val stored = """["BOOKMARKS","CHARACTER_LIST","LISTENING_LOG","PLAYBACK_SPEED"]"""
    json.decodeFromString(LenientToolbarActionsSerializer, stored) shouldBe setOf(PlaybackToolbarAction.PLAYBACK_SPEED)
  }
}
