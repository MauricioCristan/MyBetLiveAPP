package com.mybetlive.data

fun hockeyScopeLabel(scope:String)=when(scope){"REGULATION"->"60 minutos";"P1"->"1.er período";"P2"->"2.º período";"P3"->"3.er período";else->"Partido completo · incluye prórroga y shootout"}
fun hockeyScopes(market:String):List<String> = when(market){"MONEYLINE","OVERTIME"->listOf("MATCH");"RESULT_3WAY","DOUBLE_CHANCE","BOTH_TEAMS_SCORE","CORRECT_SCORE"->listOf("REGULATION","P1","P2","P3");else->listOf("MATCH","REGULATION","P1","P2","P3")}
fun needsMarketLine(market:String)=market !in listOf("MONEYLINE","RESULT_3WAY","DOUBLE_CHANCE","BOTH_TEAMS_SCORE","CORRECT_SCORE","OVERTIME")
fun EventSnapshot.hockeyScore(scope:String):PeriodScore? {
    if(scope=="MATCH")return if(homeScore!=null&&awayScore!=null)PeriodScore(homeScore,awayScore) else null
    if(scope!="REGULATION")return periodScores[scope]
    if(status=="LIVE"&&period in listOf("P1","P2","P3","BT")&&!periodScores.containsKey("OT")&&!periodScores.containsKey("PT"))
        return if(homeScore!=null&&awayScore!=null)PeriodScore(homeScore,awayScore) else null
    val periods=listOf("P1","P2","P3").map { periodScores[it] ?: return null }
    return PeriodScore(periods.sumOf { it.home },periods.sumOf { it.away })
}
fun BetView.scopedScores():Pair<Int?,Int?> = if(liveScopeScoresPresent)liveScopeScores?.home to liveScopeScores?.away else if(event.sport=="NHL")event.hockeyScore(scope ?: "MATCH").let { it?.home to it?.away } else event.homeScore to event.awayScore
