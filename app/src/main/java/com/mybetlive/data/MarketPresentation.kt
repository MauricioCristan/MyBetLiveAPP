package com.mybetlive.data

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.ceil

val settledStatuses = setOf("WON", "LOST", "PUSH", "VOID")
enum class MarketVisual { TOTAL, MONEYLINE, MARGIN, PLAYER, OUTCOME }
data class TeamEmphasis(val tone: ProgressTone, val size: Int, val selected: Boolean, val faded: Boolean)
data class MarketPresentation(
    val visual: MarketVisual, val tone: ProgressTone, val status: String, val value: String,
    val caption: String, val unit: String, val provisional: Boolean,
    val home: TeamEmphasis? = null, val away: TeamEmphasis? = null
)

fun marketPresentation(sport: String, market: String, selection: String, line: Double?, value: Double?,
    status: String, homeScore: Int?, awayScore: Int?, stale: Boolean = false,scope:String="MATCH",targetScore:String?=null): MarketPresentation {
    if(market=="BET_BUILDER"){
        val final=status in settledStatuses;val muted=stale&&!final||status in listOf("PENDING","AWAITING_DATA","VOID")
        val tone=if(muted)ProgressTone.MUTED else if(status in listOf("WON","LIVE_WINNING"))ProgressTone.GREEN else if(status in listOf("LOST","LIVE_LOSING"))ProgressTone.RED else ProgressTone.AMBER
        return MarketPresentation(MarketVisual.OUTCOME,tone,statusLabels[status]?:status,"${if(muted)"—" else value.display()} / ${line.display()}","Selecciones favorables · liquidación por confirmar","selecciones",!final)
    }
    if((sport=="SOCCER"&&market=="BOTH_TEAMS_SCORE")||sport=="NHL" && market in listOf("RESULT_3WAY","DOUBLE_CHANCE","BOTH_TEAMS_SCORE","CORRECT_SCORE","OVERTIME")) {
        if(market=="RESULT_3WAY")return marketPresentation("SOCCER","MONEYLINE",selection,null,value,status,homeScore,awayScore,stale).copy(caption=hockeyScopeLabel(scope),unit="goles")
        val final=status in settledStatuses
        val muted=stale&&!final || status in listOf("PENDING","AWAITING_DATA","VOID")
        val tone=if(muted)ProgressTone.MUTED else when(status){"WON","LIVE_WINNING"->ProgressTone.GREEN;"LOST","LIVE_LOSING"->ProgressTone.RED;else->ProgressTone.AMBER}
        val text=when { muted->"—";market=="OVERTIME"->if(value==1.0)"Sí" else "No";market=="BOTH_TEAMS_SCORE"->if(value==2.0)"Sí" else "No";else->"${homeScore?:"—"} : ${awayScore?:"—"}" }
        return MarketPresentation(MarketVisual.OUTCOME,tone,statusLabels[status]?:"Esperando datos",text,
            listOf(if(sport=="SOCCER")"90 minutos más compensación" else hockeyScopeLabel(scope),targetScore?.let {"Objetivo $it"}.orEmpty()).filter {it.isNotBlank()}.joinToString(" · "),"",!final)
    }
    val visual = when { market == "MONEYLINE" -> MarketVisual.MONEYLINE; market == "SPREAD" -> MarketVisual.MARGIN
        market.startsWith("PLAYER_") -> MarketVisual.PLAYER; else -> MarketVisual.TOTAL }
    val final = status in settledStatuses
    val outdated = stale && !final
    val missing = status in setOf("PENDING", "AWAITING_DATA", "VOID") ||
        (if (visual == MarketVisual.MONEYLINE) homeScore == null || awayScore == null else value == null)
    var tone = if (outdated || missing) ProgressTone.MUTED else when (status) {
        "WON", "LIVE_WINNING", "THRESHOLD_REACHED" -> ProgressTone.GREEN
        "LOST", "LIVE_LOSING" -> ProgressTone.RED
        "PUSH", "LIVE_PUSH" -> ProgressTone.AMBER
        else -> ProgressTone.MUTED
    }
    val unit = when {
        market == "TOTAL_CORNERS" -> "tiros de esquina"
        market == "TOTAL_CARDS" -> "tarjetas"
        market == "TOTAL_SHOTS" -> "remates"
        market == "TOTAL_SHOTS_ON_TARGET" -> "remates a puerta"
        market.endsWith("YARDS") -> "yardas"; market == "PLAYER_RECEPTIONS" -> "recepciones"
        market == "PLAYER_PASSING_TOUCHDOWNS" -> "touchdowns"; market == "PLAYER_ASSISTS" -> "asistencias"
        market == "PLAYER_REBOUNDS" -> "rebotes"
        market in setOf("PLAYER_THREE_POINTERS_MADE", "PLAYER_THREES") -> "triples"
        market in setOf("PLAYER_POINTS_REBOUNDS_ASSISTS", "PLAYER_PRA") -> "PRA"
        market == "PLAYER_SHOTS_ON_TARGET" -> "tiros a puerta"; market == "PLAYER_SHOTS" -> "tiros"
        market == "PLAYER_SAVES" -> "atajadas"
        sport in listOf("SOCCER","NHL") -> "goles"; sport == "MLB" -> "carreras"; else -> "puntos"
    }
    val adjusted = if (visual == MarketVisual.MARGIN && value != null && line != null) value + line else null
    val margin = if (homeScore != null && awayScore != null) homeScore - awayScore else null
    val selectedMargin = if (selection == "AWAY") margin?.let { -it } else margin
    if (!final && !outdated && !missing && visual == MarketVisual.MONEYLINE) tone = when {
        selection == "DRAW" -> if (margin == 0) ProgressTone.GREEN else ProgressTone.RED
        margin == 0 -> ProgressTone.AMBER
        (selectedMargin ?: 0) > 0 -> ProgressTone.GREEN
        else -> ProgressTone.RED
    }
    val label = when {
        outdated -> "Dato guardado · esperando actualización"
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
        val accent = if (outdated || missing) ProgressTone.MUTED else if (draw || tie || selected) tone else ProgressTone.MUTED
        val size = if (outdated || missing) 48 else if (tie) 52 else if (selected) if (winner) 66 else 36 else if (winner) 58 else 44
        return TeamEmphasis(accent, size, selected, !outdated && !missing && selected && !winner && !tie)
    }
    val formatted = when (visual) {
        MarketVisual.MONEYLINE -> if (homeScore == null || awayScore == null) "— : —" else "$homeScore : $awayScore"
        MarketVisual.MARGIN -> "Margen ${if (adjusted == null || missing) "—" else (if (adjusted > 0) "+" else "") + adjusted.display()}"
        else -> "${if (missing) "—" else value.display()} / ${line.display()}"
    }
    val caption = when { outdated -> "Último valor guardado; aún no es actual."
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
fun BetView.presentation(offline: Boolean = false):MarketPresentation {
    if(!builderSelections.isNullOrEmpty()){
        val final=status in settledStatuses
        val outdated=(offline||stale)&&!final
        val tone=if(outdated||status in listOf("PENDING","AWAITING_DATA","VOID"))ProgressTone.MUTED else when(status){"WON","LIVE_WINNING","THRESHOLD_REACHED"->ProgressTone.GREEN;"LOST","LIVE_LOSING"->ProgressTone.RED;else->ProgressTone.AMBER}
        val value=if(outdated||currentValue==null)"— / ${builderSelections.size}" else "${currentValue.display()} / ${builderSelections.size}"
        val review=builderSelections.all{it.status in settledStatuses}&&builderSelections.any{it.status in listOf("PUSH","VOID")}&&status=="AWAITING_DATA"
        return MarketPresentation(MarketVisual.OUTCOME,tone,if(review)"Liquidación por revisar" else statusLabels[status]?:status,value,
            if(review)"Hay una selección reembolsada. Confirma la cuota y el retorno con tu casino." else "Selecciones favorables · confirma la liquidación de tu casino","selecciones",!final)
    }
    val (home,away)=scopedScores()
    val effectiveScope=scope ?: "MATCH"
    val p=marketPresentation(event.sport,market,selection,line,currentValue,status,home,away,(stale||offline)&&status !in settledStatuses,effectiveScope,targetScore)
    return if(event.sport=="NHL" && !p.caption.contains(hockeyScopeLabel(effectiveScope)))p.copy(caption="${p.caption} · ${hockeyScopeLabel(effectiveScope)}") else p
}

/** Spread uses the adjusted margin with the coverage boundary at zero. ML uses a scoreboard. */
fun BetView.visualProgress(offline: Boolean = false): BetProgress? {
    if(!builderSelections.isNullOrEmpty())return null
    val (home,away)=scopedScores()
    return marketProgress(event.sport,market,selection,line,currentValue,status,home,away,
        (stale || offline) && status !in settledStatuses)
}

/** One scale and visual policy for cards, previews and RemoteViews notifications. */
fun marketProgress(sport: String, market: String, selection: String, line: Double?, value: Double?, status: String,
    homeScore: Int?, awayScore: Int?, stale: Boolean = false): BetProgress? {
    if (market in listOf("MONEYLINE","BET_BUILDER")) return null
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
