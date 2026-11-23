package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CarrierType
import com.example.model.ConnectivityMatrixItem
import com.example.model.LabRunResult
import com.example.model.SimBalanceState
import com.example.model.ZeroBalanceClassification
import com.example.ui.ConnectivityLabUiState
import com.example.ui.components.TechInfoCalloutCard
import com.example.ui.theme.AfricellPurple
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.UnitelOrange
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultCoralRed
import com.example.ui.theme.VaultElectricBlue
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultNavy
import com.example.ui.theme.VaultWhite
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConnectivityLabScreen(
    uiState: ConnectivityLabUiState,
    onBack: () -> Unit,
    onSelectCarrier: (CarrierType) -> Unit,
    onSelectBalanceState: (SimBalanceState) -> Unit,
    onRunTest: () -> Unit,
    onCaptureBeforeVpn: () -> Unit,
    onCaptureAfterVpn: () -> Unit,
    onToggleVpn: () -> Unit,
    onAddDocumentedService: (CarrierType, String, String, Int, String, String, String, String, Boolean) -> Unit,
    onDeleteDocumentedService: (Long) -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    onClearHistory: () -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Matriz & Teste", "Antes / Depois VPN", "Serviços Oficiais", "Histórico Lab")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Laboratório de Conectividade",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Unitel (631-02) & Africell (631-05)",
                            style = MaterialTheme.typography.labelMedium,
                            color = VaultWhite.copy(alpha = 0.78f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("lab_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VaultNavy,
                    titleContentColor = VaultWhite,
                    navigationIconContentColor = VaultWhite
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .fillMaxSize()
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 12.dp
                ) {
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(title) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> LabMatrixTestTab(
                        uiState = uiState,
                        onSelectCarrier = onSelectCarrier,
                        onSelectBalanceState = onSelectBalanceState,
                        onRunTest = onRunTest,
                        onExportPdf = onExportPdf,
                        onExportCsv = onExportCsv
                    )
                    1 -> LabVpnCompareTab(
                        uiState = uiState,
                        onCaptureBefore = onCaptureBeforeVpn,
                        onCaptureAfter = onCaptureAfterVpn,
                        onToggleVpn = onToggleVpn
                    )
                    2 -> LabDocumentedServicesTab(
                        services = uiState.documentedServices,
                        onAddService = onAddDocumentedService,
                        onDeleteService = onDeleteDocumentedService
                    )
                    3 -> LabHistoryTab(
                        reports = uiState.labReports,
                        onExportPdf = onExportPdf,
                        onExportCsv = onExportCsv,
                        onClearReports = onClearHistory
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabMatrixTestTab(
    uiState: ConnectivityLabUiState,
    onSelectCarrier: (CarrierType) -> Unit,
    onSelectBalanceState: (SimBalanceState) -> Unit,
    onRunTest: () -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Independent Carrier Selection
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Science,
                            contentDescription = null,
                            tint = VaultElectricBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. Perfil Independente de Operadora",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Cada rede é avaliada com os seus próprios domínios, serviços documentados e validação independente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = uiState.targetCarrier == CarrierType.UNITEL,
                            onClick = { onSelectCarrier(CarrierType.UNITEL) },
                            label = { Text("Unitel Angola (631-02)") },
                            modifier = Modifier.testTag("lab_select_unitel")
                        )
                        FilterChip(
                            selected = uiState.targetCarrier == CarrierType.AFRICELL,
                            onClick = { onSelectCarrier(CarrierType.AFRICELL) },
                            label = { Text("Africell Angola (631-05)") },
                            modifier = Modifier.testTag("lab_select_africell")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "2. Modo Sem Saldo / Estado do Cartão SIM",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SimBalanceState.entries.forEach { bal ->
                            FilterChip(
                                selected = uiState.balanceState == bal,
                                onClick = { onSelectBalanceState(bal) },
                                label = { Text(bal.shortTag) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onRunTest,
                        enabled = !uiState.isRunningTest,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VaultElectricBlue,
                            contentColor = VaultWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("run_lab_matrix_button")
                    ) {
                        if (uiState.isRunningTest) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = VaultWhite,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("A executar matriz técnica real...")
                        } else {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Executar Teste Completo (${uiState.targetCarrier.shortName})")
                        }
                    }
                }
            }
        }

        // 2. Results if run
        val result = uiState.latestResult
        if (result != null) {
            item {
                ClassificationResultCard(result = result)
            }

            item {
                TechInfoCalloutCard(
                    title = "Regra de Evidência & Conformidade",
                    body = result.complianceNoticePt,
                    accentColor = VaultAmber,
                    icon = Icons.Filled.Shield
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportPdf,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar PDF")
                    }
                    OutlinedButton(
                        onClick = onExportCsv,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar CSV")
                    }
                }
            }

            item {
                Text(
                    text = "MATRIZ DE CONECTIVIDADE (9 VERIFICAÇÕES)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(result.matrixItems, key = { it.key }) { item ->
                MatrixItemRow(item = item)
            }
        }
    }
}

@Composable
private fun ClassificationResultCard(result: LabRunResult) {
    val accent = when (result.classification) {
        ZeroBalanceClassification.VPN_FUNCTIONAL,
        ZeroBalanceClassification.PUBLIC_INTERNET_AVAILABLE -> VaultEmerald
        ZeroBalanceClassification.LIMITED_TO_SPECIFIC_SERVICES,
        ZeroBalanceClassification.VPN_SERVER_REACHABLE_NO_FORWARDING -> VaultAmber
        else -> VaultCoralRed
    }
    val carrierAccent = if (result.carrierTested == CarrierType.AFRICELL) AfricellPurple else UnitelOrange

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = carrierAccent.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "${result.carrierTested.displayName} · ${result.balanceState.shortTag}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = carrierAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = FormatUtils.formatTimeShort(result.executedAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = result.classification.titlePt,
                style = MaterialTheme.typography.titleLarge,
                color = accent
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.classification.descriptionPt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "IP Público: ${result.publicIpObserved ?: "Indisponível"} · VPN: ${if (result.vpnActive) "Activa (${result.vpnIp})" else "Inactiva"} · Consumo: ${FormatUtils.formatBytes(result.dataConsumedBytes)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = accent.copy(alpha = 0.08f)
            ) {
                Text(
                    text = result.technicalConclusionPt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun MatrixItemRow(item: ConnectivityMatrixItem) {
    val color = if (item.passed) VaultEmerald else VaultCoralRed
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.passed) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                contentDescription = item.statusPt,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.labelPt,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.detailPt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.statusPt,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = color
                )
                if (item.latencyMs != null) {
                    Text(
                        text = "${item.latencyMs} ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
