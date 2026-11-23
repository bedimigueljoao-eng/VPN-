package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.UsageSessionEntity
import com.example.model.VpnConnectionState
import com.example.ui.VaultUiState
import com.example.ui.components.CarrierBadgeChip
import com.example.ui.components.MetricTelemetryCard
import com.example.ui.components.TechInfoCalloutCard
import com.example.ui.components.ThreePillarNetworkStrip
import com.example.ui.components.VaultBrandLogo
import com.example.ui.components.VpnHeroPowerButton
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultCoralRed
import com.example.ui.theme.VaultElectricBlue
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultNavy
import com.example.ui.theme.VaultNavyElevated
import com.example.ui.theme.VaultWhite
import com.example.util.FormatUtils

@Composable
fun DashboardScreen(
    uiState: VaultUiState,
    onToggleVpn: () -> Unit,
    onRefreshNetwork: () -> Unit,
    onNavigateToCarriers: () -> Unit,
    onNavigateToServers: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onOpenConnectivityLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connState = uiState.connectionState
    val net = uiState.networkStatus
    val telemetry = uiState.telemetry
    val selectedServer = uiState.selectedServer
    val vpnIp = (connState as? VpnConnectionState.Connected)?.vpnIp ?: net.vpnIpAddress ?: "—"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Hero Header Card with Logo, Network Badge, and Main Power Button
            item {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = VaultNavy,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, VaultElectricBlue.copy(alpha = 0.25f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_vault_network_banner),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(0.26f)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            VaultNavy.copy(alpha = 0.72f),
                                            VaultNavyElevated.copy(alpha = 0.90f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VaultBrandLogo(lightContent = true)
                                IconButton(
                                    onClick = onRefreshNetwork,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("refresh_network_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Actualizar estado da rede",
                                        tint = VaultWhite
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CarrierBadgeChip(
                                    carrier = net.detectedCarrier,
                                    transportType = net.transportType
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            VpnHeroPowerButton(
                                connectionState = connState,
                                onClick = onToggleVpn
                            )

                            if (connState is VpnConnectionState.Error) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = VaultCoralRed.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, VaultCoralRed.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.WarningAmber,
                                            contentDescription = "Erro de ligação",
                                            tint = VaultCoralRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = connState.reasonPt,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = VaultWhite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Three-pillar status (VPN active, Server available, Effective Internet)
            item {
                ThreePillarNetworkStrip(status = net)
            }

            // 3. Selected Carrier & Server quick cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickSelectorCard(
                        title = "Operadora Activa",
                        primary = net.detectedCarrier.displayName,
                        secondary = "Código ${net.mccMnc} · ${net.transportType.labelPt}",
                        icon = Icons.Filled.CellTower,
                        accent = VaultElectricBlue,
                        onClick = onNavigateToCarriers,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_carrier_card")
                    )
                    QuickSelectorCard(
                        title = "Servidor Seleccionado",
                        primary = selectedServer?.name ?: "Nenhum servidor",
                        secondary = if (selectedServer != null) {
                            "${selectedServer.host}:${selectedServer.port}"
                        } else {
                            "Toque para configurar"
                        },
                        icon = Icons.Filled.Dns,
                        accent = VaultEmerald,
                        onClick = onNavigateToServers,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_server_card")
                    )
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clip(MaterialTheme.shapes.medium)
                        .testTag("dashboard_open_lab_card")
                        .clickable(onClick = onOpenConnectivityLab),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, VaultElectricBlue.copy(alpha = 0.45f)),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(VaultElectricBlue.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Science,
                                contentDescription = "Laboratório de Conectividade",
                                tint = VaultElectricBlue
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Laboratório Unitel & Africell",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Matriz de 9 pontos, Modo Sem Saldo, Comparação VPN e PDF/CSV.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Abrir Laboratório",
                            tint = VaultElectricBlue
                        )
                    }
                }
            }

            // 4. Live Telemetry Metrics (IP da VPN, Tempo, Download, Upload, Dados Consumidos)
            item {
                Text(
                    text = "TELEMETRIA EM TEMPO REAL",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTelemetryCard(
                        title = "IP da VPN",
                        value = vpnIp,
                        subtitle = if (connState is VpnConnectionState.Connected) "Interface tun0" else "Sem túnel activo",
                        accentColor = VaultElectricBlue,
                        icon = Icons.Filled.Lan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTelemetryCard(
                        title = "Tempo de Ligação",
                        value = FormatUtils.formatDuration(telemetry.sessionDurationSeconds),
                        subtitle = if (connState is VpnConnectionState.Connected) "Sessão em curso" else "Em espera",
                        accentColor = VaultEmerald,
                        icon = Icons.Filled.Schedule,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTelemetryCard(
                        title = "Download",
                        value = FormatUtils.formatSpeed(telemetry.downloadSpeedBps),
                        subtitle = "Recebido: ${FormatUtils.formatBytes(telemetry.sessionRxBytes)}",
                        accentColor = VaultEmerald,
                        icon = Icons.Filled.ArrowDownward,
                        sparklineSamples = telemetry.downloadHistory,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTelemetryCard(
                        title = "Upload",
                        value = FormatUtils.formatSpeed(telemetry.uploadSpeedBps),
                        subtitle = "Enviado: ${FormatUtils.formatBytes(telemetry.sessionTxBytes)}",
                        accentColor = VaultElectricBlue,
                        icon = Icons.Filled.ArrowUpward,
                        sparklineSamples = telemetry.uploadHistory,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                val totalSessionBytes = telemetry.sessionRxBytes + telemetry.sessionTxBytes
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(VaultElectricBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DataUsage,
                                    contentDescription = "Dados consumidos",
                                    tint = VaultElectricBlue
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Dados Consumidos (Sessão)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FormatUtils.formatBytes(totalSessionBytes),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Pacotes TUN",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${telemetry.tunPacketsHandled}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = VaultEmerald
                            )
                        }
                    }
                }
            }

            // 5. Network Status Explanation Banner
            item {
                TechInfoCalloutCard(
                    title = "Estado da Rede · ${net.transportType.labelPt}",
                    body = net.diagnosticSummaryPt,
                    accentColor = if (net.hasEffectiveInternet) VaultEmerald else VaultAmber,
                    icon = Icons.Filled.VerifiedUser
                )
            }

            // 6. Recent History Preview
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HISTÓRICO DE UTILIZAÇÃO RECENTE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("view_all_history_button")
                    ) {
                        Text("Ver tudo")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Ver todo o histórico",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (uiState.usageSessions.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Ainda sem sessões registadas. Ligue a VPN para iniciar a monitorização.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.usageSessions.take(3), key = { it.id }) { session ->
                    RecentSessionRow(session = session)
                }
            }
        }
    }
}

@Composable
private fun QuickSelectorCard(
    title: String,
    primary: String,
    secondary: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                    tint = accent,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = primary,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = secondary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RecentSessionRow(session: UsageSessionEntity) {
    val totalBytes = session.rxBytes + session.txBytes
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.serverName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${session.carrierType} · ${session.transportType} · ${FormatUtils.formatDateTime(session.startedAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatUtils.formatBytes(totalBytes),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = VaultElectricBlue
                )
                Text(
                    text = FormatUtils.formatDuration(session.durationSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
