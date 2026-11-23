package com.example

import com.example.model.CarrierType
import com.example.model.VpnProtocol
import com.example.util.FormatUtils
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
}
