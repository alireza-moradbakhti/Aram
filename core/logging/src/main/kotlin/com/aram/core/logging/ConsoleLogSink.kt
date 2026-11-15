package com.aram.core.logging

/**
 * Plain-text sink for JVM, desktop, and iOS targets. Android uses its own
 * logcat sink so release builds keep WARN/ERROR output.
 */
class ConsoleLogSink(private val tag: String = "Aram") : LogSink {

    override fun write(event: LogEvent) {
        println("$tag [${event.level.name}] ${event.name}: ${event.message}")
        event.throwable?.let { println(it.stackTraceToString()) }
    }
}
