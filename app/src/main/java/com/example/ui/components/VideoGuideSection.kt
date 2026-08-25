package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.config.VideoGuideConfig
import com.example.data.AdManager
import com.example.model.UserEntitlement
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Reusable 16:9 Video Guide Component with:
 * - 16:9 aspect ratio
 * - Autoplay = OFF (Initial idle thumbnail preview with Play button)
 * - User tap starts playback
 * - Loop = OFF, Repeat = OFF (stops at end)
 * - Full lifecycle cleanup: stops and destroys player when leaving the screen
 * - Restores to initial idle preview on re-entry
 * - Graceful fallback on network/video error
 */
@Composable
fun VideoGuideCard(
    videoUrl: String,
    modifier: Modifier = Modifier,
    toolTitle: String = "Tool"
) {
    val context = LocalContext.current
    val videoId = remember(videoUrl) { VideoGuideConfig.extractYouTubeVideoId(videoUrl) }
    var isPlaying by remember(videoUrl) { mutableStateOf(false) }
    var hasError by remember(videoUrl) { mutableStateOf(videoId == null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Guide label badge + Title + External YouTube button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.video_guide_label),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.video_guide_title),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (videoId != null) {
                    OutlinedButton(
                        onClick = { openYouTubeExternally(context, videoUrl) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open video in browser or YouTube",
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "YouTube",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 16:9 Aspect Ratio Player Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                if (hasError || videoId == null) {
                    // Graceful fallback view
                    VideoFallbackView(
                        videoUrl = videoUrl,
                        onOpenExternal = { openYouTubeExternally(context, videoUrl) }
                    )
                } else if (!isPlaying) {
                    // 1. Initial State: 16:9 Thumbnail Preview + Explicit Tap-to-play button
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { isPlaying = true },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                            contentDescription = "Video Guide Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Translucent contrast overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f))
                        )

                        // Central Play Button
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 6.dp,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Guide Video",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Instructional badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.72f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Tap to watch tutorial",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    // 2. Active Playing State: Web Player (Autoplay initiated by user tap, No loop)
                    YouTubePlayerWebView(
                        videoId = videoId,
                        onError = { hasError = true }
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubePlayerWebView(
    videoId: String,
    onError: () -> Unit
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPlayerLoading by remember { mutableStateOf(true) }

    // Immediate cleanup on dispose/exit
    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef?.apply {
                    loadUrl("about:blank")
                    onPause()
                    pauseTimers()
                    destroy()
                }
            } catch (_: Exception) {}
            webViewRef = null
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                try {
                    WebView(ctx).apply {
                        webViewRef = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            loadWithOverviewMode = true
                            useWideViewPort = true
                        }
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isPlayerLoading = false
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    isPlayerLoading = false
                                    onError()
                                }
                            }
                        }

                        val html = getYouTubeEmbedHtml(videoId)
                        loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                    }
                } catch (_: Throwable) {
                    onError()
                    android.view.View(ctx)
                }
            },
            update = {
                webViewRef = it as? WebView
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isPlayerLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
                strokeWidth = 2.5.dp
            )
        }
    }
}

@Composable
private fun VideoFallbackView(
    videoUrl: String,
    onOpenExternal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(30.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.video_guide_unavailable),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFF8FAFC),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.video_guide_offline_hint),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onOpenExternal,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayCircleOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.btn_watch_youtube),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Clean helper to open video URL externally in YouTube app or web browser.
 */
private fun openYouTubeExternally(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

/**
 * Standard responsive HTML wrapper for YouTube iframe player.
 * Autoplay enabled once user taps thumbnail. Loop disabled.
 */
private fun getYouTubeEmbedHtml(videoId: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; background-color: #0F172A; }
                html, body { width: 100%; height: 100%; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                iframe { width: 100%; height: 100%; border: 0; }
            </style>
        </head>
        <body>
            <iframe 
                src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&rel=0&playsinline=1&modestbranding=1&loop=0" 
                frameborder="0" 
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                allowfullscreen>
            </iframe>
        </body>
        </html>
    """.trimIndent()
}

/**
 * Large, responsive Google AdMob-supported Ad Card (Medium Rectangle 300x250 format).
 * - Legitimate AdMob format (AdSize.MEDIUM_RECTANGLE)
 * - Zero creative distortion
 * - Graceful failure handling: collapses completely if load fails or for Premium users
 * - Full lifecycle cleanup
 */
@Composable
fun AdMobLargeCard(
    entitlement: UserEntitlement,
    modifier: Modifier = Modifier
) {
    if (entitlement == UserEntitlement.PREMIUM) return

    var isAdLoaded by remember { mutableStateOf(false) }
    var hasAdFailed by remember { mutableStateOf(false) }
    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                adViewRef?.destroy()
            } catch (_: Exception) {}
            adViewRef = null
        }
    }

    if (hasAdFailed) {
        // Collapse gracefully without blocking user workflow
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Google AdMob Attribution Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "SPONSORED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "Advertisement",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // AdMob 300x250 Medium Rectangle Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.size(width = 300.dp, height = 250.dp),
                    factory = { ctx ->
                        try {
                            AdView(ctx).apply {
                                adViewRef = this
                                setAdSize(AdSize.MEDIUM_RECTANGLE)
                                adUnitId = AdManager.MEDIUM_RECTANGLE_TEST_ID
                                adListener = object : AdListener() {
                                    override fun onAdLoaded() {
                                        super.onAdLoaded()
                                        isAdLoaded = true
                                        hasAdFailed = false
                                    }

                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        super.onAdFailedToLoad(error)
                                        isAdLoaded = false
                                        hasAdFailed = true
                                    }
                                }
                                loadAd(AdRequest.Builder().build())
                            }
                        } catch (_: Throwable) {
                            hasAdFailed = true
                            android.view.View(ctx)
                        }
                    },
                    update = {
                        adViewRef = it as? AdView
                    }
                )
            }
        }
    }
}

/**
 * Bottom-positioned section for PDF Tool pages:
 * UX Hierarchy:
 * USER TOOL CONTROLS (Top)
 *      ↓
 * 28dp Spacing
 *      ↓
 * 16:9 Video Guide Component (Tap-to-play, no loop, stops on exit)
 *      ↓
 * 28dp Spacing
 *      ↓
 * Large AdMob-supported Responsive Ad (Collapses on failure/premium)
 *      ↓
 * 28dp Spacing before page bottom
 */
@Composable
fun ToolGuideAndAdSection(
    toolKey: String,
    entitlement: UserEntitlement,
    guideVideosEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 1. Spacing after user controls
        Spacer(modifier = Modifier.height(28.dp))

        // 2. 16:9 Video Guide Component (Rendered strictly when guideVideosEnabled is true)
        if (guideVideosEnabled) {
            VideoGuideCard(
                videoUrl = VideoGuideConfig.getVideoUrlForTool(toolKey),
                toolTitle = toolKey.replace("_", " ").replaceFirstChar { it.uppercase() }
            )

            // 3. Spacing between Video and Large Ad
            Spacer(modifier = Modifier.height(28.dp))
        }

        // 4. Large AdMob-supported Responsive Ad Card
        AdMobLargeCard(
            entitlement = entitlement,
            modifier = Modifier.fillMaxWidth()
        )

        // 5. Bottom spacing
        Spacer(modifier = Modifier.height(28.dp))
    }
}
