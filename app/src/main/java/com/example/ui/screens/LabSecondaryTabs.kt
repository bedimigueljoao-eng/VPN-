package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ConnectivityLabReportEntity
import com.example.data.local.DocumentedServiceEntity
import com.example.model.CarrierType
import com.example.model.LabVpnSnapshot
import com.example.model.VpnConnectionState
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
import com.example.ui.theme.VaultWhite
import com.example.util.FormatUtils
import com.example.util.ReportExporter

@Composable
fun LabVpnCompareTab(
    uiState: ConnectivityLabUiState,
    onCaptureBefore: () -> Unit,
    onCaptureAfter: () -> Unit,
    onToggleVpn: () -> Unit
) {
    val isVpnConnected = uiState.connectionState is VpnConnectionState.Connected

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TechInfoCalloutCard(
                title = "Comparação Controlada: Antes vs Depois da VPN",
                body = "1) Com a VPN desligada, toque em 'Capturar Antes'. 2) Ligue a VPN abaixo e toque em 'Capturar Depois' para comparar IP público, túneis, latência e consumo real.",
                accentColor = VaultElectricBlue,
                icon = Icons.Filled.CompareArrows
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCaptureBefore,
                    enabled = !uiState.isCapturingSnapshot,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("capture_before_vpn_button")
                ) {
                    Text("1. Capturar Antes", maxLines = 1)
                }
                Button(
                    onClick = onToggleVpn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isVpnConnected) VaultEmerald else VaultElectricBlue,
                        contentColor = VaultWhite
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("lab_toggle_vpn_button")
                ) {
                    Text(if (isVpnConnected) "Desligar VPN" else "Ligar VPN")
                }
                Button(
                    onClick = onCaptureAfter,
                    enabled = !uiState.isCapturingSnapshot,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaultEmerald,
                        contentColor = VaultWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("capture_after_vpn_button")
                ) {
                    if (uiState.isCapturingSnapshot) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = VaultWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("2. Capturar Depois", maxLines = 1)
                    }
                }
            }
        }

        item {
            VpnComparisonTableCard(
                before = uiState.beforeVpnSnapshot,
                after = uiState.afterVpnSnapshot
            )
        }
    }
}

@Composable
private fun VpnComparisonTableCard(
    before: LabVpnSnapshot?,
    after: LabVpnSnapshot?
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quadro Comparativo (Sem VPN vs Com VPN)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            CompareRow(
                metric = "IP Público Observado",
                beforeVal = before?.publicIp ?: "Não capturado",
                afterVal = after?.publicIp ?: "Não capturado"
            )
            CompareRow(
                metric = "Estado do Túnel TUN",
                beforeVal = before?.let { if (it.vpnActive) "Activo (${it.vpnIp})" else "Inactivo" } ?: "—",
                afterVal = after?.let { if (it.vpnActive) "Activo (${it.vpnIp})" else "Inactivo" } ?: "—"
            )
            CompareRow(
                metric = "Servidor Alcançado",
                beforeVal = before?.let { if (it.serverReachable) "Sim (${it.serverLatencyMs} ms)" else "Inacessível" } ?: "—",
                afterVal = after?.let { if (it.serverReachable) "Sim (${it.serverLatencyMs} ms)" else "Inacessível" } ?: "—"
            )
            CompareRow(
                metric = "Internet Pública",
                beforeVal = before?.let { if (it.publicInternetReachable) "Sim (${it.publicInternetLatencyMs} ms)" else "Bloqueada" } ?: "—",
                afterVal = after?.let { if (it.publicInternetReachable) "Sim (${it.publicInternetLatencyMs} ms)" else "Bloqueada" } ?: "—"
            )
            CompareRow(
                metric = "Dados Utilizados",
                beforeVal = before?.let { FormatUtils.formatBytes(it.dataUsedBytes) } ?: "—",
                afterVal = after?.let { FormatUtils.formatBytes(it.dataUsedBytes) } ?: "—"
            )
            CompareRow(
                metric = "Pacotes TUN",
                beforeVal = before?.tunPackets?.toString() ?: "—",
                afterVal = after?.tunPackets?.toString() ?: "—"
            )
            CompareRow(
                metric = "Erros de Ligação",
                beforeVal = before?.errorDetailPt ?: if (before != null) "Sem erros" else "—",
                afterVal = after?.errorDetailPt ?: if (after != null) "Sem erros" else "—"
            )
        }
    }
}

@Composable
private fun CompareRow(metric: String, beforeVal: String, afterVal: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = metric,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "Antes: $beforeVal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = VaultElectricBlue.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "Depois: $afterVal",
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultElectricBlue,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun LabDocumentedServicesTab(
    services: List<DocumentedServiceEntity>,
    onAddService: (CarrierType, String, String, Int, String, String, String, String, Boolean) -> Unit,
    onDeleteService: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TechInfoCalloutCard(
                title = "Registo de Serviços Documentados",
                body = "Registe apenas serviços oficialmente documentados pela Unitel ou Africell com fonte verificável, data e condições. O laboratório nunca assume gratuidade sem evidência e autorização.",
                accentColor = VaultAmber,
                icon = Icons.Filled.Description
            )
        }

        item {
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = VaultElectricBlue,
                    contentColor = VaultWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("add_documented_service_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Registar Serviço Documentado")
            }
        }

        items(services, key = { it.id }) { service ->
            DocumentedServiceCard(
                service = service,
                onDelete = { onDeleteService(service.id) }
            )
        }
    }

    if (showAddDialog) {
        AddDocumentedServiceDialog(
            onDismiss = { showAddDialog = false },
            onSave = { carrier, name, host, port, source, date, conditions, validity, auth ->
                onAddService(carrier, name, host, port, source, date, conditions, validity, auth)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun DocumentedServiceCard(
    service: DocumentedServiceEntity,
    onDelete: () -> Unit
) {
    val accent = if (service.carrierType == CarrierType.AFRICELL.name) AfricellPurple else UnitelOrange
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.serviceName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${service.carrierType} · ${service.domainOrHost}:${service.port}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
                if (service.isUserAdded) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Remover serviço",
                            tint = VaultCoralRed
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = if (service.isOfficiallyAuthorized) VaultEmerald.copy(alpha = 0.14f) else VaultAmber.copy(alpha = 0.14f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (service.isOfficiallyAuthorized) Icons.Filled.Verified else Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = if (service.isOfficiallyAuthorized) VaultEmerald else VaultAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (service.isOfficiallyAuthorized) {
                            "Documentado · Autorização Oficial"
                        } else {
                            "Em Investigação · Sem Autorização Confirmada"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (service.isOfficiallyAuthorized) VaultEmerald else VaultAmber
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Fonte: ${service.sourceReference}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Verificado em: ${service.verifiedDate} · Validade: ${service.validityNotePt}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Condições: ${service.conditionsPt}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddDocumentedServiceDialog(
    onDismiss: () -> Unit,
    onSave: (CarrierType, String, String, Int, String, String, String, String, Boolean) -> Unit
) {
    var carrier by remember { mutableStateOf(CarrierType.UNITEL) }
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf("443") }
    var source by remember { mutableStateOf("") }
    var verifiedDate by remember { mutableStateOf("23/11/2026") }
    var conditions by remember { mutableStateOf("") }
    var validity by remember { mutableStateOf("Sujeito às regras da operadora") }
    var officiallyAuth by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registar Serviço Documentado") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = carrier == CarrierType.UNITEL,
                        onClick = { carrier = CarrierType.UNITEL },
                        label = { Text("Unitel (631-02)") }
                    )
                    FilterChip(
                        selected = carrier == CarrierType.AFRICELL,
                        onClick = { carrier = CarrierType.AFRICELL },
                        label = { Text("Africell (631-05)") }
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Serviço") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Domínio / Host") },
                        singleLine = true,
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it.filter(Char::isDigit) },
                        label = { Text("Porta") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Fonte Oficial / Referência") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = verifiedDate,
                    onValueChange = { verifiedDate = it },
                    label = { Text("Data de Verificação") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = conditions,
                    onValueChange = { conditions = it },
                    label = { Text("Condições de Utilização") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = validity,
                    onValueChange = { validity = it },
                    label = { Text("Validade") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = officiallyAuth,
                        onCheckedChange = { officiallyAuth = it }
                    )
                    Text(
                        text = "Possui autorização oficial comprovada pela operadora",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (error != null) {
                    Text(error!!, color = VaultCoralRed, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portText.toIntOrNull()
                    when {
                        name.isBlank() -> error = "Indique o nome do serviço."
                        host.isBlank() -> error = "Indique o domínio ou host."
                        source.isBlank() -> error = "Indique a fonte documental."
                        port == null || port !in 1..65535 -> error = "Porta inválida."
                        else -> onSave(carrier, name, host, port, source, verifiedDate, conditions.ifBlank { "Sem notas adicionais" }, validity, officiallyAuth)
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun LabHistoryTab(
    reports: List<ConnectivityLabReportEntity>,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    onClearReports: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExportPdf,
                    enabled = reports.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaultElectricBlue,
                        contentColor = VaultWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("export_lab_pdf_button")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar PDF")
                }
                OutlinedButton(
                    onClick = onExportCsv,
                    enabled = reports.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("export_lab_csv_button")
                ) {
                    Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar CSV")
                }
                if (reports.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearReports,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "Limpar histórico do laboratório", tint = VaultCoralRed)
                    }
                }
            }
        }

        if (reports.isEmpty()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = "Ainda não existem testes guardados no Laboratório. Execute um teste para Unitel ou Africell no primeiro separador.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(reports, key = { it.id }) { report ->
                SavedLabReportCard(report = report)
            }
        }
    }
}

@Composable
private fun SavedLabReportCard(report: ConnectivityLabReportEntity) {
    val classificationTitle = runCatching {
        ZeroBalanceClassification.valueOf(report.classification).titlePt
    }.getOrDefault(report.classification)
    val destinations = remember(report.destinationsJson) {
        ReportExporter.parseDestinations(report.destinationsJson)
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${report.carrierTested} (${report.detectedMccMnc})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = FormatUtils.formatDateTime(report.executedAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Saldo: ${report.balanceState} · ${report.transportType} · VPN: ${if (report.vpnActive) "Activa (${report.vpnIp})" else "Inactiva"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = VaultElectricBlue.copy(alpha = 0.12f)
            ) {
                Text(
                    text = classificationTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = VaultElectricBlue,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "IP Antes: ${report.publicIpBefore} · IP Depois: ${report.publicIpAfter} · Consumo: ${FormatUtils.formatBytes(report.dataConsumedBytes)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (destinations.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                destinations.forEach { d ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${d.labelPt} (${d.host})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (d.reachable) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = if (d.reachable) VaultEmerald else VaultCoralRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (d.reachable) "${d.latencyMs ?: 0}ms" else "Bloq.",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (d.reachable) VaultEmerald else VaultCoralRed
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = report.technicalConclusionPt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
