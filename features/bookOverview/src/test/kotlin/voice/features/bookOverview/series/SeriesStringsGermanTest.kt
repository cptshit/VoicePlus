package voice.features.bookOverview.series

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import voice.core.strings.R as StringsR

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "de")
class SeriesStringsGermanTest {

  private val resources = ApplicationProvider.getApplicationContext<Context>().resources

  @Test
  fun episodeNumberAboveTheTitle() {
    resources.getString(StringsR.string.series_volume, "2") shouldBe "Folge 2"
  }

  @Test
  fun stackCountsEpisodes() {
    resources.getQuantityString(StringsR.plurals.series_book_count, 1, 1) shouldBe "1 Folge"
    resources.getQuantityString(StringsR.plurals.series_book_count, 45, 45) shouldBe "45 Folgen"
  }
}
