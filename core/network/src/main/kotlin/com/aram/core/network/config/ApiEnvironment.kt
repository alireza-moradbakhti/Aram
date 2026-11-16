package com.aram.core.network.config

/**
 * The two deployments in `docs/API_CONTRACT.md`. [isProduction] is the single
 * production check; nothing else in the codebase should compare environments.
 */
enum class ApiEnvironment {
    STAGE,
    PRODUCTION,
    ;

    val isProduction: Boolean get() = this == PRODUCTION
}
