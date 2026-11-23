package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.InternetVaultApp
import com.example.data.local.ConnectivityLabReportEntity
import com.example.data.local.DocumentedServiceEntity
import com.example.data.local.ServerProfileEntity
import com.example.model.CarrierType
import com.example.model.LabRunResult
import com.example.model.LabVpnSnapshot
import com.example.model.NetworkProbeStatus
import com.example.model.SimBalanceState
import com.example.model.TrafficTelemetry
import com.example.model.VpnConnectionState
import com.example.util.ReportExporter
import com.example.vpn.VpnRuntimeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

data class ConnectivityLabUiState(
    val targetCarrier: CarrierType = CarrierType.UNITEL,
    val balanceState: SimBalanceState = SimBalanceState.NO_BALANCE,
    val connectionState: VpnConnectionState = VpnConnectionState.Disconnected,
    val telemetry: TrafficTelemetry = TrafficTelemetry(),
    val networkStatus: NetworkProbeStatus = NetworkProbeStatus(),
    val selectedServer: ServerProfileEntity? = null,
    val latestResult: LabRunResult? = null,
    val beforeVpnSnapshot: LabVpnSnapshot? = null,
    val afterVpnSnapshot: LabVpnSnapshot? = null,
    val labReports: List<ConnectivityLabReportEntity> = emptyList(),
    val documentedServices: List<DocumentedServiceEntity> = emptyList(),
    val isRunningTest: Boolean = false,
    val isCapturingSnapshot: Boolean = false
)

class ConnectivityLabViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as InternetVaultApp
    private val repository = app.repository
    private val networkMonitor = app.networkMonitor
    private val labEngine = app.labEngine

    private val _targetCarrier = MutableStateFlow(CarrierType.UNITEL)
    private val _balanceState = MutableStateFlow(SimBalanceState.NO_BALANCE)
    private val _latestResult = MutableStateFlow<LabRunResult?>(null)
    private val _beforeSnapshot = MutableStateFlow<LabVpnSnapshot?>(null)
    private val _afterSnapshot = MutableStateFlow<LabVpnSnapshot?>(null)
    private val _isRunningTest = MutableStateFlow(false)
    private val _isCapturingSnapshot = MutableStateFlow(false)

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val configFlow = combine(
        _targetCarrier,
        _balanceState,
        _latestResult,
        _beforeSnapshot,
        _afterSnapshot
    ) { carrier, balance, result, before, after ->
        LabConfigState(carrier, balance, result, before, after)
    }

    private val runtimeFlow = combine(
        VpnRuntimeState.connectionState,
        VpnRuntimeState.telemetry,
        networkMonitor.status,
        repository.selectedServerFlow
    ) { conn, tel, net, server ->
        LabRuntimeSubState(conn, tel, net, server)
    }

    private val dbAndFlagsFlow = combine(
        repository.labReportsFlow,
        repository.documentedServicesFlow,
        _isRunningTest,
        _isCapturingSnapshot
    ) { reports, docs, running, capturing ->
        LabDbFlags(reports, docs, running, capturing)
    }

    val uiState: StateFlow<ConnectivityLabUiState> = combine(
        configFlow,
        runtimeFlow,
        dbAndFlagsFlow
    ) { cfg, rt, db ->
        ConnectivityLabUiState(
            targetCarrier = cfg.carrier,
            balanceState = cfg.balance,
            connectionState = rt.conn,
            telemetry = rt.tel,
            networkStatus = rt.net,
            selectedServer = rt.server,
            latestResult = cfg.result,
            beforeVpnSnapshot = cfg.before,
            afterVpnSnapshot = cfg.after,
            labReports = db.reports,
            documentedServices = db.docs,
            isRunningTest = db.running,
            isCapturingSnapshot = db.capturing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConnectivityLabUiState()
    )

    fun selectTargetCarrier(carrier: CarrierType) {
        _targetCarrier.value = carrier
    }

    fun selectBalanceState(balanceState: SimBalanceState) {
        _balanceState.value = balanceState
    }

    fun runConnectivityTest() {
        viewModelScope.launch {
            _isRunningTest.value = true
            val carrier = _targetCarrier.value
            val balance = _balanceState.value
            val server = repository.getSelectedServerOnce()
            val docs = repository.getDocumentedServicesByCarrier(carrier.name)
            val conn = VpnRuntimeState.connectionState.value
            val isVpnRunning = conn is VpnConnectionState.Connected
            val vpnIp = (conn as? VpnConnectionState.Connected)?.vpnIp
            val tel = VpnRuntimeState.telemetry.value
            val dataUsed = tel.sessionRxBytes + tel.sessionTxBytes

            val result = labEngine.runOperatorLabTest(
                targetCarrier = carrier,
                balanceState = balance,
                server = server,
                documentedServices = docs,
                isVpnRunning = isVpnRunning,
                vpnIp = vpnIp,
                tunPacketsCount = tel.tunPacketsHandled,
                dataConsumedBytes = dataUsed
            )

            _latestResult.value = result
            repository.saveLabReport(
                result = result,
                beforeSnapshot = _beforeSnapshot.value,
                afterSnapshot = _afterSnapshot.value
            )
            _isRunningTest.value = false
            _messages.emit("Teste ${carrier.shortName} guardado no histórico do Laboratório.")
        }
    }

    fun captureBeforeVpnSnapshot() {
        viewModelScope.launch {
            if (VpnRuntimeState.connectionState.value is VpnConnectionState.Connected) {
                _messages.emit("Desligue a VPN primeiro para registar a referência ANTES da VPN.")
                return@launch
            }
            _isCapturingSnapshot.value = true
            val server = repository.getSelectedServerOnce()
            val snap = labEngine.captureVpnSnapshot(
                labelPt = "Antes da VPN",
                targetCarrier = _targetCarrier.value,
                server = server,
                isVpnRunning = false,
                vpnIp = null,
                dataUsedBytes = 0L,
                tunPackets = 0L
            )
            _beforeSnapshot.value = snap
            _isCapturingSnapshot.value = false
            _messages.emit("Referência ANTES da VPN capturada.")
        }
    }

    fun captureAfterVpnSnapshot() {
        viewModelScope.launch {
            val conn = VpnRuntimeState.connectionState.value
            if (conn !is VpnConnectionState.Connected) {
                _messages.emit("Ligue a VPN no Painel Principal antes de capturar o estado DEPOIS da VPN.")
                return@launch
            }
            _isCapturingSnapshot.value = true
            val server = repository.getSelectedServerOnce()
            val tel = VpnRuntimeState.telemetry.value
            val snap = labEngine.captureVpnSnapshot(
                labelPt = "Depois da VPN",
                targetCarrier = _targetCarrier.value,
                server = server,
                isVpnRunning = true,
                vpnIp = conn.vpnIp,
                dataUsedBytes = tel.sessionRxBytes + tel.sessionTxBytes,
                tunPackets = tel.tunPacketsHandled
            )
            _afterSnapshot.value = snap
            _isCapturingSnapshot.value = false
            _messages.emit("Estado DEPOIS da VPN capturado com sucesso.")
        }
    }

    fun addDocumentedService(
        carrier: CarrierType,
        serviceName: String,
        domainOrHost: String,
        port: Int,
        sourceReference: String,
        verifiedDate: String,
        conditionsPt: String,
        validityNotePt: String,
        isOfficiallyAuthorized: Boolean
    ) {
        viewModelScope.launch {
            repository.addDocumentedService(
                DocumentedServiceEntity(
                    carrierType = carrier.name,
                    serviceName = serviceName.trim(),
                    domainOrHost = domainOrHost.trim(),
                    port = port.coerceIn(1, 65535),
                    sourceReference = sourceReference.trim(),
                    verifiedDate = verifiedDate.trim(),
                    conditionsPt = conditionsPt.trim(),
                    validityNotePt = validityNotePt.trim(),
                    isOfficiallyAuthorized = isOfficiallyAuthorized,
                    isUserAdded = true
                )
            )
            _messages.emit("Serviço documentado adicionado para ${carrier.shortName}.")
        }
    }

    fun deleteDocumentedService(id: Long) {
        viewModelScope.launch {
            repository.deleteDocumentedService(id)
            _messages.emit("Registo removido.")
        }
    }

    fun clearLabReports() {
        viewModelScope.launch {
            repository.clearLabReports()
            _messages.emit("Histórico do Laboratório limpo.")
        }
    }

    suspend fun exportReportsCsv(outputStream: OutputStream, reports: List<ConnectivityLabReportEntity>) {
        withContext(Dispatchers.IO) {
            ReportExporter.exportCsv(outputStream, reports)
        }
        _messages.emit("Relatório CSV exportado com sucesso.")
    }

    suspend fun exportReportsPdf(outputStream: OutputStream, reports: List<ConnectivityLabReportEntity>) {
        withContext(Dispatchers.IO) {
            ReportExporter.exportPdf(outputStream, reports)
        }
        _messages.emit("Relatório PDF exportado com sucesso.")
    }

    private data class LabConfigState(
        val carrier: CarrierType,
        val balance: SimBalanceState,
        val result: LabRunResult?,
        val before: LabVpnSnapshot?,
        val after: LabVpnSnapshot?
    )

    private data class LabRuntimeSubState(
        val conn: VpnConnectionState,
        val tel: TrafficTelemetry,
        val net: NetworkProbeStatus,
        val server: ServerProfileEntity?
    )

    private data class LabDbFlags(
        val reports: List<ConnectivityLabReportEntity>,
        val docs: List<DocumentedServiceEntity>,
        val running: Boolean,
        val capturing: Boolean
    )
}
