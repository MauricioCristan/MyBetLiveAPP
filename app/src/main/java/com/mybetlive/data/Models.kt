package com.mybetlive.data
data class RegisterEmail(val username:String,val email:String,val password:String)
data class EmailAddress(val email:String)
data class EmailToken(val token:String)
data class NewPassword(val token:String,val newPassword:String)

data class Team(val id: String, val name: String, val logoUrl: String? = null)
data class Player(val id: String, val name: String, val teamId: String, val availableMarkets: List<String>? = null,
    val imageUrl: String? = null)
fun Player.supports(market: String) = if (market == "PLAYER_SAVES") availableMarkets?.contains(market) == true
    else availableMarkets.isNullOrEmpty() || market in availableMarkets
data class LineRule(val min: Double,val max: Double,val step: Double,val unit: String,val example: Double)
data class SportInfo(val sport: String, val enabled: Boolean, val markets: List<String>,val lineRules: Map<String,LineRule>? = null)
data class EventSnapshot(
    val sport: String, val provider: String, val externalId: String, val startsAt: String,
    val home: Team, val away: Team, val homeScore: Int?, val awayScore: Int?,
    val status: String, val period: String, val clock: String?, val observedAt: String,
    val leagueId: String? = null, val leagueName: String? = null, val leagueCountry: String? = null,
    val periodScores: Map<String,PeriodScore> = emptyMap(),val leagueLogoUrl:String?=null
)
data class PeriodScore(val home:Int,val away:Int)
data class CalendarInfo(val timeZone: String, val date: String, val serverTime: String)

fun EventSnapshot.canStartTracking() = status !in setOf("FINAL", "CANCELLED")
data class EventView(val id: String, val event: EventSnapshot, val stale: Boolean, val localStartsAt: String? = null)
data class BetView(
    val id: String, val eventId: String, val event: EventSnapshot, val market: String,
    val selection: String, val line: Double?, val team: String?, val playerId: String?, val playerName: String?,
    val stake: Double?, val decimalOdds: Double?, val bookmaker: String?, val status: String,
    val currentValue: Double?, val remaining: Double?, val updatedAt: String, val stale: Boolean,
    val playerImageUrl: String? = null, val localStartsAt: String? = null, val localUpdatedAt: String? = null,
    val scope:String? = null,val targetScore:String? = null,
    val liveScopeScores:PeriodScore? = null,val liveScopeScoresPresent:Boolean = false
)
data class CreateBet(
    val eventId: String, val market: String, val selection: String, val line: Double?,
    val team: String?, val playerId: String?, val stake: Double?, val decimalOdds: Double?, val bookmaker: String?,
    val scope:String = "MATCH",val targetScore:String? = null
)
data class Login(val username: String, val password: String)
data class GoogleChallenge(val serverClientId: String, val nonce: String)
data class GoogleLogin(val idToken: String, val nonce: String)
data class GoogleSession(val tokens: Tokens, val displayName: String, val created: Boolean)
data class Refresh(val refreshToken: String)
data class DeviceToken(val token: String)
data class Tokens(val accessToken: String, val refreshToken: String)
data class Session(val baseUrl: String, val username: String, val tokens: Tokens,
    val generation: String? = java.util.UUID.randomUUID().toString())

val marketLabels = mapOf(
    "TOTAL_CORNERS" to "Tiros de esquina", "TOTAL_CARDS" to "Total de tarjetas",
    "TOTAL_SHOTS" to "Remates", "TOTAL_SHOTS_ON_TARGET" to "Remates a puerta",
    "RESULT_3WAY" to "Resultado 1X2", "DOUBLE_CHANCE" to "Doble oportunidad", "BOTH_TEAMS_SCORE" to "Ambos equipos anotan",
    "CORRECT_SCORE" to "Marcador exacto", "OVERTIME" to "¿Habrá prórroga?",
    "MONEYLINE" to "Moneyline", "SPREAD" to "Spread", "TOTAL" to "Total del partido",
    "TEAM_TOTAL" to "Total del equipo", "PLAYER_RUSHING_YARDS" to "Yardas terrestres",
    "PLAYER_RECEIVING_YARDS" to "Yardas de recepción", "PLAYER_RECEPTIONS" to "Recepciones",
    "PLAYER_PASSING_YARDS" to "Yardas de pase", "PLAYER_PASSING_TOUCHDOWNS" to "Touchdowns de pase",
    "PLAYER_GOALS" to "Goles", "PLAYER_ASSISTS" to "Asistencias", "PLAYER_SHOTS" to "Tiros totales",
    "PLAYER_SHOTS_ON_TARGET" to "Tiros a puerta", "PLAYER_SAVES" to "Atajadas del portero",
    "PLAYER_POINTS" to "Puntos", "PLAYER_REBOUNDS" to "Rebotes",
    "PLAYER_THREE_POINTERS_MADE" to "Triples", "PLAYER_THREES" to "Triples",
    "PLAYER_POINTS_REBOUNDS_ASSISTS" to "Puntos + rebotes + asistencias", "PLAYER_PRA" to "Puntos + rebotes + asistencias"
)
fun sportLabel(sport: String) = when(sport) { "SOCCER" -> "Fútbol"; else -> sport }
fun marketLabel(market: String, sport: String) = when(sport) {
    "NHL" -> when(market) {"MONEYLINE" -> "Ganador · incluye prórroga y shootout";"SPREAD" -> "Puck Line";"TOTAL" -> "Total de goles";"TEAM_TOTAL" -> "Goles del equipo";else -> marketLabels[market] ?: market}
    "NBA" -> when(market) {
        "MONEYLINE" -> "Ganador · incluye prórroga"; "SPREAD" -> "Hándicap · incluye prórroga"
        "TOTAL" -> "Total de puntos"; "TEAM_TOTAL" -> "Puntos del equipo"
        else -> marketLabels[market] ?: market
    }
    "MLB" -> when(market) {
        "MONEYLINE" -> "Ganador · juego completo"; "SPREAD" -> "Run Line · juego completo"
        "TOTAL" -> "Total de carreras"; "TEAM_TOTAL" -> "Carreras del equipo"
        else -> marketLabels[market] ?: market
    }
    "SOCCER" -> when(market) {
    "MONEYLINE" -> "Resultado 1X2 · 90 minutos"; "SPREAD" -> "Hándicap asiático · 90 minutos"
    "TOTAL" -> "Total de goles · 90 minutos"; "TEAM_TOTAL" -> "Goles del equipo · 90 minutos"
    else -> marketLabels[market] ?: market
    }
    else -> marketLabels[market] ?: market
}
val statusLabels = mapOf(
    "PENDING" to "Por comenzar", "LIVE_WINNING" to "Va ganando", "LIVE_LOSING" to "Va perdiendo",
    "LIVE_PUSH" to "En la línea", "THRESHOLD_REACHED" to "Línea alcanzada · provisional",
    "AWAITING_DATA" to "Esperando estadísticas", "WON" to "Ganada según los datos",
    "LOST" to "Perdida según los datos", "PUSH" to "Push · empate en la línea", "VOID" to "Evento cancelado"
)
fun Double?.display(): String = this?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "—"
fun BetView.title(): String = selectionTitle(market, selection, line, team, playerName, event,scope ?: "MATCH",targetScore)
fun CreateBet.title(event: EventSnapshot, playerName: String?): String = selectionTitle(market, selection, line, team, playerName, event,scope,targetScore)
internal fun selectionTitle(market: String, selection: String, line: Double?, team: String?, playerName: String?, event: EventSnapshot,scope:String="MATCH",targetScore:String?=null): String {
    val target = playerName ?: when (if (market == "TEAM_TOTAL") team else selection) { "HOME" -> event.home.name; "AWAY" -> event.away.name; "DRAW" -> "Empate"; else -> "" }
    val side = when(selection) { "OVER" -> "Over"; "UNDER" -> "Under"; "YES" -> if(market=="CORRECT_SCORE") "" else "Sí";"NO"->"No";"HOME_DRAW"->"${event.home.name} o empate";"AWAY_DRAW"->"${event.away.name} o empate";"HOME_AWAY"->"Cualquiera gana · sin empate";else -> "" }
    return listOf(target, marketLabel(market,event.sport), side, line?.display() ?: "",targetScore.orEmpty(),if(event.sport=="NHL"&&market!="MONEYLINE") hockeyScopeLabel(scope) else "").filter { it.isNotEmpty() }.joinToString(" · ")
}
// The backend accounts for its configured feed interval; callers add the device's offline state.
fun BetView.isStale(): Boolean = stale
