package voice.core.scanner.hoerspiel

/**
 * Looks at all file names of one book at once. A file name only tells something about the
 * episode if every track says the same: `01 - Die Brut`, `02 - Die Brut`, … yields the title
 * `Die Brut`, whereas `Track 01`, `Track 02` or real chapter names (`Prolog`, `Epilog`) yield none.
 */
internal data class FileNameAnalysis(
  val series: String?,
  val number: Int?,
  val title: String?,
) {

  companion object {

    // "01 - ", "01-", "01 ", "01-01 " (disc-track), "027 - "
    private val LEADING_TRACK = Regex("""^\d{1,3}(?:[-.]\d{1,3})?(?:\s*[-–.]\s*|\s+)(?=\S)""")

    // "Zwiespalt - 01"
    private val TRAILING_TRACK = Regex("""\s*[-–]\s*\d{1,3}$""")

    fun analyze(fileNames: List<String>): FileNameAnalysis {
      if (fileNames.isEmpty()) return FileNameAnalysis(null, null, null)
      val parsed = fileNames.map { EpisodeNameParser.parse(stripTrack(it.substringAfterLast('/'))) }
      // A number that changes from track to track is a track number, so the split around it
      // (e.g. "gabriel burns - angst - kapitel 01 - fkkab") says nothing about the episode.
      if (parsed.map { it.number }.distinct().size > 1) return FileNameAnalysis(null, null, null)
      return FileNameAnalysis(
        series = parsed.commonValue { it.series },
        number = parsed.commonValue { it.number },
        title = parsed.commonValue { it.title }?.takeUnless { isGenericTrackName(it) },
      )
    }

    internal fun stripTrack(name: String): String {
      return name.trim()
        .replace(LEADING_TRACK, "")
        .replace(TRAILING_TRACK, "")
    }

    private fun <T : Any> List<ParsedName>.commonValue(selector: (ParsedName) -> T?): T? {
      val values = map(selector)
      val first = values.first() ?: return null
      return first.takeIf { values.all { it == first } }
    }
  }
}

// "Track 01", "AudioTrack 01", "Kapitel 01", "Angst-Kapitel 01". A letter suffix ("Kapitel 04a")
// is not generic: it tells where a chapter was split across discs.
private val GENERIC_TRACK_NAME = Regex(
  """^(?:audio\s*track|track|kapitel|chapter|teil|part|cd|disc|\p{L}+-kapitel)[\s_-]*\d+$""",
  RegexOption.IGNORE_CASE,
)

internal fun isGenericTrackName(name: String): Boolean = GENERIC_TRACK_NAME.matches(name.trim())
