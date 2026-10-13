package com.mybetlive.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybetlive.application.compactFloating
import com.mybetlive.data.*
import com.mybetlive.ui.*

@Composable internal fun CompactFloatingPick(bet:BetView,offline:Boolean,modifier:Modifier) {
    val p=bet.compactFloating(offline)
    val tone=Color(p.tone.argb)
    val scoreboardOnly=p.value==null
    Box(modifier.width(p.widthDp.dp).heightIn(min=76.dp)) {
        Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(24.dp)).border(2.dp,tone,RoundedCornerShape(24.dp))
            .padding(horizontal=8.dp,vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(4.dp)) {
            if(bet.market.startsWith("PLAYER_")) {
                PlayerAvatar(bet.playerImageUrl,size=28.dp)
            } else if(scoreboardOnly) {
                val (home,away)=bet.scopedScores()
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)) {
                        CompactTeam(bet.event.home,bet.selection=="HOME",tone,"home",28)
                        FittingScore(home?.toString() ?: "—",18.sp,Modifier.width(28.dp).testTag("floating-score-home"),minFontSize=10.sp)
                    }
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)) {
                        CompactTeam(bet.event.away,bet.selection=="AWAY",tone,"away",28)
                        FittingScore(away?.toString() ?: "—",18.sp,Modifier.width(28.dp).testTag("floating-score-away"),minFontSize=10.sp)
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                    CompactTeam(bet.event.home,bet.selection=="HOME" || bet.market=="TEAM_TOTAL" && bet.team=="HOME",tone,"home",28)
                    CompactTeam(bet.event.away,bet.selection=="AWAY" || bet.market=="TEAM_TOTAL" && bet.team=="AWAY",tone,"away",28)
                }
            }
            p.value?.let {FittingScore(it.removePrefix("Margen "),18.sp,Modifier.fillMaxWidth().testTag("floating-value"),minFontSize=8.sp)}
            p.phase?.let{phase->
                val translated=com.mybetlive.i18n.AppStrings.translate(phase,LocalContext.current.resources)
                CompositionLocalProvider(LocalContentColor provides tone) {
                    FittingScore(translated,10.sp,Modifier.fillMaxWidth().testTag("floating-phase"),minFontSize=7.sp)
                }
            }
        }
    }
}

@Composable private fun CompactTeam(team:Team,selected:Boolean,tone:Color,tag:String,size:Int=32) {
    Box(Modifier.size(size.dp).testTag("floating-team-$tag").then(if(selected)Modifier.border(1.dp,tone,RoundedCornerShape(9.dp)) else Modifier),
        contentAlignment=Alignment.Center){TeamLogo(team,size=26.dp)}
}
