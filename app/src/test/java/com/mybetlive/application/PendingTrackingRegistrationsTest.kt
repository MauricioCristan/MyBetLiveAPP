package com.mybetlive.application

import com.mybetlive.data.CreateBet
import org.junit.Assert.*
import org.junit.Test

class PendingTrackingRegistrationsTest {
    private val draft=CreateBet("event","MONEYLINE","HOME",null,null,null,100.25,1.85,"Casino",bankId="bank",operationId="operation",bankVersion=1)
    @Test fun refreshingTheBankAfterALostResponseKeepsTheOriginalWrite(){
        val pending=PendingTrackingRegistrations()
        assertEquals(draft,pending.request("owner",draft))
        assertEquals(draft,pending.request("owner",draft.copy(bankVersion=2)))
        pending.forget(draft.operationId)
        assertEquals(2L,pending.request("owner",draft.copy(bankVersion=2)).bankVersion)
    }
    @Test fun pendingWritesNeverCarryOverToAnotherAccount(){
        val pending=PendingTrackingRegistrations()
        pending.request("first",draft)
        val second=draft.copy(bankVersion=7)
        assertEquals(second,pending.request("second",second))
    }
    @Test fun anOperationCannotSilentlyReuseAnotherBetsAmounts(){
        val pending=PendingTrackingRegistrations()
        pending.request("owner",draft)
        assertThrows(IllegalArgumentException::class.java){pending.request("owner",draft.copy(stake=200.0))}
    }
}
