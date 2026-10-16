package com.mybetlive.ui
import android.content.Intent
import android.content.ClipData
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.mybetlive.data.*
import com.mybetlive.bank.BankReport
import java.io.File
@Composable internal fun BankSharePreview(summary:BankSummary,statistics:BankStatistics?,from:String,to:String,dismiss:()->Unit){
 val context=LocalContext.current;val image=remember(summary,from,to){BankReport.render(context,summary,from,to)}
 var error by remember{mutableStateOf<String?>(null)}
 DisposableEffect(image){onDispose{image.recycle()}}
 BankDialog("Vista previa del resumen",false,dismiss){
  Image(image.asImageBitmap(),contentDescription="Resumen del bank para compartir",modifier=Modifier.fillMaxWidth(),contentScale=ContentScale.FillWidth)
  LocalizedText("Compartirás únicamente esta imagen, sin correo ni identificadores de cuenta.")
  error?.let{LocalizedText(it,color=Red)}
  Button(onClick={runCatching{
   val folder=File(context.cacheDir,"bank-reports").apply{mkdirs()};val file=File(folder,"mybetlive-resumen.png")
   file.outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)}
   val uri=FileProvider.getUriForFile(context,context.packageName+".bankfiles",file)
   val intent=Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
   intent.clipData=ClipData.newRawUri("Resumen MyBetLive",uri);context.startActivity(Intent.createChooser(intent,"Compartir resumen"))
  }.onFailure{error="No se pudo preparar la imagen para compartir."}},modifier=Modifier.fillMaxWidth()){LocalizedText("Compartir imagen")}
 }
}

