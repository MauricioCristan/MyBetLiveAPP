package com.mybetlive.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.*
import java.time.format.DateTimeFormatter

@Composable internal fun BankDateField(label:String,value:String,onChange:(String)->Unit,enabled:Boolean=true,zone:String=ZoneId.systemDefault().id) {
    var open by rememberSaveable { mutableStateOf(false) }
    val context=LocalContext.current
    val currentChange by rememberUpdatedState(onChange)
    val date=runCatching{LocalDate.parse(value)}.getOrElse{LocalDate.now(ZoneId.of(zone))}
    BankDateTimeButton(label,date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),false,enabled){open=true}
    if(open&&enabled) DisposableEffect(context) {
        val picker=DatePickerDialog(context,android.R.style.Theme_Material_Dialog_Alert,
            {_,year,month,day->currentChange(LocalDate.of(year,month+1,day).toString());open=false},
            date.year,date.monthValue-1,date.dayOfMonth)
        picker.setOnDismissListener{open=false}
        picker.show()
        onDispose{picker.setOnDismissListener(null);picker.dismiss()}
    }
}

@Composable internal fun BankTimeField(label:String,value:String,onChange:(String)->Unit,enabled:Boolean=true) {
    var open by rememberSaveable { mutableStateOf(false) }
    val context=LocalContext.current
    val currentChange by rememberUpdatedState(onChange)
    val time=runCatching{LocalTime.parse(value)}.getOrElse{LocalTime.now()}
    val use24Hours=DateFormat.is24HourFormat(context)
    val display=time.format(DateTimeFormatter.ofPattern(if(use24Hours)"HH:mm" else "hh:mm a",java.util.Locale.getDefault()))
    BankDateTimeButton(label,display,true,enabled){open=true}
    if(open&&enabled) DisposableEffect(context) {
        val picker=TimePickerDialog(context,android.R.style.Theme_Material_Dialog_Alert,
            {_,hour,minute->currentChange(LocalTime.of(hour,minute).format(DateTimeFormatter.ofPattern("HH:mm")));open=false},
            time.hour,time.minute,use24Hours)
        picker.setOnDismissListener{open=false}
        picker.show()
        onDispose{picker.setOnDismissListener(null);picker.dismiss()}
    }
}

@Composable private fun BankDateTimeButton(label:String,value:String,clock:Boolean,enabled:Boolean,onClick:()->Unit) {
    OutlinedButton(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(10.dp),contentPadding=PaddingValues(12.dp)) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            LocalizedText(label,color=Muted,fontSize=12.sp)
            LocalizedText(value,fontSize=14.sp,fontWeight=FontWeight.Medium,color=MaterialTheme.colorScheme.onSurface)
        }
        val color=if(enabled)Green else Muted
        Canvas(Modifier.size(20.dp)) {
            val stroke=Stroke(1.5.dp.toPx())
            if(clock){
                drawCircle(color,radius=size.minDimension*.42f,style=stroke)
                drawLine(color,center,Offset(center.x,size.height*.23f),strokeWidth=stroke.width)
                drawLine(color,center,Offset(size.width*.7f,size.height*.62f),strokeWidth=stroke.width)
            }else{
                drawRect(color,Offset(size.width*.12f,size.height*.2f),Size(size.width*.76f,size.height*.68f),style=stroke)
                drawLine(color,Offset(size.width*.12f,size.height*.4f),Offset(size.width*.88f,size.height*.4f),strokeWidth=stroke.width)
                for(x in listOf(.32f,.68f))drawLine(color,Offset(size.width*x,size.height*.1f),Offset(size.width*x,size.height*.3f),strokeWidth=stroke.width)
            }
        }
    }
}
