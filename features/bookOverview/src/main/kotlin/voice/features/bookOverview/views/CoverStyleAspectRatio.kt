package voice.features.bookOverview.views

import voice.core.data.hoerspiel.CoverStyle

/** Width / height of a cover: square CDs, portrait books. */
internal val CoverStyle.aspectRatio: Float
  get() = when (this) {
    CoverStyle.Cd -> 1f
    CoverStyle.Book -> 2f / 3f
  }
