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

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.bundles.lifecycle)
    implementation(libs.androidx.compose.material.icons.core)

    debugImplementation(libs.bundles.composeDebug)

    testImplementation(libs.bundles.unitTest)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.androidTest)
}
