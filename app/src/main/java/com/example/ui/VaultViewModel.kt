package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.VpnService
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.InternetVaultApp
import com.example.data.local.CarrierUsageSummary
import com.example.data.local.ServerProfileEntity
import com.example.data.local.UsageSessionEntity
import com.example.data.local.VaultPreferences
import com.example.model.CarrierSelectionMode
import com.example.model.CarrierType
import com.example.model.DiagnosticStepResult
import com.example.model.NetworkProbeStatus
import com.example.model.SponsoredPartnershipInfo
import com.example.model.TrafficTelemetry
import com.example.model.TransportType
import com.example.model.VpnConnectionState
import com.example.ui.theme.ThemeMode
import com.example.vpn.InternetVaultVpnService
import com.example.vpn.VpnRuntimeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class VaultUiState(
    val connectionState: VpnConnectionState = VpnConnectionState.Disconnected,
    val telemetry: TrafficTelemetry = TrafficTelemetry(),
    val networkStatus: NetworkProbeStatus = NetworkProbeStatus(),
    val servers: List<ServerProfileEntity> = emptyList(),
    val selectedServer: ServerProfileEntity? = null,
    val usageSessions: List<UsageSessionEntity> = emptyList(),
    val carrierSummaries: List<CarrierUsageSummary> = emptyList(),
    val preferences: VaultPreferences = VaultPreferences(),
    val diagnosticSteps: List<DiagnosticStepResult> = emptyList(),
    val isRunningDiagnostics: Boolean = false,
    val isPreparingVault: Boolean = false,
    val isTestingServers: Boolean = false,
    val statusBannerMessage: String? = null
)

sealed interface VaultUiEvent {
    data class RequestVpnPermission(val intent: Intent) : VaultUiEvent
    data class ShowMessage(val message: String) : VaultUiEvent
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as InternetVaultApp
    private val repository = app.repository
    private val networkMonitor = app.networkMonitor

    private val _diagnosticSteps = MutableStateFlow<List<DiagnosticStepResult>>(emptyList())
    private val _isRunningDiagnostics = MutableStateFlow(false)
    private val _isPreparingVault = MutableStateFlow(false)
    private val _isTestingServers = MutableStateFlow(false)
    private val _statusBanner = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<VaultUiEvent>()
    val events: SharedFlow<VaultUiEvent> = _events.asSharedFlow()

    private val extraFlagsFlow = combine(
        _diagnosticSteps,
        _isRunningDiagnostics,
        _isPreparingVault,
        _isTestingServers,
        _statusBanner
    ) { steps, runningDiag, preparing, testing, banner ->
        ExtraUiFlags(steps, runningDiag, preparing, testing, banner)
    }

    private val coreDataFlow = combine(
        VpnRuntimeState.connectionState,
        VpnRuntimeState.telemetry,
        networkMonitor.status,
        repository.serversFlow,
        repository.selectedServerFlow
    ) { conn, telemetry, net, servers, selected ->
        CoreUiState(conn, telemetry, net, servers, selected)
    }

    private val historyPrefsFlow = combine(
        repository.usageSessionsFlow,
        repository.carrierSummariesFlow,
        repository.preferencesFlow
    ) { sessions, summaries, prefs ->
        Triple(sessions, summaries, prefs)
    }

    val uiState: StateFlow<VaultUiState> = combine(
        coreDataFlow,
        historyPrefsFlow,
        extraFlagsFlow
    ) { core, histPrefs, flags ->
        VaultUiState(
            connectionState = core.connectionState,
            telemetry = core.telemetry,
            networkStatus = core.networkStatus,
            servers = core.servers,
            selectedServer = core.selectedServer,
            usageSessions = histPrefs.first,
            carrierSummaries = histPrefs.second,
            preferences = histPrefs.third,
            diagnosticSteps = flags.diagnosticSteps,
            isRunningDiagnostics = flags.isRunningDiagnostics,
            isPreparingVault = flags.isPreparingVault,
            isTestingServers = flags.isTestingServers,
            statusBannerMessage = flags.statusBannerMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VaultUiState()
    )

    val sponsoredPartnerships: List<SponsoredPartnershipInfo> = listOf(
        SponsoredPartnershipInfo(
            carrier = CarrierType.UNITEL,
            titlePt = "Unitel Angola · Conectividade Patrocinada B2B",
            statusBadgePt = "Arquitectura Preparada · Acordo Comercial",
            mechanismPt = "Integração por lista branca de IPs corporativos, APN dedicada ou API de patrocínio de tráfego (Zero-Rating) para que o acesso ao servidor autorizado não consuma o saldo principal do cliente.",
            technicalRequirementsPt = listOf(
                "Registo do IP estático e porta do servidor próprio junto da equipa técnica B2B da Unitel.",
                "Identificação legítima por cabeçalho/domínio ou sub-rede APN dedicada autorizada.",
                "Monitorização de quota e auditoria de tráfego sem qualquer evasão de tarifação."
            )
        ),
        SponsoredPartnershipInfo(
            carrier = CarrierType.AFRICELL,
            titlePt = "Africell Angola · Parcerias Data-Free & Zero-Rating",
            statusBadgePt = "Compatível com Whitelist · Em Exploração",
            mechanismPt = "Acordo de tráfego patrocinado (Reverse Billing / Zero-Rating) onde o tráfego destinado ao endpoint do Internet Vault é facturado à entidade patrocinadora ou integrado em pacotes específicos.",
            technicalRequirementsPt = listOf(
                "Endpoint TLS/HTTPS estável com certificado válido e endereço IP dedicado.",
                "Contrato comercial de dados patrocinados com limite de largura de banda definido.",
                "Separação de tráfego (Split-Tunneling) para distinguir serviços patrocinados da navegação geral."
            )
        )
    )

    init {
        viewModelScope.launch {
            repository.preferencesFlow.collect { prefs ->
                networkMonitor.updateCarrierSelectionMode(prefs.carrierSelectionMode)
            }
        }

        // Periodic reachability verification for active server and effective internet
        viewModelScope.launch {
            while (isActive) {
                probeSelectedServerAndInternet()
                delay(15_000L)
            }
        }
    }

    fun probeSelectedServerAndInternet() {
        viewModelScope.launch {
            val server = repository.getSelectedServerOnce()
            val (internetOk, _) = networkMonitor.probeEffectiveInternet()
            val latency = if (server != null) {
                networkMonitor.probeServerTcp(server.host, server.port).also { ms ->
                    repository.updateServerLatency(server.id, ms)
                }
            } else {
                null
            }
            val isVpnRunning = VpnRuntimeState.connectionState.value is VpnConnectionState.Connected
            networkMonitor.refreshSync(
                isVpnRunning = isVpnRunning,
                serverReachable = latency != null,
                serverLatencyMs = latency,
                effectiveInternet = internetOk
            )
        }
    }

    fun onConnectButtonPressed() {
        val currentState = VpnRuntimeState.connectionState.value
        if (currentState is VpnConnectionState.Connected ||
            currentState is VpnConnectionState.Connecting ||
            currentState is VpnConnectionState.Preparing
        ) {
            stopVpnService()
            return
        }

        val prepareIntent = VpnService.prepare(app)
        if (prepareIntent != null) {
            viewModelScope.launch {
                _events.emit(VaultUiEvent.RequestVpnPermission(prepareIntent))
            }
        } else {
            startVpnService()
        }
    }

    fun onVpnPermissionResult(granted: Boolean) {
        if (granted) {
            startVpnService()
        } else {
            viewModelScope.launch {
                _events.emit(
                    VaultUiEvent.ShowMessage("Permissão de VPN recusada. É necessária para criar o túnel seguro.")
                )
            }
        }
    }

    private fun startVpnService() {
        val selectedId = uiState.value.selectedServer?.id ?: -1L
        val intent = Intent(app, InternetVaultVpnService::class.java).apply {
            action = InternetVaultVpnService.ACTION_CONNECT
            putExtra(InternetVaultVpnService.EXTRA_SERVER_ID, selectedId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(intent)
        } else {
            app.startService(intent)
        }
    }

    private fun stopVpnService() {
        val intent = Intent(app, InternetVaultVpnService::class.java).apply {
            action = InternetVaultVpnService.ACTION_DISCONNECT
        }
        app.startService(intent)
    }

    fun selectCarrierMode(mode: CarrierSelectionMode) {
        viewModelScope.launch {
            repository.setCarrierSelectionMode(mode)
            networkMonitor.updateCarrierSelectionMode(mode)
            _events.emit(VaultUiEvent.ShowMessage("Operadora configurada: ${mode.labelPt}"))
        }
    }

    fun selectServer(server: ServerProfileEntity) {
        viewModelScope.launch {
            repository.selectServer(server.id)
            probeSelectedServerAndInternet()
            _events.emit(VaultUiEvent.ShowMessage("Servidor activo: ${server.name}"))
        }
    }

    fun saveServer(server: ServerProfileEntity) {
        viewModelScope.launch {
            val savedId = repository.saveServer(server)
            val latency = networkMonitor.probeServerTcp(server.host, server.port)
            repository.updateServerLatency(savedId, latency)
            _events.emit(VaultUiEvent.ShowMessage("Perfil '${server.name}' guardado no Cofre."))
        }
    }

    fun deleteServer(server: ServerProfileEntity) {
        viewModelScope.launch {
            repository.deleteServer(server.id)
            _events.emit(VaultUiEvent.ShowMessage("Servidor '${server.name}' removido."))
        }
    }

    fun testSingleServer(server: ServerProfileEntity) {
        viewModelScope.launch {
            val latency = networkMonitor.probeServerTcp(server.host, server.port)
            repository.updateServerLatency(server.id, latency)
            val msg = if (latency != null) {
                "${server.name} disponível ($latency ms)"
            } else {
                "${server.name} indisponível em ${server.host}:${server.port}"
            }
            _events.emit(VaultUiEvent.ShowMessage(msg))
        }
    }

    fun testAllServers() {
        viewModelScope.launch {
            _isTestingServers.value = true
            val list = uiState.value.servers
            var onlineCount = 0
            list.forEach { server ->
                val latency = networkMonitor.probeServerTcp(server.host, server.port, 2000)
                if (latency != null) onlineCount++
                repository.updateServerLatency(server.id, latency)
            }
            _isTestingServers.value = false
            _events.emit(
                VaultUiEvent.ShowMessage("Verificação concluída: $onlineCount de ${list.size} servidores disponíveis.")
            )
        }
    }

    fun prepareVaultOverWifi() {
        viewModelScope.launch {
            _isPreparingVault.value = true
            networkMonitor.refreshSync()
            val transport = networkMonitor.status.value.transportType
            val now = System.currentTimeMillis()
            val servers = uiState.value.servers
            var validatedCount = 0

            servers.forEach { server ->
                val latency = networkMonitor.probeServerTcp(server.host, server.port, 2000)
                if (latency != null) validatedCount++
                repository.markServerPreparedOffline(
                    id = server.id,
                    prepared = true,
                    timestamp = now,
                    latencyMs = latency
                )
            }
            repository.updateLastWifiPrep(now)
            _isPreparingVault.value = false

            val msg = if (transport == TransportType.WIFI) {
                "Cofre sincronizado via Wi-Fi! ${servers.size} perfis guardados ($validatedCount online) para uso posterior em Dados Móveis."
            } else {
                "Perfis guardados localmente no Cofre ($validatedCount online). Nota: para poupar dados móveis, faça esta preparação em Wi-Fi."
            }
            _statusBanner.value = msg
            _events.emit(VaultUiEvent.ShowMessage(msg))
        }
    }

    fun runFullDiagnostics() {
        viewModelScope.launch {
            _isRunningDiagnostics.value = true
            val server = repository.getSelectedServerOnce()
            val steps = networkMonitor.runFullDiagnostics(
                serverName = server?.name ?: "Sem servidor",
                serverHost = server?.host ?: "1.1.1.1",
                serverPort = server?.port ?: 443
            )
            _diagnosticSteps.value = steps
            _isRunningDiagnostics.value = false
        }
    }

    suspend fun exportVaultJson(): String = repository.exportServersToJson()

    fun importVaultJson(json: String) {
        viewModelScope.launch {
            val count = runCatching { repository.importServersFromJson(json) }.getOrDefault(0)
            val msg = if (count > 0) {
                "$count perfis importados para o Cofre Offline com sucesso."
            } else {
                "Ficheiro JSON inválido ou sem perfis de servidor válidos."
            }
            _events.emit(VaultUiEvent.ShowMessage(msg))
        }
    }

    fun clearUsageHistory() {
        viewModelScope.launch {
            repository.clearUsageHistory()
            _events.emit(VaultUiEvent.ShowMessage("Histórico de sessões limpo."))
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setAutoPrepareOnWifi(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoPrepareOnWifi(enabled) }
    }

    fun setDnsLeakGuard(enabled: Boolean) {
        viewModelScope.launch { repository.setDnsLeakGuard(enabled) }
    }

    fun setBypassLan(enabled: Boolean) {
        viewModelScope.launch { repository.setBypassLan(enabled) }
    }

    fun dismissBanner() {
        _statusBanner.value = null
    }

    private data class CoreUiState(
        val connectionState: VpnConnectionState,
        val telemetry: TrafficTelemetry,
        val networkStatus: NetworkProbeStatus,
        val servers: List<ServerProfileEntity>,
        val selectedServer: ServerProfileEntity?
    )

    private data class ExtraUiFlags(
        val diagnosticSteps: List<DiagnosticStepResult>,
        val isRunningDiagnostics: Boolean,
        val isPreparingVault: Boolean,
        val isTestingServers: Boolean,
        val statusBannerMessage: String?
    )
}
