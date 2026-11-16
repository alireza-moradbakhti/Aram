package com.aram.core.network

import com.aram.core.network.config.ApiConfig
import com.aram.core.network.config.ApiEnvironment
import com.aram.core.network.config.ApiMode
import com.aram.core.network.config.ApiPaths
import com.aram.core.network.config.EndpointProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ApiConfigTest {

    private fun endpointProvider(baseUrl: String = testConfig.baseUrl) = EndpointProvider(
        testConfig.copy(baseUrl = baseUrl),
    )

    @Test
    fun `paths resolve under the version prefix`() {
        val endpoints = endpointProvider()
        assertEquals("https://api.aram.test/api/v1/auth/login", endpoints.url(ApiPaths.AUTH_LOGIN))
        assertEquals("https://api.aram.test/api/v1/accounts", endpoints.url("accounts"))
        assertEquals("https://api.aram.test/api/v1/sync/push", endpoints.url(ApiPaths.SYNC_PUSH))
    }

    @Test
    fun `trailing slash is normalized`() {
        assertEquals(
            "https://api.aram.test/api/v1/me",
            endpointProvider("https://api.aram.test/").url(ApiPaths.ME),
        )
    }

    @Test
    fun `path ids are percent encoded`() {
        assertEquals("/transactions/a%20b", ApiPaths.byId(ApiPaths.TRANSACTIONS, "a b"))
        assertEquals("/goals/g1/contributions/c2", ApiPaths.contribution("g1", "c2"))
        assertEquals("/goals/g1/contributions", ApiPaths.contribution("g1"))
    }

    @Test
    fun `blank base url is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ApiConfig(ApiEnvironment.STAGE, ApiMode.REMOTE, "   ", "1.0")
        }
    }

    @Test
    fun `non https base url requires the cleartext escape hatch`() {
        assertThrows(IllegalArgumentException::class.java) {
            ApiConfig(ApiEnvironment.STAGE, ApiMode.REMOTE, "http://10.0.2.2:8080", "1.0")
        }
        val local = ApiConfig(
            ApiEnvironment.STAGE,
            ApiMode.REMOTE,
            "http://10.0.2.2:8080",
            "1.0",
            allowCleartext = true,
        )
        assertEquals("http://10.0.2.2:8080", local.normalizedBaseUrl)
    }

    @Test
    fun `production never allows cleartext`() {
        assertThrows(IllegalArgumentException::class.java) {
            ApiConfig(
                ApiEnvironment.PRODUCTION,
                ApiMode.REMOTE,
                "http://prod.example",
                "1.0",
                allowCleartext = true,
            )
        }
    }

    @Test
    fun `base url must not include the api prefix`() {
        assertThrows(IllegalArgumentException::class.java) {
            ApiConfig(ApiEnvironment.STAGE, ApiMode.MOCK, "https://api.aram.test/api/v1", "1.0")
        }
    }

    @Test
    fun `only production reports as production`() {
        assertEquals(false, ApiEnvironment.STAGE.isProduction)
        assertEquals(true, ApiEnvironment.PRODUCTION.isProduction)
    }
}
