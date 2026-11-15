package com.aram.app.logging

import android.util.Log
import com.aram.core.logging.LogEvent
import com.aram.core.logging.LogLevel
import com.aram.core.logging.LogSink

class AndroidLogSink(private val baseTag: String = "Aram") : LogSink {

    override fun write(event: LogEvent) {
        val priority = when (event.level) {
            LogLevel.VERBOSE -> Log.VERBOSE
            LogLevel.DEBUG -> Log.DEBUG
            LogLevel.INFO -> Log.INFO
            LogLevel.WARN -> Log.WARN
            LogLevel.ERROR -> Log.ERROR
        }
        val line = event.throwable
            ?.let { "${event.message}\n${it.stackTraceToString()}" }
            ?: event.message
        Log.println(priority, "$baseTag/${event.name}", line)
    }
}
