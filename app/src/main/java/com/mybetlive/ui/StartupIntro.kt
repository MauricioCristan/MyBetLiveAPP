package com.mybetlive.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybetlive.R
import kotlinx.coroutines.flow.first

/** Uses the approved emblem paths, with each piece translated independently. */
@Composable internal fun StartupIntro(initialProgressMs: Int = 0, contentReady: Boolean = true,
    animationReady: Boolean = true, onFinished: () -> Unit) {
    val clock = remember { Animatable(initialProgressMs.toFloat()) }
    val finished by rememberUpdatedState(onFinished)
    val ready by rememberUpdatedState(contentReady)
    val canAnimate by rememberUpdatedState(animationReady)
    val white = remember { PathParser().parsePathString("M32,48H106L88,80H68L90,146L114,170L96,202L52,156Z").toPath() }
    val green = remember { PathParser().parsePathString("M122,48H208L192,112L120,202L108,162L164,100L170,80H104Z").toPath() }
    val font = remember { FontFamily(Font(R.font.barlow_condensed_bold_italic, FontWeight.Bold, FontStyle.Italic)) }
    LaunchedEffect(Unit) {
        snapshotFlow { canAnimate }.first { it }
        clock.animateTo(700f, tween(700 - initialProgressMs, easing = LinearEasing))
        // Keep the finished emblem visible if local initialization needs another frame.
        snapshotFlow { ready }.first { it }
        clock.animateTo(900f, tween(200, easing = LinearEasing))
        finished()
    }
    Box(Modifier.fillMaxSize().graphicsLayer {
        alpha = 1f - ((clock.value - 700f) / 200f).coerceIn(0f, 1f)
    }.background(Ink).pointerInput(Unit) {
        awaitPointerEventScope { while(true)awaitPointerEvent().changes.forEach { it.consume() } }
    }.clearAndSetSemantics { contentDescription = "MyBetLive" }) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.size(112.dp).align(Alignment.Center)) {
                val remaining = 1f - FastOutSlowInEasing.transform((clock.value / 500f).coerceIn(0f, 1f))
                val shift = 24.dp.toPx() * remaining
                scale(size.width / 240f, size.height / 240f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                    translate(left = -shift * 240f / size.width) { drawPath(white, Color(0xFFF4F6F8)) }
                    translate(left = shift * 240f / size.width) { drawPath(green, Green) }
                }
            }
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFFF4F6F8))) { append("MyBet") }
                withStyle(SpanStyle(color = Green)) { append("Live") }
            }, modifier = Modifier.align(Alignment.Center).offset(y = 90.dp).graphicsLayer {
                alpha = ((clock.value - 500f) / 200f).coerceIn(0f, 1f)
            }, fontFamily = font, fontSize = 28.sp, lineHeight = 28.sp,
                fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, maxLines = 1)
        }
    }
}
