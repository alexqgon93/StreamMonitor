package com.alexqgon.streammonitor.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NumberParserTest {
    @Nested
    inner class ValidBytes {
        @Test
        fun `byte 4 decodes section 0 item 1 without a result`() {
            NumberParser.parse(4) shouldBe ParseResult.Valid(0, 1, false)
        }

        @Test
        fun `byte 150 decodes section 2 item 5 with a result`() {
            NumberParser.parse(150) shouldBe ParseResult.Valid(2, 5, true)
        }

        @Test
        fun `byte 12 decodes section 0 item 3 without a result`() {
            NumberParser.parse(12) shouldBe ParseResult.Valid(0, 3, false)
        }

        @Test
        fun `byte 21 decodes section 1 item 5 without a result`() {
            NumberParser.parse(21) shouldBe ParseResult.Valid(1, 5, false)
        }

        @Test
        fun `byte 136 decodes section 0 item 2 with a result`() {
            NumberParser.parse(136) shouldBe ParseResult.Valid(0, 2, true)
        }

        @Test
        fun `byte 16 decodes section 0 item 4 without a result`() {
            NumberParser.parse(16) shouldBe ParseResult.Valid(0, 4, false)
        }

        @Test
        fun `byte 131 decodes section 3 item 0 with a result`() {
            NumberParser.parse(131) shouldBe ParseResult.Valid(3, 0, true)
        }
    }

    @Nested
    inner class InvalidBytes {
        @Test
        fun `byte 24 rejects item 6`() {
            NumberParser.parse(24) shouldBe ParseResult.Invalid(InvalidReason.ITEM_OUT_OF_RANGE)
        }

        @Test
        fun `byte 255 rejects item 31`() {
            NumberParser.parse(255) shouldBe ParseResult.Invalid(InvalidReason.ITEM_OUT_OF_RANGE)
        }

        @Test
        fun `all 256 byte values contain exactly 48 valid and 208 invalid values`() {
            val results = (0..255).map(NumberParser::parse)

            results.count { it is ParseResult.Valid } shouldBe 48
            results.count { it is ParseResult.Invalid } shouldBe 208
        }

        @Test
        fun `null input is an invalid null value`() {
            val result = NumberParser.parse(null)

            result.shouldBeInstanceOf<ParseResult.Invalid>().reason shouldBe InvalidReason.NULL_VALUE
        }

        @Test
        fun `an integer outside byte range is rejected without throwing`() {
            NumberParser.parse(256) shouldBe ParseResult.Invalid(InvalidReason.OUT_OF_BYTE_RANGE)
        }
    }
}
