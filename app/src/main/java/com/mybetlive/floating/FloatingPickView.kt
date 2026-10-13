package com.mybetlive.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybetlive.data.*
import com.mybetlive.ui.*

@Composable internal fun FloatingPickView(bet:BetView,offline:Boolean,expanded:Boolean,maxHeight:Float,
    expand:()->Unit,minimize:()->Unit,close:()->Unit,open:()->Unit,
    dragStart:()->Unit,drag:(Float,Float)->Unit,dragEnd:()->Unit) {
    MaterialTheme(colorScheme=darkColorScheme(primary=Green,surface=Panel,onSurface=Color(0xFFE8F0F4))) {
        CompositionLocalProvider(LocalContentColor provides Color(0xFFE8F0F4)) {
        val p=bet.presentation(offline)
        val tone=if(offline && bet.status !in settledStatuses)Color(0xFFF2BD5B) else Color(p.tone.argb)
        if(!expanded) {
            CompactFloatingPick(bet,offline,Modifier.testTag("floating-bubble").pointerInput(Unit){detectDragGestures(
                onDragStart={dragStart()},onDragEnd=dragEnd,onDragCancel=dragEnd,
                onDrag={change,amount->change.consume();drag(amount.x,amount.y)})}.clickable(onClick=expand))
        } else {
            Column(Modifier.fillMaxWidth().heightIn(max=maxHeight.dp).background(Panel,RoundedCornerShape(24.dp))
                .border(1.dp,Color(0xFF2C414F),RoundedCornerShape(24.dp)).padding(16.dp)
                .testTag("floating-card"),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)){BrandLogo(height=22.dp)}
                    TextButton(onClick=minimize,modifier=Modifier.size(48.dp).testTag("floating-minimize")){LocalizedText("−",fontSize=24.sp)}
                    TextButton(onClick=close,modifier=Modifier.size(48.dp).testTag("floating-close")){LocalizedText("×",fontSize=24.sp)}
                }
                Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()).testTag("floating-content"),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    LocalizedText(bet.title(),fontSize=15.sp,fontWeight=FontWeight.SemiBold)
                    MarketCardContent(bet,offline,compact=true)
                    if(offline && bet.status !in settledStatuses)LocalizedText("Sin conexión · mostrando la última actualización guardada",color=Color(0xFFF2BD5B),fontSize=12.sp)
                }
                Button(onClick=open,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("floating-open")){
                    LocalizedText(if(bet.status in settledStatuses)"Ver resultado" else "Abrir seguimiento",color=Ink)
                }
                if(bet.status in settledStatuses)OutlinedButton(onClick=close,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){LocalizedText("Cerrar burbuja")}
            }
        }
        }
    }
}
