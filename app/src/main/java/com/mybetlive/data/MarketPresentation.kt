package com.mybetlive.data

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.ceil

val settledStatuses = setOf("WON", "LOST", "PUSH", "VOID")
enum class MarketVisual { TOTAL, MONEYLINE, MARGIN, PLAYER }
data class TeamEmphasis(val tone: ProgressTone, val size: Int, val selected: Boolean, val faded: Boolean)
data class MarketPresentation(
    val visual: MarketVisual, val tone: ProgressTone, val status: String, val value: String,
    val caption: String, val unit: String, val provisional: Boolean,
    val home: TeamEmphasis? = null, val away: TeamEmphasis? = null
)

fun marketPresentation(sport: String, market: String, selection: String, line: Double?, value: Double?,
    status: String, homeScore: Int?, awayScore: Int?, stale: Boolean = false): MarketPresentation {
    val visual = when { market == "MONEYLINE" -> MarketVisual.MONEYLINE; market == "SPREAD" -> MarketVisual.MARGIN
        market.startsWith("PLAYER_") -> MarketVisual.PLAYER; else -> MarketVisual.TOTAL }
    val final = status in settledStatuses
    val missing = status in setOf("PENDING", "AWAITING_DATA", "VOID") ||
        (if (visual == MarketVisual.MONEYLINE) homeScore == null || awayScore == null else value == null)
    var tone = if (stale || missing) ProgressTone.MUTED else when (status) {
        "WON", "LIVE_WINNING", "THRESHOLD_REACHED" -> ProgressTone.GREEN
        "LOST", "LIVE_LOSING" -> ProgressTone.RED
        "PUSH", "LIVE_PUSH" -> ProgressTone.AMBER
        else -> ProgressTone.MUTED
    }
    val unit = when {
        market.endsWith("YARDS") -> "yardas"; market == "PLAYER_RECEPTIONS" -> "recepciones"
        market == "PLAYER_PASSING_TOUCHDOWNS" -> "touchdowns"; market == "PLAYER_ASSISTS" -> "asistencias"
        market == "PLAYER_REBOUNDS" -> "rebotes"
        market in setOf("PLAYER_THREE_POINTERS_MADE", "PLAYER_THREES") -> "triples"
        market in setOf("PLAYER_POINTS_REBOUNDS_ASSISTS", "PLAYER_PRA") -> "PRA"
        market == "PLAYER_SHOTS_ON_TARGET" -> "tiros a puerta"; market == "PLAYER_SHOTS" -> "tiros"
        sport == "SOCCER" -> "goles"; sport == "MLB" -> "carreras"; else -> "puntos"
    }
    val adjusted = if (visual == MarketVisual.MARGIN && value != null && line != null) value + line else null
    val margin = if (homeScore != null && awayScore != null) homeScore - awayScore else null
    val selectedMargin = if (selection == "AWAY") margin?.let { -it } else margin
    if (!final && !stale && !missing && visual == MarketVisual.MONEYLINE) tone = when {
        selection == "DRAW" -> if (margin == 0) ProgressTone.GREEN else ProgressTone.RED
        margin == 0 -> ProgressTone.AMBER
        (selectedMargin ?: 0) > 0 -> ProgressTone.GREEN
        else -> ProgressTone.RED
    }
    val label = when {
        stale -> "Dato guardado · esperando actualización"
        status == "PENDING" -> "Por comenzar"
        status == "VOID" -> "Cancelada"
        missing -> "Esperando ${if (visual == MarketVisual.PLAYER) "estadística del jugador" else "datos del partido"}"
        final -> when(status) { "WON" -> "Ganada"; "LOST" -> "Perdida"; else -> "Push · empate en la línea" }
        visual == MarketVisual.MONEYLINE -> when {
            selection == "DRAW" -> if (margin == 0) "Va empatando · favorable por ahora" else "No hay empate · desfavorable"
            margin == 0 -> "Va empatando · juego en curso"
            (selectedMargin ?: 0) > 0 -> "Va ganando · favorable por ahora"
            else -> "Va perdiendo · desfavorable"
        }
        visual == MarketVisual.MARGIN -> when { adjusted == null -> "Esperando marcador"
            adjusted > 0 -> "Cubre · favorable por ahora"; adjusted < 0 -> "No cubre · desfavorable"; else -> "En la línea · push si termina así" }
        value == line -> "En la línea · push si termina así"
        selection == "OVER" && value != null && line != null && value < line -> "Faltan ${(floor(line) + 1 - value).display()} $unit para superar la línea"
        tone == ProgressTone.GREEN -> "Selección favorable por ahora"
        else -> "Selección desfavorable"
    }
    fun team(home: Boolean): TeamEmphasis {
        val selected = selection == if (home) "HOME" else "AWAY"
        val draw = selection == "DRAW"
        val tie = margin == 0
        val winner = margin != null && if (home) margin > 0 else margin < 0
        val accent = if (stale || missing) ProgressTone.MUTED else if (draw || tie || selected) tone else ProgressTone.MUTED
        val size = if (stale || missing) 48 else if (tie) 52 else if (selected) if (winner) 66 else 36 else if (winner) 58 else 44
        return TeamEmphasis(accent, size, selected, !stale && !missing && selected && !winner && !tie)
    }
    val formatted = when (visual) {
        MarketVisual.MONEYLINE -> if (homeScore == null || awayScore == null) "— : —" else "$homeScore : $awayScore"
        MarketVisual.MARGIN -> "Margen ${if (adjusted == null || missing) "—" else (if (adjusted > 0) "+" else "") + adjusted.display()}"
        else -> "${if (missing) "—" else value.display()} / ${line.display()}"
    }
    val caption = when { stale -> "Último valor guardado; aún no es actual."
        missing -> "Sin datos no se interpreta como cero."
        final -> "Resultado final del proveedor"
        visual == MarketVisual.MONEYLINE -> if (sport == "SOCCER") "90′ más compensación, sin prórroga." else "Resultado provisional hasta finalizar."
        visual == MarketVisual.MARGIN -> "Margen ajustado = diferencia de marcador + hándicap."
        selection == "UNDER" -> "Debe terminar por debajo de ${line.display()} $unit."
        else -> "Necesita ${line?.let { floor(it) + 1 }.display()} $unit o más al finalizar."
    }
    return MarketPresentation(visual, tone, label, formatted, caption, unit, !final,
        if (visual == MarketVisual.MONEYLINE) team(true) else null, if (visual == MarketVisual.MONEYLINE) team(false) else null)
}
fun BetView.presentation(offline: Boolean = false) = marketPresentation(event.sport, market, selection, line, currentValue,
    status, event.homeScore, event.awayScore, stale || offline)

/** Spread uses the adjusted margin with the coverage boundary at zero. ML uses a scoreboard. */
fun BetView.visualProgress(offline: Boolean = false): BetProgress? {
    return marketProgress(event.sport,market,selection,line,currentValue,status,event.homeScore,event.awayScore,stale || offline)
}

/** One scale and visual policy for cards, previews and RemoteViews notifications. */
fun marketProgress(sport: String, market: String, selection: String, line: Double?, value: Double?, status: String,
    homeScore: Int?, awayScore: Int?, stale: Boolean = false): BetProgress? {
    if (market == "MONEYLINE") return null
    val original = betProgress(sport,market,selection,value,line,status,stale) ?: return null
    val p = marketPresentation(sport,market,selection,line,value,status,homeScore,awayScore,stale)
    if (market != "SPREAD") {
        // Leave room past an NBA prop/total line; the ball is a statistic, not a winning probability.
        val step=if(original.end < 10) 1.0 else if(original.end < 100) 10.0 else 25.0
        val end=if(sport=="NBA") max(1.0,ceil(original.end*1.25/step)*step) else original.end
        return original.copy(valueLabel=p.value,caption=p.caption,statusLabel=p.status,end=end)
    }
    val adjusted = original.current + (line ?: return null)
    val radius = max(marginRadius(sport), abs(adjusted))
    return original.copy(current = adjusted, target = 0.0, start = -radius, end = radius,
        tone = p.tone, valueLabel = p.value, caption = "No cubre ← 0 → Cubre", statusLabel = p.status, centered = true)
}

internal fun marginRadius(sport: String) = when(sport) { "NFL" -> 7.0; "NBA" -> 12.0; else -> 3.0 }
