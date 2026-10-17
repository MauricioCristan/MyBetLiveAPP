package com.mybetlive.application

import com.mybetlive.data.CreateBet

/** Keep the original write after an ambiguous network failure, even if banks are refreshed. */
internal class PendingTrackingRegistrations {
    private var owner:String?=null
    private val requests=mutableMapOf<String,CreateBet>()
    fun request(account:String,input:CreateBet):CreateBet {
        if(owner!=account){requests.clear();owner=account}
        val operation=requireNotNull(input.operationId)
        val existing=requests[operation]
        require(existing==null||existing.copy(bankVersion=null)==input.copy(bankVersion=null)) {
            "Hay un envío pendiente con otros datos. Revisa la apuesta antes de reintentar."
        }
        return requests.getOrPut(operation){input}
    }
    fun forget(operation:String?){requests.remove(operation)}
}
