package com.mybetlive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import com.mybetlive.BuildConfig
import com.mybetlive.R
import com.mybetlive.data.Team

@Composable internal fun TeamLogo(team: Team, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val fallback = painterResource(R.drawable.ic_team_placeholder)
    val shape = RoundedCornerShape(8.dp)
    val context = LocalContext.current
    val request = remember(team.logoUrl, context) {
        ImageRequest.Builder(context).data(team.logoUrl?.takeIf { it.startsWith("https://", ignoreCase = true) })
            .httpHeaders(NetworkHeaders.Builder().set("User-Agent", "MyBetLive/1.0 (Android)").build()).build()
    }
    // The adjacent team name supplies the accessible label. Fit preserves the entire crest.
    AsyncImage(
        model = request,
        contentDescription = null,
        placeholder = fallback, error = fallback, fallback = fallback,
        onError = { if (BuildConfig.DEBUG) android.util.Log.d("TeamArtwork", "Logo: ${it.result.throwable.javaClass.simpleName}: ${it.result.throwable.message?.take(160)}") },
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size).testTag("team-logo-${team.id}").clip(shape)
            .background(Color(0xFFF1F5F6)).border(1.dp, Color(0xFFDCE4E8), shape).padding(4.dp)
    )
}
