package voice.core.data.store

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import voice.core.data.PlaybackToolbarAction

/**
 * Reads the pinned toolbar actions and silently drops names that no longer exist, e.g. actions of
 * features that were removed. A plain enum serializer would throw on them and break the player.
 */
internal object LenientToolbarActionsSerializer : KSerializer<Set<PlaybackToolbarAction>> {

  private val delegate = SetSerializer(PlaybackToolbarAction.serializer())

  override val descriptor: SerialDescriptor = delegate.descriptor

  override fun serialize(
    encoder: Encoder,
    value: Set<PlaybackToolbarAction>,
  ) {
    delegate.serialize(encoder, value)
  }

  override fun deserialize(decoder: Decoder): Set<PlaybackToolbarAction> {
    val element = (decoder as JsonDecoder).decodeJsonElement()
    val byName = PlaybackToolbarAction.entries.associateBy { it.name }
    return element.jsonArray
      .mapNotNull { (it as? JsonPrimitive)?.content?.let(byName::get) }
      .toSet()
  }
}
