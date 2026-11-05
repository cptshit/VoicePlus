package voice.core.scanner

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileNameWithoutExtensionTest {

  @Test
  fun safDocumentUri() {
    Uri.parse(
      "content://com.android.externalstorage.documents/tree/primary%3AH%C3%B6rspiele/document/" +
        "primary%3AH%C3%B6rspiele%2FGabrielBurns%20-%20041%20-%20Verehrung%2FCD1%2F01%20-%20Einleitung.mp3",
    ).fileNameWithoutExtension() shouldBe "01 - Einleitung"
  }

  @Test
  fun fileUri() {
    Uri.parse("file:///storage/Hoerspiele/GabrielBurns%20-%20002%20-%20Die%20Brut/01%20-%20Die%20Brut.mp3")
      .fileNameWithoutExtension() shouldBe "01 - Die Brut"
  }
}
