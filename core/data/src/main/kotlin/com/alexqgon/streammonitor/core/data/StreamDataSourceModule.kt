package com.alexqgon.streammonitor.core.data

import com.alexqgon.streammonitor.domain.StreamEndpoint
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object StreamDataSourceModule {
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    @Provides
    @Singleton
    fun provideNumbersDataSource(
        config: StreamDataSourceConfig,
        client: Lazy<HttpClient>,
    ): NumbersDataSource = when (config.mode) {
        StreamDataSourceMode.MOCK -> MockNumbersDataSource(
            MockScenarios.definition(config.mockScenario),
        )

        StreamDataSourceMode.REMOTE -> RemoteNumbersDataSource(
            client = client.get(),
            endpoint = config.numbersEndpoint.requireEndpoint("numbers"),
        )
    }

    @Provides
    @Singleton
    fun provideInputsDataSource(
        config: StreamDataSourceConfig,
        client: Lazy<HttpClient>,
    ): InputsDataSource = when (config.mode) {
        StreamDataSourceMode.MOCK -> MockInputsDataSource(
            MockScenarios.definition(config.mockScenario),
        )

        StreamDataSourceMode.REMOTE -> RemoteInputsDataSource(
            client = client.get(),
            endpoint = config.inputsEndpoint.requireEndpoint("inputs"),
        )
    }

    @Provides
    @Singleton
    @NumbersEndpoint
    fun provideNumbersEndpoint(dataSource: NumbersDataSource): StreamEndpoint =
        NumbersStreamEndpoint(dataSource)

    @Provides
    @Singleton
    @InputsEndpoint
    fun provideInputsEndpoint(dataSource: InputsDataSource): StreamEndpoint =
        InputsStreamEndpoint(dataSource)
}

private fun String?.requireEndpoint(name: String): String =
    requireNotNull(this?.takeIf(String::isNotBlank)) {
        "$name endpoint must be configured for remote data sources"
    }
