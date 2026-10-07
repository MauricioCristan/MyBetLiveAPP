package com.mybetlive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mybetlive.R

@Composable
fun PlayerAvatar(imageUrl: String?, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val fallback = painterResource(R.drawable.ic_player_avatar)
    // The player's name beside the portrait supplies the accessible label.
    AsyncImage(
        model = imageUrl?.takeIf { it.startsWith("https://", ignoreCase = true) },
        contentDescription = null,
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(CircleShape)
            .background(Color(0xFF263844)).border(1.dp, Color(0xFF40515E), CircleShape)
    )
}
