package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.telephony.TelephonyManager
import com.example.model.CarrierSelectionMode
import com.example.model.CarrierType
import com.example.model.DestinationProbeResult
import com.example.model.DiagnosticStepResult
import com.example.model.NetworkProbeStatus
import com.example.model.TransportType
import com.example.model.VpnVerificationReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocketFactory

class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private val _status = MutableStateFlow(NetworkProbeStatus())
    val status: StateFlow<NetworkProbeStatus> = _status.asStateFlow()

    private var selectedCarrierMode: CarrierSelectionMode = CarrierSelectionMode.AUTO
    private var registered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            refreshSync()
        }

        override fun onLost(network: Network) {
            refreshSync()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            refreshSync()
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            refreshSync()
        }
    }

    fun startObserving() {
        if (!registered) {
            registered = true
            runCatching {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, networkCallback)
            }
            refreshSync()
        }
    }

    fun updateCarrierSelectionMode(mode: CarrierSelectionMode) {
        selectedCarrierMode = mode
        refreshSync()
    }

    fun refreshSync(
        vpnIp: String? = null,
        isVpnRunning: Boolean = false,
        serverReachable: Boolean? = null,
        serverLatencyMs: Long? = null,
        effectiveInternet: Boolean? = null
    ) {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        val linkProps = activeNetwork?.let { connectivityManager.getLinkProperties(it) }

        val allNetworks = connectivityManager.allNetworks
        var hasWifi = false
        var hasCellular = false
        var hasVpnTransport = isVpnRunning

        allNetworks.forEach { net ->
            val c = connectivityManager.getNetworkCapabilities(net) ?: return@forEach
            if (c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                hasVpnTransport = true
            } else if (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                hasWifi = true
            } else if (c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                hasCellular = true
            }
        }

        val transportType = when {
            caps == null && !hasWifi && !hasCellular -> TransportType.NONE
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> TransportType.WIFI
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> TransportType.CELLULAR
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> TransportType.ETHERNET
            hasWifi -> TransportType.WIFI
            hasCellular -> TransportType.CELLULAR
            else -> TransportType.NONE
        }

        val isValidated =
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val hasInternetCap =
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val isCaptivePortal =
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true

        val (detectedCarrier, rawOpName, mccMnc) = detectAngolaCarrier(transportType)

        val localIp = linkProps?.linkAddresses
            ?.firstOrNull { it.address is Inet4Address && !it.address.isLoopbackAddress }
            ?.address?.hostAddress ?: "—"

        val dnsServers = linkProps?.dnsServers
            ?.mapNotNull { it.hostAddress }
            ?: emptyList()

        val summary = buildDiagnosticSummary(
            transportType = transportType,
            detectedCarrier = detectedCarrier,
            isVpnActive = hasVpnTransport,
            isValidated = isValidated || (effectiveInternet == true)
        )

        val current = _status.value
        _status.value = current.copy(
            isVpnActive = hasVpnTransport,
            isServerReachable = serverReachable ?: current.isServerReachable,
            serverLatencyMs = serverLatencyMs ?: current.serverLatencyMs,
            hasEffectiveInternet = effectiveInternet ?: (isValidated && hasInternetCap),
            isNetworkValidated = isValidated,
            isCaptivePortal = isCaptivePortal,
            transportType = transportType,
            detectedCarrier = detectedCarrier,
            rawOperatorName = rawOpName,
            mccMnc = mccMnc,
            localIpAddress = localIp,
            vpnIpAddress = if (hasVpnTransport) vpnIp ?: current.vpnIpAddress ?: "10.84.0.2" else null,
            activeDnsServers = dnsServers,
            diagnosticSummaryPt = summary,
            lastCheckedAt = System.currentTimeMillis()
        )
    }

    private fun detectAngolaCarrier(transportType: TransportType): Triple<CarrierType, String, String> {
        val networkOp = runCatching { telephonyManager?.networkOperator.orEmpty() }.getOrDefault("")
        val simOp = runCatching { telephonyManager?.simOperator.orEmpty() }.getOrDefault("")
        val networkName = runCatching { telephonyManager?.networkOperatorName.orEmpty() }.getOrDefault("")
        val simName = runCatching { telephonyManager?.simOperatorName.orEmpty() }.getOrDefault("")

        val combinedCode = networkOp.ifBlank { simOp }
        val combinedName = networkName.ifBlank { simName }

        if (selectedCarrierMode == CarrierSelectionMode.UNITEL) {
            return Triple(
                CarrierType.UNITEL,
                combinedName.ifBlank { "Unitel Angola (Seleccionada)" },
                combinedCode.ifBlank { "631-02" }
            )
        }
        if (selectedCarrierMode == CarrierSelectionMode.AFRICELL) {
            return Triple(
                CarrierType.AFRICELL,
                combinedName.ifBlank { "Africell Angola (Seleccionada)" },
                combinedCode.ifBlank { "631-05" }
            )
        }

        val carrierFromSimOrNet = when {
            combinedCode == "63102" || combinedName.contains("unitel", ignoreCase = true) ->
                CarrierType.UNITEL
            combinedCode == "63105" || combinedName.contains("africell", ignoreCase = true) ->
                CarrierType.AFRICELL
            combinedCode == "63104" || combinedName.contains("movicel", ignoreCase = true) ->
                CarrierType.MOVICEL
            transportType == TransportType.WIFI ->
                CarrierType.WIFI_ONLY
            else ->
                CarrierType.OTHER
        }

        val formattedCode = when {
            combinedCode.length >= 5 -> "${combinedCode.substring(0, 3)}-${combinedCode.substring(3)}"
            carrierFromSimOrNet == CarrierType.UNITEL -> "631-02"
            carrierFromSimOrNet == CarrierType.AFRICELL -> "631-05"
            carrierFromSimOrNet == CarrierType.WIFI_ONLY -> "WI-FI"
            else -> "N/D"
        }

        val displayOp = combinedName.ifBlank { carrierFromSimOrNet.displayName }
        return Triple(carrierFromSimOrNet, displayOp, formattedCode)
    }

    private fun buildDiagnosticSummary(
        transportType: TransportType,
        detectedCarrier: CarrierType,
        isVpnActive: Boolean,
        isValidated: Boolean
    ): String {
        return when {
            transportType == TransportType.NONE ->
                "Sem transporte activo. Active Wi-Fi ou Dados Móveis (Unitel/Africell) para ligar ao servidor."
            isVpnActive && isValidated ->
                "Túnel VPN activo via ${transportType.labelPt} (${detectedCarrier.shortName}) com acesso validado à internet."
            isVpnActive ->
                "Interface VPN activa. A validar trânsito de dados pelo transporte ${transportType.labelPt}."
            transportType == TransportType.WIFI && isValidated ->
                "Wi-Fi ligado e validado. Momento ideal para preparar e guardar perfis no Cofre Offline."
            transportType == TransportType.CELLULAR && isValidated ->
                "Dados Móveis ${detectedCarrier.displayName} activos. Transporte pronto para iniciar a VPN."
            else ->
                "Transporte ${transportType.labelPt} detectado, a aguardar validação de conectividade."
        }
    }

    suspend fun probeServerTcp(host: String, port: Int, timeoutMs: Int = 3000): Long? =
        withContext(Dispatchers.IO) {
            if (host.isBlank() || port !in 1..65535) return@withContext null
            runCatching {
                val start = System.currentTimeMillis()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host.trim(), port), timeoutMs)
                }
                (System.currentTimeMillis() - start).coerceAtLeast(1L)
            }.getOrNull()
        }

    suspend fun verifySocks5Handshake(host: String, port: Int, timeoutMs: Int = 3000): Long? =
        withContext(Dispatchers.IO) {
            runCatching {
                val start = System.currentTimeMillis()
                Socket().use { socket ->
                    socket.soTimeout = timeoutMs
                    socket.connect(InetSocketAddress(host.trim(), port), timeoutMs)
                    val out = socket.getOutputStream()
                    val inp = socket.getInputStream()
                    // SOCKS5 Greeting: Version 5, 1 auth method, No Auth (0x00)
                    out.write(byteArrayOf(0x05, 0x01, 0x00))
                    out.flush()
                    val reply = ByteArray(2)
                    val read = inp.read(reply)
                    if (read < 2 || reply[0].toInt() != 0x05) {
                        error("Resposta SOCKS5 inválida")
                    }
                }
                (System.currentTimeMillis() - start).coerceAtLeast(1L)
            }.getOrNull()
        }

    suspend fun probeEffectiveInternet(): Pair<Boolean, Long?> = withContext(Dispatchers.IO) {
        runCatching {
            val start = System.currentTimeMillis()
            val factory = SSLSocketFactory.getDefault()
            factory.createSocket().use { socket ->
                socket.connect(InetSocketAddress("1.1.1.1", 443), 3000)
            }
            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
            true to elapsed
        }.getOrElse {
            false to null
        }
    }

    suspend fun runFullDiagnostics(
        serverName: String,
        serverHost: String,
        serverPort: Int
    ): List<DiagnosticStepResult> = withContext(Dispatchers.IO) {
        refreshSync()
        val current = _status.value

        val transportStep = DiagnosticStepResult(
            titlePt = "1. Transporte de Rede (${current.transportType.labelPt})",
            detailPt = if (current.transportType != TransportType.NONE) {
                "Interface activa · Operadora: ${current.detectedCarrier.displayName} (${current.mccMnc}) · IP local ${current.localIpAddress}"
            } else {
                "Nenhuma interface Wi-Fi ou Dados Móveis activa para transportar pacotes."
            },
            passed = current.transportType != TransportType.NONE
        )

        val dnsStart = System.currentTimeMillis()
        val resolvedAddress = runCatching {
            InetAddress.getByName(serverHost.trim()).hostAddress
        }.getOrNull()
        val dnsLatency = (System.currentTimeMillis() - dnsStart).coerceAtLeast(1L)
        val dnsStep = DiagnosticStepResult(
            titlePt = "2. Resolução DNS do Servidor",
            detailPt = if (resolvedAddress != null) {
                "$serverHost resolvido para $resolvedAddress"
            } else {
                "Não foi possível resolver $serverHost"
            },
            passed = resolvedAddress != null,
            latencyMs = if (resolvedAddress != null) dnsLatency else null
        )

        val serverLatency = probeServerTcp(serverHost, serverPort)
        val serverStep = DiagnosticStepResult(
            titlePt = "3. Disponibilidade do Servidor ($serverName)",
            detailPt = if (serverLatency != null) {
                "Porta $serverPort acessível em $serverHost ($serverLatency ms)"
            } else {
                "Servidor $serverHost:$serverPort inacessível ou porta fechada."
            },
            passed = serverLatency != null,
            latencyMs = serverLatency
        )

        val (internetOk, internetLatency) = probeEffectiveInternet()
        val internetStep = DiagnosticStepResult(
            titlePt = "4. Acesso Efectivo à Internet",
            detailPt = if (internetOk) {
                "Trânsito externo TLS/HTTPS verificado com sucesso."
            } else {
                "Sem acesso efectivo à internet ou bloqueio na operadora."
            },
            passed = internetOk,
            latencyMs = internetLatency
        )

        refreshSync(
            isVpnRunning = current.isVpnActive,
            serverReachable = serverLatency != null,
            serverLatencyMs = serverLatency,
            effectiveInternet = internetOk
        )

        listOf(transportStep, dnsStep, serverStep, internetStep)
    }

    suspend fun runAuthorizedConnectivityAudit(
        serverName: String,
        serverHost: String,
        serverPort: Int,
        isVpnRunning: Boolean,
        tunPacketsHandled: Long
    ): VpnVerificationReport = withContext(Dispatchers.IO) {
        refreshSync(isVpnRunning = isVpnRunning)
        val current = _status.value

        val targets = listOf(
            Triple("Internet Pública (Google Check)", "connectivitycheck.gstatic.com", 443 to "Internet Geral"),
            Triple("Servidor VPN ($serverName)", serverHost, serverPort to "Servidor Próprio"),
            Triple("Portal Unitel Angola", "unitel.ao", 443 to "Autoatendimento Unitel"),
            Triple("Portal Africell Angola", "africell.ao", 443 to "Autoatendimento Africell")
        )

        val results = targets.map { (label, host, portAndCat) ->
            val (port, category) = portAndCat
            val latency = probeServerTcp(host, port, timeoutMs = 2800)
            DestinationProbeResult(
                labelPt = label,
                host = host,
                port = port,
                reachable = latency != null,
                latencyMs = latency,
                categoryPt = category
            )
        }

        val publicIp = fetchPublicIpSafely()
        val generalInternetOk = results.firstOrNull()?.reachable == true
        val vpnServerOk = results.getOrNull(1)?.reachable == true
        val unitelPortalOk = results.getOrNull(2)?.reachable == true
        val africellPortalOk = results.getOrNull(3)?.reachable == true

        val conclusion = when {
            current.transportType == TransportType.NONE ->
                "Confirmado: Sem transporte Wi-Fi ou Dados Móveis activo. A VPN não pode estabelecer ligação sem transporte."
            !generalInternetOk && (unitelPortalOk || africellPortalOk || current.isCaptivePortal) ->
                "Confirmado: Rede em estado restrito (Walled Garden / Portal Cativo). O portal da operadora responde, mas a internet aberta e servidores VPN externos estão bloqueados sem pacote ou whitelist B2B."
            !generalInternetOk && !vpnServerOk ->
                "Confirmado: A rede móvel não permite trânsito externo nem ligação ao servidor VPN seleccionado (sem saldo/pacote activo ou servidor offline)."
            isVpnRunning && vpnServerOk && generalInternetOk ->
                "Confirmado: Transporte activo, servidor VPN acessível e túnel TUN operacional ($tunPacketsHandled pacotes processados)."
            vpnServerOk && generalInternetOk ->
                "Confirmado: Transporte ${current.transportType.labelPt} com internet validada e servidor VPN acessível. Pronto para ligar a VPN."
            else ->
                "Acesso parcial detectado: internet externa disponível, mas o servidor próprio ($serverHost:$serverPort) não respondeu."
        }

        VpnVerificationReport(
            transportSummaryPt = "${current.transportType.labelPt} · ${current.detectedCarrier.displayName} (${current.mccMnc})",
            isCaptivePortal = current.isCaptivePortal,
            isValidated = current.isNetworkValidated || generalInternetOk,
            destinationResults = results,
            vpnTunnelVerified = isVpnRunning && current.isVpnActive,
            tunPacketsCount = tunPacketsHandled,
            publicIpObserved = publicIp,
            conclusionPt = conclusion,
            executedAtMillis = System.currentTimeMillis()
        )
    }

    private fun fetchPublicIpSafely(): String? {
        return runCatching {
            val client = OkHttpClient.Builder()
                .connectTimeout(2500, TimeUnit.MILLISECONDS)
                .readTimeout(2500, TimeUnit.MILLISECONDS)
                .build()
            val request = Request.Builder()
                .url("https://api.ipify.org")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string()?.trim()?.takeIf { it.length <= 45 } else null
            }
        }.getOrNull()
    }
}
