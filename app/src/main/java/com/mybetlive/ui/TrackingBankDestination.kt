package com.mybetlive.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybetlive.data.BankView

internal fun trackingBankDestination(banks:List<BankView>,selectedId:String?):BankView? {
    val writable=banks.filter{!it.archived&&it.writable}
    return writable.singleOrNull() ?: writable.firstOrNull{it.id==selectedId}
}

@Composable internal fun TrackingBankDestination(
    banks:List<BankView>,selectedId:String?,loading:Boolean,error:String?,enabled:Boolean,
    onSelected:(String)->Unit,onReload:()->Unit
) {
    val available=banks.filter{!it.archived&&it.writable}
    val selected=trackingBankDestination(banks,selectedId)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            LocalizedText("Registrar también en Bank",fontWeight=FontWeight.SemiBold)
            when {
                loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                error!=null -> LocalizedText(error,color=Red)
                available.isEmpty() -> LocalizedText("Crea un bank activo en la sección Bank antes de guardar el importe y el momio.",color=Muted)
                available.size==1 -> LocalizedText(selected!!.name+" · "+selected.currency,color=Green)
                else -> BankChoice("¿A qué bank va esta apuesta?",selected?.id.orEmpty(),
                    listOf("" to "Selecciona un bank")+available.map{it.id to it.name+" · "+it.currency},enabled,onSelected)
            }
            if(!loading&&error==null&&available.isNotEmpty())
                LocalizedText("El importe se registrará en "+(selected?.currency?:"la moneda del bank elegido")+". La apuesta quedará pendiente de liquidación.",color=Muted,fontSize=12.sp)
            if(!loading) TextButton(onClick=onReload,enabled=enabled){LocalizedText("Actualizar banks")}
        }
    }
}
