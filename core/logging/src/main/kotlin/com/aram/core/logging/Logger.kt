package com.aram.core.logging

/**
 * Structured logger shared by every module.
 *
 * Platform-free on purpose: no Android or `java.*` types, so this module can
 * move to `commonMain` when the project adopts Kotlin Multiplatform. Sinks are
 * the only platform boundary.
 *
 * `name` identifies the component (for example `"TransactionsRepository"`);
 * `message` states what happened. Events marked `sendToServer = true` are
 * forwarded only when [LogConfig.serverUploadEnabled] is on and a
 * [RemoteLogSink] is installed, so nothing leaves the device by accident.
 */
class Logger(
    private val config: LogConfig = LogConfig(),
    private val sinks: List<LogSink> = emptyList(),
    private val remoteSink: RemoteLogSink? = null,
) {

    fun log(
        level: LogLevel,
        name: String,
        message: String,
        throwable: Throwable? = null,
        sendToServer: Boolean = false,
    ) {
        if (level.priority < config.minLevel.priority) return

        val event = LogEvent(level, name, message, throwable, sendToServer)
        sinks.forEach { sink ->
            // Logging must never break the caller.
            try {
                sink.write(event)
            } catch (_: Throwable) {
            }
        }

        if (event.sendToServer && config.serverUploadEnabled) {
            try {
                remoteSink?.send(event)
            } catch (_: Throwable) {
            }
        }
    }

    fun v(name: String, message: String, sendToServer: Boolean = false) =
        log(LogLevel.VERBOSE, name, message, sendToServer = sendToServer)

    fun d(name: String, message: String, sendToServer: Boolean = false) =
        log(LogLevel.DEBUG, name, message, sendToServer = sendToServer)

    fun i(name: String, message: String, sendToServer: Boolean = false) =
        log(LogLevel.INFO, name, message, sendToServer = sendToServer)

    fun w(
        name: String,
        message: String,
        throwable: Throwable? = null,
        sendToServer: Boolean = false,
    ) = log(LogLevel.WARN, name, message, throwable, sendToServer)

    fun e(
        name: String,
        message: String,
        throwable: Throwable? = null,
        sendToServer: Boolean = false,
    ) = log(LogLevel.ERROR, name, message, throwable, sendToServer)
}
