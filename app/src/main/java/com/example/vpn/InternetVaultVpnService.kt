package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.TrafficStats
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.InternetVaultApp
import com.example.MainActivity
import com.example.R
import com.example.data.local.ServerProfileEntity
import com.example.model.TransportType
import com.example.model.TunnelMode
import com.example.model.VpnConnectionState
import com.example.model.VpnProtocol
import com.example.util.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class InternetVaultVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.example.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.ACTION_DISCONNECT"
        const val EXTRA_SERVER_ID = "extra_server_id"

        private const val CHANNEL_ID = "internet_vault_vpn_channel"
        private const val NOTIFICATION_ID = 4041
        private const val VPN_LOCAL_IP = "10.84.0.2"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tunInterface: ParcelFileDescriptor? = null
    private var forwarderJob: Job? = null
    private var telemetryJob: Job? = null
    private var currentSessionId: Long? = null
    private var connectedAtMillis: Long = 0L

    private var lastRxBytes: Long = 0L
    private var lastTxBytes: Long = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                disconnectAndStop("CONCLUÍDA")
                return START_NOT_STICKY
            }
            ACTION_CONNECT, null -> {
                val serverId = intent?.getLongExtra(EXTRA_SERVER_ID, -1L) ?: -1L
                startForegroundWithNotification("A preparar túnel Internet Vault...")
                serviceScope.launch {
                    connectToServer(serverId)
                }
                return START_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun connectToServer(serverId: Long) {
        val app = application as InternetVaultApp
        val repo = app.repository
        val networkMonitor = app.networkMonitor

        VpnRuntimeState.setState(VpnConnectionState.Preparing, serverId.takeIf { it > 0 })

        networkMonitor.refreshSync(isVpnRunning = false)
        val netStatus = networkMonitor.status.value
        if (netStatus.transportType == TransportType.NONE) {
            val msg = "Sem ligação de transporte (Wi-Fi ou Dados Móveis). Active uma rede para ligar ao servidor."
            VpnRuntimeState.setState(VpnConnectionState.Error(msg))
            networkMonitor.refreshSync(isVpnRunning = false)
            stopSelfCleanly()
            return
        }

        val server: ServerProfileEntity? = if (serverId > 0L) {
            repo.getServerById(serverId)
        } else {
            repo.getSelectedServerOnce()
        }

        if (server == null) {
            VpnRuntimeState.setState(VpnConnectionState.Error("Nenhum servidor seleccionado no Cofre."))
            stopSelfCleanly()
            return
        }

        VpnRuntimeState.setState(VpnConnectionState.Connecting, server.id)
        updateNotification("A verificar ${server.name}...")

        val isFullTunnel = server.tunnelMode == TunnelMode.FULL_ENDPOINT_TUNNEL.name
        val latencyMs: Long? = when {
            server.protocol == VpnProtocol.SOCKS5_PROXY.name && isFullTunnel ->
                networkMonitor.verifySocks5Handshake(server.host, server.port)
            isFullTunnel ->
                networkMonitor.probeServerTcp(server.host, server.port)
            else ->
                networkMonitor.probeServerTcp(server.host, server.port)
                    ?: networkMonitor.probeServerTcp(server.dnsPrimary, 443)
        }

        if (isFullTunnel && latencyMs == null) {
            val errorMsg = "Servidor próprio ${server.host}:${server.port} indisponível. Verifique o endereço ou mude para Cofre DNS."
            VpnRuntimeState.setState(VpnConnectionState.Error(errorMsg))
            networkMonitor.refreshSync(
                isVpnRunning = false,
                serverReachable = false,
                serverLatencyMs = null
            )
            stopSelfCleanly()
            return
        }

        repo.updateServerLatency(server.id, latencyMs)

        val descriptor = buildTunInterface(server)
        if (descriptor == null) {
            VpnRuntimeState.setState(
                VpnConnectionState.Error("Falha ao criar interface TUN. Verifique a permissão de VPN do sistema.")
            )
            stopSelfCleanly()
            return
        }

        tunInterface = descriptor
        connectedAtMillis = System.currentTimeMillis()
        VpnRuntimeState.setState(
            VpnConnectionState.Connected(
                vpnIp = VPN_LOCAL_IP,
                serverName = server.name,
                connectedAtMillis = connectedAtMillis
            ),
            server.id
        )

        networkMonitor.refreshSync(
            vpnIp = VPN_LOCAL_IP,
            isVpnRunning = true,
            serverReachable = latencyMs != null,
            serverLatencyMs = latencyMs,
            effectiveInternet = true
        )

        currentSessionId = repo.startSession(
            server = server,
            carrierType = netStatus.detectedCarrier.name,
            transportType = netStatus.transportType.name,
            vpnIp = VPN_LOCAL_IP
        )

        val forwarder = TunPacketForwarder(this@InternetVaultVpnService, server.dnsPrimary)
        forwarderJob?.cancel()
        forwarderJob = serviceScope.launch {
            forwarder.runLoop(descriptor)
        }

        startTelemetryLoop(server, forwarder)
    }

    private fun buildTunInterface(server: ServerProfileEntity): ParcelFileDescriptor? {
        return runCatching {
            val builder = Builder()
                .setSession("Internet Vault · ${server.name}")
                .addAddress(VPN_LOCAL_IP, 32)
                .setMtu(server.mtu.coerceIn(1280, 1500))

            runCatching { builder.addDnsServer(server.dnsPrimary) }
            if (server.dnsSecondary.isNotBlank()) {
                runCatching { builder.addDnsServer(server.dnsSecondary) }
            }

            // Route tunnel subnet and configured DNS through TUN so real DNS forwarding occurs
            runCatching { builder.addRoute("10.84.0.0", 24) }
            runCatching { builder.addRoute(server.dnsPrimary, 32) }

            // Allow our own app's direct diagnostics to probe underlying carrier/Wi-Fi transport
            runCatching { builder.addDisallowedApplication(packageName) }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            builder.establish()
        }.getOrNull()
    }

    private fun startTelemetryLoop(server: ServerProfileEntity, forwarder: TunPacketForwarder) {
        telemetryJob?.cancel()
        val initialRx = TrafficStats.getTotalRxBytes().takeIf { it >= 0 } ?: 0L
        val initialTx = TrafficStats.getTotalTxBytes().takeIf { it >= 0 } ?: 0L
        lastRxBytes = initialRx
        lastTxBytes = initialTx

        telemetryJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                val nowRx = TrafficStats.getTotalRxBytes().takeIf { it >= 0 } ?: initialRx
                val nowTx = TrafficStats.getTotalTxBytes().takeIf { it >= 0 } ?: initialTx

                val deltaRx = (nowRx - lastRxBytes).coerceAtLeast(0L)
                val deltaTx = (nowTx - lastTxBytes).coerceAtLeast(0L)
                lastRxBytes = nowRx
                lastTxBytes = nowTx

                val sessionRx = ((nowRx - initialRx).coerceAtLeast(0L)) + forwarder.rxBytes.get()
                val sessionTx = ((nowTx - initialTx).coerceAtLeast(0L)) + forwarder.txBytes.get()
                val duration = ((System.currentTimeMillis() - connectedAtMillis) / 1000L).coerceAtLeast(0L)

                VpnRuntimeState.updateTelemetry(
                    downloadSpeedBps = deltaRx,
                    uploadSpeedBps = deltaTx,
                    sessionRxBytes = sessionRx,
                    sessionTxBytes = sessionTx,
                    durationSeconds = duration,
                    tunPacketsHandled = forwarder.packetsHandled.get()
                )

                val statusText = "${server.name} · ${VPN_LOCAL_IP} · ↓${FormatUtils.formatSpeed(deltaRx)} ↑${FormatUtils.formatSpeed(deltaTx)}"
                updateNotification(statusText)
            }
        }
    }

    private fun disconnectAndStop(sessionStatus: String) {
        forwarderJob?.cancel()
        telemetryJob?.cancel()

        val telemetry = VpnRuntimeState.telemetry.value
        val sessionId = currentSessionId
        currentSessionId = null

        runCatching { tunInterface?.close() }
        tunInterface = null

        VpnRuntimeState.setState(VpnConnectionState.Disconnected)

        val app = application as InternetVaultApp
        app.networkMonitor.refreshSync(vpnIp = null, isVpnRunning = false)

        if (sessionId != null) {
            serviceScope.launch {
                app.repository.finishSession(
                    sessionId = sessionId,
                    durationSeconds = telemetry.sessionDurationSeconds,
                    rxBytes = telemetry.sessionRxBytes,
                    txBytes = telemetry.sessionTxBytes,
                    status = sessionStatus
                )
            }
        }

        stopSelfCleanly()
    }

    private fun stopSelfCleanly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onRevoke() {
        disconnectAndStop("REVOGADA")
        super.onRevoke()
    }

    override fun onDestroy() {
        forwarderJob?.cancel()
        telemetryJob?.cancel()
        runCatching { tunInterface?.close() }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.vpn_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.vpn_notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification(contentText: String) {
        val notification = buildNotification(contentText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            100,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val disconnectIntent = PendingIntent.getService(
            this,
            101,
            Intent(this, InternetVaultVpnService::class.java).apply { action = ACTION_DISCONNECT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(getString(R.string.vpn_notification_title_connected))
            .setContentText(contentText)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.vpn_notification_action_disconnect),
                disconnectIntent
            )
            .build()
    }
}
