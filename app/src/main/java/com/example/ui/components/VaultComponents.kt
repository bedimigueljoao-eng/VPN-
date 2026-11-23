package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CarrierType
import com.example.model.NetworkProbeStatus
import com.example.model.TransportType
import com.example.model.VpnConnectionState
import com.example.ui.theme.AfricellPurple
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.UnitelOrange
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultCoralRed
import com.example.ui.theme.VaultElectricBlue
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultNavy
import com.example.ui.theme.VaultNavyElevated
import com.example.ui.theme.VaultWhite

@Composable
fun VaultBrandLogo(
    modifier: Modifier = Modifier,
    lightContent: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(VaultElectricBlue, VaultEmerald)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = "Logótipo Internet Vault",
                tint = VaultWhite,
                modifier = Modifier.size(21.dp)
            )
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = VaultNavy,
                modifier = Modifier.size(10.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "INTERNET VAULT",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (lightContent) VaultWhite else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "CONECTIVIDADE & VPN ANGOLA",
                style = MaterialTheme.typography.labelSmall,
                color = if (lightContent) VaultWhite.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VpnHeroPowerButton(
    connectionState: VpnConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isConnected = connectionState is VpnConnectionState.Connected
    val isBusy = connectionState is VpnConnectionState.Connecting ||
        connectionState is VpnConnectionState.Preparing

    val infiniteTransition = rememberInfiniteTransition(label = "vault_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnected || isBusy) 1.18f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isBusy) 800 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isBusy) 800 else 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val coreColor by animateColorAsState(
        targetValue = when (connectionState) {
            is VpnConnectionState.Connected -> VaultEmerald
            is VpnConnectionState.Connecting, is VpnConnectionState.Preparing -> VaultElectricBlue
            is VpnConnectionState.Error -> VaultCoralRed
            is VpnConnectionState.Disconnected -> VaultNavyElevated
        },
        animationSpec = tween(300),
        label = "core_color"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isConnected) 1.03f else 1f,
        animationSpec = spring(),
        label = "btn_scale"
    )

    val labelPt = when (connectionState) {
        is VpnConnectionState.Connected -> "CONECTADO · TOQUE PARA DESLIGAR"
        is VpnConnectionState.Connecting -> "A ESTABELECER TÚNEL..."
        is VpnConnectionState.Preparing -> "A PREPARAR INTERFACE..."
        is VpnConnectionState.Error -> "ERRO · TOQUE PARA TENTAR NOVAMENTE"
        is VpnConnectionState.Disconnected -> "DESCONECTADO · TOQUE PARA LIGAR"
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(168.dp)
        ) {
            // Outer animated pulse aura
            Box(
                modifier = Modifier
                    .size(152.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(coreColor.copy(alpha = if (isConnected || isBusy) pulseAlpha else 0.12f))
            )

            // Secondary ring
            Surface(
                modifier = Modifier.size(134.dp),
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(2.dp, coreColor.copy(alpha = 0.55f))
            ) {}

            // Primary clickable circular button
            Surface(
                modifier = Modifier
                    .size(114.dp)
                    .scale(buttonScale)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .testTag("vpn_power_button")
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    },
                shape = CircleShape,
                color = coreColor,
                shadowElevation = 12.dp,
                border = BorderStroke(2.dp, VaultWhite.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = labelPt,
                        tint = VaultWhite,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isConnected) "ACTIVO" else if (isBusy) "..." else "LIGAR",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = VaultWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(50),
            color = coreColor.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, coreColor.copy(alpha = 0.35f))
        ) {
            Text(
                text = labelPt,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (connectionState is VpnConnectionState.Disconnected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    coreColor
                },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun ThreePillarNetworkStrip(
    status: NetworkProbeStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusPillarCard(
            title = "VPN",
            value = if (status.isVpnActive) "Activa" else "Inactiva",
            icon = Icons.Filled.Shield,
            accentColor = if (status.isVpnActive) VaultEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
            isPositive = status.isVpnActive,
            modifier = Modifier.weight(1f)
        )
        StatusPillarCard(
            title = "Servidor",
            value = when {
                status.isServerReachable && status.serverLatencyMs != null -> "${status.serverLatencyMs} ms"
                status.isServerReachable -> "Online"
                else -> "Offline"
            },
            icon = Icons.Filled.Dns,
            accentColor = if (status.isServerReachable) VaultElectricBlue else VaultAmber,
            isPositive = status.isServerReachable,
            modifier = Modifier.weight(1f)
        )
        StatusPillarCard(
            title = "Internet",
            value = when {
                status.hasEffectiveInternet -> "Validada"
                status.transportType == TransportType.NONE -> "Sem Rede"
                else -> "Sem Acesso"
            },
            icon = if (status.hasEffectiveInternet) Icons.Filled.Public else Icons.Filled.CloudOff,
            accentColor = if (status.hasEffectiveInternet) VaultEmerald else VaultCoralRed,
            isPositive = status.hasEffectiveInternet,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusPillarCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isPositive) accentColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline
        ),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isPositive) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CarrierBadgeChip(
    carrier: CarrierType,
    transportType: TransportType,
    modifier: Modifier = Modifier
) {
    val badgeColor = when (carrier) {
        CarrierType.UNITEL -> UnitelOrange
        CarrierType.AFRICELL -> AfricellPurple
        CarrierType.WIFI_ONLY -> VaultElectricBlue
        else -> VaultEmerald
    }
    val icon = if (transportType == TransportType.WIFI) Icons.Filled.Wifi else Icons.Filled.CellTower

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = badgeColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = carrier.displayName,
                tint = badgeColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "${carrier.shortName} · ${transportType.labelPt}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = badgeColor
            )
        }
    }
}

@Composable
fun SpeedSparklineCanvas(
    samples: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (samples.size < 2) return@Canvas
        val maxVal = (samples.maxOrNull() ?: 1f).coerceAtLeast(10f)
        val stepX = size.width / (samples.size - 1).coerceAtLeast(1)
        val path = Path()
        val fillPath = Path()

        samples.forEachIndexed { index, value ->
            val x = index * stepX
            val normalized = (value / maxVal).coerceIn(0f, 1f)
            val y = size.height - (normalized * (size.height - 8f)) - 4f
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)
            )
        )
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
        val lastY = size.height - ((samples.last() / maxVal).coerceIn(0f, 1f) * (size.height - 8f)) - 4f
        drawCircle(
            color = lineColor,
            radius = 5f,
            center = Offset(size.width, lastY)
        )
    }
}

@Composable
fun MetricTelemetryCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    sparklineSamples: List<Float>? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (sparklineSamples != null) {
                Spacer(modifier = Modifier.height(8.dp))
                SpeedSparklineCanvas(
                    samples = sparklineSamples,
                    lineColor = accentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )
            }
        }
    }
}

@Composable
fun TechInfoCalloutCard(
    title: String,
    body: String,
    accentColor: Color = VaultElectricBlue,
    icon: ImageVector = Icons.Filled.Bolt,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = accentColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
