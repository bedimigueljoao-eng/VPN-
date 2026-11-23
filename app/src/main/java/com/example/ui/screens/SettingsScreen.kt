package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DiagnosticStepResult
import com.example.ui.VaultUiState
import com.example.ui.components.TechInfoCalloutCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.VaultCoralRed
import com.example.ui.theme.VaultElectricBlue
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultWhite

@Composable
fun SettingsScreen(
    uiState: VaultUiState,
    onRunDiagnostics: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetAutoPrepareWifi: (Boolean) -> Unit,
    onSetDnsLeakGuard: (Boolean) -> Unit,
    onSetBypassLan: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.preferences

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
            item {
                Column {
                    Text(
                        text = "Definições & Diagnóstico",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Verifique o estado técnico da ligação e personalize o funcionamento da VPN.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Real-Time Diagnostic Suite
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.NetworkPing,
                                contentDescription = "Diagnóstico de rede",
                                tint = VaultElectricBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Diagnóstico Completo de Conectividade",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Testa transporte activo, resolução DNS, handshake TCP com o servidor seleccionado e acesso efectivo à internet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRunDiagnostics,
                            enabled = !uiState.isRunningDiagnostics,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VaultElectricBlue,
                                contentColor = VaultWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("run_diagnostics_button")
                        ) {
                            if (uiState.isRunningDiagnostics) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = VaultWhite,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("A testar rede e servidor...")
                            } else {
                                Text("Executar Diagnóstico Completo")
                            }
                        }
                    }
                }
            }

            if (uiState.diagnosticSteps.isNotEmpty()) {
                items(uiState.diagnosticSteps, key = { it.titlePt }) { step ->
                    DiagnosticStepCard(step = step)
                }
            }

            // 2. Appearance & VPN Preferences
            item {
                Text(
                    text = "PREFERÊNCIAS DE INTERFACE & TÚNEL",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Tema Visual",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = prefs.themeMode == mode,
                                    onClick = { onSetThemeMode(mode) },
                                    label = { Text(mode.labelPt) }
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        SettingToggleRow(
                            title = "Preparar Cofre ao ligar Wi-Fi",
                            subtitle = "Actualiza latência e validação de perfis automaticamente em Wi-Fi.",
                            checked = prefs.autoPrepareOnWifi,
                            onCheckedChange = onSetAutoPrepareWifi,
                            testTag = "toggle_auto_wifi"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        SettingToggleRow(
                            title = "Protecção contra Fugas DNS",
                            subtitle = "Encaminha resoluções DNS pelo túnel TUN configurado.",
                            checked = prefs.dnsLeakGuardEnabled,
                            onCheckedChange = onSetDnsLeakGuard,
                            testTag = "toggle_dns_guard"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        SettingToggleRow(
                            title = "Ignorar Rede Local (LAN Bypass)",
                            subtitle = "Mantém acesso directo a impressoras e routers na rede local.",
                            checked = prefs.bypassLanEnabled,
                            onCheckedChange = onSetBypassLan,
                            testTag = "toggle_bypass_lan"
                        )
                    }
                }
            }

            // 3. Technical Transparency & Compliance
            item {
                TechInfoCalloutCard(
                    title = "Arquitectura Transparente & Ética",
                    body = "O Internet Vault utiliza a API oficial VpnService do Android para estabelecer túneis seguros para os seus próprios servidores. Não realiza falsificação de tráfego, exploração de vulnerabilidades nem contorno de autenticação ou tarifação das operadoras Unitel e Africell.",
                    accentColor = VaultEmerald,
                    icon = Icons.Filled.Policy
                )
            }

            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = VaultElectricBlue
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Internet Vault · Versão 1.0",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Motor TUN IPv4/UDP + Monitor Unitel/Africell Angola",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticStepCard(step: DiagnosticStepResult) {
    val accent = if (step.passed) VaultEmerald else VaultCoralRed
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (step.passed) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                contentDescription = if (step.passed) "Aprovado" else "Falhou",
                tint = accent
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.titlePt,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = step.detailPt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (step.latencyMs != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${step.latencyMs} ms",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = accent
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
