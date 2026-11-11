package voice.core.data

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.Test

class MediaButtonClickActionSerializerTest {

  private val json = Json

  @Test
  fun `known actions round-trip as their plain enum name`() {
    MediaButtonClickAction.entries.forEach { action ->
      val encoded = json.encodeToString(MediaButtonClickAction.serializer(), action)
      encoded shouldBe "\"${action.name}\""
      json.decodeFromString(MediaButtonClickAction.serializer(), encoded) shouldBe action
    }
  }

  @Test
  fun `an action that no longer exists decodes to NONE instead of failing`() {
    json.decodeFromString(MediaButtonClickAction.serializer(), "\"QUICK_BOOKMARK\"") shouldBe MediaButtonClickAction.NONE
  }
}
