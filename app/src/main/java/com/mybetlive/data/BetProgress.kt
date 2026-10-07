package com.mybetlive.data

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.floor

enum class ProgressTone(val argb: Int) {
    RED(0xFFE86A6A.toInt()), GREEN(0xFF40C983.toInt()), AMBER(0xFFF2BD5B.toInt()), MUTED(0xFF9AAEBB.toInt())
}

data class BetProgress(
    val sport: String, val current: Double, val target: Double,
    val start: Double, val end: Double, val tone: ProgressTone,
    val valueLabel: String, val caption: String, val statusLabel: String,
    val centered: Boolean = false
) {
    val fraction: Float get() = ((current - start) / (end - start)).toFloat().coerceIn(0f, 1f)
    val targetFraction: Float get() = ((target - start) / (end - start)).toFloat().coerceIn(0f, 1f)
    val description: String get() = "$valueLabel. $statusLabel. $caption"
}

fun BetView.progress(offline: Boolean = false): BetProgress? = betProgress(
    event.sport, market, selection, currentValue, line, status, stale || offline
)

/** Position represents the measured statistic or score margin, never the chance of winning. */
fun betProgress(sport: String, market: String, selection: String, value: Double?, line: Double?,
    status: String, stale: Boolean = false): BetProgress? {
    val current = value?.takeIf { it.isFinite() } ?: return null
    if (status in listOf("PENDING", "AWAITING_DATA", "VOID")) return null
    if (sport == "SOCCER" && market == "MONEYLINE" && selection == "DRAW") {
        val tone = if(stale) ProgressTone.MUTED else if(status in listOf("WON","LIVE_WINNING")) ProgressTone.GREEN else ProgressTone.RED
        val radius = max(1.0, abs(current))
        return BetProgress(sport,current,0.0,-radius,radius,tone,"Diferencia ${current.display()} · Empate = 0",
            if(stale) "Último valor guardado · pendiente de actualizar" else "Empate a los 90 minutos más compensación",
            if(stale) "Datos pendientes de actualizar" else statusLabels[status] ?: "Estado por confirmar")
    }
    val margin = market == "MONEYLINE" || market == "SPREAD"
    val target = when {
        market == "MONEYLINE" && selection in listOf("HOME", "AWAY") -> 0.0
        market == "SPREAD" && selection in listOf("HOME", "AWAY") -> -(line?.takeIf { it.isFinite() } ?: return null)
        (market in listOf("TOTAL", "TEAM_TOTAL") || market.startsWith("PLAYER_")) && selection in listOf("OVER", "UNDER") ->
            line?.takeIf { it.isFinite() && it >= 0 } ?: return null
        else -> return null
    }
    val tone = if (stale) ProgressTone.MUTED else when (status) {
        "WON", "THRESHOLD_REACHED", "LIVE_WINNING" -> ProgressTone.GREEN
        "LOST", "LIVE_LOSING" -> ProgressTone.RED
        "PUSH", "LIVE_PUSH" -> ProgressTone.AMBER
        else -> ProgressTone.MUTED
    }
    val radius = max(if(sport in listOf("SOCCER", "MLB")) 1.0 else 7.0, max(abs(current), abs(target)))
    val start = if (margin) -radius else min(0.0, current)
    val end = if (margin) radius else max(1.0, max(target, current))
    val final = status in listOf("WON", "LOST", "PUSH")
    val valueLabel = if (margin) "Diferencia ${current.display()} · Objetivo > ${target.display()}"
        else "${if (final) "Resultado " else ""}${current.display()} / ${target.display()} · ${if (selection == "OVER") "Over" else "Under"}"
    val caption = when {
        stale -> "Último valor guardado · pendiente de actualizar"
        final -> if (status == "PUSH") "Terminó exactamente en la línea" else "Resultado del partido finalizado"
        margin -> when(sport) { "SOCCER" -> "Diferencia de goles · 90 minutos más compensación"; "MLB" -> "Diferencia de carreras · incluye entradas extra"; else -> "Diferencia de puntos · resultado provisional" }
        selection == "UNDER" -> "Debe terminar por debajo de ${target.display()} · provisional"
        current > target -> "Línea superada · resultado provisional"
        current == target -> "En la línea · necesita superar ${target.display()}"
        else -> "Faltan ${(floor(target) + 1 - current).display()} para superar la línea"
    }
    return BetProgress(sport, current, target, start, end, tone, valueLabel, caption,
        if (stale) "Datos pendientes de actualizar" else statusLabels[status] ?: "Estado por confirmar")
}
