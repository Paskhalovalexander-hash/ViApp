package com.example.vitanlyapp.ui.screen.main

import android.util.Log

private const val CHART_DIAGNOSTICS = true
private const val CAPACITY = 120
private const val LOG_TAG = "KCAL_TRACE"

private val buffer = Array(CAPACITY) { "" }
@Volatile private var writePos = 0
@Volatile private var storedCount = 0
private val lastBySource = mutableMapOf<String, String>()
private val lock = Any()

internal object KcalTraceLog {

    fun traceIfChanged(source: String, payload: String) {
        if (!CHART_DIAGNOSTICS) return
        synchronized(lock) {
            if (payload == lastBySource[source]) return
            lastBySource[source] = payload
            val ts = android.os.SystemClock.uptimeMillis()
            val line = "$LOG_TAG $ts $source $payload"
            buffer[writePos % CAPACITY] = line
            writePos++
            storedCount = (storedCount + 1).coerceAtMost(CAPACITY)
        }
    }

    fun dumpToLogcat() {
        if (!CHART_DIAGNOSTICS) return
        synchronized(lock) {
            val n = storedCount
            if (n == 0) {
                Log.d(LOG_TAG, "KCAL_TRACE (empty)")
                return@synchronized
            }
            val start = (writePos - n + CAPACITY) % CAPACITY
            for (i in 0 until n) {
                val line = buffer[(start + i) % CAPACITY]
                Log.d(LOG_TAG, line)
            }
        }
    }
}
