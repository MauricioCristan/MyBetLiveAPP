package com.mybetlive.ui
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mybetlive.bank.*
import com.mybetlive.data.*
import java.time.*
import java.time.format.DateTimeFormatter
@Composable internal fun BankDialog(title:String,busy:Boolean,onDismiss:()->Unit,content:@Composable ColumnScope.()->Unit){
 Dialog(onDismissRequest={if(!busy)onDismiss()},properties=DialogProperties(usePlatformDefaultWidth=false)){
  Surface(Modifier.fillMaxSize(),color=Ink){Column(Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){LocalizedText(title,fontSize=24.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));TextButton(onClick=onDismiss,enabled=!busy){LocalizedText("Cerrar")}}
   if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
   content()
  }}
 }
}
@Composable internal fun BankField(label:String,value:String,onChange:(String)->Unit,number:Boolean=false,enabled:Boolean=true){
 OutlinedTextField(value,onChange,label={LocalizedText(label)},singleLine=true,enabled=enabled,modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=if(number)KeyboardType.Decimal else KeyboardType.Text),shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
}
@Composable internal fun BankChoice(label:String,value:String,options:List<Pair<String,String>>,enabled:Boolean=true,change:(String)->Unit){
 var expanded by remember{mutableStateOf(false)}
 Box{OutlinedButton(onClick={expanded=true},enabled=enabled,modifier=Modifier.fillMaxWidth()){LocalizedText(label+": "+(options.find{it.first==value}?.second?:value),modifier=Modifier.weight(1f));LocalizedText("⌄")}
 DropdownMenu(expanded,onDismissRequest={expanded=false}){options.forEach{(key,name)->DropdownMenuItem(text={LocalizedText(name)},onClick={change(key);expanded=false})}}}
}
@Composable private fun BankFormFeedback(state:BankState,validation:String?){validation?.let{LocalizedText(it,color=Red)};state.error?.let{LocalizedText(it,color=Red)}}
internal fun bankFormTime(date:String,time:String,zone:String):String?=runCatching{LocalDate.parse(date).atTime(LocalTime.parse(time)).atZone(ZoneId.of(zone)).toInstant().toString()}.getOrNull()
@Composable private fun BankToggle(label:String,value:Boolean,enabled:Boolean,change:(Boolean)->Unit){
 Row(Modifier.fillMaxWidth().toggleable(value=value,enabled=enabled,role=Role.Checkbox,onValueChange=change),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Checkbox(value,null,enabled=enabled);LocalizedText(label,modifier=Modifier.weight(1f))}
}
@Composable internal fun BankEditor(bank:BankView?,state:BankState,model:BankViewModel,dismiss:()->Unit){
 var name by rememberSaveable{mutableStateOf(bank?.name?:"Personal")};var currency by rememberSaveable{mutableStateOf(bank?.currency?:"MXN")}
 var timezone by rememberSaveable{mutableStateOf(bank?.timezone?:ZoneId.systemDefault().id)};var initial by rememberSaveable{mutableStateOf(bank?.initialCapital?:"")}
 val originalDate=bank?.openedAt?.let{Instant.parse(it).atZone(ZoneId.of(timezone)).toLocalDate().toString()}
 var opened by rememberSaveable{mutableStateOf(originalDate?:LocalDate.now().toString())};var notes by rememberSaveable{mutableStateOf(bank?.notes.orEmpty())}
 var bookmaker by rememberSaveable{mutableStateOf(bank?.defaultBookmaker.orEmpty())};var stake by rememberSaveable{mutableStateOf(bank?.defaultStake.orEmpty())}
 var odds by rememberSaveable{mutableStateOf(bank?.defaultOdds.orEmpty())};var sport by rememberSaveable{mutableStateOf(bank?.defaultSport?:"SOCCER")}
 var archived by rememberSaveable{mutableStateOf(bank?.archived?:false)};var validation by remember{mutableStateOf<String?>(null)}
 BankDialog(if(bank==null)"Crear bank" else "Editar bank",state.busy,dismiss){
  BankFormFeedback(state,validation);BankField("Nombre del bank",name,{name=it},enabled=!state.busy)
  if(bank==null)BankChoice("Moneda",currency,listOf("MXN" to "Pesos mexicanos · MXN","USD" to "Dólares · USD","EUR" to "Euros · EUR"),!state.busy){currency=it}
  else LocalizedText("Moneda: "+currency+" · no se convierte el historial",color=Muted,fontSize=12.sp)
  BankField("Capital inicial",initial,{initial=it},true,!state.busy);BankDateField("Fecha de apertura",opened,{opened=it},enabled=!state.busy,zone=timezone)
  BankField("Zona horaria",timezone,{timezone=it},enabled=!state.busy)
  if(bank!=null){
   BankField("Notas del bank",notes,{notes=it},enabled=!state.busy);LocalizedText("Valores predeterminados",fontWeight=FontWeight.SemiBold)
   BankBookmakerSelect(bookmaker,state,model,label="Casino predeterminado"){bookmaker=it};BankField("Deporte predeterminado",sport,{sport=it},enabled=!state.busy)
   BankField("Importe predeterminado · opcional",stake,{stake=it},true,!state.busy);BankField("Cuota predeterminada · opcional",odds,{odds=it},true,!state.busy)
   BankToggle("Archivar bank · conservar historial",archived,!state.busy){archived=it}
  }
  Button(onClick={
   val amount=bankDecimal(initial);val date=runCatching{if(bank!=null&&opened==originalDate)bank.openedAt else LocalDate.parse(opened).atStartOfDay(ZoneId.of(timezone)).toInstant().toString()}.getOrNull()
   if(name.isBlank()||amount==null||date==null){validation="Revisa nombre, importe, fecha y zona horaria.";return@Button}
   val key=listOf(name,currency,timezone,amount,date,notes,bookmaker,stake,odds,sport,archived).joinToString("|")
   model.write("bank|"+key,{api,op,v->if(bank==null)api.create(CreateBankInput(op,name,currency,timezone,date,amount))
    else api.edit(bank.id,EditBankInput(op,v,name,timezone,notes,bookmaker,sport,bankDecimal(stake),bankDecimal(odds),amount,date,archived))},dismiss)
  },enabled=!state.busy&&!state.offline,modifier=Modifier.fillMaxWidth()){LocalizedText("Guardar bank")}
 }
}
private data class LegDraft(val title:String="",val sport:String="SOCCER",val market:String="",val odds:String="",val trackedBetId:String?=null)
@Composable internal fun BankBetEditor(bank:BankView,bet:BankBet?,state:BankState,model:BankViewModel,tracked:List<BetView>,dismiss:()->Unit){
 val advanced=state.summary?.access?.advanced==true;val initialTime=bet?.placedAt?.let{Instant.parse(it).atZone(ZoneId.of(bank.timezone))}?:ZonedDateTime.now(ZoneId.of(bank.timezone))
 var date by rememberSaveable{mutableStateOf(initialTime.toLocalDate().toString())};var time by rememberSaveable{mutableStateOf(initialTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")))}
 var format by rememberSaveable{mutableStateOf(bet?.format?:"BACK")};var stake by rememberSaveable{mutableStateOf(bet?.stake?:bank.defaultStake.orEmpty())}
 var percentage by rememberSaveable{mutableStateOf(false)};var odds by rememberSaveable{mutableStateOf(bet?.odds?:bank.defaultOdds.orEmpty())}
 var closing by rememberSaveable{mutableStateOf(bet?.closingOdds.orEmpty())};var freeBet by rememberSaveable{mutableStateOf(bet?.freeBet?:false)}
 var bonus by rememberSaveable{mutableStateOf(bet?.bonusPercent?:"0")};var commission by rememberSaveable{mutableStateOf(bet?.commissionPercent?:"0")}
 var bookmaker by rememberSaveable{mutableStateOf(bet?.bookmaker?:bank.defaultBookmaker)};var notes by rememberSaveable{mutableStateOf(bet?.notes.orEmpty())}
 val legs=remember{mutableStateListOf<LegDraft>().apply{addAll(bet?.selections?.map{LegDraft(it.title,it.sport,it.market.orEmpty(),it.odds.orEmpty(),it.trackedBetId)}?:listOf(LegDraft(sport=bank.defaultSport)))}}
 var validation by remember{mutableStateOf<String?>(null)};var details by rememberSaveable{mutableStateOf(bet!=null&&(bet.freeBet||bet.bonusPercent!="0"||bet.commissionPercent!="0"||bet.closingOdds!=null))}
 var dataExpanded by rememberSaveable{mutableStateOf(false)}
 var selectionExpanded by rememberSaveable{mutableStateOf(true)}
 var amountExpanded by rememberSaveable{mutableStateOf(false)}
 val formats=if(advanced)listOf("BACK","COMBINED","LAY")+(if(bet?.format=="BUILDER")listOf("BUILDER")else emptyList())else listOf("BACK")+(bet?.format?.takeIf{it!="BACK"}?.let{listOf(it)}?:emptyList())
 val parsedStake=bankDecimal(stake)?.toBigDecimalOrNull()
 val ownStake=if(percentage)parsedStake?.let{bank.available.toBigDecimalOrNull()?.multiply(it)?.divide(java.math.BigDecimal("100"))}else parsedStake
 val exposure=if(freeBet&&format!="LAY")java.math.BigDecimal.ZERO else if(format=="LAY")ownStake?.let{amount->bankDecimal(odds)?.toBigDecimalOrNull()?.let{amount*(it-java.math.BigDecimal.ONE)}}else ownStake
 BankBetLayout(if(bet==null)"Añadir apuesta" else "Modificar apuesta",bank.name+" · "+bank.currency,state.busy,!state.busy&&!state.offline,dismiss,onSave={
   val placed=bankFormTime(date,time,bank.timezone);val amount=bankDecimal(stake);val price=bankDecimal(odds)
   if(placed==null||amount==null||price==null||legs.any{it.title.isBlank()}||format!="COMBINED"&&legs.size!=1){dataExpanded=true;selectionExpanded=true;amountExpanded=true;validation="Revisa la fecha, el importe, la cuota y las selecciones.";return@BankBetLayout}
   val selections=legs.map{BankLegInput(it.title,it.sport,it.market.takeIf(String::isNotBlank),bankDecimal(it.odds),it.trackedBetId)}
   val key=listOf(bet?.id,placed,format,amount,percentage,price,closing,freeBet,bonus,commission,bookmaker,notes,selections).joinToString("|")
   model.write("bet|"+key,{api,op,v->val input=BankBetInput(op,v,placed,format,if(percentage)null else amount,if(percentage)amount else null,price,bankDecimal(closing),freeBet,bankDecimal(bonus)?:"0",bankDecimal(commission)?:"0",bookmaker,notes,selections);if(bet==null)api.addBet(bank.id,input)else api.editBet(bank.id,bet.id,input)},dismiss)

 }){
  BankFormFeedback(state,validation)
  BankBetSection("01 · Datos del boleto",listOf(bookmaker.takeIf{it.isNotBlank()}?:"Casa de apuestas",date,time).joinToString(" · "),dataExpanded,!state.busy,{dataExpanded=!dataExpanded}){
   Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Column(Modifier.weight(1f)){BankDateField("Fecha",date,{date=it},enabled=!state.busy,zone=bank.timezone)};Column(Modifier.weight(1f)){BankTimeField("Hora",time,{time=it},enabled=!state.busy)}}
   BankBookmakerSelect(bookmaker,state,model,extraNames=tracked.mapNotNull{it.bookmaker}){bookmaker=it}
  }
  BankBetSection("02 · Selección",legs.joinToString(" + "){it.title.ifBlank{"Título de la apuesta"}},selectionExpanded,!state.busy,{selectionExpanded=!selectionExpanded}){
  legs.forEachIndexed{index,leg->Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
   if(format=="COMBINED")LocalizedText("Selección "+(index+1),fontWeight=FontWeight.SemiBold)
   BankBetField("Título de la apuesta",leg.title,{legs[index]=leg.copy(title=it)},enabled=!state.busy)
   Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
    Column(Modifier.weight(1f)){BankBetChoice("Deporte",leg.sport,listOf("SOCCER" to "Fútbol","NFL" to "Fútbol americano","MLB" to "Béisbol","NBA" to "Básquetbol","NHL" to "Hockey","OTHER" to "Otro"),!state.busy){legs[index]=leg.copy(sport=it)}}
    if(format!="COMBINED")Column(Modifier.weight(1f)){BankBetField("Cuota",odds,{odds=it},true,!state.busy)}
   }
   BankBetField("Mercado · opcional",leg.market,{legs[index]=leg.copy(market=it)},enabled=!state.busy)
   if(format=="COMBINED")BankBetField("Cuota de selección · opcional",leg.odds,{legs[index]=leg.copy(odds=it)},true,!state.busy)
   BankBetChoice("Vincular seguimiento",leg.trackedBetId.orEmpty(),listOf("" to "Registro manual")+tracked.map{it.id to it.title()},!state.busy){id->
    val selection=tracked.find{it.id==id};legs[index]=if(selection==null)leg.copy(trackedBetId=null)else leg.copy(trackedBetId=selection.id,title=selection.title(),sport=selection.event.sport,market=selection.market,odds=selection.decimalOdds?.let{java.math.BigDecimal.valueOf(it).toPlainString()}.orEmpty())
    if(selection!=null&&format!="COMBINED"&&bet==null){if(stake.isBlank())stake=selection.stake?.let{java.math.BigDecimal.valueOf(it).toPlainString()}.orEmpty();if(odds.isBlank())odds=selection.decimalOdds?.let{java.math.BigDecimal.valueOf(it).toPlainString()}.orEmpty();if(bookmaker.isBlank())bookmaker=selection.bookmaker.orEmpty()}
   }
   if(format=="COMBINED"&&legs.size>2)TextButton(onClick={legs.removeAt(index)},enabled=!state.busy){LocalizedText("Quitar selección",color=Red)}
  }}
  if(format=="COMBINED"&&advanced)OutlinedButton(onClick={legs.add(LegDraft())},enabled=!state.busy&&legs.size<20,modifier=Modifier.fillMaxWidth()){LocalizedText("Añadir selección")}

  }
  BankBetSection("03 · Importe y formato",(if(percentage)stake+"%" else bankMoney(bankDecimal(stake),bank.currency))+" · "+bankFormatLabel(format),amountExpanded,!state.busy,{amountExpanded=!amountExpanded}){
   BankBetChoice("Formato",format,formats.map{it to bankFormatLabel(it)},!state.busy){next->format=next;if(next=="COMBINED"&&legs.size==1)legs.add(LegDraft());if(next!="COMBINED")while(legs.size>1)legs.removeAt(legs.lastIndex);if(next=="LAY")freeBet=false;selectionExpanded=true}
   if(format=="LAY")LocalizedText("El importe es la apuesta de la contraparte. La exposición es importe × (cuota − 1).",color=Muted,fontSize=12.sp)
  if(format=="COMBINED"){
   val product=legs.mapNotNull{bankDecimal(it.odds)?.toBigDecimalOrNull()}.takeIf{it.size==legs.size}?.fold(java.math.BigDecimal.ONE){a,b->a*b}
   product?.let{LocalizedText("Producto de cuotas: "+it.setScale(4,java.math.RoundingMode.HALF_UP).toPlainString()+" · usa la cuota total real del boleto",fontSize=11.sp,color=Muted)}
  }
  if(format=="COMBINED")BankBetField("Cuota total del boleto",odds,{odds=it},true,!state.busy)
  if(advanced)BankToggle("Importe en % del disponible",percentage,!state.busy){percentage=it}
  BankBetField(if(percentage)"Porcentaje del disponible" else "Importe · "+bank.currency,stake,{stake=it},true,!state.busy)

  }
  BankBetSection("Detalles y promociones",if(advanced)"Premium · CLV, bonos y comisión" else "Notas",details,!state.busy,{details=!details}){
   if(advanced||bet?.freeBet==true)BankToggle("Apuesta gratuita",freeBet,!state.busy&&format!="LAY"){freeBet=it}
   if(advanced||bet?.closingOdds!=null)BankBetField("Cuota de cierre · CLV opcional",closing,{closing=it},true,!state.busy)
   if(advanced||bet?.bonusPercent?.toBigDecimalOrNull()?.signum()==1)BankBetField("Bono sobre beneficio · %",bonus,{bonus=it},true,!state.busy)
   if(advanced||bet?.commissionPercent?.toBigDecimalOrNull()?.signum()==1)BankBetField("Comisión sobre beneficio · %",commission,{commission=it},true,!state.busy)
   BankBetField("Notas",notes,{notes=it},enabled=!state.busy)

  }
  BankPanel{
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){LocalizedText("Exposición propia",color=Muted);LocalizedText(bankMoney(exposure?.toPlainString(),bank.currency),color=Green,fontWeight=FontWeight.SemiBold)}
   LocalizedText("Vista previa · el saldo lo confirma el servidor",color=Muted,fontSize=11.sp)
  }
  LocalizedText("Sin separador de miles. El registro no realiza ninguna apuesta ni mueve dinero.",color=Muted,fontSize=11.sp)
 }
}
@Composable internal fun BankSettlementEditor(bank:BankView,bet:BankBet,state:BankState,model:BankViewModel,dismiss:()->Unit){
 var result by rememberSaveable{mutableStateOf(bet.suggestedResult?:bet.result.takeIf{it!="PENDING"}?:"WON")}
 var payout by rememberSaveable{mutableStateOf(bet.payout?:bet.suggestedPayout.orEmpty())};val initial=bet.settledAt?.let{Instant.parse(it).atZone(ZoneId.of(bank.timezone))}?:ZonedDateTime.now(ZoneId.of(bank.timezone))
 var date by rememberSaveable{mutableStateOf(initial.toLocalDate().toString())};var time by rememberSaveable{mutableStateOf(initial.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")))};var validation by remember{mutableStateOf<String?>(null)}
 BankDialog("Confirmar liquidación",state.busy,dismiss){
  BankFormFeedback(state,validation);LocalizedText(bet.selections.joinToString(" + "){it.title},fontWeight=FontWeight.SemiBold)
  LocalizedText("Los datos deportivos proponen un resultado. Confirma lo liquidado por tu casino.",color=Muted,fontSize=12.sp)
  BankChoice("Resultado",result,listOf("WON","LOST","PUSH","VOID","CASHOUT","PARTIAL").map{it to bankResultLabel(it)},!state.busy){result=it;if(it=="LOST")payout="0.00" else if(it in listOf("PUSH","VOID"))payout=bet.exposure}
  BankField("Retorno total recibido · "+bank.currency,payout,{payout=it},true,!state.busy)
  BankDateField("Fecha de liquidación",date,{date=it},enabled=!state.busy,zone=bank.timezone);BankTimeField("Hora",time,{time=it},enabled=!state.busy)
  LocalizedText("Exposición: "+bankMoney(bet.exposure,bank.currency),color=Muted)
  bankDecimal(payout)?.toBigDecimalOrNull()?.let{LocalizedText("Beneficio neto: "+bankMoney((it-bet.exposure.toBigDecimal()).toPlainString(),bank.currency),fontWeight=FontWeight.SemiBold)}
  if(result in listOf("CASHOUT","PARTIAL"))LocalizedText("Esta liquidación cierra el boleto completo. No queda una parte abierta.",color=Muted,fontSize=12.sp)
  Button(onClick={val amount=bankDecimal(payout);val settled=bankFormTime(date,time,bank.timezone);if(amount==null||settled==null){validation="Revisa el retorno y la fecha.";return@Button}
   model.write("settle|"+listOf(bet.id,result,amount,settled).joinToString("|"),{api,op,v->api.settle(bank.id,bet.id,BankSettlement(op,v,result,amount,settled))},dismiss)
  },enabled=!state.busy&&!state.offline,modifier=Modifier.fillMaxWidth()){LocalizedText("Confirmar retorno recibido")}
 }
}
@Composable internal fun BankPeriodEditor(state:BankState,model:BankViewModel,dismiss:()->Unit){
 var from by rememberSaveable{mutableStateOf(state.from)};var to by rememberSaveable{mutableStateOf(state.to)}
 var validation by remember{mutableStateOf<String?>(null)}
 BankDialog("Periodo",state.busy,dismiss){
  BankFormFeedback(state,validation)
  BankDateField("Desde",from,{from=it},enabled=!state.busy,zone=state.summary?.bank?.timezone?:ZoneId.systemDefault().id)
  BankDateField("Hasta",to,{to=it},enabled=!state.busy,zone=state.summary?.bank?.timezone?:ZoneId.systemDefault().id)
  Button(onClick={
   if(runCatching{!LocalDate.parse(from).isAfter(LocalDate.parse(to))}.getOrDefault(false)){model.period(from,to);dismiss()}
   else validation="Revisa el periodo seleccionado."
  },enabled=!state.busy&&!state.offline,modifier=Modifier.fillMaxWidth()){LocalizedText("Aplicar periodo")}
 }
}
@Composable internal fun BankHistorySearch(state:BankState,model:BankViewModel){
 var result by rememberSaveable(state.summary?.bank?.id){mutableStateOf("")}
 var query by rememberSaveable(state.summary?.bank?.id){mutableStateOf("")}
 val enabled=!state.offline&&!state.busy
 fun apply(){model.history(0,result.takeIf(String::isNotBlank),null,null,null,null,query.trim().takeIf(String::isNotBlank))}
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
  OutlinedTextField(value=query,onValueChange={query=it.take(120)},placeholder={LocalizedText("Buscar apuesta",fontSize=13.sp)},singleLine=true,enabled=!state.offline,
   modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=Panel,focusedContainerColor=Panel,unfocusedBorderColor=Panel),
   keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={if(enabled)apply()}),
   trailingIcon={TextButton(onClick={apply()},enabled=enabled){LocalizedText("Buscar",fontSize=12.sp)}})
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   FilterChip(selected=result.isEmpty(),onClick={result="";apply()},enabled=enabled,label={LocalizedText("Todas · "+(state.summary?.totalBets?:0),fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)},modifier=Modifier.weight(1f),shape=RoundedCornerShape(20.dp),border=null,colors=FilterChipDefaults.filterChipColors(containerColor=Panel,selectedContainerColor=Green.copy(alpha=.14f),labelColor=Muted,selectedLabelColor=Green))
   FilterChip(selected=result=="PENDING",onClick={result="PENDING";apply()},enabled=enabled,label={LocalizedText("Pendientes · "+(state.summary?.pending?:0),fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)},modifier=Modifier.weight(1f),shape=RoundedCornerShape(20.dp),border=null,colors=FilterChipDefaults.filterChipColors(containerColor=Panel,selectedContainerColor=Green.copy(alpha=.14f),labelColor=Muted,selectedLabelColor=Green))
  }
 }
}
@Composable internal fun BankStatisticsFilter(state:BankState,model:BankViewModel){
 var sport by rememberSaveable{mutableStateOf("")};var market by rememberSaveable{mutableStateOf("")};var bookmaker by rememberSaveable{mutableStateOf("")}
 BankPanel{
  LocalizedText(state.from+" · "+state.to,color=Muted,fontSize=12.sp)
  BankField("Deporte · código opcional",sport,{sport=it});BankField("Mercado · código opcional",market,{market=it});BankField("Casino · opcional",bookmaker,{bookmaker=it})
  Button(onClick={model.statistics(sport.takeIf(String::isNotBlank),market.takeIf(String::isNotBlank),bookmaker.takeIf(String::isNotBlank))},enabled=!state.offline&&!state.busy,modifier=Modifier.fillMaxWidth()){LocalizedText("Aplicar filtros")}
 }
}
@Composable internal fun BankCsvEditor(bank:BankView,state:BankState,model:BankViewModel,onPlans:()->Unit,dismiss:()->Unit){
 val context=LocalContext.current;val kind="BETS";var text by remember{mutableStateOf("")};var validatedText by remember{mutableStateOf("")};var localError by remember{mutableStateOf<String?>(null)};var exportText by remember{mutableStateOf("")}
 val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){uri->uri?.let{runCatching{context.contentResolver.openOutputStream(it)?.use{out->out.write(exportText.toByteArray(Charsets.UTF_8))}}.onFailure{localError="No se pudo guardar el archivo."}}}
 val load=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{runCatching{context.contentResolver.openInputStream(it)?.bufferedReader(Charsets.UTF_8)?.use{r->val buffer=CharArray(5_000_001);var count=0;while(count<buffer.size){val read=r.read(buffer,count,buffer.size-count);if(read<0)break;count+=read};require(count<=5_000_000);String(buffer,0,count)}}.onSuccess{text=it.orEmpty();validatedText=""}.onFailure{localError="No se pudo leer el CSV o excede 5 MB."}}}
 BankDialog("Importar / exportar CSV",state.busy,dismiss){
  LocalizedText("Plantillas versión 1 · moneda "+bank.currency+" · fechas ISO · hasta 1000 filas por lote",color=Muted,fontSize=12.sp)
  OutlinedButton(onClick={model.export(kind,true){exportText=it;export.launch("mybetlive-plantilla-"+kind.lowercase()+".csv")}},enabled=!state.busy&&!state.offline,modifier=Modifier.fillMaxWidth()){LocalizedText("Descargar plantilla CSV")}
  OutlinedButton(onClick={model.export(kind,false){exportText=it;export.launch("mybetlive-"+kind.lowercase()+".csv")}},enabled=!state.busy&&!state.offline,modifier=Modifier.fillMaxWidth()){LocalizedText("Exportar mis datos")}
  OutlinedButton(onClick={model.export("LEDGER",false){exportText=it;export.launch("mybetlive-historial-financiero.csv")}},enabled=!state.busy&&!state.offline,modifier=Modifier.fillMaxWidth()){LocalizedText("Exportar historial financiero completo")}
  if(state.summary?.access?.advanced==true){
   OutlinedButton(onClick={load.launch(arrayOf("text/*","application/csv","application/octet-stream"))},enabled=!state.busy&&!state.offline,modifier=Modifier.fillMaxWidth()){LocalizedText("Seleccionar CSV")}
   if(text.isNotBlank())LocalizedText("Archivo cargado · "+text.length+" caracteres",fontSize=12.sp,color=Muted)
   Button(onClick={validatedText=text;model.preview(kind,text)},enabled=text.isNotBlank()&&!state.busy&&!state.offline,modifier=Modifier.fillMaxWidth()){LocalizedText("Validar importación")}
   state.preview?.takeIf{text==validatedText}?.let{preview->
    LocalizedText("Válidas: "+preview.valid+" · duplicadas: "+preview.duplicates+" · inválidas: "+preview.invalid,fontSize=13.sp)
    preview.rows.take(100).forEach{row->LocalizedText("Fila "+row.row+": "+(row.error?:if(row.duplicate)"Ya importada · se omitirá" else row.title),color=if(row.error!=null)Red else Muted,fontSize=11.sp)}
    Button(onClick={model.write("csv|"+kind+"|"+preview.checksum,{api,op,v->api.importCsv(bank.id,BankCsvImport(op,v,kind,text,preview.checksum))},dismiss)},enabled=preview.invalid==0&&preview.valid>0&&!state.busy&&!state.offline,modifier=Modifier.fillMaxWidth()){LocalizedText("Confirmar importación")}
   }
  }else OutlinedButton(onClick=onPlans){LocalizedText("Importación disponible con Premium")}
  localError?.let{LocalizedText(it,color=Red)};state.error?.let{LocalizedText(it,color=Red)}
 }
}

