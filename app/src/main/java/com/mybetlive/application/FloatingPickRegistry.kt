package com.mybetlive.application

import com.mybetlive.data.BetView
import com.mybetlive.data.settledStatuses

/** Stable identities and selection for the single floating carousel. */
class FloatingPickRegistry {
    private val enabled=linkedSetOf<String>()
    private val dismissed=mutableSetOf<String>()
    val ids get()=enabled.toList()
    var selectedId:String?=null
        private set
    fun restore(ids:List<String>,selected:String?,bets:List<BetView>){
        enabled.clear();dismissed.clear();enabled.addAll(ids);selectedId=selected;sync(bets)
    }
    fun navigate(direction:Int){
        if(enabled.isEmpty())return
        val rows=ids;selectedId=rows[Math.floorMod(rows.indexOf(selectedId).coerceAtLeast(0)+direction,rows.size)]
    }
    fun activate(id:String,bets:List<BetView>) {
        dismissed.remove(id)
        sync(bets)
        if(id in enabled)selectedId=id
    }
    fun sync(bets:List<BetView>) {
        enabled.retainAll(bets.map{it.id}.toSet())
        bets.filter{it.status !in settledStatuses && it.id !in dismissed}.forEach{enabled.add(it.id)}
        if(selectedId !in enabled)selectedId=enabled.firstOrNull()
    }
    fun close(id:String){enabled.remove(id);dismissed.add(id);if(selectedId==id)selectedId=enabled.firstOrNull()}
}
