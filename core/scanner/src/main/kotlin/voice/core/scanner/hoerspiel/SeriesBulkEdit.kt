package voice.core.scanner.hoerspiel

import voice.core.data.BookId

/** The editable fields of one episode, as shown in the bulk editor. */
public data class EpisodeDraft(
  val id: BookId,
  val name: String,
)

/** One step of a bulk correction. Rules are applied in order to every episode. */
public sealed interface BulkRule {

  /** Replaces [find] in titles; an empty [find] does nothing. */
  public data class Replace(
    val find: String,
    val replacement: String,
    val ignoreCase: Boolean = false,
  ) : BulkRule

  /** Removes [prefix] (and a following ` - `, `:` or spaces) from the start of titles. */
  public data class StripPrefix(val prefix: String) : BulkRule

  /** Collapses runs of whitespace and trims the ends of titles. */
  public data object TidyWhitespace : BulkRule
}

/**
 * Old and new title of one episode; [changed] is false when the rules left it alone. Only titles
 * are corrected: the episode number is re-read from tags and file names on every scan, so a
 * manual number would not survive a re-scan.
 */
public data class EpisodeChange(
  val id: BookId,
  val oldName: String,
  val newName: String,
) {
  val changed: Boolean get() = oldName != newName
}

/**
 * Applies [rules] to [episodes] without touching anything: the result is the preview shown before
 * the user confirms. A rule never produces an empty title; the previous title is kept instead.
 */
public fun previewBulkEdit(
  episodes: List<EpisodeDraft>,
  rules: List<BulkRule>,
): List<EpisodeChange> {
  var current = episodes
  rules.forEach { rule -> current = current.applyRule(rule) }
  return episodes.zip(current) { old, new ->
    EpisodeChange(old.id, old.name, new.name)
  }
}

private fun List<EpisodeDraft>.applyRule(rule: BulkRule): List<EpisodeDraft> = when (rule) {
  is BulkRule.Replace -> if (rule.find.isEmpty()) {
    this
  } else {
    map { it.withName(it.name.replace(rule.find, rule.replacement, rule.ignoreCase)) }
  }
  is BulkRule.StripPrefix -> map { it.withName(it.name.stripPrefix(rule.prefix)) }
  BulkRule.TidyWhitespace -> map { it.withName(it.name.trim().replace(Regex("\\s+"), " ")) }
}

private fun EpisodeDraft.withName(new: String): EpisodeDraft = if (new.isBlank()) this else copy(name = new)

private fun String.stripPrefix(prefix: String): String {
  val trimmed = prefix.trim()
  if (trimmed.isEmpty() || !startsWith(trimmed, ignoreCase = true)) return this
  return substring(trimmed.length).trimStart(' ', '\t', '-', '–', ':')
}
