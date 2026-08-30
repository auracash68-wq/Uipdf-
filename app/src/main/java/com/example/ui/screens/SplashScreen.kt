package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

/**
 * High-end, professional 2-second Splash Screen for Sweet PDF.
 *
 * Presents a refined 3D-inspired "PDF" visual centerpiece, responsive branding,
 * lightweight subtle loading animation, and company presentation credit.
 */
@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 2-second display timer (2000 milliseconds)
    LaunchedEffect(Unit) {
        delay(2000L)
        onTimeout()
    }

    // Smooth entrance animation
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "splash_alpha"
    )

    val contentScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.94f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )

    // Subtle, lightweight loading pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("splash_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // Main Central Branding Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .alpha(contentAlpha)
                    .scale(contentScale),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 3D-inspired Graphical "PDF" Emblem
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.widthIn(max = 260.dp)
                ) {
                    // Subtle outer soft glow/shadow backing
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .offset(y = 8.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.22f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // 3D Layered Card Container
                    Box(
                        modifier = Modifier
                            .size(136.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(32.dp),
                                spotColor = primaryColor.copy(alpha = 0.35f),
                                ambientColor = primaryColor.copy(alpha = 0.15f)
                            )
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        surfaceColor,
                                        surfaceColor.copy(alpha = 0.95f),
                                        primaryContainer.copy(alpha = 0.25f)
                                    )
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.45f),
                                        primaryColor.copy(alpha = 0.10f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(32.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Multi-layer 3D Typography for "PDF"
                        Box(contentAlignment = Alignment.Center) {
                            // Deep shadow layer for subtle dimensional depth
                            Text(
                                text = stringResource(R.string.splash_brand_pdf),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 44.sp,
                                    letterSpacing = 2.sp
                                ),
                                color = primaryColor.copy(alpha = 0.25f),
                                modifier = Modifier.offset(x = 1.5.dp, y = 2.5.dp)
                            )

                            // Foreground crisp 3D gradient text
                            Text(
                                text = stringResource(R.string.splash_brand_pdf),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 44.sp,
                                    letterSpacing = 2.sp
                                ),
                                color = primaryColor
                            )
                        }
                    }
                }

                // Balanced spacing between 3D branding and company loading text (~1 inch / responsive dp)
                Spacer(modifier = Modifier.height(32.dp))

                // "ViridOrigin Systems · loading"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "ViridOrigin Systems",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = onSurfaceColor
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "· loading",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            letterSpacing = 0.3.sp
                        ),
                        color = onSurfaceVariantColor.copy(alpha = 0.75f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Minimal, subtle pulsing loading dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .alpha(pulseAlpha)
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Subtle, elegant micro progress bar indicator
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(onSurfaceVariantColor.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(pulseAlpha)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.2f),
                                        primaryColor,
                                        primaryColor.copy(alpha = 0.2f)
                                    )
                                )
                            )
                    )
                }
            }

            // Bottom Credit: "presented by ViridOrigin Systems"
            Text(
                text = stringResource(R.string.splash_bottom_credit),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp
                ),
                color = onSurfaceVariantColor.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .alpha(contentAlpha)
            )
        }
    }
}
