package com.mybetlive.application

import com.mybetlive.data.*

data class FloatingCompactPresentation(val score:String,val value:String?,val label:String,val phase:String?,val tone:ProgressTone) {
    val widthDp get()=80
}

/** Compact data stays readable without replacing the scoreboard with a result symbol. */
fun BetView.compactFloating(offline:Boolean=false):FloatingCompactPresentation {
    val p=presentation(offline)
    val (home,away)=scopedScores()
    val score="${home ?: "—"} : ${away ?: "—"}"
    val final=status in settledStatuses
    val staleData=(offline || stale) && !final
    val phase=when {
        final->when(status){"WON"->"Ganada";"LOST"->"Perdida";"PUSH"->"Push";else->"Cancelada"}
        staleData->"Dato guardado"
        status=="PENDING"->"Por comenzar"
        status=="AWAITING_DATA"->"Sin datos"
        else->null
    }
    val label=when {
        market in listOf("MONEYLINE","RESULT_3WAY")->if(selection=="DRAW")"Empate" else "Ganador"
        market=="SPREAD"->"Hándicap ${line.display()}"
        market=="DOUBLE_CHANCE"->when(selection){"HOME_DRAW"->"1X";"AWAY_DRAW"->"X2";else->"12"}
        market=="CORRECT_SCORE"->"Exacto ${targetScore.orEmpty()}"
        market=="OVERTIME"->"Prórroga · ${if(selection=="YES")"Sí" else "No"}"
        market=="BOTH_TEAMS_SCORE"->"Ambos anotan · ${if(selection=="YES")"Sí" else "No"}"
        else->"${if(selection=="UNDER")"Under" else "Over"} · ${p.unit}"
    }
    val value=when(p.visual){MarketVisual.MONEYLINE->null;else->p.value}
    return FloatingCompactPresentation(score,value,label,phase,if(staleData)ProgressTone.AMBER else p.tone)
}
