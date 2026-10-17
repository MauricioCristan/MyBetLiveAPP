package com.mybetlive.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mybetlive.bank.*
import com.mybetlive.data.*

@Composable internal fun BankBetDetail(bank:BankView,bet:BankBet,state:BankState,model:BankViewModel,
    onDismiss:()->Unit,onEdit:()->Unit,onSettle:()->Unit) {
    var menu by remember { mutableStateOf(false) }
    var delete by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    val pending=bet.result=="PENDING"
    val enabled=!state.busy&&!state.offline
    val amber=Color(0xFFF2BD5B)
    val resultColor=when(bet.result){"PENDING"->amber;"WON"->Green;"LOST"->Red;else->Muted}
    val single=bet.selections.singleOrNull()
    val title=single?.title?.substringBefore(" · ")?:"Combinada"
    val subtitle=single?.title?.substringAfter(" · ","").orEmpty()
    val resources=LocalContext.current.resources
    val closeLabel=com.mybetlive.i18n.AppStrings.translate("Cerrar",resources)
    val actionsLabel=com.mybetlive.i18n.AppStrings.translate("Acciones de la apuesta",resources)
    Dialog(onDismissRequest={if(!state.busy)onDismiss()},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=Ink) {
            Column(Modifier.safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
                    TextButton(onClick=onDismiss,enabled=!state.busy,modifier=Modifier.semantics{contentDescription=closeLabel}){LocalizedText("‹",fontSize=30.sp)}
                    LocalizedText("Detalle de apuesta",fontSize=16.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
                    Box {
                        IconButton(onClick={menu=true},enabled=!state.busy,
                            modifier=Modifier.semantics{contentDescription=actionsLabel}){LocalizedText("⋮",fontSize=26.sp,color=Muted)}
                        DropdownMenu(menu,onDismissRequest={menu=false}) {
                            DropdownMenuItem(text={LocalizedText("Modificar apuesta")},enabled=enabled,onClick={menu=false;onEdit()})
                            DropdownMenuItem(text={LocalizedText("Copiar como apuesta nueva")},enabled=enabled&&bank.writable,
                                onClick={menu=false;model.write("copy|"+bet.id,{api,op,v->api.copy(bank.id,bet.id,BankCommand(op,v))},onDismiss)})
                            HorizontalDivider()
                            DropdownMenuItem(text={LocalizedText("Eliminar apuesta",color=Red)},enabled=enabled,onClick={menu=false;delete=true})
                        }
                    }
                }
                if(state.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=12.dp),
                    verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    if(state.offline)LocalizedText("Sin conexión · consulta de datos guardados",color=Muted,fontSize=12.sp)
                    state.error?.let{LocalizedText(it,color=Red)}
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        LocalizedText(bank.name+" · "+bank.currency,color=Muted,fontSize=11.sp)
                        LocalizedText(title,fontSize=26.sp,lineHeight=32.sp,fontWeight=FontWeight.Bold)
                        if(subtitle.isNotBlank())LocalizedText(subtitle,color=Muted,fontSize=14.sp,lineHeight=20.sp)
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                            BankDetailBadge(bankResultLabel(bet.result),resultColor)
                            BankDetailBadge(when(bet.format){"LAY"->"Simple · Lay";"COMBINED"->"Combinada";"BUILDER"->"Creador de apuestas";else->"Simple · Back"},Muted)
                            Spacer(Modifier.weight(1f))
                            if(single!=null)LocalizedText(sportLabel(single.sport),fontSize=11.sp,color=Muted)
                        }
                    }
                    BankPanel {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1.6f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                LocalizedText(if(bet.freeBet)"Importe promocional" else if(bet.format=="LAY")"Importe de contraparte" else "Importe apostado",color=Muted,fontSize=11.sp)
                                LocalizedText(bankMoney(bet.stake,bank.currency),fontSize=25.sp,lineHeight=32.sp,fontWeight=FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                LocalizedText("Cuota",color=Muted,fontSize=11.sp)
                                LocalizedText(bet.odds,fontSize=23.sp,lineHeight=32.sp,fontWeight=FontWeight.Bold)
                            }
                        }
                        HorizontalDivider(color=Muted.copy(alpha=.2f))
                        BankDetailPair("Exposición propia",bankMoney(bet.exposure,bank.currency))
                    }
                    Surface(color=if(pending)Color(0xFF1B2C22)else resultColor.copy(alpha=.12f),shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            LocalizedText(if(pending)"Retorno potencial" else "Retorno recibido",color=if(pending)Green else resultColor,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
                            LocalizedText(bankMoney(if(pending)bet.potentialReturn else bet.payout,bank.currency),fontSize=32.sp,lineHeight=38.sp,fontWeight=FontWeight.Bold,color=if(pending)Green else resultColor)
                            if(pending)LocalizedText("Aún no se incluye en tu saldo.",color=Muted,fontSize=11.sp)
                            else BankDetailPair("Beneficio neto",bankMoney(bet.profit,bank.currency),bankTone(bet.profit))
                        }
                    }
                    if(single==null)BankPanel {
                        LocalizedText("Selecciones",fontWeight=FontWeight.SemiBold)
                        bet.selections.forEachIndexed{index,leg->
                            if(index>0)HorizontalDivider(color=Muted.copy(alpha=.2f))
                            LocalizedText(leg.title,fontSize=14.sp)
                            LocalizedText(listOf(sportLabel(leg.sport),leg.odds?.let{"Cuota: "+it}).filterNotNull().joinToString(" · "),color=Muted,fontSize=12.sp)
                        }
                    }
                    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        LocalizedText("Información del boleto",fontWeight=FontWeight.SemiBold,fontSize=14.sp)
                        BankPanel {
                            BankDetailPair("Casa de apuestas",bet.bookmaker.ifBlank{"Sin especificar"})
                            HorizontalDivider(color=Muted.copy(alpha=.2f))
                            BankDetailPair("Fecha y hora",bankDate(bet.placedAt,bank.timezone))
                            bet.settledAt?.let{BankDetailPair("Fecha de liquidación",bankDate(it,bank.timezone))}
                        }
                    }
                    if(pending&&bet.suggestedResult!=null)BankPanel {
                        LocalizedText("Propuesta deportiva: "+bankResultLabel(bet.suggestedResult),fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=amber)
                        LocalizedText("Confirma el retorno que liquidó tu casa de apuestas. El saldo cambiará cuando lo confirmes.",color=Muted,fontSize=11.sp,lineHeight=16.sp)
                    }
                    if(bet.notes.isNotBlank()||bet.closingOdds!=null||bet.freeBet||bet.bonusPercent.toBigDecimalOrNull()?.signum()==1||bet.commissionPercent.toBigDecimalOrNull()?.signum()==1) {
                        BankBetSection("Notas y detalles","",details,true,{details=!details}) {
                            bet.closingOdds?.let{BankDetailPair("Cuota de cierre",it)}
                            if(bet.freeBet)LocalizedText("Apuesta gratuita",fontSize=12.sp,color=Muted)
                            if(bet.bonusPercent.toBigDecimalOrNull()?.signum()==1)BankDetailPair("Bono sobre beneficio",bankPercent(bet.bonusPercent))
                            if(bet.commissionPercent.toBigDecimalOrNull()?.signum()==1)BankDetailPair("Comisión sobre beneficio",bankPercent(bet.commissionPercent))
                            if(bet.notes.isNotBlank())LocalizedText(bet.notes,fontSize=13.sp)
                        }
                    }
                }
                HorizontalDivider(color=Muted.copy(alpha=.2f))
                Button(onClick=onSettle,enabled=enabled,modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=12.dp),
                    shape=RoundedCornerShape(12.dp),contentPadding=PaddingValues(16.dp)) {
                    LocalizedText(if(pending)"Confirmar liquidación" else "Corregir liquidación",fontWeight=FontWeight.SemiBold)
                }
            }
        }
    }
    if(delete)AlertDialog(onDismissRequest={delete=false},title={LocalizedText("Eliminar apuesta")},
        text={LocalizedText("Se revertirá su efecto financiero y se conservará la corrección en el historial.")},
        confirmButton={TextButton(onClick={delete=false;model.write("delete|"+bet.id,{api,op,v->api.delete(bank.id,bet.id,BankCommand(op,v))},onDismiss)},enabled=enabled){LocalizedText("Eliminar")}},
        dismissButton={TextButton(onClick={delete=false}){LocalizedText("Cancelar")}})
}

@Composable private fun BankDetailBadge(label:String,color:Color) {
    Surface(color=color.copy(alpha=.12f),shape=RoundedCornerShape(8.dp)) {
        LocalizedText(label,color=color,fontSize=11.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(horizontal=10.dp,vertical=7.dp))
    }
}

@Composable private fun BankDetailPair(label:String,value:String,color:Color=MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        LocalizedText(label,color=Muted,fontSize=12.sp,modifier=Modifier.weight(1f))
        LocalizedText(value,color=color,fontSize=12.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.End)
    }
}
