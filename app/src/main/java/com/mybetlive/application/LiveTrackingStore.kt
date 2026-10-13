package com.mybetlive.application

import com.mybetlive.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

data class LiveTrackingState(val owner: String? = null, val bets: List<BetView> = emptyList(), val offline: Boolean = false)
fun Session.trackingOwner() = "$baseUrl|$username|$generation"

/** Shared snapshots, scoped to one login. Partial reads never replace settled history. */
class LiveTrackingStore {
    private val mutable = MutableStateFlow(LiveTrackingState())
    val state = mutable.asStateFlow()
    private val deleted = mutableSetOf<String>()
    @Synchronized fun bind(owner: String?, cached: List<BetView> = emptyList()) {
        if (mutable.value.owner != owner) { deleted.clear(); mutable.value = LiveTrackingState(owner, cached, true) }
    }
    private fun newer(incoming: BetView, old: BetView?): BetView {
        if (old == null) return incoming
        val comparison = runCatching { Instant.parse(incoming.updatedAt).compareTo(Instant.parse(old.updatedAt)) }.getOrDefault(0)
        return if (comparison < 0 || old.status in settledStatuses && incoming.status !in settledStatuses) old else incoming
    }
    @Synchronized fun accept(owner: String, incoming: List<BetView>, partial: Boolean = false) {
        if (owner != mutable.value.owner) return
        val old = mutable.value.bets.associateBy { it.id }
        val rows = incoming.filter { it.id !in deleted }.map { newer(it, old[it.id]) }
        val history = if (partial) mutable.value.bets.filter { it.status in settledStatuses && rows.none { n -> n.id == it.id } } else emptyList()
        mutable.value = mutable.value.copy(bets = rows + history, offline = false)
    }
    @Synchronized fun remove(id: String) {
        deleted.add(id); mutable.value = mutable.value.copy(bets = mutable.value.bets.filterNot { it.id == id })
    }
    @Synchronized fun offline(owner: String) { if (owner == mutable.value.owner) mutable.value = mutable.value.copy(offline = true) }
    @Synchronized fun push(owner: String, data: Map<String,String>) {
        if (owner != mutable.value.owner) return
        val old = mutable.value.bets.find { it.id == data["betId"] } ?: return
        if (old.id in deleted) return
        val scopePresent = old.event.sport == "NHL" && old.scope != null && old.scope != "MATCH"
        val scoped = if (scopePresent) data["scopeHomeScore"]?.toIntOrNull()?.let { h -> data["scopeAwayScore"]?.toIntOrNull()?.let { PeriodScore(h,it) } } else null
        val row = old.copy(status = data["status"] ?: old.status, currentValue = data["currentValue"]?.toDoubleOrNull(),
            remaining = data["remaining"]?.toDoubleOrNull(), updatedAt = data["updatedAt"] ?: old.updatedAt,
            localUpdatedAt = null, stale = data["stale"] == "true", liveScopeScores = scoped, liveScopeScoresPresent = scopePresent,
            event = old.event.copy(homeScore = data["homeScore"]?.toIntOrNull(), awayScore = data["awayScore"]?.toIntOrNull(),
                status = data["eventStatus"] ?: old.event.status, period = data["period"] ?: old.event.period, clock = data["clock"]))
        mutable.value = mutable.value.copy(bets = mutable.value.bets.map { if(it.id==row.id)newer(row,it) else it })
    }
}
