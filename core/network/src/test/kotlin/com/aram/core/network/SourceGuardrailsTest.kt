package com.aram.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Portability guardrail (MASTER_PLAN K0): the network module must stay free of
 * Android, Compose, and Hilt types, and no API host may leak into Kotlin.
 */
class SourceGuardrailsTest {

    private val sources: List<File> = File("src/main/kotlin")
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .toList()

    @Test
    fun `sources are found`() {
        assertTrue("expected Kotlin sources in src/main/kotlin", sources.size >= 10)
    }

    @Test
    fun `no android compose or androidx imports`() {
        sources.forEach { file ->
            val text = file.readText()
            assertFalse(file.path, Regex("""^import android\.""", RegexOption.MULTILINE).containsMatchIn(text))
            assertFalse(file.path, Regex("""^import androidx\.""", RegexOption.MULTILINE).containsMatchIn(text))
        }
    }

    @Test
    fun `no hilt or inject imports`() {
        sources.forEach { file ->
            val text = file.readText()
            assertFalse(file.path, Regex("""^import dagger\.hilt""", RegexOption.MULTILINE).containsMatchIn(text))
            assertFalse(file.path, Regex("""^import javax\.inject""", RegexOption.MULTILINE).containsMatchIn(text))
        }
    }

    @Test
    fun `contract hosts never appear in Kotlin`() {
        sources.forEach { file ->
            val text = file.readText()
            assertFalse(file.path, text.contains("myendpoint"))
        }
    }

    @Test
    fun `raw requests only happen in the result gate`() {
        val offenders = sources
            .filter { file -> file.readText().contains("client.request(") }
            .map { it.name }
        assertEquals(listOf("SafeApiCall.kt"), offenders)
    }
}
