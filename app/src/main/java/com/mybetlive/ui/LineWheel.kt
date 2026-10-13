package com.mybetlive.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybetlive.application.propLineChoices
import com.mybetlive.data.LineRule
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable internal fun LineWheel(rule: LineRule?,value: String,onValueChange: (String)->Unit,enabled: Boolean=true) {
    var halves by rememberSaveable(rule?.unit) {mutableStateOf(true)}
    var anchor by rememberSaveable(rule?.unit) {mutableStateOf(rule?.example?.takeIf {it.isFinite()}?.toString().orEmpty())}
    val options=remember(rule,halves){propLineChoices(rule,halves)}
    LaunchedEffect(options,value) {
        if(value.isNotBlank()&&value !in options)onValueChange("")
    }
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        LocalizedText(rule?.unit?.replaceFirstChar {it.uppercase()}?:"Línea",fontWeight=FontWeight.SemiBold)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(selected=halves,onClick={if(!halves){if(value.isNotBlank())anchor=value;onValueChange("");halves=true}},enabled=enabled,label={LocalizedText("Con .5")},modifier=Modifier.testTag("line-half"))
            FilterChip(selected=!halves,onClick={if(halves){if(value.isNotBlank())anchor=value;onValueChange("");halves=false}},enabled=enabled,label={LocalizedText("Enteras")},modifier=Modifier.testTag("line-whole"))
        }
        if(options.isEmpty())LocalizedText("No hay líneas disponibles. Actualiza el catálogo.",color=Muted)
        else key(options) {LineWheelValues(options,value,anchor,rule,onValueChange,enabled)}
        LocalizedText(if(value.isBlank())"Desliza arriba o abajo y toca una línea para elegirla."
            else "Línea elegida: $value ${rule?.unit.orEmpty()}",color=Muted,fontSize=12.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun LineWheelValues(options: List<String>,value: String,anchor: String,rule: LineRule?,onValueChange: (String)->Unit,enabled: Boolean) {
    val initial=remember(options) {options.indexOf(value).takeIf {it>=0}
        ?:anchor.toBigDecimalOrNull()?.let {target->options.indices.minByOrNull {options[it].toBigDecimal().subtract(target).abs()}}?:0}
    val list=rememberLazyListState(initialFirstVisibleItemIndex=initial)
    val scope=rememberCoroutineScope();val haptic=LocalHapticFeedback.current
    val callback by rememberUpdatedState(onValueChange);val active by rememberUpdatedState(enabled)
    var emitted by remember {mutableStateOf(value)}
    val center by remember {derivedStateOf {
        val layout=list.layoutInfo;val middle=(layout.viewportStartOffset+layout.viewportEndOffset)/2
        layout.visibleItemsInfo.minByOrNull {kotlin.math.abs(it.offset+it.size/2-middle)}?.index
    }}
    fun clearChoice(){emitted="";callback("")}
    fun commit(index: Int){
        if(emitted!=options[index]){emitted=options[index];callback(options[index]);haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)}
    }
    fun choose(index: Int) {
        if(!active)return
        clearChoice()
        scope.launch {
            list.animateScrollToItem(index)
            if(active)commit(index)
        }
    }
    LaunchedEffect(list,options,enabled) {
        var moved=false
        snapshotFlow {list.isScrollInProgress to center}.distinctUntilChanged().collect { (moving,index) ->
            if(moving&&enabled){if(!moved)clearChoice();moved=true}
            else if(!moving&&moved&&index!=null&&enabled) {
                commit(index)
                moved=false
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(168.dp).clip(RoundedCornerShape(16.dp)).background(Panel)
        .testTag("prop-line-wheel").semantics {
            stateDescription=if(value.isBlank())"Sin línea elegida" else "$value ${rule?.unit.orEmpty()}"
            if(!enabled)disabled()
            customActions=if(enabled)listOf(CustomAccessibilityAction("Línea anterior"){choose(((center?:initial)-1).coerceAtLeast(0));true},
                CustomAccessibilityAction("Línea siguiente"){choose(((center?:initial)+1).coerceAtMost(options.lastIndex));true}) else emptyList()
        }) {
        Box(Modifier.align(Alignment.Center).padding(horizontal=12.dp).fillMaxWidth().height(56.dp)
            .background(Green.copy(alpha=.15f),RoundedCornerShape(12.dp)))
        LazyColumn(state=list,flingBehavior=rememberSnapFlingBehavior(list),userScrollEnabled=enabled,
            contentPadding=PaddingValues(vertical=56.dp),modifier=Modifier.fillMaxSize().testTag("prop-line-list")) {
            itemsIndexed(options,key={_,option->option}) {index,option->
                Box(Modifier.fillMaxWidth().height(56.dp).clickable(enabled){choose(index)}
                    .testTag("line-choice-$option").semantics {selected=option==value;contentDescription=com.mybetlive.i18n.AppStrings.translate("$option ${rule?.unit.orEmpty()}")},
                    contentAlignment=Alignment.Center) {
                    LocalizedText(option,fontSize=if(index==center)32.sp else 23.sp,
                        fontWeight=if(index==center)FontWeight.Bold else FontWeight.Normal,
                        color=if(index==center)if(option==value)Green else MaterialTheme.colorScheme.onSurface else Muted.copy(alpha=.5f))
                }
            }
        }
    }
}
