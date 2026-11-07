package voice.core.data.hoerspiel

import dev.zacsweers.metro.Qualifier
import kotlinx.serialization.Serializable

/** How covers are drawn in the bookshelf view. */
@Serializable
public enum class CoverStyle {
  /** Portrait jacket with spine and page edge, cropped to 2:3 (upstream look). */
  Book,

  /** Square, uncropped cover in a jewel case: audio dramas come on CDs. */
  Cd,
}

@Qualifier
public annotation class CoverStyleStore
