package com.mybetlive.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.mybetlive.R
@Composable internal fun LeagueLogo(url:String?) {
    val context=LocalContext.current
    val fallback=painterResource(R.drawable.ic_league_placeholder)
    val request=remember(url,context){ImageRequest.Builder(context).data(url?.takeIf{it.startsWith("https://",ignoreCase=true)}).build()}
    AsyncImage(model=request,contentDescription=null,placeholder=fallback,error=fallback,fallback=fallback,contentScale=ContentScale.Fit,
        modifier=Modifier.size(26.dp).background(Color(0xFFF4F6F8),RoundedCornerShape(5.dp)).padding(2.dp))
}
