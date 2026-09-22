package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanDark
import com.example.ui.theme.StarkGold
import com.example.ui.theme.WarningRed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorCore(
    modifier: Modifier = Modifier,
    size: Dp = 230.dp,
    powerPct: Int = 100,
    isSpeaking: Boolean = false,
    isListening: Boolean = false,
    isProcessing: Boolean = false,
    isOverclocked: Boolean = false,
    onCoreClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorTransitions")

    // Slow clockwise rotation for outer gyro ring
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 6000 else 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRotation"
    )

    // Counter-clockwise rotation for middle segmented ring
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 4000 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRotation"
    )

    // Pulsing core energy scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = if (isSpeaking || isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isSpeaking) 450 else if (isListening) 650 else 1600,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val coreGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = if (isSpeaking || isListening) 0.95f else 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isSpeaking) 400 else 1400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coreGlowAlpha"
    )

    val primaryThemeColor = when {
        isOverclocked -> WarningRed
        isListening -> StarkGold
        else -> ArcCyan
    }

    val secondaryThemeColor = when {
        isOverclocked -> StarkGold
        isListening -> ArcCyan
        else -> ArcBlue
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("arc_reactor_core")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onCoreClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2f - 12.dp.toPx()

            // 1. Outer Atmospheric Ambient Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryThemeColor.copy(alpha = 0.22f * coreGlowAlpha),
                        secondaryThemeColor.copy(alpha = 0.08f * coreGlowAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.2f
                ),
                radius = baseRadius * 1.15f,
                center = center
            )

            // 2. Outer Static HUD Thin Perimeter Ring
            drawCircle(
                color = primaryThemeColor.copy(alpha = 0.35f),
                radius = baseRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 3. Rotating Outer Ring (Clockwise) with Tick Segments
            rotate(outerRotation, pivot = center) {
                // Dashed / segmented outer ring
                val segmentCount = 12
                for (i in 0 until segmentCount) {
                    val angleDeg = (i * 360f / segmentCount)
                    val angleRad = (angleDeg * PI / 180.0)
                    val r1 = baseRadius - 2.dp.toPx()
                    val r2 = baseRadius - 10.dp.toPx()
                    val start = Offset(
                        center.x + (r1 * cos(angleRad)).toFloat(),
                        center.y + (r1 * sin(angleRad)).toFloat()
                    )
                    val end = Offset(
                        center.x + (r2 * cos(angleRad)).toFloat(),
                        center.y + (r2 * sin(angleRad)).toFloat()
                    )
                    drawLine(
                        color = if (i % 3 == 0) primaryThemeColor else primaryThemeColor.copy(alpha = 0.5f),
                        start = start,
                        end = end,
                        strokeWidth = if (i % 3 == 0) 2.5.dp.toPx() else 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Arc segments
                drawArc(
                    color = primaryThemeColor.copy(alpha = 0.6f),
                    startAngle = 15f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius + 8.dp.toPx(), center.y - baseRadius + 8.dp.toPx()),
                    size = Size((baseRadius - 8.dp.toPx()) * 2, (baseRadius - 8.dp.toPx()) * 2),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawArc(
                    color = primaryThemeColor.copy(alpha = 0.6f),
                    startAngle = 195f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius + 8.dp.toPx(), center.y - baseRadius + 8.dp.toPx()),
                    size = Size((baseRadius - 8.dp.toPx()) * 2, (baseRadius - 8.dp.toPx()) * 2),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 4. Middle Main Power Ring
            val middleRadius = baseRadius * 0.76f
            drawCircle(
                color = ArcCyanDark.copy(alpha = 0.4f),
                radius = middleRadius,
                center = center,
                style = Stroke(width = 6.dp.toPx())
            )

            // Dynamic Power Level Arc (0 to 360 deg based on battery %)
            val sweepPower = (powerPct.coerceIn(0, 100) / 100f) * 360f
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(primaryThemeColor, secondaryThemeColor, primaryThemeColor)
                ),
                startAngle = -90f,
                sweepAngle = sweepPower,
                useCenter = false,
                topLeft = Offset(center.x - middleRadius, center.y - middleRadius),
                size = Size(middleRadius * 2, middleRadius * 2),
                style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 5. Rotating Inner Coil Ring (Counter-Clockwise)
            rotate(innerRotation, pivot = center) {
                val coilRadius = middleRadius * 0.82f
                val coilCount = 10
                for (i in 0 until coilCount) {
                    val angleDeg = (i * 360f / coilCount)
                    val angleRad = (angleDeg * PI / 180.0)
                    val cx = center.x + (coilRadius * cos(angleRad)).toFloat()
                    val cy = center.y + (coilRadius * sin(angleRad)).toFloat()

                    drawCircle(
                        color = primaryThemeColor.copy(alpha = 0.85f),
                        radius = 2.8.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }

                // Inner triangular reactor housing
                val triRadius = coilRadius * 0.72f
                val path = Path().apply {
                    val p1 = Offset(
                        center.x + (triRadius * cos(-PI / 2)).toFloat(),
                        center.y + (triRadius * sin(-PI / 2)).toFloat()
                    )
                    val p2 = Offset(
                        center.x + (triRadius * cos(-PI / 2 + 2 * PI / 3)).toFloat(),
                        center.y + (triRadius * sin(-PI / 2 + 2 * PI / 3)).toFloat()
                    )
                    val p3 = Offset(
                        center.x + (triRadius * cos(-PI / 2 + 4 * PI / 3)).toFloat(),
                        center.y + (triRadius * sin(-PI / 2 + 4 * PI / 3)).toFloat()
                    )
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                    close()
                }
                drawPath(
                    path = path,
                    color = primaryThemeColor.copy(alpha = 0.7f),
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }

            // 6. Central Glowing Palladium Core with Pulse Animation
            val coreRadius = (baseRadius * 0.36f) * pulseScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryThemeColor.copy(alpha = coreGlowAlpha),
                        secondaryThemeColor.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius * 1.5f
                ),
                radius = coreRadius,
                center = center
            )

            // Inner crisp center light
            drawCircle(
                color = Color.White,
                radius = coreRadius * 0.38f,
                center = center
            )
        }

        // Core Center Telemetry Label (Battery % or Status)
        Box(
            modifier = Modifier.padding(top = (size * 0.42f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isSpeaking) "VOCALIZING" else if (isListening) "LISTENING" else if (isProcessing) "PROCESSING" else "$powerPct% PWR",
                color = primaryThemeColor,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}
