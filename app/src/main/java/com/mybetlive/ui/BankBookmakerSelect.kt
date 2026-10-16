package com.mybetlive.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybetlive.bank.BankState
import com.mybetlive.bank.BankViewModel

@Composable internal fun BankBookmakerSelect(
    value:String, state:BankState, model:BankViewModel,
    label:String="Casa de apuestas", extraNames:List<String> = emptyList(), change:(String)->Unit
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var validation by rememberSaveable { mutableStateOf<String?>(null) }
    val names=(state.bookmakers.map{it.name}+state.banks.map{it.defaultBookmaker}+
        state.history?.content.orEmpty().map{it.bookmaker}+extraNames+value)
        .filter{it.isNotBlank()}.distinctBy{it.trim().lowercase(java.util.Locale.ROOT)}.sortedBy{it.lowercase(java.util.Locale.ROOT)}
    val options=listOf("" to "Sin especificar")+names.map{it to it}+listOf("\u0000add" to "+ Añadir casa de apuestas")
    BankBetChoice(label,value,options,!state.busy&&!state.offline){
        if(it=="\u0000add"){name="";validation=null;creating=true}else change(it)
    }
    if(creating) Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            LocalizedText("Añadir casa de apuestas",fontWeight=FontWeight.SemiBold)
            LocalizedText("Se guardará en tu cuenta para usarla en tus apuestas.",color=Muted,fontSize=13.sp)
            BankBetField("Nombre de la casa",name,{name=it;validation=null},enabled=!state.busy)
            validation?.let{LocalizedText(it,color=Red)}
            state.error?.let{LocalizedText(it,color=Red)}
            if(state.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
            TextButton(onClick={creating=false},enabled=!state.busy){LocalizedText("Cancelar")}
            TextButton(onClick={
            val clean=name.trim().replace(Regex("\\s+")," ")
            if(clean.isBlank()||clean.length>80||clean.any{it.isISOControl()}){
                validation="Introduce un nombre válido de hasta 80 caracteres."
            }else model.addBookmaker(clean){saved->change(saved);creating=false}
        },enabled=!state.busy&&!state.offline){LocalizedText("Guardar casa")}
        }
    }
}
