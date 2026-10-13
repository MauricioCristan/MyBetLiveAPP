package com.mybetlive.application

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow

/** Serializes foreground and floating-window reads, including lifecycle transitions. */
class LiveTrackingCoordinator {
    val foreground = MutableStateFlow(false)
    private val lock = Mutex()
    suspend fun <T> read(block: suspend () -> T): T = lock.withLock { block() }
    suspend fun backgroundRead(block: suspend () -> Unit) = lock.withLock { if (!foreground.value) block() }
}

interface FloatingTracking {
    fun activate(id: String)
    fun stop()
}
