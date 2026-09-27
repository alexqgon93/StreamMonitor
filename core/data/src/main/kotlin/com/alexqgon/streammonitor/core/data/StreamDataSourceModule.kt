package com.alexqgon.streammonitor.core.data

import com.alexqgon.streammonitor.domain.StreamEndpoint
import com.alexqgon.streammonitor.domain.StreamCoordinator
import com.alexqgon.streammonitor.domain.StreamCoordinatorFactory
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StreamDataSourceModule {
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = streamHttpClient(OkHttp.create())

    @Provides
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
    @NumbersEndpoint
    fun provideNumbersEndpoint(dataSource: NumbersDataSource): StreamEndpoint =
        NumbersStreamEndpoint(dataSource)

    @Provides
    @InputsEndpoint
    fun provideInputsEndpoint(dataSource: InputsDataSource): StreamEndpoint =
        InputsStreamEndpoint(dataSource)

    /**
     * Builds one coordinator per run over freshly resolved endpoints.
     *
     * The data sources are deliberately unscoped: a mock source owns a read cursor, so sharing one
     * across runs would make a restart resume from wherever the previous run stopped. Resolving the
     * providers inside [StreamCoordinatorFactory.create] gives every run a cursor at zero without
     * relying on the previous run having been drained.
     */
    @Provides
    @Singleton
    fun provideStreamCoordinatorFactory(
        @NumbersEndpoint numbers: Provider<StreamEndpoint>,
        @InputsEndpoint inputs: Provider<StreamEndpoint>,
    ): StreamCoordinatorFactory = StreamCoordinatorFactory {
        StreamCoordinator(numbers = numbers.get(), inputs = inputs.get())
    }
}

private fun String?.requireEndpoint(name: String): String =
    requireNotNull(this?.takeIf(String::isNotBlank)) {
        "$name endpoint must be configured for remote data sources"
    }
