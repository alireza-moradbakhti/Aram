package com.aram.app.crash

import com.aram.core.logging.Logger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reports an uncaught exception once, flags the crash for the next launch, and
 * lets the platform handler finish killing the process.
 */
class AramCrashHandler(
    private val marker: CrashMarker,
    private val logger: Logger,
    private val delegate: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {

    private val reported = AtomicBoolean(false)

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            if (reported.compareAndSet(false, true)) {
                marker.mark()
                logger.e(
                    name = COMPONENT,
                    message = "Uncaught exception on ${thread.name}",
                    throwable = throwable,
                    sendToServer = true,
                )
            }
        } finally {
            delegate?.uncaughtException(thread, throwable)
        }
    }

    private companion object {
        const val COMPONENT = "CrashHandler"
    }
}
