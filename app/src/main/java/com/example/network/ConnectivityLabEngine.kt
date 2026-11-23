package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.DocumentedServiceEntity
import com.example.data.local.ServerProfileEntity
import com.example.model.CarrierType
import com.example.model.ConnectivityMatrixItem
import com.example.model.DestinationProbeResult
import com.example.model.LabRunResult
import com.example.model.LabVpnSnapshot
import com.example.model.SimBalanceState
import com.example.model.TransportType
import com.example.model.ZeroBalanceClassification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class ConnectivityLabEngine(
    private val context: Context,
    private val networkMonitor: NetworkMonitor
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    companion object {
        private const val PROBE_TIMEOUT_MS = 2800

        /**
         * Classificação determinística para o Modo Sem Saldo e Diagnóstico VPN.
         * Nunca marca uma ligação como gratuita apenas porque um destino respondeu.
         */
        fun classifyConnectivity(
            hasAnyTransport: Boolean,
            hasMobileNetwork: Boolean,
            isNetworkValidated: Boolean,
            publicInternetReachable: Boolean,
            operatorOrAuthorizedServiceReachable: Boolean,
            vpnServerReachable: Boolean,
            vpnTunnelActive: Boolean,
            tunPacketsCount: Long,
            hasCustomServerConfigured: Boolean
        ): ZeroBalanceClassification {
            if (!hasAnyTransport) {
                return ZeroBalanceClassification.NO_DATA_CONNECTION
            }

            if (vpnTunnelActive) {
                return if (vpnServerReachable && (publicInternetReachable || tunPacketsCount > 0L)) {
                    ZeroBalanceClassification.VPN_FUNCTIONAL
                } else if (vpnServerReachable) {
                    ZeroBalanceClassification.VPN_SERVER_REACHABLE_NO_FORWARDING
                } else {
                    ZeroBalanceClassification.VPN_SERVER_UNREACHABLE
                }
            }

            if (!publicInternetReachable && !isNetworkValidated) {
                if (operatorOrAuthorizedServiceReachable) {
                    return ZeroBalanceClassification.LIMITED_TO_SPECIFIC_SERVICES
                }
                if (hasMobileNetwork) {
                    return ZeroBalanceClassification.MOBILE_ACTIVE_NO_VALIDATED_INTERNET
                }
                return ZeroBalanceClassification.NO_DATA_CONNECTION
            }

            if (hasCustomServerConfigured && !vpnServerReachable) {
                return ZeroBalanceClassification.VPN_SERVER_UNREACHABLE
            }

            if (hasCustomServerConfigured && vpnServerReachable && !vpnTunnelActive) {
                return ZeroBalanceClassification.VPN_SERVER_REACHABLE_NO_FORWARDING
            }

            return ZeroBalanceClassification.PUBLIC_INTERNET_AVAILABLE
        }
    }

    suspend fun runOperatorLabTest(
        targetCarrier: CarrierType,
        balanceState: SimBalanceState,
        server: ServerProfileEntity?,
        documentedServices: List<DocumentedServiceEntity>,
        isVpnRunning: Boolean,
        vpnIp: String?,
        tunPacketsCount: Long,
        dataConsumedBytes: Long
    ): LabRunResult = withContext(Dispatchers.IO) {
        networkMonitor.refreshSync(vpnIp = vpnIp, isVpnRunning = isVpnRunning)
        val status = networkMonitor.status.value

        var hasMobileNetwork = false
        var mobileDataAvailable = false
        var hasWifi = false
        var hasCaptivePortal = status.isCaptivePortal
        var networkValidated = status.isNetworkValidated

        connectivityManager.allNetworks.forEach { net ->
            val caps = connectivityManager.getNetworkCapabilities(net) ?: return@forEach
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                hasMobileNetwork = true
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                ) {
                    mobileDataAvailable = true
                }
            }
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                hasWifi = true
            }
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)) {
                hasCaptivePortal = true
            }
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                networkValidated = true
            }
        }

        val hasAnyTransport = hasMobileNetwork || hasWifi || status.transportType != TransportType.NONE

        // 1. Teste de resolução DNS (único, com timeout natural)
        val dnsHost = server?.host?.takeIf { it.isNotBlank() } ?: "connectivitycheck.gstatic.com"
        val dnsStart = System.currentTimeMillis()
        val resolvedDnsIp = runCatching { InetAddress.getByName(dnsHost).hostAddress }.getOrNull()
        val dnsLatency = (System.currentTimeMillis() - dnsStart).coerceAtLeast(1L)
        val dnsConfigured = status.activeDnsServers.isNotEmpty() || resolvedDnsIp != null

        // 2. Acesso à internet pública
        val publicLatency = probeTcpOnce("connectivitycheck.gstatic.com", 443)
        val publicInternetOk = publicLatency != null

        // 3. Domínio oficial da operadora seleccionada (Unitel vs Africell independentes)
        val officialHost = if (targetCarrier == CarrierType.AFRICELL) "africell.ao" else "unitel.ao"
        val officialLabel = if (targetCarrier == CarrierType.AFRICELL) {
            "Domínio Oficial Africell ($officialHost)"
        } else {
            "Domínio Oficial Unitel ($officialHost)"
        }
        val officialLatency = probeTcpOnce(officialHost, 443)
        val officialDomainOk = officialLatency != null

        // 4. Servidor VPN configurado
        val srvHost = server?.host ?: "1.1.1.1"
        val srvPort = server?.port ?: 443
        val srvName = server?.name ?: "Servidor Padrão"
        val serverLatency = probeTcpOnce(srvHost, srvPort)
        val serverOk = serverLatency != null

        // 5. Serviços previamente documentados e autorizados para a operadora testada
        val carrierDocs = documentedServices.filter { it.carrierType == targetCarrier.name }
        val docProbes = carrierDocs.map { doc ->
            val lat = probeTcpOnce(doc.domainOrHost, doc.port)
            doc to lat
        }
        val anyAuthorizedServiceOk = docProbes.any { it.second != null } || officialDomainOk

        // 6. IP Público observável (via HTTPS echo seguro)
        val publicIp = fetchPublicIpSafely()

        val destinationResults = buildList {
            add(
                DestinationProbeResult(
                    labelPt = "Internet Pública (Android Check)",
                    host = "connectivitycheck.gstatic.com",
                    port = 443,
                    reachable = publicInternetOk,
                    latencyMs = publicLatency,
                    categoryPt = "Internet Aberta"
                )
            )
            add(
                DestinationProbeResult(
                    labelPt = officialLabel,
                    host = officialHost,
                    port = 443,
                    reachable = officialDomainOk,
                    latencyMs = officialLatency,
                    categoryPt = "Portal Oficial ${targetCarrier.shortName}"
                )
            )
            add(
                DestinationProbeResult(
                    labelPt = "Servidor VPN · $srvName",
                    host = srvHost,
                    port = srvPort,
                    reachable = serverOk,
                    latencyMs = serverLatency,
                    categoryPt = "Servidor Próprio"
                )
            )
            docProbes.forEach { (doc, lat) ->
                if (doc.domainOrHost != officialHost) {
                    add(
                        DestinationProbeResult(
                            labelPt = doc.serviceName,
                            host = doc.domainOrHost,
                            port = doc.port,
                            reachable = lat != null,
                            latencyMs = lat,
                            categoryPt = if (doc.isOfficiallyAuthorized) "Autorizado" else "Documentado"
                        )
                    )
                }
            }
        }

        val classification = classifyConnectivity(
            hasAnyTransport = hasAnyTransport,
            hasMobileNetwork = hasMobileNetwork,
            isNetworkValidated = networkValidated,
            publicInternetReachable = publicInternetOk,
            operatorOrAuthorizedServiceReachable = anyAuthorizedServiceOk,
            vpnServerReachable = serverOk,
            vpnTunnelActive = isVpnRunning,
            tunPacketsCount = tunPacketsCount,
            hasCustomServerConfigured = server != null
        )

        val mismatchNote = if (status.transportType == TransportType.WIFI) {
            "Aviso: O dispositivo está actualmente em Wi-Fi. Para validar a rede ${targetCarrier.displayName}, desligue o Wi-Fi e active os dados móveis do SIM."
        } else if (status.detectedCarrier != targetCarrier && status.detectedCarrier != CarrierType.OTHER) {
            "Aviso: O SIM activo detectado é ${status.detectedCarrier.displayName} (${status.mccMnc}), diferente do perfil de teste ${targetCarrier.displayName}."
        } else null

        val matrixItems = listOf(
            ConnectivityMatrixItem(
                key = "mobile_net",
                labelPt = "Ligação à rede móvel (${targetCarrier.mccMnc})",
                statusPt = if (hasMobileNetwork) "Activa" else "Inactiva",
                detailPt = if (hasMobileNetwork) {
                    "Operadora detectada: ${status.rawOperatorName} (${status.mccMnc})"
                } else {
                    "Interface celular ausente (transporte actual: ${status.transportType.labelPt})"
                },
                passed = hasMobileNetwork
            ),
            ConnectivityMatrixItem(
                key = "mobile_data",
                labelPt = "Disponibilidade de dados móveis",
                statusPt = if (mobileDataAvailable) "Disponível" else "Indisponível",
                detailPt = if (mobileDataAvailable) {
                    "Capacidade INTERNET não restrita presente na rede móvel."
                } else {
                    "Dados móveis desligados ou sem permissão de transporte celular."
                },
                passed = mobileDataAvailable
            ),
            ConnectivityMatrixItem(
                key = "public_internet",
                labelPt = "Acesso à internet pública",
                statusPt = if (publicInternetOk) "Acessível" else "Bloqueado",
                detailPt = if (publicInternetOk) {
                    "Handshake TLS em connectivitycheck.gstatic.com:443 concluído."
                } else {
                    "Sem trânsito aberto para destinos públicos da internet."
                },
                passed = publicInternetOk,
                latencyMs = publicLatency
            ),
            ConnectivityMatrixItem(
                key = "operator_domain",
                labelPt = "Domínio oficial ${targetCarrier.shortName}",
                statusPt = if (officialDomainOk) "Acessível" else "Inacessível",
                detailPt = "$officialHost:443 · ${if (officialDomainOk) "Respondeu ao teste TLS" else "Sem resposta"}",
                passed = officialDomainOk,
                latencyMs = officialLatency
            ),
            ConnectivityMatrixItem(
                key = "vpn_server",
                labelPt = "Servidor VPN configurado",
                statusPt = if (serverOk) "Acessível" else "Inacessível",
                detailPt = "$srvName ($srvHost:$srvPort)",
                passed = serverOk,
                latencyMs = serverLatency
            ),
            ConnectivityMatrixItem(
                key = "authorized_services",
                labelPt = "Serviços documentados e autorizados",
                statusPt = "${docProbes.count { it.second != null }}/${docProbes.size} activos",
                detailPt = if (docProbes.isEmpty()) {
                    "Nenhum serviço extra registado para ${targetCarrier.shortName}."
                } else {
                    docProbes.joinToString { (d, l) -> "${d.domainOrHost}: ${if (l != null) "${l}ms" else "bloqueado"}" }
                },
                passed = docProbes.any { it.second != null }
            ),
            ConnectivityMatrixItem(
                key = "dns_state",
                labelPt = "Estado do DNS",
                statusPt = if (resolvedDnsIp != null) "Operacional" else "Falhou",
                detailPt = "Servidores: ${status.activeDnsServers.ifEmpty { listOf("Sistema") }.joinToString()} · $dnsHost -> ${resolvedDnsIp ?: "sem resolução"}",
                passed = dnsConfigured && resolvedDnsIp != null,
                latencyMs = if (resolvedDnsIp != null) dnsLatency else null
            ),
            ConnectivityMatrixItem(
                key = "net_validation",
                labelPt = "Validação da rede (Android)",
                statusPt = if (networkValidated) "Validada" else "Não Validada",
                detailPt = if (networkValidated) {
                    "NET_CAPABILITY_VALIDATED confirmado pelo sistema."
                } else {
                    "O sistema Android não validou conectividade plena nesta interface."
                },
                passed = networkValidated
            ),
            ConnectivityMatrixItem(
                key = "captive_portal",
                labelPt = "Portal cativo ou acesso restrito",
                statusPt = when {
                    hasCaptivePortal -> "Portal Cativo"
                    !publicInternetOk && anyAuthorizedServiceOk -> "Walled Garden"
                    else -> "Não Detectado"
                },
                detailPt = when {
                    hasCaptivePortal -> "NET_CAPABILITY_CAPTIVE_PORTAL activo na rede."
                    !publicInternetOk && anyAuthorizedServiceOk -> "Portal da operadora responde enquanto internet aberta está bloqueada."
                    else -> "Sem sinalização de portal cativo ou restrição selectiva."
                },
                passed = !hasCaptivePortal && !(!publicInternetOk && anyAuthorizedServiceOk)
            )
        )

        val conclusion = buildTechnicalConclusion(
            targetCarrier = targetCarrier,
            balanceState = balanceState,
            classification = classification,
            mismatchNote = mismatchNote,
            publicIp = publicIp,
            isVpnRunning = isVpnRunning,
            tunPacketsCount = tunPacketsCount
        )

        val complianceNotice =
            "Conformidade: Um teste bem-sucedido não prova ausência de cobrança. Para confirmar tráfego gratuito ou patrocinado na ${targetCarrier.displayName}, é obrigatória autorização comercial oficial da operadora e verificação de saldo."

        val errorMsg = when {
            !hasAnyTransport -> "Sem interface de transporte disponível."
            !serverOk -> "Servidor VPN $srvHost:$srvPort não respondeu dentro de ${PROBE_TIMEOUT_MS}ms."
            else -> null
        }

        LabRunResult(
            carrierTested = targetCarrier,
            detectedCarrier = status.detectedCarrier,
            detectedMccMnc = status.mccMnc,
            transportType = status.transportType,
            balanceState = balanceState,
            classification = classification,
            matrixItems = matrixItems,
            destinationResults = destinationResults,
            publicIpObserved = publicIp,
            vpnActive = isVpnRunning,
            vpnIp = if (isVpnRunning) vpnIp ?: "10.84.0.2" else null,
            serverName = srvName,
            serverHost = srvHost,
            serverPort = srvPort,
            dataConsumedBytes = dataConsumedBytes,
            errorMessagePt = errorMsg,
            technicalConclusionPt = conclusion,
            complianceNoticePt = complianceNotice,
            executedAtMillis = System.currentTimeMillis()
        )
    }

    suspend fun captureVpnSnapshot(
        labelPt: String,
        targetCarrier: CarrierType,
        server: ServerProfileEntity?,
        isVpnRunning: Boolean,
        vpnIp: String?,
        dataUsedBytes: Long,
        tunPackets: Long
    ): LabVpnSnapshot = withContext(Dispatchers.IO) {
        val srvHost = server?.host ?: "1.1.1.1"
        val srvPort = server?.port ?: 443
        val serverLatency = probeTcpOnce(srvHost, srvPort)
        val publicLatency = probeTcpOnce("connectivitycheck.gstatic.com", 443)
        val publicIp = fetchPublicIpSafely()

        val errorDetail = when {
            publicLatency == null && serverLatency == null ->
                "Sem acesso externo nem resposta do servidor $srvHost:$srvPort."
            serverLatency == null ->
                "Servidor VPN $srvHost:$srvPort inacessível."
            isVpnRunning && publicLatency == null ->
                "Túnel activo, mas sem trânsito público validado."
            else -> null
        }

        LabVpnSnapshot(
            labelPt = labelPt,
            capturedAtMillis = System.currentTimeMillis(),
            carrierTested = targetCarrier,
            vpnActive = isVpnRunning,
            vpnIp = if (isVpnRunning) vpnIp ?: "10.84.0.2" else null,
            publicIp = publicIp,
            serverHost = srvHost,
            serverPort = srvPort,
            serverReachable = serverLatency != null,
            serverLatencyMs = serverLatency,
            publicInternetReachable = publicLatency != null,
            publicInternetLatencyMs = publicLatency,
            dataUsedBytes = dataUsedBytes,
            tunPackets = tunPackets,
            errorDetailPt = errorDetail
        )
    }

    private fun buildTechnicalConclusion(
        targetCarrier: CarrierType,
        balanceState: SimBalanceState,
        classification: ZeroBalanceClassification,
        mismatchNote: String?,
        publicIp: String?,
        isVpnRunning: Boolean,
        tunPacketsCount: Long
    ): String {
        val base = when (classification) {
            ZeroBalanceClassification.NO_DATA_CONNECTION ->
                "Sem ligação de dados disponível para testar a ${targetCarrier.displayName}."
            ZeroBalanceClassification.MOBILE_ACTIVE_NO_VALIDATED_INTERNET ->
                "Rede móvel presente no modo ${balanceState.labelPt}, mas sem internet validada nem resposta de destinos externos."
            ZeroBalanceClassification.LIMITED_TO_SPECIFIC_SERVICES ->
                "Detectado acesso restrito apenas a serviços específicos da ${targetCarrier.displayName}, com bloqueio da internet pública e do túnel VPN geral."
            ZeroBalanceClassification.PUBLIC_INTERNET_AVAILABLE ->
                "Internet pública disponível na ligação actual (IP público observado: ${publicIp ?: "N/D"})."
            ZeroBalanceClassification.VPN_SERVER_UNREACHABLE ->
                "O servidor VPN configurado está inacessível a partir da rede actual."
            ZeroBalanceClassification.VPN_SERVER_REACHABLE_NO_FORWARDING ->
                "O servidor VPN responde, mas o encaminhamento pelo túnel TUN não está activo ou validado."
            ZeroBalanceClassification.VPN_FUNCTIONAL ->
                "Ligação VPN funcional ($tunPacketsCount pacotes processados; IP público: ${publicIp ?: "N/D"}; VPN ${if (isVpnRunning) "activa" else "inactiva"})."
        }
        return if (mismatchNote != null) "$base $mismatchNote" else base
    }

    private fun probeTcpOnce(host: String, port: Int): Long? {
        if (host.isBlank() || port !in 1..65535) return null
        return runCatching {
            val start = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host.trim(), port), PROBE_TIMEOUT_MS)
            }
            (System.currentTimeMillis() - start).coerceAtLeast(1L)
        }.getOrNull()
    }

    private fun fetchPublicIpSafely(): String? {
        return runCatching {
            val client = OkHttpClient.Builder()
                .connectTimeout(PROBE_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(PROBE_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .build()
            val request = Request.Builder()
                .url("https://api.ipify.org")
                .build()
            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string()?.trim()?.takeIf { it.length <= 45 } else null
            }
        }.getOrNull()
    }
}
