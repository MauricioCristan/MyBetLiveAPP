package com.mybetlive.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable internal fun BankBetField(label:String,value:String,onChange:(String)->Unit,number:Boolean=false,enabled:Boolean=true) {
    OutlinedTextField(value,onChange,label={LocalizedText(label,fontSize=12.sp)},singleLine=true,
        enabled=enabled,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(10.dp),
        textStyle=LocalTextStyle.current.copy(fontSize=14.sp),
        keyboardOptions=KeyboardOptions(keyboardType=if(number)KeyboardType.Decimal else KeyboardType.Text),
        colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=Ink.copy(alpha=.45f),
            focusedContainerColor=Ink.copy(alpha=.45f),unfocusedBorderColor=Muted.copy(alpha=.3f)))
}

@Composable internal fun BankBetChoice(label:String,value:String,options:List<Pair<String,String>>,enabled:Boolean=true,change:(String)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick={expanded=true},enabled=enabled,modifier=Modifier.fillMaxWidth(),
            shape=RoundedCornerShape(10.dp),contentPadding=PaddingValues(12.dp)) {
            LocalizedText(label+": "+(options.find{it.first==value}?.second?:value),fontSize=13.sp,
                modifier=Modifier.weight(1f))
            LocalizedText("⌄",color=Green)
        }
        DropdownMenu(expanded,onDismissRequest={expanded=false}) {
            options.forEach{(key,name)->DropdownMenuItem(text={LocalizedText(name)},onClick={change(key);expanded=false})}
        }
    }
}

/** The action stays outside the scrolling form, including when the keyboard is open. */
@Composable internal fun BankBetLayout(
    title: String, subtitle: String, busy: Boolean, enabled: Boolean,
    onDismiss: () -> Unit, onSave: () -> Unit, content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(Modifier.safeDrawingPadding().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    LocalizedText(title, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss, enabled = !busy) { LocalizedText("Cerrar") }
                }
                LocalizedText(subtitle, color = Muted, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
                HorizontalDivider(color = Muted.copy(alpha = .2f))
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = onSave, enabled = enabled, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(16.dp)) {
                        LocalizedText("Guardar apuesta", fontWeight = FontWeight.SemiBold)
                    }
                    LocalizedText("El resultado se confirma en la liquidación", color = Muted,
                        fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable internal fun BankBetSection(
    title: String, summary: String, expanded: Boolean, enabled: Boolean,
    onToggle: () -> Unit, content: @Composable ColumnScope.() -> Unit
) {
    Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            Column(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onToggle)
                .semantics { stateDescription = if (expanded) "Expandido" else "Contraído" }
                .padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LocalizedText(title, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                    LocalizedText(if (expanded) "⌃" else "⌄", color = Green)
                }
                if (!expanded && summary.isNotBlank()) LocalizedText(summary, color = Muted, fontSize = 12.sp)
            }
            if (expanded) Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}
