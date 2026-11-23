package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CarrierSelectionMode
import com.example.model.CarrierType
import com.example.model.SponsoredPartnershipInfo
import com.example.model.TransportType
import com.example.ui.VaultUiState
import com.example.ui.components.TechInfoCalloutCard
import com.example.ui.theme.AfricellPurple
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.UnitelOrange
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultElectricBlue
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultNavy
import com.example.ui.theme.VaultWhite
import com.example.util.FormatUtils

@Composable
fun CarriersScreen(
    uiState: VaultUiState,
    sponsoredPartnerships: List<SponsoredPartnershipInfo>,
    onSelectCarrierMode: (CarrierSelectionMode) -> Unit,
    onPrepareVaultOverWifi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val net = uiState.networkStatus
    val selectedMode = uiState.preferences.carrierSelectionMode
    val unitelUsage = uiState.carrierSummaries
        .firstOrNull { it.carrierType == CarrierType.UNITEL.name }
    val africellUsage = uiState.carrierSummaries
        .firstOrNull { it.carrierType == CarrierType.AFRICELL.name }

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
            // 1. Live Carrier & Network Identification Header
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = VaultNavy,
                    border = BorderStroke(1.dp, VaultElectricBlue.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (net.transportType == TransportType.WIFI) {
                                    Icons.Filled.Wifi
                                } else {
                                    Icons.Filled.SignalCellularAlt
                                },
                                contentDescription = "Identificação da rede",
                                tint = VaultEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "MÓDULO DE OPERADORAS · ANGOLA",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VaultElectricBlue
                                )
                                Text(
                                    text = net.rawOperatorName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = VaultWhite
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NetworkSpecChip(
                                label = "Transporte",
                                value = net.transportType.labelPt,
                                modifier = Modifier.weight(1f)
                            )
                            NetworkSpecChip(
                                label = "MCC / MNC",
                                value = net.mccMnc,
                                modifier = Modifier.weight(1f)
                            )
                            NetworkSpecChip(
                                label = "IP Local",
                                value = net.localIpAddress,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 2. Carrier Selection (Auto, Unitel, Africell)
            item {
                Text(
                    text = "SELECÇÃO DE OPERADORA",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                CarrierOptionCard(
                    title = "Detecção Automática (SIM / Rede Activa)",
                    subtitle = "Identifica automaticamente Unitel (631-02), Africell (631-05) ou Wi-Fi.",
                    mccMnc = "AUTO",
                    usageBytes = null,
                    accentColor = VaultElectricBlue,
                    selected = selectedMode == CarrierSelectionMode.AUTO,
                    onClick = { onSelectCarrierMode(CarrierSelectionMode.AUTO) },
                    testTag = "carrier_auto_card"
                )
            }

            item {
                val unitelBytes = (unitelUsage?.totalRxBytes ?: 0L) + (unitelUsage?.totalTxBytes ?: 0L)
                CarrierOptionCard(
                    title = "Unitel Angola",
                    subtitle = "APN: ${CarrierType.UNITEL.apnReference} · Prioridade 4G/5G nacional · ${unitelUsage?.sessionCount ?: 0} sessões.",
                    mccMnc = CarrierType.UNITEL.mccMnc,
                    usageBytes = unitelBytes,
                    accentColor = UnitelOrange,
                    selected = selectedMode == CarrierSelectionMode.UNITEL,
                    onClick = { onSelectCarrierMode(CarrierSelectionMode.UNITEL) },
                    testTag = "carrier_unitel_card"
                )
            }

            item {
                val africellBytes = (africellUsage?.totalRxBytes ?: 0L) + (africellUsage?.totalTxBytes ?: 0L)
                CarrierOptionCard(
                    title = "Africell Angola",
                    subtitle = "APN: ${CarrierType.AFRICELL.apnReference} · Rede LTE de alta capacidade · ${africellUsage?.sessionCount ?: 0} sessões.",
                    mccMnc = CarrierType.AFRICELL.mccMnc,
                    usageBytes = africellBytes,
                    accentColor = AfricellPurple,
                    selected = selectedMode == CarrierSelectionMode.AFRICELL,
                    onClick = { onSelectCarrierMode(CarrierSelectionMode.AFRICELL) },
                    testTag = "carrier_africell_card"
                )
            }

            // 3. Wi-Fi Pre-Staging -> Mobile Data Workflow
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, VaultElectricBlue.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = "Preparação via Wi-Fi",
                                tint = VaultElectricBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Preparação via Wi-Fi → Uso em Dados Móveis",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Valide e guarde todas as configurações de servidores, DNS e parâmetros do túnel enquanto está ligado ao Wi-Fi, deixando o Cofre Offline pronto para ligação rápida quando passar para Unitel ou Africell.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Última sincronização do Cofre: ${FormatUtils.formatDateTime(uiState.preferences.lastWifiPrepTimestamp)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = VaultEmerald
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onPrepareVaultOverWifi,
                            enabled = !uiState.isPreparingVault,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VaultElectricBlue,
                                contentColor = VaultWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("prepare_vault_wifi_button")
                        ) {
                            if (uiState.isPreparingVault) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = VaultWhite,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("A validar servidores no Cofre...")
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Preparar e Validar Cofre Agora")
                            }
                        }
                    }
                }
            }

            // 4. Transparent Technical Note (No false balance / no exploit)
            item {
                TechInfoCalloutCard(
                    title = "Nota Técnica sobre Transporte & Saldo Móvel",
                    body = "Uma ligação Wi-Fi anterior guarda configurações e valida servidores localmente, mas não se converte em saldo de dados móveis. Para o túnel VPN alcançar o servidor próprio na rua, é sempre necessário um transporte activo (Unitel, Africell ou Wi-Fi).",
                    accentColor = VaultAmber,
                    icon = Icons.Filled.Info
                )
            }

            // 5. Sponsored / Zero-Rating Partnerships Section
            item {
                Text(
                    text = "CONECTIVIDADE PATROCINADA & ACORDOS B2B",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(sponsoredPartnerships, key = { it.carrier.name }) { partner ->
                SponsoredPartnerCard(info = partner)
            }
        }
    }
}

@Composable
private fun NetworkSpecChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = VaultWhite.copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = VaultWhite.copy(alpha = 0.65f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = VaultWhite,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CarrierOptionCard(
    title: String,
    subtitle: String,
    mccMnc: String,
    usageBytes: Long?,
    accentColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.medium)
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outline
        ),
        tonalElevation = if (selected) 3.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CellTower,
                    contentDescription = title,
                    tint = accentColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = mccMnc,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (usageBytes != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Consumo monitorizado na VPN: ${FormatUtils.formatBytes(usageBytes)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                        color = accentColor
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (selected) "Seleccionada" else "Seleccionar",
                tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SponsoredPartnerCard(info: SponsoredPartnershipInfo) {
    val accent = if (info.carrier == CarrierType.UNITEL) UnitelOrange else AfricellPurple
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Handshake,
                    contentDescription = info.titlePt,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = info.titlePt,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = VaultEmerald.copy(alpha = 0.12f)
            ) {
                Text(
                    text = info.statusBadgePt,
                    style = MaterialTheme.typography.labelMedium,
                    color = VaultEmerald,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = info.mechanismPt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            info.technicalRequirementsPt.forEach { req ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent
                    )
                    Text(
                        text = req,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
