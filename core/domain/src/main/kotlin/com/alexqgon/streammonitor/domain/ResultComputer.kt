package com.alexqgon.streammonitor.domain

/**
 * Computes the display value for a 16-bit unsigned input.
 *
 * The bounded loop is an explicit CPU-cost simulation, not business logic. It performs
 * [SIMULATED_WORK_ITERATIONS] integer operations. Its duration is deliberately not fixed:
 * it depends on the device and build mode. The constant can be set to zero or removed without
 * changing the returned value.
 */
object ResultComputer {
    fun compute(input: UShort): String {
        var accumulator = input.toInt()
        repeat(SIMULATED_WORK_ITERATIONS) { iteration ->
            accumulator = accumulator * 31 + iteration
        }
        simulationSink = accumulator

        return (input.toInt() % 100).toString().padStart(2, '0')
    }

    private const val SIMULATED_WORK_ITERATIONS = 100_000

    @Volatile
    private var simulationSink: Int = 0
}
