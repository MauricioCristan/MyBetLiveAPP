package com.mybetlive.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybetlive.data.BetProgress
import com.mybetlive.presentation.ProgressTrackRenderer

@Composable fun BetProgressTrack(progress: BetProgress, compact: Boolean = false) {
    val context = LocalContext.current
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val position by animateFloatAsState(progress.fraction, label = "bet-progress")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LocalizedText(progress.valueLabel, fontWeight = FontWeight.Bold, color = Color(progress.tone.argb), fontSize = if (compact) 26.sp else 32.sp)
        Canvas(Modifier.fillMaxWidth().height(if (compact) 56.dp else 76.dp).semantics { contentDescription = com.mybetlive.i18n.AppStrings.translate(progress.description) }) {
            ProgressTrackRenderer.draw(context, drawContext.canvas.nativeCanvas, size.width, size.height,
                progress, labelColor, position = position)
        }
    }
}
