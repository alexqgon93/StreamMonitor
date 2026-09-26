package com.alexqgon.streammonitor.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ResultComputerTest {
    @Nested
    inner class Formatting {
        @Test
        fun `result is always two characters with leading zero padding`() {
            ResultComputer.compute(3u.toUShort()) shouldBe "03"
            ResultComputer.compute(48u.toUShort()) shouldBe "48"
            ResultComputer.compute(65_535u.toUShort()) shouldHaveLength 2
        }
    }

    @Nested
    inner class RepeatedResults {
        @Test
        fun `different inputs repeat the same result every one hundred values`() {
            ResultComputer.compute(3u.toUShort()) shouldBe ResultComputer.compute(103u.toUShort())
            ResultComputer.compute(48u.toUShort()) shouldBe ResultComputer.compute(148u.toUShort())
        }
    }
}
