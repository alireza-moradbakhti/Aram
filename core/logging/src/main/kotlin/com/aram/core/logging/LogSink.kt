package com.aram.core.logging

fun interface LogSink {
    fun write(event: LogEvent)
}

/**
 * Destination for events explicitly marked `sendToServer = true`.
 * Implemented when a backend exists; until then the logger keeps them local.
 */
fun interface RemoteLogSink {
    fun send(event: LogEvent)
}
