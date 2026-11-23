package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ServerProfileEntity
import com.example.model.TunnelMode
import com.example.model.VpnProtocol
import com.example.ui.VaultUiState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultCoralRed
import com.example.ui.theme.VaultElectricBlue
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultWhite

private enum class ServerFilter(val labelPt: String) {
    ALL("Todos"),
    PREPARED("Cofre Offline"),
    UNITEL("Unitel"),
    AFRICELL("Africell"),
    CUSTOM("Próprios")
}

@Composable
fun ServersScreen(
    uiState: VaultUiState,
    onSelectServer: (ServerProfileEntity) -> Unit,
    onSaveServer: (ServerProfileEntity) -> Unit,
    onDeleteServer: (ServerProfileEntity) -> Unit,
    onTestServer: (ServerProfileEntity) -> Unit,
    onTestAllServers: () -> Unit,
    onExportJsonClick: () -> Unit,
    onImportJsonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(ServerFilter.ALL) }
    var editingServer by remember { mutableStateOf<ServerProfileEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val filteredServers = remember(uiState.servers, activeFilter) {
        when (activeFilter) {
            ServerFilter.ALL -> uiState.servers
            ServerFilter.PREPARED -> uiState.servers.filter { it.isPreparedOffline }
            ServerFilter.UNITEL -> uiState.servers.filter { it.preferredCarrier == "UNITEL" || it.preferredCarrier == "ALL" }
            ServerFilter.AFRICELL -> uiState.servers.filter { it.preferredCarrier == "AFRICELL" || it.preferredCarrier == "ALL" }
            ServerFilter.CUSTOM -> uiState.servers.filter { it.isCustom }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingServer = null
                    showEditor = true
                },
                containerColor = VaultElectricBlue,
                contentColor = VaultWhite,
                modifier = Modifier.testTag("add_server_fab")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Adicionar servidor próprio"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header & Bundle Actions
                item {
                    Column {
                        Text(
                            text = "Servidores Próprios & Cofre",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Configure os seus servidores privados, teste a latência TCP e guarde perfis no Cofre Offline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onTestAllServers,
                                enabled = !uiState.isTestingServers,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("test_all_servers_button")
                            ) {
                                if (uiState.isTestingServers) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Speed,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testar Todos", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = onExportJsonClick,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("export_json_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FileUpload,
                                    contentDescription = "Exportar perfis JSON",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Exportar")
                            }
                            OutlinedButton(
                                onClick = onImportJsonClick,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("import_json_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FileDownload,
                                    contentDescription = "Importar perfis JSON",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Importar")
                            }
                        }
                    }
                }

                // Filter Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ServerFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = activeFilter == filter,
                                onClick = { activeFilter = filter },
                                label = { Text(filter.labelPt) }
                            )
                        }
                    }
                }

                items(filteredServers, key = { it.id }) { server ->
                    ServerProfileCard(
                        server = server,
                        onSelect = { onSelectServer(server) },
                        onTest = { onTestServer(server) },
                        onEdit = {
                            editingServer = server
                            showEditor = true
                        },
                        onDelete = { onDeleteServer(server) }
                    )
                }
            }
        }
    }

    if (showEditor) {
        ServerEditorDialog(
            initialServer = editingServer,
            onDismiss = { showEditor = false },
            onConfirm = { saved ->
                onSaveServer(saved)
                showEditor = false
            }
        )
    }
}

@Composable
private fun ServerProfileCard(
    server: ServerProfileEntity,
    onSelect: () -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val protocolEnum = runCatching { VpnProtocol.valueOf(server.protocol) }
        .getOrDefault(VpnProtocol.DNS_VAULT_GUARD)
    val tunnelEnum = runCatching { TunnelMode.valueOf(server.tunnelMode) }
        .getOrDefault(TunnelMode.DNS_VAULT_SPLIT)

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (server.isSelected) 2.dp else 1.dp,
            color = if (server.isSelected) VaultElectricBlue else MaterialTheme.colorScheme.outline
        ),
        tonalElevation = if (server.isSelected) 3.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (server.isSelected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Servidor seleccionado",
                                tint = VaultElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${server.host}:${server.port} · DNS ${server.dnsPrimary}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Latency pill
                val latency = server.lastLatencyMs
                val latencyColor = when {
                    latency == null -> VaultAmber
                    latency < 120 -> VaultEmerald
                    else -> VaultElectricBlue
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = latencyColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (latency != null) "$latency ms" else "Não testado",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = latencyColor,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TagPill(text = protocolEnum.displayName, color = VaultElectricBlue)
                TagPill(text = server.preferredCarrier, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (server.isPreparedOffline) {
                    TagPill(text = "Cofre Offline", color = VaultEmerald, icon = Icons.Filled.Lock)
                }
            }

            if (server.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = server.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Modo: ${tunnelEnum.labelPt} · MTU ${server.mtu}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSelect,
                    enabled = !server.isSelected,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (server.isSelected) VaultEmerald else VaultElectricBlue,
                        contentColor = VaultWhite
                    ),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(if (server.isSelected) "Activo" else "Seleccionar")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTest,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NetworkCheck,
                            contentDescription = "Testar latência do servidor",
                            tint = VaultElectricBlue
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Editar servidor",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (server.isCustom) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Eliminar servidor",
                                tint = VaultCoralRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagPill(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.11f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ServerEditorDialog(
    initialServer: ServerProfileEntity?,
    onDismiss: () -> Unit,
    onConfirm: (ServerProfileEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialServer?.name ?: "") }
    var host by remember { mutableStateOf(initialServer?.host ?: "") }
    var portText by remember { mutableStateOf((initialServer?.port ?: 443).toString()) }
    var dnsPrimary by remember { mutableStateOf(initialServer?.dnsPrimary ?: "1.1.1.1") }
    var dnsSecondary by remember { mutableStateOf(initialServer?.dnsSecondary ?: "8.8.8.8") }
    var mtuText by remember { mutableStateOf((initialServer?.mtu ?: 1420).toString()) }
    var preferredCarrier by remember { mutableStateOf(initialServer?.preferredCarrier ?: "ALL") }
    var selectedProtocol by remember {
        mutableStateOf(
            initialServer?.protocol?.let { runCatching { VpnProtocol.valueOf(it) }.getOrNull() }
                ?: VpnProtocol.WIREGUARD_TUNNEL
        )
    }
    var selectedTunnelMode by remember {
        mutableStateOf(
            initialServer?.tunnelMode?.let { runCatching { TunnelMode.valueOf(it) }.getOrNull() }
                ?: TunnelMode.FULL_ENDPOINT_TUNNEL
        )
    }
    var publicKeyOrToken by remember { mutableStateOf(initialServer?.publicKeyOrToken ?: "") }
    var notes by remember { mutableStateOf(initialServer?.notes ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialServer == null) "Novo Servidor Próprio" else "Editar Servidor",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Perfil") },
                    placeholder = { Text("Ex: Servidor Próprio Luanda") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("IP ou Domínio") },
                        placeholder = { Text("197.216.x.x") },
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

                Text(
                    text = "Protocolo",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VpnProtocol.entries.forEach { proto ->
                        FilterChip(
                            selected = selectedProtocol == proto,
                            onClick = {
                                selectedProtocol = proto
                                if (initialServer == null) {
                                    portText = proto.defaultPort.toString()
                                }
                            },
                            label = { Text(proto.displayName) }
                        )
                    }
                }

                Text(
                    text = "Modo do Túnel TUN",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TunnelMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedTunnelMode == mode,
                            onClick = { selectedTunnelMode = mode },
                            label = { Text(mode.labelPt) }
                        )
                    }
                }

                Text(
                    text = "Operadora Preferida",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ALL" to "Todas", "UNITEL" to "Unitel", "AFRICELL" to "Africell").forEach { (code, label) ->
                        FilterChip(
                            selected = preferredCarrier == code,
                            onClick = { preferredCarrier = code },
                            label = { Text(label) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dnsPrimary,
                        onValueChange = { dnsPrimary = it },
                        label = { Text("DNS Primário") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = mtuText,
                        onValueChange = { mtuText = it.filter(Char::isDigit) },
                        label = { Text("MTU") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                }

                OutlinedTextField(
                    value = publicKeyOrToken,
                    onValueChange = { publicKeyOrToken = it },
                    label = { Text("Chave Pública / Token (Opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas Técnicas") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VaultCoralRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portText.toIntOrNull()
                    val mtu = (mtuText.toIntOrNull() ?: 1420).coerceIn(1280, 1500)
                    when {
                        name.isBlank() -> validationError = "Indique um nome para o servidor."
                        host.isBlank() -> validationError = "Indique o endereço IP ou domínio do servidor."
                        port == null || port !in 1..65535 -> validationError = "Porta inválida (1 a 65535)."
                        else -> {
                            onConfirm(
                                ServerProfileEntity(
                                    id = initialServer?.id ?: 0L,
                                    name = name.trim(),
                                    host = host.trim(),
                                    port = port,
                                    protocol = selectedProtocol.name,
                                    tunnelMode = selectedTunnelMode.name,
                                    dnsPrimary = dnsPrimary.trim().ifBlank { "1.1.1.1" },
                                    dnsSecondary = dnsSecondary.trim().ifBlank { "8.8.8.8" },
                                    mtu = mtu,
                                    preferredCarrier = preferredCarrier,
                                    publicKeyOrToken = publicKeyOrToken.trim(),
                                    isPreparedOffline = true,
                                    lastPreparedAt = System.currentTimeMillis(),
                                    lastLatencyMs = initialServer?.lastLatencyMs,
                                    isSelected = initialServer?.isSelected ?: false,
                                    isCustom = initialServer?.isCustom ?: true,
                                    notes = notes.trim()
                                )
                            )
                        }
                    }
                }
            ) {
                Text("Guardar no Cofre")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
