package com.alexqgon.streammonitor.di

import com.alexqgon.streammonitor.BuildConfig
import com.alexqgon.streammonitor.core.data.MockScenario
import com.alexqgon.streammonitor.core.data.StreamDataSourceConfig
import com.alexqgon.streammonitor.core.data.StreamDataSourceMode
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StreamDataSourceConfigModule {
    @Provides
    @Singleton
    fun provideStreamDataSourceConfig(): StreamDataSourceConfig = StreamDataSourceConfig(
        mode = StreamDataSourceMode.valueOf(BuildConfig.STREAM_DATA_SOURCE.uppercase(Locale.ROOT)),
        mockScenario = MockScenario.valueOf(BuildConfig.MOCK_SCENARIO.uppercase(Locale.ROOT)),
        numbersEndpoint = BuildConfig.NUMBERS_ENDPOINT,
        inputsEndpoint = BuildConfig.INPUTS_ENDPOINT,
    )
}
