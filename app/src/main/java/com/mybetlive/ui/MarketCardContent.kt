package com.mybetlive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybetlive.data.*

@Composable internal fun MarketCardContent(bet: BetView, offline: Boolean, compact: Boolean = false) {
    val p = bet.presentation(offline)
    val tone = Color(p.tone.argb)
    Text(listOf(sportLabel(bet.event.sport), bet.event.period, bet.event.clock.orEmpty()).filter { it.isNotBlank() }.joinToString(" · "),
        color = Muted, fontSize = 11.sp)
    Text(p.status, color = tone, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().background(tone.copy(alpha=.12f), RoundedCornerShape(8.dp)).padding(10.dp))
    if (p.visual == MarketVisual.MONEYLINE) {
        Row(Modifier.fillMaxWidth().testTag("moneyline-score"), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmphasizedTeam(bet.event.home, p.home!!, Modifier.weight(1f))
            Column(Modifier.weight(1.15f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(p.value, fontSize = if(compact) 28.sp else 36.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                if(bet.selection == "DRAW") Text("APUESTA: EMPATE", color = tone, fontSize = 9.sp)
            }
            EmphasizedTeam(bet.event.away, p.away!!, Modifier.weight(1f))
        }
    } else {
        bet.visualProgress(offline)?.let { BetProgressTrack(it, compact) }
            ?: Text("${p.value} ${p.unit}", color = tone, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
    Text(p.caption, color = if (offline || bet.stale) Muted else tone, fontSize = 12.sp)
    val phase = when { !p.provisional -> "Resultado final"; bet.status == "PENDING" -> "Por comenzar"
        bet.status == "AWAITING_DATA" -> "Esperando datos"; offline || bet.stale -> "Dato guardado"; else -> "En vivo · provisional" }
    Text("$phase · último dato ${updatedTime(bet.updatedAt)}",
        color = Muted, fontSize = 10.sp)
}

@Composable private fun EmphasizedTeam(team: Team, emphasis: TeamEmphasis, modifier: Modifier) {
    val tone = Color(emphasis.tone.argb)
    Column(modifier.heightIn(min=126.dp).background(tone.copy(alpha=.08f), RoundedCornerShape(14.dp))
        .border(1.dp, tone.copy(alpha=.55f), RoundedCornerShape(14.dp)).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(if(emphasis.selected) "TU EQUIPO" else "", color = tone, fontSize = 9.sp)
        Box(Modifier.height(68.dp), contentAlignment = Alignment.Center) {
            TeamLogo(team, size = emphasis.size.dp, modifier = Modifier.alpha(if(emphasis.faded) .58f else 1f))
        }
        Text(team.name, color = if(emphasis.tone == ProgressTone.MUTED) Muted else tone, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
