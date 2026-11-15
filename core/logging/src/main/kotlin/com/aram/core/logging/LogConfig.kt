package com.aram.core.logging

data class LogConfig(
    val minLevel: LogLevel = LogLevel.DEBUG,
    val serverUploadEnabled: Boolean = false,
)
