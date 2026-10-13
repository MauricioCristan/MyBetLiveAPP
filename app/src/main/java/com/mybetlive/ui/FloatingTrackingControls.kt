package com.mybetlive.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybetlive.floating.FloatingTrackingController
import com.mybetlive.data.*

@Composable internal fun FloatingTrackingControls(bet:BetView,busy:Boolean) {
    if(bet.status in settledStatuses)return
    val context=LocalContext.current
    var explain by rememberSaveable(bet.id){mutableStateOf(false)}
    var message by remember(bet.id){mutableStateOf<String?>(null)}
    fun activate(){message=runCatching{FloatingTrackingController(context).activate(bet.id);"Burbuja activada. Puedes cambiar de aplicación."}.getOrElse{it.message?:"No se pudo activar la burbuja."}}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(Settings.canDrawOverlays(context))activate() else message="Habilita el permiso para mostrar la burbuja sobre otras aplicaciones."
    }
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick={if(Settings.canDrawOverlays(context))activate() else explain=true},enabled=!busy,
            modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("floating-activate")){LocalizedText("Ver en burbuja")}
        message?.let{LocalizedText(it,color=Muted,fontSize=12.sp)}
    }
    if(explain)AlertDialog(onDismissRequest={explain=false},title={LocalizedText("Lleva tu pick contigo")},
        text={LocalizedText("Consulta el avance mientras usas otras apps. Android te pedirá habilitar el permiso para mostrar sobre otras aplicaciones. Puedes cerrar la burbuja sin borrar tu seguimiento.")},
        confirmButton={TextButton(onClick={explain=false;runCatching{permission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:${context.packageName}")))}.onFailure{message="No se pudo abrir la configuración de Android."}}){LocalizedText("Habilitar burbuja")}},
        dismissButton={TextButton(onClick={explain=false}){LocalizedText("Ahora no")}})
}
