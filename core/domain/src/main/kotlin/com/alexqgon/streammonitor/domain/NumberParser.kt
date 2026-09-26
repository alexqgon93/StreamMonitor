package com.alexqgon.streammonitor.domain

/**
 * Decodes one raw number value without I/O, logging, or exceptions for invalid input.
 */
object NumberParser {
    fun parse(value: Int?): ParseResult {
        if (value == null) {
            return ParseResult.Invalid(InvalidReason.NULL_VALUE)
        }
        if (value !in 0..255) {
            return ParseResult.Invalid(InvalidReason.OUT_OF_BYTE_RANGE)
        }

        val sectionIndex = value and SECTION_MASK
        val itemIndex = (value shr ITEM_SHIFT) and ITEM_MASK
        val needsResult = (value and RESULT_FLAG_MASK) != 0

        return if (itemIndex > MAX_ITEM_INDEX) {
            ParseResult.Invalid(InvalidReason.ITEM_OUT_OF_RANGE)
        } else {
            ParseResult.Valid(sectionIndex, itemIndex, needsResult)
        }
    }

    private const val SECTION_MASK = 0b11
    private const val ITEM_SHIFT = 2
    private const val ITEM_MASK = 0b1_1111
    private const val RESULT_FLAG_MASK = 1 shl 7
    private const val MAX_ITEM_INDEX = 5
}
