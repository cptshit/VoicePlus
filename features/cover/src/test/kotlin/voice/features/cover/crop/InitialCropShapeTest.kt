package voice.features.cover.crop

import io.kotest.matchers.shouldBe
import org.junit.Test
import voice.core.data.GridMode
import voice.core.data.hoerspiel.CoverStyle

class InitialCropShapeTest {

  @Test
  fun cdLookStartsSquareInEveryView() {
    GridMode.entries.forEach { initiallyBookShaped(it, CoverStyle.Cd) shouldBe false }
  }

  @Test
  fun jacketLookStartsBookShapedOnlyInTheShelfView() {
    initiallyBookShaped(GridMode.BOOKS, CoverStyle.Book) shouldBe true
    initiallyBookShaped(GridMode.LIST, CoverStyle.Book) shouldBe false
    initiallyBookShaped(GridMode.GRID, CoverStyle.Book) shouldBe false
  }
}
