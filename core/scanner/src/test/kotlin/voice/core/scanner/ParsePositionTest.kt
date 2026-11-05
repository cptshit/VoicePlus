package voice.core.scanner

import io.kotest.matchers.shouldBe
import org.junit.Test

class ParsePositionTest {

  @Test
  fun formatsFoundInRealRips() {
    // all variants of TRCK/TPOS seen in fixtures/tags (see docs/hoerspiel-tagging.md)
    parsePosition("1") shouldBe (1 to null)
    parsePosition("01") shouldBe (1 to null)
    parsePosition("1/10") shouldBe (1 to 10)
    parsePosition("01/08") shouldBe (1 to 8)
    parsePosition("4/4") shouldBe (4 to 4)
  }

  @Test
  fun whitespaceIsTolerated() {
    parsePosition(" 2 / 4 ") shouldBe (2 to 4)
  }

  @Test
  fun garbageIsRejected() {
    parsePosition("") shouldBe null
    parsePosition("A") shouldBe null
    parsePosition("1/") shouldBe null
    parsePosition("CD 1") shouldBe null
  }
}
