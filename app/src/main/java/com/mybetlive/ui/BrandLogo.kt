package com.mybetlive.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybetlive.R

private val BrandFont = FontFamily(Font(R.font.barlow_condensed_bold_italic, FontWeight.Bold, FontStyle.Italic))

/** Approved Penpot identity: Impulso emblem and Barlow Condensed Bold Italic. */
@Composable internal fun BrandLogo(modifier: Modifier = Modifier, height: Dp = 32.dp) {
    Row(modifier.clearAndSetSemantics { contentDescription = "MyBetLive" },
        horizontalArrangement = Arrangement.spacedBy((height.value * .18f).dp),
        verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_mybet_brand), contentDescription = null, modifier = Modifier.size(height))
        val size = height.value * .82f
        Text(buildAnnotatedString {
            withStyle(SpanStyle(color = Color(0xFFF4F6F8))) { append("MYBET") }
            withStyle(SpanStyle(color = Green)) { append("LIVE") }
        }, fontFamily = BrandFont, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic,
            fontSize = size.sp, letterSpacing = (size / 142f).sp, maxLines = 1)
    }
}
