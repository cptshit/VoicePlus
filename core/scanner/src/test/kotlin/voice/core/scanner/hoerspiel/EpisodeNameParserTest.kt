package voice.core.scanner.hoerspiel

import io.kotest.matchers.shouldBe
import org.junit.Test

class EpisodeNameParserTest {

  private fun parse(name: String) = EpisodeNameParser.parse(name)

  @Test
  fun folderPatterns() {
    parse("GabrielBurns - 002 - Die Brut") shouldBe ParsedName("Gabriel Burns", 2, "Die Brut")
    parse("Gabriel.Burns.01.-.Der.Fluesterer") shouldBe ParsedName("Gabriel Burns", 1, "Der Fluesterer")
    parse("GabrielBurns - 015 - Ohne Bewusstsein Teil 1") shouldBe ParsedName("Gabriel Burns", 15, "Ohne Bewusstsein Teil 1")
    parse("GabrielBurns - 026 - R") shouldBe ParsedName("Gabriel Burns", 26, "R")
  }

  @Test
  fun albumPatterns() {
    parse("(41) Verehrung") shouldBe ParsedName(null, 41, "Verehrung")
    parse("37/Der Atem der Fahlen") shouldBe ParsedName(null, 37, "Der Atem der Fahlen")
    parse("32 -  Die, die nicht bluten") shouldBe ParsedName(null, 32, "Die, die nicht bluten")
    parse("33 Schmerz") shouldBe ParsedName(null, 33, "Schmerz")
    parse("F34-Ich weiss, was Angst ist") shouldBe ParsedName(null, 34, "Ich weiss, was Angst ist")
    parse("F43_Fern von allen Tiefen") shouldBe ParsedName(null, 43, "Fern von allen Tiefen")
    parse("Gabriel Burns 36 - Am Ende aller Tage") shouldBe ParsedName("Gabriel Burns", 36, "Am Ende aller Tage")
    parse("Rand der Gezeiten") shouldBe ParsedName(null, null, "Rand der Gezeiten")
  }

  @Test
  fun commonVariants() {
    parse("Folge 12: Titel") shouldBe ParsedName(null, 12, "Titel")
    parse("Serie - Folge 12 - Titel") shouldBe ParsedName("Serie", 12, "Titel")
  }

  @Test
  fun namesWithoutEpisodeNumberStayWhole() {
    parse("Welt der Dämmerung - Directors Cut [Teil 2]") shouldBe ParsedName(null, null, "Welt der Dämmerung - Directors Cut [Teil 2]")
    parse("Kapitel 04a") shouldBe ParsedName(null, null, "Kapitel 04a")
    parse("1984") shouldBe ParsedName(null, null, "1984")
    parse("Dr. No") shouldBe ParsedName(null, null, "Dr. No")
  }

  @Test
  fun camelCaseOnlyWithoutSpaces() {
    EpisodeNameParser.splitCamelCase("GabrielBurns") shouldBe "Gabriel Burns"
    EpisodeNameParser.splitCamelCase("TKKG") shouldBe "TKKG"
    EpisodeNameParser.splitCamelCase("Die drei ???") shouldBe "Die drei ???"
  }

  @Test
  fun comparisonKeyIgnoresSpellingDetails() {
    comparisonKey("GabrielBurns") shouldBe comparisonKey("Gabriel Burns")
    comparisonKey("Der Fluesterer") shouldBe comparisonKey("Der Flüsterer")
    comparisonKey("Ohne Bewusstsein Teil 1") shouldBe comparisonKey("Ohne Bewusstsein [Teil 1]")
  }

  @Test
  fun stripTrack() {
    FileNameAnalysis.stripTrack("01 - Die Brut") shouldBe "Die Brut"
    FileNameAnalysis.stripTrack("01-Nebelsee") shouldBe "Nebelsee"
    FileNameAnalysis.stripTrack("01 Am Ende aller Tage") shouldBe "Am Ende aller Tage"
    FileNameAnalysis.stripTrack("01-01 Zwei Horizonte") shouldBe "Zwei Horizonte"
    FileNameAnalysis.stripTrack("027 - Zwiespalt - 01") shouldBe "Zwiespalt"
    FileNameAnalysis.stripTrack("Track 01") shouldBe "Track 01"
  }
}
