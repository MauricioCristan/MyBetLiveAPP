package com.mybetlive.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybetlive.data.*

@Composable internal fun TrackingBuilderSelections(legs:List<TrackingBuilderSelection>,event:EventSnapshot,enabled:Boolean,remove:(Int)->Unit){
    Card(Modifier.fillMaxWidth()){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            LocalizedText("Tu boleto · ${legs.size}/8 selecciones",fontWeight=FontWeight.SemiBold)
            if(legs.isEmpty())LocalizedText("Elige el primer mercado y añádelo al boleto.",color=Muted,fontSize=12.sp)
            legs.forEachIndexed{index,leg->
                Row(Modifier.fillMaxWidth()){
                    LocalizedText("${index+1}. "+leg.title(event),modifier=Modifier.weight(1f),fontSize=13.sp)
                    TextButton(onClick={remove(index)},enabled=enabled){LocalizedText("Quitar")}
                }
            }
            LocalizedText("Un seguimiento · un importe · el mismo partido",color=Green,fontSize=12.sp)
        }
    }
}
