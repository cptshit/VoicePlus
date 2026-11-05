package voice.core.scanner

import voice.core.data.MarkData
import kotlin.time.Duration

internal data class Metadata(
  val duration: Long,
  val artist: String?,
  val album: String?,
  val title: String?,
  val fileName: String,
  val chapters: List<MarkData>,
  val genre: String?,
  val narrator: String?,
  val series: String?,
  val part: String?,
  val trackNumber: Int? = null,
  val discNumber: Int? = null,
  val discCount: Int? = null,
  val albumArtist: String? = null,
  val grouping: String? = null,
  val year: Int? = null,
) {

  internal class Builder(val fileName: String) {
    var artist: String? = null
    var album: String? = null
    var title: String? = null
    val chapters = mutableListOf<MarkData>()
    var genre: String? = null
    var narrator: String? = null
    var series: String? = null
    var part: String? = null
    var trackNumber: Int? = null
    var discNumber: Int? = null
    var discCount: Int? = null
    var albumArtist: String? = null
    var grouping: String? = null
    var year: Int? = null
    val vorbisChapterNames = mutableMapOf<Int, String>()
    val vorbisChapterStarts = mutableMapOf<Int, Long>()

    fun build(duration: Duration): Metadata {
      vorbisChapterNames.keys.toList()
        .sorted()
        .mapNotNullTo(chapters) { index ->
          val name = vorbisChapterNames[index]
          val start = vorbisChapterStarts[index]
          if (name != null && start != null) {
            MarkData(startMs = start, name = name)
          } else {
            null
          }
        }

      return Metadata(
        duration = duration.inWholeMilliseconds,
        artist = artist,
        album = album,
        title = title ?: fileName,
        fileName = fileName,
        chapters = chapters,
        genre = genre,
        narrator = narrator,
        series = series,
        part = part,
        trackNumber = trackNumber,
        discNumber = discNumber,
        discCount = discCount,
        albumArtist = albumArtist,
        grouping = grouping,
        year = year,
      )
    }

    /** Parses position tags such as "03", "3/10" or " 2 / 4 " into track or disc number. */
    fun setTrack(value: String) {
      trackNumber = parsePosition(value)?.first ?: trackNumber
    }

    fun setDisc(value: String) {
      val (number, count) = parsePosition(value) ?: return
      discNumber = number
      discCount = count ?: discCount
    }

    fun setYear(value: String) {
      year = YEAR.find(value)?.value?.toIntOrNull() ?: year
    }
  }
}

private val YEAR = Regex("""\b\d{4}\b""")
private val POSITION = Regex("""^\s*(\d+)\s*(?:/\s*(\d+)\s*)?$""")

internal fun parsePosition(value: String): Pair<Int, Int?>? {
  val match = POSITION.find(value) ?: return null
  val number = match.groupValues[1].toIntOrNull() ?: return null
  val count = match.groupValues[2].takeIf { it.isNotEmpty() }?.toIntOrNull()
  return number to count
}
