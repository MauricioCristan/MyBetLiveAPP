package com.mybetlive.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Keep both teams' scores visible in the space between their logos. */
@Composable internal fun FittingScore(text: String, maxFontSize: TextUnit, modifier: Modifier = Modifier,
    minFontSize: TextUnit = 12.sp,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null) {
    val measurer = rememberTextMeasurer()
    val style = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold)
    val density = LocalDensity.current
    BoxWithConstraints(modifier,contentAlignment=Alignment.Center) {
        val width = with(density) { maxWidth.toPx() }
        var size = maxFontSize.value
        while (size > minFontSize.value && measurer.measure(text, style.copy(fontSize = size.sp),
                softWrap = false, maxLines = 1).size.width > width) size -= 1f
        LocalizedText(text, fontSize = size.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, softWrap = false, onTextLayout = onTextLayout)
    }
}
