package com.aram.app.di

import com.aram.app.BuildConfig
import com.aram.core.network.config.ApiConfig
import com.aram.core.network.config.ApiEnvironment
import com.aram.core.network.config.ApiMode
import com.aram.core.network.config.EndpointProvider
import com.aram.core.network.http.AramHttpClient
import com.aram.core.network.http.TokenProvider
import com.aram.core.network.mock.MockApiEngine
import com.aram.core.network.mock.MockApiStore
import com.aram.core.network.mock.MockScenarioController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApiConfig(): ApiConfig = ApiConfig(
        environment = ApiEnvironment.valueOf(BuildConfig.API_ENV),
        mode = ApiMode.valueOf(BuildConfig.API_MODE),
        baseUrl = BuildConfig.API_BASE_URL,
        appVersion = BuildConfig.VERSION_NAME,
        allowCleartext = BuildConfig.API_ALLOW_CLEARTEXT,
    )

    @Provides
    @Singleton
    fun provideEndpointProvider(config: ApiConfig): EndpointProvider = EndpointProvider(config)

    @Provides
    @Singleton
    fun provideMockScenarioController(): MockScenarioController = MockScenarioController()

    @Provides
    @Singleton
    fun provideMockApiStore(): MockApiStore = MockApiStore()

    @Provides
    @Singleton
    fun provideHttpClient(
        config: ApiConfig,
        scenarios: MockScenarioController,
        store: MockApiStore,
    ): HttpClient = AramHttpClient.create(
        config = config,
        engine = if (config.mode == ApiMode.MOCK) {
            MockApiEngine.create(store = store, scenarios = scenarios)
        } else {
            OkHttp.create()
        },
        // AR-040 swaps in the Keystore-backed session.
        tokenProvider = TokenProvider.None,
    )
}
