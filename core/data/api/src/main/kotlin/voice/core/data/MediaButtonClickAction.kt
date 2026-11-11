package voice.core.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = MediaButtonClickActionSerializer::class)
public enum class MediaButtonClickAction {
  NONE,
  SKIP_FORWARD,
  SKIP_BACKWARD,
  SKIP_FORWARD_CHAPTER,
  SKIP_BACKWARD_CHAPTER,
}

/**
 * Stores the enum name like the default enum serializer does, but maps a name that no longer exists
 * (e.g. the removed quick-bookmark action) to [MediaButtonClickAction.NONE] instead of failing to
 * read the whole setting.
 */
internal object MediaButtonClickActionSerializer : KSerializer<MediaButtonClickAction> {

  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("voice.core.data.MediaButtonClickAction", PrimitiveKind.STRING)

  override fun serialize(
    encoder: Encoder,
    value: MediaButtonClickAction,
  ) {
    encoder.encodeString(value.name)
  }

  override fun deserialize(decoder: Decoder): MediaButtonClickAction {
    val name = decoder.decodeString()
    return MediaButtonClickAction.entries.firstOrNull { it.name == name } ?: MediaButtonClickAction.NONE
  }
}
