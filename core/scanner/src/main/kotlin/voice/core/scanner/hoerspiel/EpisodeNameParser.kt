package voice.core.scanner.hoerspiel

/**
 * Parsed parts of a single name (folder name, album tag or file name).
 * Any part may be missing; [title] is never blank.
 */
internal data class ParsedName(
  val series: String?,
  val number: Int?,
  val title: String?,
)

/**
 * Splits names like `GabrielBurns - 002 - Die Brut`, `Gabriel.Burns.01.-.Der.Fluesterer`,
 * `(41) Verehrung`, `37/Der Atem der Fahlen`, `F43_Fern von allen Tiefen` or
 * `Gabriel Burns 36 - Am Ende aller Tage` into series, episode number and title.
 *
 * Fields are only ever split at dashes, colons, slashes or the number itself, never at commas,
 * so titles like `Die, die nicht bluten` survive.
 */
internal object EpisodeNameParser {

  private const val NUMBER = """(?:Folge\s*|Episode\s*|F)?(?<num>\d{1,3})"""

  // "<series> [-] <number> - <title>", series must contain a letter.
  private val SERIES_NUMBER_TITLE = Regex(
    """^(?<series>[^\p{L}]*\p{L}.*?)\s*(?:[-–:]\s*)?$NUMBER\s*[-–:.]\s*(?<title>.*\S.*)$""",
  )

  // "(41) Title"
  private val PARENTHESIZED_NUMBER_TITLE = Regex("""^\((?<num>\d{1,3})\)\s*(?<title>.*\S.*)$""")

  // "41 - Title", "41-Title", "41/Title", "41 Title", "F43_Title", "Folge 12: Title"
  private val NUMBER_TITLE = Regex("""^$NUMBER(?:\s*[-–:./_]\s*|\s+)(?<title>.*\S.*)$""")

  fun parse(name: String): ParsedName {
    val cleaned = normalizeSeparators(name).trim()
    if (cleaned.isEmpty()) return ParsedName(null, null, null)

    PARENTHESIZED_NUMBER_TITLE.matchEntire(cleaned)?.let {
      return ParsedName(series = null, number = it.number(), title = it.title())
    }
    NUMBER_TITLE.matchEntire(cleaned)?.let {
      return ParsedName(series = null, number = it.number(), title = it.title())
    }
    SERIES_NUMBER_TITLE.matchEntire(cleaned)?.let {
      val series = it.groups["series"]!!.value.trim().trimEnd('-', '–', ':').trim()
      return ParsedName(series = splitCamelCase(series), number = it.number(), title = it.title())
    }
    return ParsedName(series = null, number = null, title = cleaned)
  }

  /**
   * Scene-style names use dots or underscores instead of spaces
   * (`Gabriel.Burns.01.-.Der.Fluesterer`). Only names without any space are converted, so
   * regular titles keep their dots (`Dr. No`).
   */
  internal fun normalizeSeparators(name: String): String {
    if (name.contains(' ') || !(name.contains('.') || name.contains('_'))) return name
    return name.replace(Regex("[._]+"), " ").replace(Regex("""\s*-\s*"""), " - ")
  }

  /** `GabrielBurns` -> `Gabriel Burns`. Names that already contain spaces stay untouched. */
  internal fun splitCamelCase(name: String): String {
    if (name.contains(' ')) return name
    return name.replace(Regex("""(?<=\p{Ll})(?=\p{Lu})"""), " ")
  }

  private fun MatchResult.number(): Int? = groups["num"]?.value?.toIntOrNull()

  private fun MatchResult.title(): String? = groups["title"]?.value?.trim()?.takeIf { it.isNotEmpty() }
}

/**
 * Key used to decide whether two spellings mean the same thing: case, whitespace, punctuation
 * and umlaut transliteration are ignored. `GabrielBurns`, `Gabriel.Burns` and `Gabriel Burns`
 * share a key, as do `Der Fluesterer` and `Der Flüsterer`.
 */
internal fun comparisonKey(value: String): String {
  return value.lowercase()
    .replace("ä", "ae")
    .replace("ö", "oe")
    .replace("ü", "ue")
    .replace("ß", "ss")
    .filter { it.isLetterOrDigit() }
}

internal fun String.hasGermanSpecialCharacters(): Boolean = any { it in "äöüÄÖÜß" }
