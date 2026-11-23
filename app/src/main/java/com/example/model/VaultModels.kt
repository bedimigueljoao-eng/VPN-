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

data class SponsoredPartnershipInfo(
    val carrier: CarrierType,
    val titlePt: String,
    val statusBadgePt: String,
    val mechanismPt: String,
    val technicalRequirementsPt: List<String>
)
