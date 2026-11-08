package voice.features.bookOverview.views

import io.kotest.matchers.shouldBe
import org.junit.Test

class GeneratedCoverTest {

  @Test
  fun abbreviatesByInitials() {
    seriesAbbreviation("Gabriel Burns") shouldBe "GB"
    seriesAbbreviation("Fünf Freunde") shouldBe "FF"
    seriesAbbreviation("Die drei Fragezeichen") shouldBe "DF"
    seriesAbbreviation("Die drei ???") shouldBe "DD"
    seriesAbbreviation("Sherlock Holmes - Die geheimen Fälle") shouldBe "SHD"
  }

  @Test
  fun keepsShortCapitalisedNamesAndCutsLongSingleWords() {
    seriesAbbreviation("TKKG") shouldBe "TKKG"
    seriesAbbreviation("Bibi") shouldBe "BIB"
    seriesAbbreviation("") shouldBe ""
    seriesAbbreviation("???") shouldBe ""
  }

  @Test
  fun sameSeriesSameColourIgnoringCase() {
    seriesCoverColor("Gabriel Burns") shouldBe seriesCoverColor("gabriel burns")
  }
}
