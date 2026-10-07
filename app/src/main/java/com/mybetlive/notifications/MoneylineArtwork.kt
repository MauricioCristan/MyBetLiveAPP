package com.mybetlive.notifications

import android.graphics.*
import com.mybetlive.data.*

internal fun moneylineArtwork(data: Map<String,String>, p: MarketPresentation, compact: Boolean): Bitmap {
    val bitmap=Bitmap.createBitmap(960,if(compact) 72 else 270,Bitmap.Config.ARGB_8888)
    val canvas=Canvas(bitmap);val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    canvas.drawColor(0xFF17232E.toInt())
    if(compact) {
        paint.color=p.tone.argb;paint.textSize=38f;paint.typeface=Typeface.DEFAULT_BOLD
        canvas.drawText(p.value,16f,49f,paint);paint.textSize=26f;canvas.drawText(p.status.take(45),235f,46f,paint)
        return bitmap
    }
    for((home,x) in listOf(true to 16f,false to 744f)) {
        val e=if(home)p.home!! else p.away!!;paint.color=e.tone.argb;paint.alpha=40
        canvas.drawRoundRect(x,8f,x+200f,254f,24f,24f,paint);paint.alpha=255;paint.style=Paint.Style.STROKE;paint.strokeWidth=3f
        canvas.drawRoundRect(x,8f,x+200f,254f,24f,24f,paint);paint.style=Paint.Style.FILL
        val size=e.size*2f;val y=68f+(132f-size)/2f
        val image=NotificationArtwork.cached(data[if(home) "homeLogoUrl" else "awayLogoUrl"])
        if(image!=null){paint.alpha=if(e.faded)148 else 255;canvas.drawBitmap(image,null,RectF(x+(200-size)/2,y,x+(200+size)/2,y+size),paint);paint.alpha=255}
        else {paint.textSize=62f;paint.textAlign=Paint.Align.CENTER;canvas.drawText((data[if(home)"homeName" else "awayName"]?:"—").take(2).uppercase(),x+100,154f,paint)}
        paint.textAlign=Paint.Align.CENTER;paint.textSize=22f;paint.typeface=Typeface.DEFAULT_BOLD
        if(e.selected)canvas.drawText("TU EQUIPO",x+100,43f,paint)
        canvas.drawText((data[if(home)"homeName" else "awayName"]?:"—").take(17),x+100,231f,paint)
    }
    paint.color=0xFFE8F0F4.toInt();paint.textSize=86f;paint.textAlign=Paint.Align.CENTER
    canvas.drawText(p.value,480f,143f,paint)
    if(data["selection"]=="DRAW"){paint.color=p.tone.argb;paint.textSize=24f;canvas.drawText("APUESTA: EMPATE",480f,204f,paint)}
    return bitmap
}
