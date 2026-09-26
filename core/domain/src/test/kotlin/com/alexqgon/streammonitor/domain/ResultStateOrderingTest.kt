package com.alexqgon.streammonitor.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class ResultStateOrderingTest {
    @Test
    fun `states expose the documented deterministic order`() {
        assertEquals(
            listOf(0, 1, 2, 3),
            listOf(
                ResultState.NotNeeded.sortOrder,
                ResultState.Pending.sortOrder,
                ResultState.Ready("03").sortOrder,
                ResultState.Failed(FailureReason.COMPUTATION_FAILED).sortOrder,
            ),
        )
    }
}
