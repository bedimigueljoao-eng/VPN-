package com.example

import com.example.data.local.ConnectivityLabReportEntity
import com.example.model.CarrierType
import com.example.model.VpnProtocol
import com.example.model.ZeroBalanceClassification
import com.example.network.ConnectivityLabEngine
import com.example.util.FormatUtils
import com.example.util.ReportExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `format utils formats duration and speeds cleanly`() {
        assertEquals("00:00:00", FormatUtils.formatDuration(0L))
        assertEquals("01:01:05", FormatUtils.formatDuration(3665L))
        assertEquals("0 KB/s", FormatUtils.formatSpeed(0L))
        assertTrue(FormatUtils.formatBytes(2048L).contains("KB"))
    }

    @Test
    fun `angola carriers have valid mcc mnc codes`() {
        assertEquals("631-02", CarrierType.UNITEL.mccMnc)
        assertEquals("631-05", CarrierType.AFRICELL.mccMnc)
        assertEquals("631-04", CarrierType.MOVICEL.mccMnc)
    }

    @Test
    fun `vpn protocols have valid default ports`() {
        VpnProtocol.entries.forEach { protocol ->
            assertTrue(protocol.defaultPort in 1..65535)
            assertTrue(protocol.displayName.isNotBlank())
        }
    }

    @Test
    fun `lab engine classifies all seven connectivity states deterministically`() {
        assertEquals(
            ZeroBalanceClassification.NO_DATA_CONNECTION,
            ConnectivityLabEngine.classifyConnectivity(
                hasAnyTransport = false,
                hasMobileNetwork = false,
                isNetworkValidated = false,
                publicInternetReachable = false,
                operatorOrAuthorizedServiceReachable = false,
                vpnServerReachable = false,
                vpnTunnelActive = false,
                tunPacketsCount = 0L,
                hasCustomServerConfigured = true
            )
        )

        assertEquals(
            ZeroBalanceClassification.MOBILE_ACTIVE_NO_VALIDATED_INTERNET,
            ConnectivityLabEngine.classifyConnectivity(
                hasAnyTransport = true,
                hasMobileNetwork = true,
                isNetworkValidated = false,
                publicInternetReachable = false,
                operatorOrAuthorizedServiceReachable = false,
                vpnServerReachable = false,
                vpnTunnelActive = false,
                tunPacketsCount = 0L,
                hasCustomServerConfigured = true
            )
        )

        assertEquals(
            ZeroBalanceClassification.LIMITED_TO_SPECIFIC_SERVICES,
            ConnectivityLabEngine.classifyConnectivity(
                hasAnyTransport = true,
                hasMobileNetwork = true,
                isNetworkValidated = false,
                publicInternetReachable = false,
                operatorOrAuthorizedServiceReachable = true,
                vpnServerReachable = false,
                vpnTunnelActive = false,
                tunPacketsCount = 0L,
                hasCustomServerConfigured = true
            )
        )

        assertEquals(
            ZeroBalanceClassification.PUBLIC_INTERNET_AVAILABLE,
            ConnectivityLabEngine.classifyConnectivity(
                hasAnyTransport = true,
                hasMobileNetwork = true,
                isNetworkValidated = true,
                publicInternetReachable = true,
                operatorOrAuthorizedServiceReachable = true,
                vpnServerReachable = false,
                vpnTunnelActive = false,
                tunPacketsCount = 0L,
                hasCustomServerConfigured = false
            )
        )

        assertEquals(
            ZeroBalanceClassification.VPN_SERVER_UNREACHABLE,
            ConnectivityLabEngine.classifyConnectivity(
                hasAnyTransport = true,
                hasMobileNetwork = true,
                isNetworkValidated = true,
                publicInternetReachable = true,
                operatorOrAuthorizedServiceReachable = true,
                vpnServerReachable = false,
                vpnTunnelActive = false,
                tunPacketsCount = 0L,
                hasCustomServerConfigured = true
            )
        )

        assertEquals(
            ZeroBalanceClassification.VPN_SERVER_REACHABLE_NO_FORWARDING,
            ConnectivityLabEngine.classifyConnectivity(
                hasAnyTransport = true,
                hasMobileNetwork = true,
                isNetworkValidated = true,
                publicInternetReachable = true,
                operatorOrAuthorizedServiceReachable = true,
                vpnServerReachable = true,
                vpnTunnelActive = false,
                tunPacketsCount = 0L,
                hasCustomServerConfigured = true
            )
        )

        assertEquals(
            ZeroBalanceClassification.VPN_FUNCTIONAL,
            ConnectivityLabEngine.classifyConnectivity(
                hasAnyTransport = true,
                hasMobileNetwork = true,
                isNetworkValidated = true,
                publicInternetReachable = true,
                operatorOrAuthorizedServiceReachable = true,
                vpnServerReachable = true,
                vpnTunnelActive = true,
                tunPacketsCount = 12L,
                hasCustomServerConfigured = true
            )
        )
    }

    @Test
    fun `report exporter builds valid semicolon csv with portuguese headers and escaping`() {
        val sampleReports = listOf(
            ConnectivityLabReportEntity(
                id = 1L,
                executedAtMillis = 1700000000000L,
                carrierTested = "UNITEL",
                detectedMccMnc = "631-02",
                transportType = "MOBILE",
                balanceState = "Sem Saldo / Sem Pacote",
                classification = ZeroBalanceClassification.LIMITED_TO_SPECIFIC_SERVICES.name,
                vpnActive = false,
                vpnServerName = "Luanda WireGuard",
                vpnIp = "N/D",
                publicIpBefore = "197.149.10.1",
                publicIpAfter = "N/D",
                dataConsumedBytes = 2048L,
                connectivitySummaryPt = "Unitel 631-02; teste controlado",
                destinationsJson = """[{"labelPt":"Portal Unitel","host":"unitel.ao","port":443,"categoryPt":"Portal Oficial","reachable":true,"latencyMs":42}]""",
                technicalConclusionPt = "Acesso limitado ao portal oficial."
            ),
            ConnectivityLabReportEntity(
                id = 2L,
                executedAtMillis = 1700000060000L,
                carrierTested = "AFRICELL",
                detectedMccMnc = "631-05",
                transportType = "MOBILE",
                balanceState = "Com Saldo Activo",
                classification = ZeroBalanceClassification.VPN_FUNCTIONAL.name,
                vpnActive = true,
                vpnServerName = "Lisboa Tun",
                vpnIp = "10.84.0.2",
                publicIpBefore = "102.212.1.5",
                publicIpAfter = "185.10.20.30",
                dataConsumedBytes = 1048576L,
                connectivitySummaryPt = "Africell 631-05 com VPN funcional",
                destinationsJson = "[]",
                technicalConclusionPt = "Túnel validado."
            )
        )

        val csv = ReportExporter.buildCsvContent(sampleReports)
        val lines = csv.trim().lines()

        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("Data_Hora;Operadora;MCC_MNC;Transporte;Estado_Saldo;Classificacao;"))
        assertTrue(lines[1].contains("UNITEL;631-02;MOBILE"))
        assertTrue(lines[1].contains("\"Unitel 631-02; teste controlado\""))
        assertTrue(lines[1].contains("Portal Unitel (unitel.ao:443): 42ms"))
        assertTrue(lines[2].contains("AFRICELL;631-05;MOBILE"))
        assertTrue(lines[2].contains("10.84.0.2;102.212.1.5;185.10.20.30"))
    }
}

