package com.alexqgon.streammonitor.core.data

interface NumbersDataSource {
    suspend fun fetch(): List<Int?>
}

interface InputsDataSource {
    suspend fun fetch(): List<Int?>
}

enum class StreamDataSourceMode {
    MOCK,
    REMOTE,
}

enum class MockScenario {
    HAPPY_PATH,
    MALFORMED,
    UNBALANCED,
    LARGE,
}

data class StreamDataSourceConfig(
    val mode: StreamDataSourceMode,
    val mockScenario: MockScenario,
    val numbersEndpoint: String? = null,
    val inputsEndpoint: String? = null,
)
