package com.mybetlive.application

import org.junit.Assert.*
import org.junit.Test

class FloatingRefreshPolicyTest {
    @Test fun exactMinuteAndNoDuplicateInitialRead() {
        assertTrue(FloatingRefreshPolicy.due(100,0,false,false,true,true))
        assertFalse(FloatingRefreshPolicy.due(60_099,100,false,false,true,true))
        assertTrue(FloatingRefreshPolicy.due(60_100,100,false,false,true,true))
    }
    @Test fun foregroundLockedOfflineAndOnlyFinalStopNetworkReads() {
        assertFalse(FloatingRefreshPolicy.due(90_000,100,true,false,true,true))
        assertFalse(FloatingRefreshPolicy.due(90_000,100,false,true,true,true))
        assertFalse(FloatingRefreshPolicy.due(90_000,100,false,false,false,true))
        assertFalse(FloatingRefreshPolicy.due(90_000,100,false,false,true,false))
    }
}
