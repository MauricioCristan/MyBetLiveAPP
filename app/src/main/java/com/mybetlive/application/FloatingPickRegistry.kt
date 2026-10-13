package com.mybetlive.application

import com.mybetlive.data.BetView
import com.mybetlive.data.settledStatuses

/** One identity per pick. Closing one is sticky for this floating session. */
class FloatingPickRegistry {
    private val enabled=linkedSetOf<String>()
    private val dismissed=mutableSetOf<String>()
    val ids get()=enabled.toList()
    fun activate(id:String,bets:List<BetView>) {
        dismissed.remove(id)
        sync(bets)
    }
    fun sync(bets:List<BetView>) {
        enabled.retainAll(bets.map{it.id}.toSet())
        bets.filter{it.status !in settledStatuses && it.id !in dismissed}.forEach{enabled.add(it.id)}
    }
    fun close(id:String){enabled.remove(id);dismissed.add(id)}
}
