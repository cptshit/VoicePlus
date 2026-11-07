package voice.core.scanner

import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoverLocationsTest {

  @Test
  fun coverAndFolderImagesComeFirst() {
    CoverLocations.sortImages(listOf("back.jpg", "Booklet 1.png", "Cover.JPG", "a.jpg")) { it } shouldBe
      listOf("Cover.JPG", "a.jpg", "back.jpg", "Booklet 1.png")
    CoverLocations.sortImages(listOf("scan.png", "folder.jpg", "front.png")) { it } shouldBe
      listOf("folder.jpg", "front.png", "scan.png")
  }

  @Test
  fun parentStaysInsideTheGrantedTree() {
    CoverLocations.parentDocumentId("primary:Hörspiele", "primary:Hörspiele/Gabriel.Burns.01.-.Der.Fluesterer") shouldBe
      "primary:Hörspiele"
    CoverLocations.parentDocumentId("primary:Hörspiele", "primary:Hörspiele/Gabriel Burns/Folge 1") shouldBe
      "primary:Hörspiele/Gabriel Burns"
    // the book is the granted folder itself, or the parent would be outside of it
    CoverLocations.parentDocumentId("primary:Hörspiele/Folge 1", "primary:Hörspiele/Folge 1") shouldBe null
    CoverLocations.parentDocumentId("primary:Hörspiele/Gabriel", "primary:Hörspiele/Gabriel Burns/Folge 1") shouldBe null
    // ids that are no paths
    CoverLocations.parentDocumentId("msf:12", "msf:13") shouldBe null
  }

  @Test
  fun parentFolderOfASafBookFolder() {
    val tree = DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", "primary:Hörspiele")
    val book = DocumentsContract.buildDocumentUriUsingTree(tree, "primary:Hörspiele/Gabriel.Burns.01.-.Der.Fluesterer")

    CoverLocations.parentFolder(book) shouldBe DocumentsContract.buildDocumentUriUsingTree(tree, "primary:Hörspiele")
  }

  @Test
  fun parentFolderOfAFile() {
    CoverLocations.parentFolder(Uri.parse("file:///storage/Hörspiele/Folge%201")) shouldBe Uri.parse("file:///storage/H%C3%B6rspiele")
  }
}
