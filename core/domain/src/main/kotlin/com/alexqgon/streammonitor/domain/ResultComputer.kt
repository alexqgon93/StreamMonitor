package com.alexqgon.streammonitor.domain

/**
 * Computes the display value for a 16-bit unsigned input.
 *
 * The bounded loop is an explicit CPU-cost simulation, not business logic. It performs
 * [SIMULATED_WORK_ITERATIONS] integer operations and is intended to simulate roughly
 * 0.1-2 ms of CPU work on development hardware. The constant can be removed or adjusted
 * without changing the returned value.
 */
object ResultComputer {
    fun compute(input: UShort): String {
        var accumulator = input.toInt()
        repeat(SIMULATED_WORK_ITERATIONS) { iteration ->
            accumulator = accumulator * 31 + iteration
        }
        if (accumulator == Int.MIN_VALUE) {
            accumulator++
        }

        return (input.toInt() % 100).toString().padStart(2, '0')
    }

    private const val SIMULATED_WORK_ITERATIONS = 100_000
}
