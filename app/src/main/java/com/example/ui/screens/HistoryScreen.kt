package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CarrierUsageSummary
import com.example.data.local.UsageSessionEntity
import com.example.model.CarrierType
import com.example.ui.VaultUiState
import com.example.ui.components.MetricTelemetryCard
import com.example.ui.theme.AfricellPurple
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.UnitelOrange
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultCoralRed
import com.example.ui.theme.VaultElectricBlue
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultWhite
import com.example.util.FormatUtils

@Composable
fun HistoryScreen(
    uiState: VaultUiState,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmClear by remember { mutableStateOf(false) }

    val totalBytes = remember(uiState.usageSessions) {
        uiState.usageSessions.sumOf { it.rxBytes + it.txBytes }
    }
    val totalDuration = remember(uiState.usageSessions) {
        uiState.usageSessions.sumOf { it.durationSeconds }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Histórico de Utilização",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Registo detalhado de sessões VPN e tráfego por operadora.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (uiState.usageSessions.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showConfirmClear = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Limpar histórico",
                                tint = VaultCoralRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Summary KPIs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTelemetryCard(
                        title = "Total na VPN",
                        value = FormatUtils.formatBytes(totalBytes),
                        subtitle = "${uiState.usageSessions.size} sessões",
                        accentColor = VaultElectricBlue,
                        icon = Icons.Filled.Storage,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTelemetryCard(
                        title = "Tempo Protegido",
                        value = FormatUtils.formatDuration(totalDuration),
                        subtitle = "Acumulado",
                        accentColor = VaultEmerald,
                        icon = Icons.Filled.Schedule,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Carrier Breakdown Chart
            if (uiState.carrierSummaries.isNotEmpty()) {
                item {
                    CarrierConsumptionChartCard(summaries = uiState.carrierSummaries)
                }
            }

            item {
                Text(
                    text = "SESSÕES REGISTADAS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.usageSessions.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Sem sessões registadas",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Quando activar a VPN no Painel Principal, os dados consumidos por Unitel, Africell ou Wi-Fi aparecerão aqui.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.usageSessions, key = { it.id }) { session ->
                    DetailedSessionCard(session = session)
                }
            }
        }
    }

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = { Text("Limpar histórico?") },
            text = { Text("Todas as sessões de utilização guardadas localmente serão removidas. Os perfis de servidor não serão afectados.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearHistory()
                        showConfirmClear = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaultCoralRed,
                        contentColor = VaultWhite
                    )
                ) {
                    Text("Limpar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun CarrierConsumptionChartCard(summaries: List<CarrierUsageSummary>) {
    val maxBytes = remember(summaries) {
        summaries.maxOfOrNull { it.totalRxBytes + it.totalTxBytes }?.coerceAtLeast(1L) ?: 1L
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription = "Consumo por operadora",
                    tint = VaultElectricBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Consumo por Operadora / Rede",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            summaries.forEach { item ->
                val bytes = item.totalRxBytes + item.totalTxBytes
                val ratio = (bytes.toFloat() / maxBytes.toFloat()).coerceIn(0.04f, 1f)
                val barColor = when (item.carrierType) {
                    CarrierType.UNITEL.name -> UnitelOrange
                    CarrierType.AFRICELL.name -> AfricellPurple
                    CarrierType.WIFI_ONLY.name -> VaultElectricBlue
                    else -> VaultEmerald
                }
                val label = runCatching { CarrierType.valueOf(item.carrierType).displayName }
                    .getOrDefault(item.carrierType)

                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$label (${item.sessionCount})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = FormatUtils.formatBytes(bytes),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = barColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    ) {
                        drawRoundRect(
                            color = trackColor,
                            size = size,
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                        drawRoundRect(
                            color = barColor,
                            size = Size(width = size.width * ratio, height = size.height),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailedSessionCard(session: UsageSessionEntity) {
    val statusColor = when (session.status) {
        "ACTIVA" -> VaultEmerald
        "CONCLUÍDA" -> VaultElectricBlue
        else -> VaultAmber
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.serverName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = session.status,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${session.serverHost} · IP ${session.vpnIp} · ${session.carrierType} (${session.transportType})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "↓ ${FormatUtils.formatBytes(session.rxBytes)}  ↑ ${FormatUtils.formatBytes(session.txBytes)}",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${FormatUtils.formatDuration(session.durationSeconds)} · ${FormatUtils.formatDateTime(session.startedAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
