package voice.core.scanner.hoerspiel

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import voice.core.scanner.Metadata
import kotlin.time.Duration.Companion.seconds

/**
 * Reads the JSONL format written by `tools/dump-tags.sh` and turns each episode folder into an
 * [EpisodeSource], translating ffprobe tag names the way Media3 would deliver them
 * (see the mapping table in docs/hoerspiel-tagging.md).
 */
internal object TagDump {

  fun parse(jsonl: String): List<EpisodeSource> {
    val audio = jsonl.lineSequence()
      .filter { it.isNotBlank() }
      .map { Json.parseToJsonElement(it).jsonObject }
      .filter { (it["kind"]?.jsonPrimitive?.contentOrNull ?: "audio") == "audio" }
      .toList()
    return audio
      .groupBy { it.string("path")!!.substringBefore('/') }
      .map { (folder, files) ->
        val sorted = files.sortedBy { it.string("path") }
        val fileNames = sorted.map { it.string("path")!!.substringAfter('/').substringBeforeLast('.') }
        EpisodeSource(
          folderName = folder,
          fileNames = fileNames,
          tags = sorted.first().toMetadata(fileNames.first().substringAfterLast('/')),
        )
      }
  }

  private fun JsonObject.toMetadata(fileName: String): Metadata? {
    val tags = this["tags"]?.jsonObject ?: return null
    if (tags.isEmpty()) return null
    val values = tags.mapKeys { it.key.lowercase() }.mapValues { it.value.jsonPrimitive.contentOrNull }
    val builder = Metadata.Builder(fileName)
    values.forEach { (key, value) ->
      if (value == null) return@forEach
      when (key) {
        "title" -> builder.title = value
        "artist" -> builder.artist = value
        "album" -> builder.album = value
        "album_artist" -> builder.albumArtist = value
        "genre" -> builder.genre = value
        "composer" -> builder.narrator = value
        "track" -> builder.setTrack(value)
        "disc" -> builder.setDisc(value)
        "date" -> builder.setYear(value)
        "grouping" -> builder.grouping = value
        "mvnm", "series" -> builder.series = value
        "mvin", "part" -> builder.part = value
      }
    }
    val duration = string("duration")?.toDoubleOrNull() ?: 1.0
    return builder.build(duration.seconds)
  }

  private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
}
