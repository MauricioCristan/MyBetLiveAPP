package com.mybetlive.ui
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.mybetlive.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mybetlive.bank.*
import com.mybetlive.data.*
import com.mybetlive.composition.ClientDependencies
import java.time.*
import java.time.format.DateTimeFormatter
import java.math.*
internal enum class BankHistoryGrouping(val label:String){DAY("Día"),WEEK("Semana"),MONTH("Mes")}
private enum class BankTab(val label:String){SUMMARY("Inicio"),HISTORY("Apuestas"),STATISTICS("Estadísticas"),TOOLS("Herramientas")}
@Composable internal fun BankRoute(tracked:List<BetView>,refresh:Int,onPlans:()->Unit,access:BankAccess?=null){
 val context=LocalContext.current;val repository=remember{ClientDependencies.bankRepository(context.applicationContext)};val owner=repository.owner()?:return
 val factory=remember(owner){object:ViewModelProvider.Factory{override fun <T:ViewModel> create(modelClass:Class<T>):T{require(modelClass==BankViewModel::class.java);@Suppress("UNCHECKED_CAST") return BankViewModel(repository) as T}}}
 val model:BankViewModel=viewModel(key="bank-"+owner,factory=factory)
 val state by model.state.collectAsStateWithLifecycle()
 LaunchedEffect(refresh,tracked.map{it.id to it.status},access){model.reload()}
 DisposableEffect(owner){onDispose{model.disposeIfChanged()}}
 BankScreen(state,model,tracked,onPlans)
}
@Composable internal fun BankScreen(state:BankState,model:BankViewModel,tracked:List<BetView>,onPlans:()->Unit){
 var tab by rememberSaveable{mutableStateOf(BankTab.SUMMARY)};var dialog by remember{mutableStateOf<String?>(null)}
 var editBet by remember{mutableStateOf<BankBet?>(null)}
 val historyGrouping=BankHistoryGrouping.MONTH
 val summary=state.summary;val bank=summary?.bank;val advanced=summary?.access?.advanced==true
 val editable=!state.offline&&!state.busy;val canCreate=editable&&bank?.writable==true
 var chartExpanded by rememberSaveable(bank?.id){mutableStateOf(false)}
 var collapsedHistoryGroups by rememberSaveable(bank?.id){mutableStateOf(emptyList<String>())}
 val listState=rememberLazyListState()
 LaunchedEffect(tab,bank?.id,state.historyRevision,historyGrouping){listState.scrollToItem(0)}
 val historyPrefetch=with(LocalDensity.current){160.dp.roundToPx()}
 val latestState by rememberUpdatedState(state)
 LaunchedEffect(tab,bank?.id){
  if(tab==BankTab.HISTORY)snapshotFlow{
   val current=latestState;val layout=listState.layoutInfo
   !current.busy&&!current.offline&&!current.historyLoadingMore&&current.historyMoreError==null&&current.selectedBet==null&&
    current.history?.let{it.page+1<it.totalPages}==true&&layout.totalItemsCount>0&&
    current.history?.content?.lastOrNull()?.let{last->!bankPeriodCollapsed(bankPlacedDate(last.placedAt,current.summary!!.bank.timezone),collapsedHistoryGroups)}==true&&
    (layout.visibleItemsInfo.lastOrNull()?.index?:-1)>=layout.totalItemsCount-5&&
    layout.visibleItemsInfo.lastOrNull()?.let{it.offset+it.size<=layout.viewportEndOffset+historyPrefetch}==true
  }.distinctUntilChanged().collect{nearEnd->if(nearEnd)model.loadMoreHistory()}
 }
 LaunchedEffect(tab,bank?.id,state.from,state.to,state.busy,state.offline){
  if(tab==BankTab.STATISTICS&&advanced&&state.statistics==null&&!state.busy&&!state.offline&&state.error==null)model.refreshStatistics()
 }
 BackHandler(enabled=state.selectedBet!=null||tab!=BankTab.SUMMARY){if(state.selectedBet!=null)model.closeBet()else tab=BankTab.SUMMARY}
 Column(Modifier.fillMaxSize()) {
 LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("bank-content"),state=listState,contentPadding=PaddingValues(start=20.dp,end=20.dp,top=16.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  item{
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
    var bankMenu by remember{mutableStateOf(false)}
    Box(Modifier.weight(1f)){
     Row(Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("bank-selector").clickable(enabled=state.banks.isNotEmpty()&&!state.busy){bankMenu=true}.semantics{role=Role.Button;contentDescription="Cambiar bank"},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
      LocalizedText(bank?.name?:"Bank",fontSize=28.sp,lineHeight=34.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f,false))
      if(state.banks.isNotEmpty())LocalizedText("⌄",color=Muted,fontSize=22.sp)
     }
     DropdownMenu(bankMenu,onDismissRequest={bankMenu=false}){
      state.banks.forEach{row->DropdownMenuItem(text={LocalizedText(row.name+" · "+row.currency+if(row.archived)" · Archivado" else "")},onClick={bankMenu=false;model.select(row.id)},trailingIcon={if(row.id==bank?.id)LocalizedText("✓",color=Green)})}
      HorizontalDivider()
      DropdownMenuItem(text={LocalizedText("Crear otro bank")},onClick={bankMenu=false;if(advanced||state.banks.none{!it.archived})dialog="createBank" else onPlans()},enabled=editable)
     }
    }
    AssistChip(onClick=onPlans,label={LocalizedText(if(advanced)"Premium" else "Free",fontSize=11.sp)},shape=RoundedCornerShape(12.dp),border=null,colors=AssistChipDefaults.assistChipColors(containerColor=Color(0xFF20313E)))
    var menu by remember{mutableStateOf(false)}
    Box{
     IconButton(onClick={menu=true},modifier=Modifier.semantics{contentDescription="Opciones del bank"}){LocalizedText("⋯",fontSize=24.sp)}
     DropdownMenu(menu,onDismissRequest={menu=false}){
      DropdownMenuItem(text={LocalizedText("Compartir imagen")},enabled=bank!=null&&!state.busy,onClick={menu=false;if(advanced)dialog="share" else onPlans()})
      DropdownMenuItem(text={LocalizedText(BankTab.TOOLS.label)},onClick={menu=false;tab=BankTab.TOOLS})
     }
    }
   }
   if(state.offline)LocalizedText("Sin conexión · consulta de datos guardados",color=Muted,fontSize=12.sp)
   if(state.offline)state.observedAt?.let{LocalizedText("Actualizado: "+bankDate(it,bank?.timezone?:ZoneId.systemDefault().id),color=Muted,fontSize=11.sp,lineHeight=15.sp)}
   state.error?.let{LocalizedText(it,color=Red)};state.message?.let{LocalizedText(it,color=Green)}
   if(state.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
  }
  if(state.banks.isNotEmpty())item{
   if(bank!=null&&!bank.writable&&!bank.archived)OutlinedButton(onClick={model.write("primary|"+bank.id,{api,op,v->api.primary(bank.id,BankCommand(op,v))})},enabled=editable,modifier=Modifier.fillMaxWidth()){LocalizedText("Usar este bank con Free")}
   if(bank?.archived==true)LocalizedText("Bank archivado · tu historial se conserva",color=Muted,fontSize=12.sp)
  }
  if(bank==null){
   item{BankPanel{LocalizedText("Tu capital, en un solo lugar.",fontSize=22.sp,fontWeight=FontWeight.Bold);LocalizedText("Registra apuestas que ya realizaste y consulta tu rendimiento.",color=Muted);Button(onClick={dialog="createBank"},enabled=editable,modifier=Modifier.fillMaxWidth()){LocalizedText("Crear mi primer bank")}}}
  }else{
   if(tab==BankTab.TOOLS)item{TextButton(onClick={tab=BankTab.SUMMARY}){LocalizedText("‹ Inicio del bank")}}
   when(tab){
    BankTab.SUMMARY->{
     item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
      BankMetric("Apuestas del periodo",summary.metrics["bets"]?:"—",Modifier.weight(1f),Green)
      BankMetric("Beneficio",bankMoney(summary.metrics["benefit"],bank.currency),Modifier.weight(1f),bankTone(summary.metrics["benefit"]))
     }}
     item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
      BankMetric("ROI",bankPercent(summary.metrics["roi"]),Modifier.weight(1f),bankTone(summary.metrics["roi"]))
      BankMetric("Progresión",bankPercent(summary.metrics["progression"]),Modifier.weight(1f),bankTone(summary.metrics["progression"]))
     }}
     item{BankPanel{
      Column(Modifier.fillMaxWidth().animateContentSize().testTag("bank-home-chart").clickable(role=Role.Button){chartExpanded=!chartExpanded}.semantics{
       contentDescription=if(chartExpanded)"Reducir gráfica" else "Ampliar gráfica"
       stateDescription=if(chartExpanded)"Ampliada" else "Compacta"
      },verticalArrangement=Arrangement.spacedBy(8.dp)){
       Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        LocalizedText(bankPeriodLabel(state.from,state.to),color=Muted,fontSize=11.sp,lineHeight=16.sp,modifier=Modifier.weight(1f))
        LocalizedText(if(chartExpanded)"−" else "⤢",color=Green,fontSize=18.sp,lineHeight=20.sp,modifier=Modifier.padding(start=8.dp))
       }
       BankGraph(summary.chart,bank.currency,if(chartExpanded)240.dp else 110.dp,state.from,state.to)
      }
      Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("bank-home-periods"),horizontalArrangement=Arrangement.spacedBy(6.dp)){
       val today=LocalDate.now(ZoneId.of(bank.timezone))
       val presets=listOf("Día" to today,"Semana" to today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),"Mes" to today.withDayOfMonth(1),"Año" to today.withDayOfYear(1))
       presets.forEach{(label,start)->
        val selected=state.from==start.toString()&&state.to==today.toString()
        FilterChip(selected=selected,onClick={model.period(start.toString(),today.toString())},enabled=editable,label={LocalizedText(label,fontSize=10.sp,lineHeight=14.sp,maxLines=1)},shape=RoundedCornerShape(12.dp),border=null,colors=FilterChipDefaults.filterChipColors(containerColor=Muted.copy(alpha=.08f),selectedContainerColor=Green.copy(alpha=.18f),labelColor=Muted,selectedLabelColor=Green))
       }
       FilterChip(selected=presets.none{state.from==it.second.toString()&&state.to==today.toString()},onClick={dialog="period"},enabled=editable,label={LocalizedText("Personalizado",fontSize=10.sp,lineHeight=14.sp,maxLines=1)},shape=RoundedCornerShape(12.dp),border=null,colors=FilterChipDefaults.filterChipColors(containerColor=Muted.copy(alpha=.08f),selectedContainerColor=Green.copy(alpha=.18f),labelColor=Muted,selectedLabelColor=Green))
      }
     }}
    }
    BankTab.HISTORY->{
     item{LocalizedText("Tus apuestas",fontSize=23.sp,lineHeight=29.sp,fontWeight=FontWeight.Bold)}
     item{LocalizedText(bankPeriodLabel(state.from,state.to),color=Muted,fontSize=12.sp)}
     item{BankHistorySearch(state,model)}
     if(state.history?.content.isNullOrEmpty()&&!state.busy)item{LocalizedText("No hay apuestas para estos filtros.",color=Muted)}
     state.history?.content.orEmpty().groupBy{bankPlacedDate(it.placedAt,bank.timezone).withDayOfMonth(1)}.forEach{(month,monthRows)->
      val monthKey="MONTH"+month
      val monthOpen=monthKey !in collapsedHistoryGroups
      item(key="group-"+monthKey){BankPeriodHeader(monthKey,bankHistoryGroupLabel(month,BankHistoryGrouping.MONTH,bank.timezone),state.history?.groups?.find{it.type=="MONTH"&&it.start==month.toString()},bank.currency,monthOpen){collapsedHistoryGroups=if(monthOpen)collapsedHistoryGroups+monthKey else collapsedHistoryGroups-monthKey}}
      if(monthOpen)monthRows.groupBy{bankMonthWeekStart(bankPlacedDate(it.placedAt,bank.timezone))}.forEach{(week,weekRows)->
       val weekKey="MONTH_WEEK"+week
       item(key="group-"+weekKey){BankWeekHeader(weekKey,bankMonthWeekLabel(week),state.history?.groups?.find{it.type=="MONTH_WEEK"&&it.start==week.toString()},bank.currency)}
       weekRows.groupBy{bankPlacedDate(it.placedAt,bank.timezone)}.forEach{(day,dayRows)->
        item(key="day-"+day){BankDayGrid(day,dayRows,state.history?.groups?.find{it.type=="DAY"&&it.start==day.toString()},bank.currency,bank.timezone){model.openBet(it)}}
       }
      }
     }
     item{
      Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
       if(state.historyLoadingMore)CircularProgressIndicator(Modifier.size(24.dp),strokeWidth=2.dp)
       else if(state.historyMoreError!=null){LocalizedText(state.historyMoreError,color=Muted,fontSize=12.sp);TextButton(onClick=model::loadMoreHistory,enabled=!state.busy){LocalizedText("Reintentar")}}
       else if(state.history?.let{it.page+1<it.totalPages}==true){
        if(state.offline)LocalizedText("Conéctate para cargar más apuestas",color=Muted,fontSize=11.sp)
        else if(state.history.content.lastOrNull()?.let{bankPeriodCollapsed(bankPlacedDate(it.placedAt,bank.timezone),collapsedHistoryGroups)}==true)TextButton(onClick=model::loadMoreHistory,enabled=!state.busy){LocalizedText("Cargar más periodos")}
        else LocalizedText("Desliza para cargar más · 10 por carga",color=Muted,fontSize=11.sp)
       }
       else if(!state.history?.content.isNullOrEmpty())LocalizedText("Has visto todas las apuestas",color=Muted,fontSize=11.sp)
      }
     }
     item{LocalizedText("Historial por fecha de colocación de la apuesta",color=Muted,fontSize=11.sp)}
    }
    BankTab.STATISTICS->{
     item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){LocalizedText(state.from+" · "+state.to,color=Muted,fontSize=12.sp,modifier=Modifier.weight(1f));TextButton(onClick={dialog="period"}){LocalizedText("Periodo")}}}
     val basicMetrics=state.statistics?.metrics?:summary.metrics
     items(listOf("benefit","roi","successPercent","bets").chunked(2)){pair->Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
      pair.forEach{key->val value=basicMetrics[key];BankMetric(bankMetricLabels.getValue(key),when(key){"benefit"->bankMoney(value,bank.currency);"bets"->value?:"—";else->bankPercent(value)},Modifier.weight(1f),if(key in listOf("benefit","roi"))bankTone(value)else MaterialTheme.colorScheme.onSurface)}
     }}
     item{BankPanel{LocalizedText("Evolución del beneficio",fontWeight=FontWeight.SemiBold);BankGraph(state.statistics?.chart?:summary.chart);LocalizedText("Beneficio confirmado de tus apuestas",color=Muted,fontSize=11.sp)}}
     if(!advanced)item{AccessGate("Rendimiento avanzado","Consulta TWR, TIR, CLV y desgloses con Premium.",onPlans){tab=BankTab.SUMMARY}}
     else{
      item{BankStatisticsFilter(state,model)}
      val metrics=state.statistics?.metrics
      if(metrics!=null){
       items(bankMetricLabels.entries.filterNot{it.key in setOf("benefit","roi","successPercent","bets")}.chunked(2)){pair->Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
        pair.forEach{(key,label)->val value=metrics[key];val formatted=when{key in moneyMetricKeys->bankMoney(value,bank.currency);key in percentageMetricKeys->bankPercent(value);else->value?:"—"}
         BankMetric(label,formatted,Modifier.weight(1f),if(key in listOf("benefit","roi","twr","irr","progression","clv","weightedClv"))bankTone(value) else MaterialTheme.colorScheme.onSurface)}
        if(pair.size==1)Spacer(Modifier.weight(1f))
       }}
       item{LocalizedText("TIR anualizada. — indica datos insuficientes o un cálculo no válido. TWR, TIR y progresión corresponden al bank completo; los demás resultados respetan los filtros.",color=Muted,fontSize=11.sp)}
       state.statistics!!.breakdowns.forEach{(kind,rows)->
        item{LocalizedText(when(kind){"sport"->"Por deporte";"market"->"Por mercado";else->"Por casino"},fontWeight=FontWeight.SemiBold)}
        items(rows){row->BankPanel{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){LocalizedText(row.label,modifier=Modifier.weight(1f));LocalizedText(bankMoney(row.profit,bank.currency),color=bankTone(row.profit))};LocalizedText(row.bets.toString()+" apuestas · ROI "+bankPercent(row.roi),fontSize=12.sp,color=Muted)}}
       }
      }
     }
    }
    BankTab.TOOLS->{
     item{BankPanel{
      LocalizedText("Tu bank es privado",fontWeight=FontWeight.Bold);LocalizedText("MyBetLive registra tus apuestas y rendimiento. No mueve dinero ni ejecuta apuestas.",color=Muted,fontSize=12.sp)
      OutlinedButton(onClick={dialog="editBank"},enabled=editable,modifier=Modifier.fillMaxWidth()){LocalizedText("Editar bank y valores predeterminados")}
      OutlinedButton(onClick={dialog="period"},modifier=Modifier.fillMaxWidth()){LocalizedText("Calendario y periodo")}
      OutlinedButton(onClick={dialog="csv"},enabled=editable,modifier=Modifier.fillMaxWidth()){LocalizedText("Importar / exportar CSV")}
      OutlinedButton(onClick={if(advanced)dialog="share" else onPlans()},modifier=Modifier.fillMaxWidth()){LocalizedText("Compartir resumen como imagen")}
      if(!advanced)LocalizedText("Premium incluye varios banks, combinadas, Lay, importación y análisis avanzado.",color=Muted,fontSize=12.sp)
     }}
     item{LocalizedText("Calendario · beneficio confirmado",fontWeight=FontWeight.SemiBold)}
     items(summary.calendar,key={it.date}){day->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){LocalizedText(day.date);LocalizedText(bankMoney(day.profit,bank.currency),color=bankTone(day.profit))}}
    }
   }
  }
  item{Spacer(Modifier.height(12.dp))}
 }
 if(bank!=null)BankFloatingNavigation(tab,{tab=it},canCreate,{editBet=null;dialog="bet"},Modifier.padding(horizontal=16.dp,vertical=12.dp))
 }
 val dismiss={dialog=null;model.clearMessage()}
 when(dialog){
  "createBank"->BankEditor(null,state,model,dismiss)
  "editBank"->bank?.let{BankEditor(it,state,model,dismiss)}
  "bet"->bank?.let{BankBetEditor(it,editBet,state,model,tracked,dismiss)}
  "settle"->bank?.let{editBet?.let{bet->BankSettlementEditor(it,bet,state,model,dismiss)}}
  "period"->BankPeriodEditor(state,model,dismiss)
  "csv"->bank?.let{BankCsvEditor(it,state,model,onPlans,dismiss)}
  "share"->summary?.let{BankSharePreview(it,state.statistics,state.from,state.to,dismiss)}
 }
 state.selectedBet?.takeIf{dialog==null}?.let{bet->BankBetDetail(bank?:return@let,bet,state,model,onDismiss=model::closeBet,
  onEdit={editBet=bet;model.closeBet();dialog="bet"},onSettle={editBet=bet;model.closeBet();dialog="settle"})}
}
@Composable private fun BankFloatingNavigation(selected:BankTab,onSelect:(BankTab)->Unit,canAdd:Boolean,onAdd:()->Unit,modifier:Modifier=Modifier){
 Surface(modifier.fillMaxWidth().testTag("bank-floating-navigation"),shape=RoundedCornerShape(26.dp),color=Color(0xFF20333D),shadowElevation=8.dp){
  Row(Modifier.selectableGroup().padding(horizontal=6.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
   listOf(BankTab.SUMMARY,BankTab.HISTORY,BankTab.STATISTICS).forEach{destination->
    if(destination==BankTab.STATISTICS)Box(Modifier.weight(.8f),contentAlignment=Alignment.Center){
     FilledIconButton(onClick=onAdd,enabled=canAdd,modifier=Modifier.size(48.dp).testTag("bank-add-bet").semantics{contentDescription=com.mybetlive.i18n.AppStrings.translate("Añadir apuesta")},colors=IconButtonDefaults.filledIconButtonColors(containerColor=Green,contentColor=Ink)){
      LocalizedText("+",fontSize=30.sp,lineHeight=32.sp,fontWeight=FontWeight.Normal)
     }
    }
    val active=selected==destination
    Column(Modifier.weight(1f).selectable(selected=active,role=Role.Tab,onClick={onSelect(destination)}).testTag("bank-nav-${destination.name}").padding(vertical=4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)){
     Icon(painterResource(when(destination){BankTab.SUMMARY->R.drawable.ic_bank_home;BankTab.HISTORY->R.drawable.ic_nav_trackers;else->R.drawable.ic_bank_statistics}),contentDescription=null,tint=if(active)Green else Muted,modifier=Modifier.size(24.dp))
     LocalizedText(destination.label,fontSize=11.sp,lineHeight=16.sp,color=if(active)Green else Muted,fontWeight=if(active)FontWeight.Bold else FontWeight.Normal,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
   }
  }
 }
}
@Composable internal fun BankPanel(content:@Composable ColumnScope.()->Unit){
 Card(shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Panel),modifier=Modifier.fillMaxWidth()){
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
 }
}
@Composable internal fun BankMetric(label:String,value:String,modifier:Modifier=Modifier,color:Color=MaterialTheme.colorScheme.onSurface){
 Card(modifier,shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=Panel)){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
  LocalizedText(label,color=Muted,fontSize=11.sp,lineHeight=15.sp);LocalizedText(value,fontSize=if(value.length>18)14.sp else 20.sp,lineHeight=26.sp,fontWeight=FontWeight.Bold,color=color)
 }}
}
internal fun bankPeriodLabel(from:String,to:String):String{
 val format=DateTimeFormatter.ofPattern("d MMM yyyy",java.util.Locale.getDefault())
 return runCatching{LocalDate.parse(from).format(format)+" – "+LocalDate.parse(to).format(format)}.getOrDefault(from+" – "+to)
}
@Composable internal fun BankGraph(points:List<BankPoint>,currency:String?=null,height:Dp=120.dp,from:String?=null,to:String?=null){
 val parsed=points.mapNotNull{p->runCatching{LocalDate.parse(p.date).toEpochDay() to p.value.toBigDecimal()}.getOrNull()}.sortedBy{it.first}
 val values=parsed.map{it.second}
 val maximum=(values.maxOrNull()?:BigDecimal.ZERO).max(BigDecimal.ZERO)
 val rawMinimum=(values.minOrNull()?:BigDecimal.ZERO).min(BigDecimal.ZERO)
 val minimum=if(rawMinimum==maximum)rawMinimum-BigDecimal.ONE else rawMinimum
 val range=maximum-minimum
 val first=from?.let{runCatching{LocalDate.parse(it).toEpochDay()}.getOrNull()}?:parsed.firstOrNull()?.first?:0L
 val last=to?.let{runCatching{LocalDate.parse(it).toEpochDay()}.getOrNull()}?:parsed.lastOrNull()?.first?:first
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   if(currency!=null)Column(Modifier.height(height).widthIn(max=84.dp),verticalArrangement=Arrangement.SpaceBetween){
    (0..4).forEach{i->val value=maximum-range.multiply(BigDecimal(i)).divide(BigDecimal(4),2,RoundingMode.HALF_UP);LocalizedText(bankMoney(value.toPlainString(),currency),color=Muted,fontSize=9.sp,lineHeight=12.sp,maxLines=1)}
   }
   Canvas(Modifier.weight(1f).height(height).semantics{contentDescription="Gráfica de beneficio confirmado"}){
    val width=size.width-8;val chartHeight=size.height-12
    repeat(5){i->drawLine(Muted.copy(alpha=.14f),androidx.compose.ui.geometry.Offset(0f,6+i*chartHeight/4),androidx.compose.ui.geometry.Offset(size.width,6+i*chartHeight/4),1f)}
    if(parsed.isEmpty())return@Canvas
    fun x(day:Long)=4+(day-first).toFloat()/(last-first).coerceAtLeast(1)*width
    fun y(value:BigDecimal)=6+chartHeight-(value-minimum).divide(range,MathContext.DECIMAL64).toFloat()*chartHeight
    val path=Path();parsed.forEachIndexed{i,(day,value)->if(i==0)path.moveTo(x(day),y(value))else path.lineTo(x(day),y(value))}
    val tail=parsed.last();path.lineTo(x(last),y(tail.second))
    drawPath(path,Green,style=Stroke(width=2.dp.toPx()))
    drawCircle(Green,3.dp.toPx(),androidx.compose.ui.geometry.Offset(x(last),y(tail.second)))
   }
  }
  if(from!=null&&to!=null)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
   val format=DateTimeFormatter.ofPattern("d MMM",java.util.Locale.getDefault())
   listOf(from,to).forEach{date->LocalizedText(runCatching{LocalDate.parse(date).format(format)}.getOrDefault(date),color=Muted,fontSize=10.sp)}
  }
 }
}

internal fun bankMonthWeekStart(day:LocalDate):LocalDate=maxOf(day.withDayOfMonth(1),bankHistoryGroup(day,BankHistoryGrouping.WEEK))
internal fun bankPeriodCollapsed(day:LocalDate,collapsed:List<String>):Boolean="MONTH"+day.withDayOfMonth(1) in collapsed
internal fun bankMonthWeekLabel(start:LocalDate):String{
 val end=minOf(start.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),start.withDayOfMonth(start.lengthOfMonth()))
 val format=DateTimeFormatter.ofPattern("d MMM",java.util.Locale.getDefault())
 return start.dayOfMonth.toString()+" a "+end.format(format)
}
internal fun bankSignedMoney(value:String?,currency:String):String=(if(value?.toBigDecimalOrNull()?.signum()==1)"+" else "")+bankMoney(value,currency)
@Composable private fun BankBalanceBadge(value:String?,currency:String){
 val tone=when(value?.toBigDecimalOrNull()?.signum()){1->Green;-1->Red;else->Muted}
 Surface(shape=RoundedCornerShape(9.dp),color=tone.copy(alpha=.12f)){
  LocalizedText(bankSignedMoney(value,currency),modifier=Modifier.padding(horizontal=9.dp,vertical=5.dp),fontSize=13.sp,lineHeight=18.sp,fontWeight=FontWeight.Bold,color=tone)
 }
}
@Composable private fun BankWeekHeader(key:String,label:String,totals:BankHistoryGroup?,currency:String){
 Row(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=2.dp).testTag("bank-period-"+key),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){
  LocalizedText(label,modifier=Modifier.weight(1f),fontSize=13.sp,lineHeight=19.sp,fontWeight=FontWeight.SemiBold,color=Muted)
  BankBalanceBadge(totals?.net,currency)
 }
}
@Composable private fun BankDayGrid(day:LocalDate,bets:List<BankBet>,totals:BankHistoryGroup?,currency:String,timezone:String,onOpen:(BankBet)->Unit){
 Card(Modifier.fillMaxWidth().testTag("bank-day-"+day),shape=RoundedCornerShape(16.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Muted.copy(alpha=.16f)),colors=CardDefaults.cardColors(containerColor=Panel)){
  Column(Modifier.padding(horizontal=12.dp)){
   Row(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){
    LocalizedText(day.format(DateTimeFormatter.ofPattern("EEEE d",java.util.Locale.getDefault())).replaceFirstChar{it.titlecase(java.util.Locale.getDefault())},modifier=Modifier.weight(1f),fontSize=14.sp,lineHeight=20.sp,fontWeight=FontWeight.Bold)
    BankBalanceBadge(totals?.net,currency)
   }
   bets.forEachIndexed{index,bet->
    if(index>0)HorizontalDivider(color=Muted.copy(alpha=.12f))
    BankBetRow(bet,currency,timezone){onOpen(bet)}
   }
  }
 }
}
@Composable private fun BankPeriodHeader(key:String,label:String,totals:BankHistoryGroup?,currency:String,expanded:Boolean,onToggle:()->Unit){
 Card(Modifier.fillMaxWidth().testTag("bank-period-"+key).clickable(onClick=onToggle).semantics{stateDescription=com.mybetlive.i18n.AppStrings.translate(if(expanded)"Expandido" else "Contraído")},
  shape=RoundedCornerShape(16.dp),border=androidx.compose.foundation.BorderStroke(1.dp,if(expanded)Green.copy(alpha=.3f)else Muted.copy(alpha=.2f)),colors=CardDefaults.cardColors(containerColor=Color(0xFF20333D))){
  Row(Modifier.fillMaxWidth().heightIn(min=64.dp).padding(horizontal=14.dp,vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){
   LocalizedText(label,modifier=Modifier.weight(1f),fontSize=15.sp,lineHeight=21.sp,fontWeight=FontWeight.Bold,maxLines=2)
   BankBalanceBadge(totals?.net,currency)
   LocalizedText(if(expanded)"⌃" else "⌄",color=if(expanded)Green else Muted,fontSize=20.sp)
  }
 }
}
internal fun bankHistoryGroup(day:LocalDate,group:BankHistoryGrouping):LocalDate=when(group){
 BankHistoryGrouping.DAY->day
 BankHistoryGrouping.WEEK->day.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
 BankHistoryGrouping.MONTH->day.withDayOfMonth(1)
}
internal fun bankHistoryGroupLabel(day:LocalDate,group:BankHistoryGrouping,timezone:String):String=when(group){
 BankHistoryGrouping.DAY->bankHistoryDay(day,timezone)
 BankHistoryGrouping.MONTH->day.format(DateTimeFormatter.ofPattern("LLLL yyyy",java.util.Locale.getDefault())).replaceFirstChar{it.titlecase(java.util.Locale.getDefault())}
 BankHistoryGrouping.WEEK->{val format=DateTimeFormatter.ofPattern("d MMM yyyy",java.util.Locale.getDefault());com.mybetlive.i18n.AppStrings.translate("Semana")+" · "+day.format(format)+" – "+day.plusDays(6).format(format)}
}
internal fun bankPlacedDate(value:String,timezone:String):LocalDate=Instant.parse(value).atZone(ZoneId.of(timezone)).toLocalDate()
internal fun bankHistoryDay(day:LocalDate,timezone:String):String{
 val today=LocalDate.now(ZoneId.of(timezone));val date=day.format(DateTimeFormatter.ofPattern("d MMMM",java.util.Locale.getDefault()))
 return when(day){today->com.mybetlive.i18n.AppStrings.translate("Hoy")+" · "+date;today.minusDays(1)->com.mybetlive.i18n.AppStrings.translate("Ayer")+" · "+date;else->day.format(DateTimeFormatter.ofPattern("EEEE d MMMM",java.util.Locale.getDefault()))}
}
@Composable internal fun BankBetRow(bet:BankBet,currency:String,timezone:String,onClick:()->Unit){
 val tone=if(bet.result=="PENDING")Muted else bankTone(bet.profit)
 val accent=when(bet.result){"WON"->Green;"LOST"->Red;else->tone}
 Box(Modifier.fillMaxWidth().testTag("bank-bet-"+bet.id).clickable(onClick=onClick)){
  Row(Modifier.fillMaxWidth().padding(horizontal=2.dp,vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){
   Box(Modifier.width(3.dp).height(52.dp).background(accent,RoundedCornerShape(2.dp)))
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){
    LocalizedText(Instant.parse(bet.placedAt).atZone(ZoneId.of(timezone)).format(DateTimeFormatter.ofPattern("HH:mm"))+" · "+when(bet.format){"COMBINED"->"Combinada";"LAY"->"Lay";else->"Simple"},fontSize=10.sp,lineHeight=14.sp,color=Muted)
    LocalizedText(bet.selections.joinToString(" + "){it.title},fontSize=14.sp,lineHeight=19.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
     LocalizedText((bet.bookmaker.takeIf{it.isNotBlank()}?:bankFormatLabel(bet.format))+" · "+bet.odds,modifier=Modifier.weight(1f),color=Muted,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
     LocalizedText(bankMoney(if(bet.result=="PENDING")bet.stake else bet.profit,currency),fontSize=13.sp,fontWeight=FontWeight.Bold,color=accent)
    }
    LocalizedText(if(bet.result=="PENDING"&&bet.suggestedResult!=null)"Confirmar liquidación" else bankResultLabel(bet.result),fontSize=11.sp,color=accent)
   }
  }
 }
}
internal fun bankDate(value:String,timezone:String):String=runCatching{Instant.parse(value).atZone(ZoneId.of(timezone)).format(DateTimeFormatter.ofPattern("dd MMM · HH:mm",java.util.Locale.getDefault()))}.getOrDefault(value)
internal fun bankTone(value:String?):Color=when(value?.toBigDecimalOrNull()?.signum()){1->Color(0xFF40C983);-1->Red;else->Muted}
internal fun bankFormatLabel(format:String)=when(format){"LAY"->"Simple en contra";"COMBINED"->"Combinada";else->"Simple a favor"}
internal fun bankResultLabel(result:String)=when(result){"WON"->"Ganada";"LOST"->"Perdida";"VOID"->"Anulada";"PUSH"->"Reembolsada";"CASHOUT"->"Cashout";"PARTIAL"->"Liquidación parcial";else->"Pendiente"}
internal val moneyMetricKeys=setOf("benefit","capital","initialCapital","reserved","available","volume","deposits","withdrawals","drawdown","averageStake","maxStake","biggestProfit","biggestLoss","promotionalProfit")
internal val percentageMetricKeys=setOf("roi","successPercent","progression","twr","irr","clv","weightedClv")
internal val bankMetricLabels=linkedMapOf("benefit" to "Beneficio","roi" to "ROI","progression" to "Progresión","twr" to "TWR","irr" to "TIR anualizada","successPercent" to "Aciertos",
 "capital" to "Capital actual","initialCapital" to "Capital inicial","available" to "Disponible","reserved" to "Comprometido","bets" to "Apuestas liquidadas","won" to "Ganadas","lost" to "Perdidas","refunded" to "Reembolsadas","pending" to "En curso","volume" to "Importe jugado",
 "drawdown" to "Drawdown","maxWinStreak" to "Mayor racha ganadora","maxLossStreak" to "Mayor racha perdedora","averageStake" to "Importe medio","maxStake" to "Importe máximo","averageOdds" to "Cuota media","maxWinningOdds" to "Mayor cuota ganada","biggestProfit" to "Mayor beneficio","biggestLoss" to "Mayor pérdida","promotionalProfit" to "Beneficio promocional","clv" to "CLV medio","weightedClv" to "CLV ponderado","clvSamples" to "Muestras CLV")

