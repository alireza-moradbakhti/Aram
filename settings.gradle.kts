pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // Myket proxy, last resort: google() is unreachable from some
        // networks; the mirror serves the same artifacts unchanged.
        maven { url = uri("https://maven.myket.ir/") }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.myket.ir/") }
    }
}

rootProject.name = "Aram"
include(":app")

include(":core:model")
include(":core:money")
include(":core:common")
include(":core:logging")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:domain")
include(":core:data")
include(":core:designsystem")

include(":feature:onboarding")
include(":feature:dashboard")
include(":feature:transactions")
include(":feature:budgets")
include(":feature:goals")
include(":feature:settings")
