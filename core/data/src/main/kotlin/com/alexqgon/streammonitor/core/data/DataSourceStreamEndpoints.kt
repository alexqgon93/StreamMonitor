package com.alexqgon.streammonitor.core.data

import com.alexqgon.streammonitor.domain.StreamEndpoint

internal class NumbersStreamEndpoint(
    private val dataSource: NumbersDataSource,
) : StreamEndpoint {
    override suspend fun poll(): List<Int?> = dataSource.fetch()
}

internal class InputsStreamEndpoint(
    private val dataSource: InputsDataSource,
) : StreamEndpoint {
    override suspend fun poll(): List<Int?> = dataSource.fetch()
}
