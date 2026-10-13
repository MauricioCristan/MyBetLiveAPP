package com.mybetlive.application

object FloatingRefreshPolicy {
    const val INTERVAL_MS=60_000L
    fun due(now:Long,lastRead:Long,foreground:Boolean,locked:Boolean,online:Boolean,hasActive:Boolean)=
        !foreground && !locked && online && hasActive && (lastRead==0L || now-lastRead>=INTERVAL_MS)
}
