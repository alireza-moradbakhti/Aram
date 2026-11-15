package com.aram.app.crash

import android.content.Context

/**
 * Remembers that the previous process died from an uncaught exception so the
 * next launch can show the recovery screen.
 */
class CrashMarker(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun mark() {
        // commit(), not apply(): the process is about to die and apply() may never flush.
        prefs.edit().putBoolean(KEY_PENDING_CRASH, true).commit()
    }

    fun hasPendingCrash(): Boolean = prefs.getBoolean(KEY_PENDING_CRASH, false)

    fun clear() {
        prefs.edit().remove(KEY_PENDING_CRASH).commit()
    }

    private companion object {
        const val PREFS_NAME = "aram_crash"
        const val KEY_PENDING_CRASH = "pending_crash"
    }
}
