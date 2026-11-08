package voice.core.strings

import io.kotest.matchers.collections.shouldBeEmpty
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Every English string needs a German one, with the same placeholders. */
class GermanStringsTest {

  private class Entry(
    val kind: String,
    val texts: List<String>,
  )

  private fun read(path: String): Map<String, Entry> {
    val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path)).documentElement
    val result = linkedMapOf<String, Entry>()
    val nodes = root.childNodes
    for (i in 0 until nodes.length) {
      val node = nodes.item(i) as? org.w3c.dom.Element ?: continue
      if (node.getAttribute("translatable") == "false") continue
      val name = node.getAttribute("name")
      result[name] = when (node.tagName) {
        "plurals" -> {
          val items = node.getElementsByTagName("item")
          Entry("plurals", (0 until items.length).map { items.item(it).textContent })
        }
        else -> Entry(node.tagName, listOf(node.textContent))
      }
    }
    return result
  }

  private val english = read("src/main/res/values/strings.xml")
  private val german = read("src/main/res/values-de/strings.xml")

  private fun placeholders(text: String) = Regex("%(\\d+\\$)?[a-z]").findAll(text).map {
    it.value.replace(Regex("^%\\d+\\$"), "%")
  }.sorted().toList()

  @Test
  fun everyEnglishStringHasAGermanOne() {
    english.keys.filter { it !in german }.shouldBeEmpty()
  }

  @Test
  fun germanHasNoStringsEnglishLacks() {
    german.keys.filter { it !in english }.shouldBeEmpty()
  }

  @Test
  fun placeholdersMatchTheEnglishText() {
    english.mapNotNull { (name, en) ->
      val de = german[name] ?: return@mapNotNull null
      val enHolders = en.texts.map(::placeholders).distinct()
      val deHolders = de.texts.map(::placeholders).distinct()
      // Plural items may differ ("one" can omit the number), but must not use something English lacks.
      name.takeUnless {
        en.kind == de.kind &&
          deHolders.all { holders -> enHolders.any { holders.toSet() == it.toSet() || holders.all(it::contains) } }
      }
    }.shouldBeEmpty()
  }
}
