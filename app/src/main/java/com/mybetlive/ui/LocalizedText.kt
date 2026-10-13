package com.mybetlive.ui
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.TextUnit
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration

@Composable internal fun LocalizedText(text:String,modifier:Modifier=Modifier,color:Color=Color.Unspecified,
    fontSize:TextUnit=TextUnit.Unspecified,fontStyle:FontStyle?=null,fontWeight:FontWeight?=null,fontFamily:FontFamily?=null,
    letterSpacing:TextUnit=TextUnit.Unspecified,textDecoration:TextDecoration?=null,textAlign:TextAlign?=null,
    lineHeight:TextUnit=TextUnit.Unspecified,overflow:TextOverflow=TextOverflow.Clip,softWrap:Boolean=true,maxLines:Int=Int.MAX_VALUE,
    minLines:Int=1,onTextLayout:((TextLayoutResult)->Unit)?=null,style:TextStyle=LocalTextStyle.current){
    LocalConfiguration.current
    androidx.compose.material3.Text(text=com.mybetlive.i18n.AppStrings.translate(text,LocalContext.current.resources),modifier=modifier,color=color,fontSize=fontSize,fontStyle=fontStyle,fontWeight=fontWeight,fontFamily=fontFamily,
        letterSpacing=letterSpacing,textDecoration=textDecoration,textAlign=textAlign,lineHeight=lineHeight,overflow=overflow,softWrap=softWrap,maxLines=maxLines,minLines=minLines,onTextLayout=onTextLayout,style=style)
}
