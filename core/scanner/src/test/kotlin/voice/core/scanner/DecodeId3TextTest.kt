package voice.core.scanner

import io.kotest.matchers.shouldBe
import org.junit.Test

class DecodeId3TextTest {

  @Test
  fun encodings() {
    decodeId3Text(byteArrayOf(0) + "Flüsterer".toByteArray(Charsets.ISO_8859_1)) shouldBe "Flüsterer"
    decodeId3Text(byteArrayOf(1) + "Flüsterer".toByteArray(Charsets.UTF_16)) shouldBe "Flüsterer"
    decodeId3Text(byteArrayOf(2) + "Flüsterer".toByteArray(Charsets.UTF_16BE)) shouldBe "Flüsterer"
    decodeId3Text(byteArrayOf(3) + "Flüsterer".toByteArray(Charsets.UTF_8)) shouldBe "Flüsterer"
  }

  @Test
  fun onlyFirstValueIsUsed() {
    decodeId3Text(byteArrayOf(3) + "a\u0000b".toByteArray()) shouldBe "a"
  }

  @Test
  fun invalidInput() {
    decodeId3Text(byteArrayOf()) shouldBe null
    decodeId3Text(byteArrayOf(3)) shouldBe null
    decodeId3Text(byteArrayOf(9, 65)) shouldBe null
  }
}
