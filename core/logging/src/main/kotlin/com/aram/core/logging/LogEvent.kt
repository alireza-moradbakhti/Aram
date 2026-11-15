package com.aram.core.logging

data class LogEvent(
    val level: LogLevel,
    val name: String,
    val message: String,
    val throwable: Throwable? = null,
    val sendToServer: Boolean = false,
)
