package com.example.model

enum class CarrierType(
    val displayName: String,
    val shortName: String,
    val mccMnc: String,
    val apnReference: String,
    val descriptionPt: String
) {
    UNITEL(
        displayName = "Unitel Angola",
        shortName = "UNITEL",
        mccMnc = "631-02",
        apnReference = "internet.unitel.co.ao",
        descriptionPt = "Rede móvel 3G/4G/5G nacional prioritária em Angola."
    ),
    AFRICELL(
        displayName = "Africell Angola",
        shortName = "AFRICELL",
        mccMnc = "631-05",
        apnReference = "internet.africell.ao",
        descriptionPt = "Operadora móvel 4G/LTE de alta capacidade em Angola."
    ),
    MOVICEL(
        displayName = "Movicel Angola",
        shortName = "MOVICEL",
        mccMnc = "631-04",
        apnReference = "internet.movicel.co.ao",
        descriptionPt = "Rede móvel nacional complementar."
    ),
    WIFI_ONLY(
        displayName = "Rede Wi-Fi",
        shortName = "WI-FI",
        mccMnc = "LOCAL",
        apnReference = "wlan0",
        descriptionPt = "Ligação Wi-Fi recomendada para preparar e sincronizar o Cofre."
    ),
    OTHER(
        displayName = "Outra Rede",
        shortName = "OUTRA",
        mccMnc = "N/D",
        apnReference = "auto",
        descriptionPt = "Rede móvel ou transporte genérico."
    )
}

enum class CarrierSelectionMode(val labelPt: String) {
    AUTO("Detecção Automática"),
    UNITEL("Unitel Angola"),
    AFRICELL("Africell Angola")
}

enum class TransportType(val labelPt: String) {
    WIFI("Wi-Fi"),
    CELLULAR("Dados Móveis"),
    ETHERNET("Ethernet"),
    NONE("Sem Ligação")
}

enum class VpnProtocol(
    val displayName: String,
    val defaultPort: Int,
    val summaryPt: String
) {
    DNS_VAULT_GUARD(
        displayName = "DNS Vault Guard (TUN)",
        defaultPort = 53,
        summaryPt = "Túnel TUN local seguro com reencaminhamento DNS protegido e controlo de fugas."
    ),
    WIREGUARD_TUNNEL(
        displayName = "WireGuard (Servidor Próprio)",
        defaultPort = 51820,
        summaryPt = "Perfil cifrado de baixa latência preparado para servidor próprio."
    ),
    SOCKS5_PROXY(
        displayName = "SOCKS5 / Túnel TCP",
        defaultPort = 1080,
        summaryPt = "Encaminhamento via servidor próprio SOCKS5/TCP com verificação de handshake."
    ),
    HTTP_CONNECT(
        displayName = "HTTPS / TLS Relay",
        defaultPort = 443,
        summaryPt = "Túnel TLS na porta 443 compatível com infraestruturas empresariais."
    )
}

enum class TunnelMode(val labelPt: String, val descriptionPt: String) {
    DNS_VAULT_SPLIT(
        labelPt = "Cofre DNS + Protecção",
        descriptionPt = "Mantém o transporte móvel fluido e cifra/encaminha resoluções DNS pelo túnel TUN."
    ),
    FULL_ENDPOINT_TUNNEL(
        labelPt = "Túnel Completo para Servidor",
        descriptionPt = "Valida handshake TCP directo com o servidor próprio antes de activar a interface TUN."
    )
}

sealed interface VpnConnectionState {
    data object Disconnected : VpnConnectionState
    data object Preparing : VpnConnectionState
    data object Connecting : VpnConnectionState
    data class Connected(
        val vpnIp: String,
        val serverName: String,
        val connectedAtMillis: Long
    ) : VpnConnectionState
    data class Error(val reasonPt: String) : VpnConnectionState
}

data class NetworkProbeStatus(
    val isVpnActive: Boolean = false,
    val isServerReachable: Boolean = false,
    val serverLatencyMs: Long? = null,
    val hasEffectiveInternet: Boolean = false,
    val isNetworkValidated: Boolean = false,
    val isCaptivePortal: Boolean = false,
    val transportType: TransportType = TransportType.NONE,
    val detectedCarrier: CarrierType = CarrierType.OTHER,
    val rawOperatorName: String = "Não detectado",
    val mccMnc: String = "—",
    val localIpAddress: String = "—",
    val vpnIpAddress: String? = null,
    val activeDnsServers: List<String> = emptyList(),
    val diagnosticSummaryPt: String = "A analisar interface de rede...",
    val lastCheckedAt: Long = 0L
)

data class TrafficTelemetry(
    val downloadSpeedBps: Long = 0L,
    val uploadSpeedBps: Long = 0L,
    val sessionRxBytes: Long = 0L,
    val sessionTxBytes: Long = 0L,
    val sessionDurationSeconds: Long = 0L,
    val tunPacketsHandled: Long = 0L,
    val downloadHistory: List<Float> = List(20) { 0f },
    val uploadHistory: List<Float> = List(20) { 0f }
)

data class DiagnosticStepResult(
    val titlePt: String,
    val detailPt: String,
    val passed: Boolean,
    val latencyMs: Long? = null
)

data class DestinationProbeResult(
    val labelPt: String,
    val host: String,
    val port: Int,
    val reachable: Boolean,
    val latencyMs: Long?,
    val categoryPt: String
)

data class VpnVerificationReport(
    val transportSummaryPt: String,
    val isCaptivePortal: Boolean,
    val isValidated: Boolean,
    val destinationResults: List<DestinationProbeResult>,
    val vpnTunnelVerified: Boolean,
    val tunPacketsCount: Long,
    val publicIpObserved: String?,
    val conclusionPt: String,
    val executedAtMillis: Long
)

data class SponsoredPartnershipInfo(
    val carrier: CarrierType,
    val titlePt: String,
    val statusBadgePt: String,
    val mechanismPt: String,
    val technicalRequirementsPt: List<String>
)

enum class SimBalanceState(val labelPt: String, val shortTag: String) {
    NO_BALANCE("Sem Saldo / Sem Pacote", "SEM SALDO"),
    WITH_BALANCE("Com Saldo / Pacote Activo", "COM SALDO"),
    UNKNOWN("Não Especificado", "NÃO ESPECIFICADO")
}

enum class ZeroBalanceClassification(
    val titlePt: String,
    val descriptionPt: String
) {
    NO_DATA_CONNECTION(
        titlePt = "Sem ligação de dados",
        descriptionPt = "Nenhuma interface de dados móveis ou transporte de rede está disponível no momento."
    ),
    MOBILE_ACTIVE_NO_VALIDATED_INTERNET(
        titlePt = "Ligação móvel activa, mas sem internet validada",
        descriptionPt = "A rede móvel está ligada ao nível de transporte, mas não há trânsito externo nem serviços acessíveis."
    ),
    LIMITED_TO_SPECIFIC_SERVICES(
        titlePt = "Acesso limitado a serviços específicos",
        descriptionPt = "A internet pública está bloqueada, mas um portal oficial da operadora ou serviço autorizado respondeu (provável Walled Garden). Isto não comprova gratuidade sem autorização oficial."
    ),
    PUBLIC_INTERNET_AVAILABLE(
        titlePt = "Internet pública disponível",
        descriptionPt = "Existe acesso aberto à internet pública pela ligação actual."
    ),
    VPN_SERVER_UNREACHABLE(
        titlePt = "Servidor VPN inacessível",
        descriptionPt = "Existe rede ou acesso parcial, mas o servidor VPN configurado não respondeu na porta indicada."
    ),
    VPN_SERVER_REACHABLE_NO_FORWARDING(
        titlePt = "Servidor VPN acessível, mas sem encaminhamento funcional",
        descriptionPt = "O servidor VPN responde ao handshake, mas o túnel TUN não está activo ou não há trânsito público validado pelo túnel."
    ),
    VPN_FUNCTIONAL(
        titlePt = "Ligação VPN funcional",
        descriptionPt = "O servidor VPN está acessível, a interface TUN está activa a processar pacotes e existe conectividade funcional."
    )
}

data class ConnectivityMatrixItem(
    val key: String,
    val labelPt: String,
    val statusPt: String,
    val detailPt: String,
    val passed: Boolean,
    val latencyMs: Long? = null
)

data class LabVpnSnapshot(
    val labelPt: String,
    val capturedAtMillis: Long,
    val carrierTested: CarrierType,
    val vpnActive: Boolean,
    val vpnIp: String?,
    val publicIp: String?,
    val serverHost: String,
    val serverPort: Int,
    val serverReachable: Boolean,
    val serverLatencyMs: Long?,
    val publicInternetReachable: Boolean,
    val publicInternetLatencyMs: Long?,
    val dataUsedBytes: Long,
    val tunPackets: Long,
    val errorDetailPt: String?
)

data class LabRunResult(
    val carrierTested: CarrierType,
    val detectedCarrier: CarrierType,
    val detectedMccMnc: String,
    val transportType: TransportType,
    val balanceState: SimBalanceState,
    val classification: ZeroBalanceClassification,
    val matrixItems: List<ConnectivityMatrixItem>,
    val destinationResults: List<DestinationProbeResult>,
    val publicIpObserved: String?,
    val vpnActive: Boolean,
    val vpnIp: String?,
    val serverName: String,
    val serverHost: String,
    val serverPort: Int,
    val dataConsumedBytes: Long,
    val errorMessagePt: String?,
    val technicalConclusionPt: String,
    val complianceNoticePt: String,
    val executedAtMillis: Long
)
