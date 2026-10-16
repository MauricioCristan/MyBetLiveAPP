package com.mybetlive.bank
import android.content.Context
import android.graphics.*
import com.mybetlive.R
import com.mybetlive.data.*
import androidx.core.content.ContextCompat
import java.math.MathContext
import com.mybetlive.i18n.AppStrings
/** A report is generated from server-confirmed data. No account identifier is drawn. */
object BankReport {
 fun render(context:Context,summary:BankSummary,from:String,to:String):Bitmap {
  val bitmap=Bitmap.createBitmap(1080,1440,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap)
  val paint=Paint(Paint.ANTI_ALIAS_FLAG);val ink=Color.parseColor("#0C141B");val panel=Color.parseColor("#17232E");val muted=Color.parseColor("#9AAEBB");val brand=Color.parseColor("#B5F36D");val white=Color.parseColor("#E8F0F4")
  canvas.drawColor(ink)
  fun text(value:String,x:Float,y:Float,size:Float,color:Int=white,bold:Boolean=false){paint.color=color;paint.textSize=size;paint.typeface=if(bold)Typeface.create("sans-serif",Typeface.BOLD)else Typeface.create("sans-serif",Typeface.NORMAL);canvas.drawText(value,x,y,paint)}
  fun label(value:String,x:Float,y:Float,size:Float,color:Int=white,bold:Boolean=false)=text(AppStrings.translate(value,context.resources),x,y,size,color,bold)
  fun tone(value:String?)=if(value?.toBigDecimalOrNull()?.signum()==-1)Color.parseColor("#FF687D")else brand
  fun box(top:Float,bottom:Float){paint.color=panel;canvas.drawRoundRect(48f,top,1032f,bottom,28f,28f,paint)}
  ContextCompat.getDrawable(context,R.drawable.ic_mybet_brand)?.let{it.setBounds(48,50,126,128);it.draw(canvas)}
  text("MYBETLIVE",142f,108f,62f,brand,true)
  text(summary.bank.name.take(35),48f,195f,48f,white,true);text(summary.bank.currency+" · "+from+" / "+to,48f,244f,27f,muted)
  box(290f,545f);label("CAPITAL DISPONIBLE",82f,350f,27f,muted);val available=bankMoney(summary.bank.available,summary.bank.currency)
  text(available,82f,440f,if(available.length>22)54f else 78f,white,true)
  label("Comprometido: "+bankMoney(summary.bank.reserved,summary.bank.currency),82f,504f,29f,muted)
  box(577f,770f);label("BENEFICIO DEL PERIODO",82f,630f,25f,muted)
  val profit=summary.metrics["benefit"]?:summary.bank.profit
  text(bankMoney(profit,summary.bank.currency),82f,704f,52f,tone(profit),true)
  text("ROI",672f,630f,25f,muted);text(bankPercent(summary.metrics["roi"]),672f,704f,48f,tone(summary.metrics["roi"]),true)
  box(802f,1140f);label("Rendimiento confirmado",82f,860f,32f,white,true)
  val values=summary.chart.mapNotNull{it.value.toBigDecimalOrNull()}
  if(values.isNotEmpty()){
   val min=values.minOrNull()!!;val range=(values.maxOrNull()!!-min).takeIf{it.signum()!=0}?:java.math.BigDecimal.ONE
   val path=Path();values.forEachIndexed{i,value->val x=82f+i.toFloat()/(values.size-1).coerceAtLeast(1)*916f;val y=1090f-(value-min).divide(range,MathContext.DECIMAL64).toFloat()*180f;if(i==0)path.moveTo(x,y)else path.lineTo(x,y)}
   paint.color=brand;paint.style=Paint.Style.STROKE;paint.strokeWidth=6f;canvas.drawPath(path,paint);paint.style=Paint.Style.FILL
  }
  label("Apuestas liquidadas: "+(summary.metrics["bets"]?:"—"),48f,1210f,30f,white)
  label("Aciertos: "+bankPercent(summary.metrics["successPercent"]),48f,1260f,30f,white)
  label("Capital actual: "+bankMoney(summary.bank.capital,summary.bank.currency),48f,1310f,27f,muted)
  label("Registro personal · resultados confirmados por el usuario",48f,1390f,23f,muted)
  return bitmap
 }
}

