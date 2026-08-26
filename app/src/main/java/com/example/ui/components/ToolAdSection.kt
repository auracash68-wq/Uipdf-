package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.AdManager
import com.example.model.UserEntitlement
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Large AdMob Medium Rectangle (300x250) banner card for tool pages.
 * Preloads and displays cleanly according to Google AdMob policies.
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
 * Bottom-positioned Ad Section for PDF Tool pages.
 */
@Composable
fun ToolGuideAndAdSection(
    toolKey: String,
    entitlement: UserEntitlement,
    guideVideosEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    ToolAdSection(
        entitlement = entitlement,
        modifier = modifier
    )
}

@Composable
fun ToolAdSection(
    entitlement: UserEntitlement,
    modifier: Modifier = Modifier
) {
    if (entitlement == UserEntitlement.PREMIUM) return

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        AdMobLargeCard(
            entitlement = entitlement,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(28.dp))
    }
}
