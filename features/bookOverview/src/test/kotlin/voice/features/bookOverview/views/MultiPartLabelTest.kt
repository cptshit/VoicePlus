package voice.features.bookOverview.views

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "de")
class MultiPartLabelTest {

  private val resources = ApplicationProvider.getApplicationContext<Context>().resources

  @Test
  fun twoParter() {
    multiPartLabel(resources, part = 1, count = 2) shouldBe "Zweiteiler · Teil 1 von 2"
    multiPartLabel(resources, part = 2, count = 2) shouldBe "Zweiteiler · Teil 2 von 2"
  }

  @Test
  fun longerStories() {
    multiPartLabel(resources, part = 3, count = 3) shouldBe "Dreiteiler · Teil 3 von 3"
    multiPartLabel(resources, part = 5, count = 6) shouldBe "Mehrteiler · Teil 5 von 6"
  }

  @Test
  fun unknownCountShowsOnlyThePart() {
    multiPartLabel(resources, part = 1, count = null) shouldBe "Teil 1"
  }

  @Test
  fun noPartNoLabel() {
    multiPartLabel(resources, part = null, count = 2) shouldBe null
  }
}
