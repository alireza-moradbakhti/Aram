import com.android.build.api.variant.BuildConfigField
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val appVersionName = providers.gradleProperty("aram.versionName").getOrElse("0.1.0")
val appVersionCode = providers.gradleProperty("aram.versionCode").getOrElse("1").toInt()
val buildDate = providers.gradleProperty("buildDate")
    .getOrElse(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE))

// Contract hosts are build configuration, never Kotlin constants.
val stageBaseUrl = providers.gradleProperty("aram.api.stageBaseUrl").get()
val prodBaseUrl = providers.gradleProperty("aram.api.prodBaseUrl").get()

// .env and local.properties are gitignored; environment variables take precedence.
val localSecrets = Properties().apply {
    listOf(rootProject.file(".env"), rootProject.file("local.properties"))
        .filter { it.exists() }
        .forEach { file -> file.inputStream().use(::load) }
}

fun secret(name: String): String? =
    providers.environmentVariable(name).orNull ?: localSecrets.getProperty(name)

val releaseKeystore = secret("ARAM_KEYSTORE_PATH")
    ?.let(::file)
    ?.takeIf { it.exists() }

android {
    namespace = "com.aram.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.aram.app"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")
        manifestPlaceholders["appName"] = "Aram"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = secret("ARAM_KEYSTORE_PASSWORD")
                keyAlias = secret("ARAM_KEY_ALIAS")
                keyPassword = secret("ARAM_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Preview releases fall back to the debug key until a release keystore exists.
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
    }

    flavorDimensions += "stage"
    productFlavors {
        create("alpha") {
            dimension = "stage"
            applicationIdSuffix = ".alpha"
            versionNameSuffix = "-alpha"
            // Launcher label stays "Aram" across stages; the alpha icon carries the marker.
            manifestPlaceholders["appName"] = "Aram"
            buildConfigField("String", "BUILD_STAGE", "\"alpha\"")
            buildConfigField("boolean", "IS_PRE_RELEASE", "true")
        }
        create("beta") {
            dimension = "stage"
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            manifestPlaceholders["appName"] = "Aram Beta"
            buildConfigField("String", "BUILD_STAGE", "\"beta\"")
            buildConfigField("boolean", "IS_PRE_RELEASE", "true")
        }
        create("prod") {
            dimension = "stage"
            manifestPlaceholders["appName"] = "Aram"
            buildConfigField("String", "BUILD_STAGE", "\"prod\"")
            buildConfigField("boolean", "IS_PRE_RELEASE", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // AGP 9.5's release lint tool (32.5.1) is not yet published; revisit when available.
        checkReleaseBuilds = false
    }
}

androidComponents {
    onVariants { variant ->
        val stage = (variant.flavorName ?: "prod").ifEmpty { "prod" }
        variant.outputs.forEach { output ->
            output.outputFileName.set(
                output.versionCode.map { code ->
                    "aram-$stage-${variant.buildType}-$code-$buildDate.apk"
                },
            )
        }

        // --- API matrix (docs/API_CONTRACT.md) ---------------------------------
        // alpha* -> stage + mock; beta* -> stage + remote; prodDebug -> stage +
        // remote; prodRelease -> production + remote. Hosts live only in
        // gradle.properties; debug builds may point ARAM_API_BASE_URL anywhere.
        val isRelease = variant.buildType == "release"
        val environment = if (stage == "prod" && isRelease) "PRODUCTION" else "STAGE"

        val requestedMode = providers.environmentVariable("ARAM_API_MODE").orNull
            ?: providers.gradleProperty("apiMode").orNull
        val mode = (requestedMode ?: if (stage == "alpha") "mock" else "remote")
            .uppercase()
            .also { require(it == "MOCK" || it == "REMOTE") { "Unknown API mode '$it'; use mock or remote." } }

        val isRequested = gradle.startParameter.taskNames.any {
            it.lowercase().contains(variant.name.lowercase())
        }
        if (isRelease && isRequested) {
            check(mode != "MOCK") {
                "Release builds cannot use the mock API; pass -PapiMode=remote."
            }
        }

        val hostOverride = providers.environmentVariable("ARAM_API_BASE_URL").orNull
            ?.takeIf { variant.buildType == "debug" && it.isNotBlank() }
        val baseUrl = hostOverride ?: if (environment == "PRODUCTION") prodBaseUrl else stageBaseUrl
        val allowCleartext = hostOverride != null && baseUrl.startsWith("http://")

        if (isRelease && isRequested) {
            check(baseUrl.startsWith("https://")) {
                "Release builds must use an HTTPS API host (got '$baseUrl')."
            }
            check(environment != "PRODUCTION" || baseUrl == prodBaseUrl) {
                "prodRelease must use the production host from aram.api.prodBaseUrl."
            }
        }

        val fields = requireNotNull(variant.buildConfigFields) {
            "buildConfig must stay enabled for the API matrix."
        }
        fields.put(
            "API_ENV",
            BuildConfigField("String", "\"$environment\"", "API environment: STAGE | PRODUCTION"),
        )
        fields.put(
            "API_MODE",
            BuildConfigField("String", "\"$mode\"", "API mode: MOCK | REMOTE"),
        )
        fields.put(
            "API_BASE_URL",
            BuildConfigField("String", "\"$baseUrl\"", "Host from gradle.properties or a debug override"),
        )
        fields.put(
            "API_ALLOW_CLEARTEXT",
            BuildConfigField("boolean", allowCleartext.toString(), "Debug-only local-server escape hatch"),
        )
    }
}

if (releaseKeystore == null) {
    logger.warn(
        "Release signing is not configured. Release variants will be signed with the debug key. " +
            "Set ARAM_KEYSTORE_PATH, ARAM_KEYSTORE_PASSWORD, ARAM_KEY_ALIAS, and ARAM_KEY_PASSWORD in .env or the environment.",
    )
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    implementation(project(":core:logging"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))

    implementation(project(":feature:onboarding"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:transactions"))
    implementation(project(":feature:budgets"))
    implementation(project(":feature:goals"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.mock)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.bundles.lifecycle)

    debugImplementation(libs.bundles.composeDebug)

    testImplementation(libs.bundles.unitTest)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.androidTest)
}
