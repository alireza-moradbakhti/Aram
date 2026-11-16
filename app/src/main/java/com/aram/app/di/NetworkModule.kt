package com.aram.app.di

import com.aram.app.BuildConfig
import com.aram.core.network.config.ApiConfig
import com.aram.core.network.config.ApiEnvironment
import com.aram.core.network.config.ApiMode
import com.aram.core.network.config.EndpointProvider
import com.aram.core.network.http.AramHttpClient
import com.aram.core.network.http.TokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
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
    fun provideHttpClient(config: ApiConfig): HttpClient = AramHttpClient.create(
        config = config,
        engine = if (config.mode == ApiMode.MOCK) mockPlaceholderEngine() else OkHttp.create(),
        // AR-040 swaps in the Keystore-backed session.
        tokenProvider = TokenProvider.None,
    )

    /** Stands in until AR-039 brings the contract-shaped fixtures. */
    private fun mockPlaceholderEngine(): HttpClientEngine = MockEngine {
        respond(
            content = """{"error":{"code":"not_implemented","message":"Mock API arrives with AR-039.","request_id":"mock"}}""",
            status = HttpStatusCode.NotImplemented,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }
}
