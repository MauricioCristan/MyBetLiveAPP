package com.mybetlive.data

import org.junit.Assert.*
import org.junit.Test

class BackendSessionPolicyTest {
    private val local="http://127.0.0.1:8081"
    private val production="https://mybetlivebackend-production.up.railway.app/"
    @Test fun explicitDebugSwitchDiscardsOtherServersSession(){
        assertTrue(BackendSessionPolicy.needsReset(true,true,production,local))
        assertTrue(BackendSessionPolicy.needsReset(true,true,"http://127.0.0.1:8080/",local))
    }
    @Test fun sameServerAndOrdinaryReleaseUpdatesPreserveSessions(){
        assertFalse(BackendSessionPolicy.needsReset(true,true,local+"/",local))
        assertFalse(BackendSessionPolicy.needsReset(true,true,null,local))
        assertFalse(BackendSessionPolicy.needsReset(false,true,production,local))
        assertFalse(BackendSessionPolicy.needsReset(true,false,production,local))
    }
}
