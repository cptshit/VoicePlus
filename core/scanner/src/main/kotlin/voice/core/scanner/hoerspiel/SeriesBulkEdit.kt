package voice.core.scanner.hoerspiel

import voice.core.data.BookId

/** The editable fields of one episode, as shown in the bulk editor. */
public data class EpisodeDraft(
  val id: BookId,
  val name: String,
  val part: String?,
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

  /** Numbers the episodes [start], [start] + 1, … in their current order. */
  public data class Renumber(val start: Int = 1) : BulkRule
}

/** Old and new title and number of one episode; [changed] is false when the rules left it alone. */
public data class EpisodeChange(
  val id: BookId,
  val oldName: String,
  val newName: String,
  val oldPart: String?,
  val newPart: String?,
) {
  val nameChanged: Boolean get() = oldName != newName
  val partChanged: Boolean get() = oldPart != newPart
  val changed: Boolean get() = nameChanged || partChanged
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
    EpisodeChange(old.id, old.name, new.name, old.part, new.part)
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
  is BulkRule.Renumber -> mapIndexed { index, episode -> episode.copy(part = (rule.start + index).toString()) }
}

private fun EpisodeDraft.withName(new: String): EpisodeDraft = if (new.isBlank()) this else copy(name = new)

private fun String.stripPrefix(prefix: String): String {
  val trimmed = prefix.trim()
  if (trimmed.isEmpty() || !startsWith(trimmed, ignoreCase = true)) return this
  return substring(trimmed.length).trimStart(' ', '\t', '-', '–', ':')
}
