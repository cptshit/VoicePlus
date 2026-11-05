package voice.core.scanner.hoerspiel

/**
 * Replaces meaningless chapter names of multi-file books with a plain numbering.
 *
 * Names are only replaced when **all** of them carry no information:
 * - every name is generic (`Track 01`, `AudioTrack 01`, `Kapitel 01`, `Angst-Kapitel 01`), or
 * - every name says the same once track numbers are removed (`01 - Die Brut`, `02 - Die Brut`;
 *   title tag = episode title in every file; `Gabriel Burns - 40` in every file).
 *
 * A single meaningful name (`Prolog`, `Kapitel 04a`) keeps the whole book as it is. Applying the
 * normalizer to its own output changes nothing, so it can run on every scan.
 *
 * Chapter names set in the chapter editor are stored separately and always win over these.
 */
internal object ChapterNameNormalizer {

  /**
   * @param names chapter names in playback order, as the scanner derived them from tags or file names
   * @param numbered name for the chapter at the given 1-based position, e.g. "Chapter 3"
   * @return the names to use, same size and order as [names]
   */
  fun normalize(
    names: List<String?>,
    numbered: (Int) -> String,
  ): List<String?> {
    if (names.size < 2) return names
    val cores = names.map { name -> name?.let(::core) }
    val meaningless = cores.all { it == null || isGenericTrackName(it) } ||
      cores.all { it != null && it == cores.first() }
    if (!meaningless) return names
    return names.indices.map { numbered(it + 1) }
  }

  /** The part of a name that is left after removing track numbers and series/episode prefixes. */
  private fun core(name: String): String? {
    val stripped = FileNameAnalysis.stripTrack(name)
    val parsed = EpisodeNameParser.parse(stripped)
    return (parsed.title ?: stripped).trim().takeIf { it.isNotEmpty() }
  }
}
