package com.example.vpn

import com.example.model.TrafficTelemetry
import com.example.model.VpnConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VpnRuntimeState {

    private val _connectionState = MutableStateFlow<VpnConnectionState>(VpnConnectionState.Disconnected)
    val connectionState: StateFlow<VpnConnectionState> = _connectionState.asStateFlow()

    private val _telemetry = MutableStateFlow(TrafficTelemetry())
    val telemetry: StateFlow<TrafficTelemetry> = _telemetry.asStateFlow()

    private val _activeServerId = MutableStateFlow<Long?>(null)
    val activeServerId: StateFlow<Long?> = _activeServerId.asStateFlow()

    fun setState(state: VpnConnectionState, serverId: Long? = _activeServerId.value) {
        _connectionState.value = state
        _activeServerId.value = when (state) {
            is VpnConnectionState.Disconnected, is VpnConnectionState.Error -> null
            else -> serverId
        }
        if (state is VpnConnectionState.Disconnected) {
            _telemetry.value = TrafficTelemetry()
        }
    }

    fun updateTelemetry(
        downloadSpeedBps: Long,
        uploadSpeedBps: Long,
        sessionRxBytes: Long,
        sessionTxBytes: Long,
        durationSeconds: Long,
        tunPacketsHandled: Long
    ) {
        val current = _telemetry.value
        val dlKb = (downloadSpeedBps / 1024f).coerceAtLeast(0f)
        val ulKb = (uploadSpeedBps / 1024f).coerceAtLeast(0f)

        _telemetry.value = current.copy(
            downloadSpeedBps = downloadSpeedBps,
            uploadSpeedBps = uploadSpeedBps,
            sessionRxBytes = sessionRxBytes,
            sessionTxBytes = sessionTxBytes,
            sessionDurationSeconds = durationSeconds,
            tunPacketsHandled = tunPacketsHandled,
            downloadHistory = (current.downloadHistory.drop(1) + dlKb),
            uploadHistory = (current.uploadHistory.drop(1) + ulKb)
        )
    }
}
