// Top-level build file where you can add configuration options common to all sub-projects/modules.
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

// Fails the build when a non-null assertion (!!) appears in main sources.
// AR-037: crashes must be typed failures, never a null-pointer surprise.
val verifyNoNotNullAssertions by tasks.registering {
    group = "verification"
    description = "Fails when !! appears in main Kotlin sources."

    val repoRoot = rootDir
    val searchRoots = listOf("app", "core", "feature")
        .map { repoRoot.resolve(it) }
        .filter { it.isDirectory }
    val assertion = Regex("""[A-Za-z0-9_)\]]!!""")

    doLast {
        val offenders = searchRoots
            .flatMap { root ->
                root.walkTopDown()
                    .filter { file ->
                        file.isFile &&
                            file.extension == "kt" &&
                            file.invariantSeparatorsPath.contains("/src/main/")
                    }
                    .toList()
            }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { (_, line) ->
                        val code = line.trim()
                        !code.startsWith("//") &&
                            !code.startsWith("*") &&
                            assertion.containsMatchIn(code)
                    }
                    .map { (index, _) ->
                        "${file.relativeTo(repoRoot).invariantSeparatorsPath}:${index + 1}"
                    }
            }

        if (offenders.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Non-null assertions (!!) are not allowed in main sources. ")
                    appendLine("Return an Outcome or use safe calls instead:")
                    offenders.forEach { appendLine("  - $it") }
                },
            )
        }
    }
}

subprojects {
    tasks.withType<Test>().configureEach {
        dependsOn(verifyNoNotNullAssertions)
    }
}
