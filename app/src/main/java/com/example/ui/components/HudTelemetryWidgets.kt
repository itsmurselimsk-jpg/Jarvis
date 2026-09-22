package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JarvisMessage
import com.example.data.model.MessageSender
import com.example.data.model.StarkProtocol
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.StarkGold
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.WarningRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusBeacon(
    modifier: Modifier = Modifier,
    isOnline: Boolean = true,
    text: String = "MARK LXXXV ONLINE"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BeaconPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconAlpha"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark.copy(alpha = 0.85f))
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isOnline) ArcCyan.copy(alpha = alpha) else WarningRed)
        )
        Text(
            text = text,
            color = if (isOnline) TextCyan else WarningRed,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun AudioWaveformEqualizer(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    barCount: Int = 16,
    color: Color = ArcCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "EqualizerTransition")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqAnim"
    )

    Row(
        modifier = modifier
            .height(28.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val factor = ((i * 3 + 2) % 7) / 7.0f
            val heightFraction = if (isActive) {
                ((animProgress + factor) % 1.0f).coerceIn(0.18f, 1.0f)
            } else {
                0.12f
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(heightFraction)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                color,
                                color.copy(alpha = 0.4f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun TelemetryGaugeCard(
    modifier: Modifier = Modifier,
    title: String,
    valueText: String,
    subText: String,
    fraction: Float,
    accentColor: Color = ArcCyan,
    icon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark.copy(alpha = 0.9f))
            .border(1.dp, GlassBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(Locale.ROOT),
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = valueText,
                color = TextPrimary,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DeepSpace)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(accentColor.copy(alpha = 0.6f), accentColor)
                            )
                        )
                )
            }

            Text(
                text = subText,
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun StarkProtocolCard(
    protocol: StarkProtocol,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeBorder = if (protocol.isActive) {
        if (protocol.isEmergency) WarningRed else ArcCyan
    } else {
        GlassBorder.copy(alpha = 0.4f)
    }

    val activeBg = if (protocol.isActive) {
        if (protocol.isEmergency) WarningRed.copy(alpha = 0.15f) else ArcCyan.copy(alpha = 0.12f)
    } else {
        SurfaceDark.copy(alpha = 0.75f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(activeBg)
            .border(1.dp, activeBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("protocol_${protocol.id}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = protocol.codeName,
                    color = if (protocol.isEmergency) WarningRed else StarkGold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                if (protocol.isActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (protocol.isEmergency) WarningRed else ArcCyan)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ENGAGED",
                            color = VoidBlack,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = protocol.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = protocol.description,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
fun TerminalMessageBubble(
    message: JarvisMessage,
    isSpeakingThis: Boolean,
    onSpeakClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isJarvis = message.sender == MessageSender.JARVIS
    val isSystem = message.sender == MessageSender.SYSTEM_ALERT

    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(message.timestamp))
    }

    val bubbleBg = when {
        isSystem -> WarningRed.copy(alpha = 0.12f)
        isJarvis -> SurfaceElevated.copy(alpha = 0.85f)
        else -> DeepSpace.copy(alpha = 0.95f)
    }

    val borderColor = when {
        isSystem -> WarningRed.copy(alpha = 0.5f)
        isJarvis -> ArcCyan.copy(alpha = 0.35f)
        else -> StarkGold.copy(alpha = 0.35f)
    }

    val senderTag = when {
        isSystem -> "DEFENSE SYSTEM ALERT"
        isJarvis -> "J.A.R.V.I.S."
        else -> "SIR (TONY STARK)"
    }

    val senderColor = when {
        isSystem -> WarningRed
        isJarvis -> ArcCyan
        else -> StarkGold
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isJarvis || isSystem) Alignment.Start else Alignment.End
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(10.dp))
                .background(bubbleBg)
                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Sender tag and timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = senderTag,
                            color = senderColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (message.protocolTag != null) {
                            Text(
                                text = "[${message.protocolTag}]",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = timeFormatted,
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (isJarvis) {
                            IconButton(
                                onClick = onSpeakClick,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeakingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isSpeakingThis) "Stop Voice" else "Vocalize",
                                    tint = if (isSpeakingThis) ArcCyan else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Message Text
                Text(
                    text = message.text,
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    fontFamily = if (message.isTelemetryReport) FontFamily.Monospace else FontFamily.SansSerif
                )
            }
        }
    }
}
