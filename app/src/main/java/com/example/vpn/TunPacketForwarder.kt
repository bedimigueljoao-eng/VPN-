package com.example.vpn

import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong

class TunPacketForwarder(
    private val vpnService: VpnService,
    private val upstreamDns: String
) {
    val rxBytes = AtomicLong(0L)
    val txBytes = AtomicLong(0L)
    val packetsHandled = AtomicLong(0L)

    suspend fun runLoop(tunDescriptor: ParcelFileDescriptor) = withContext(Dispatchers.IO) {
        val input = FileInputStream(tunDescriptor.fileDescriptor)
        val output = FileOutputStream(tunDescriptor.fileDescriptor)
        val packetBuffer = ByteArray(32767)
        val upstreamAddr = runCatching { InetAddress.getByName(upstreamDns) }
            .getOrElse { InetAddress.getByName("1.1.1.1") }

        while (isActive) {
            val length = runCatching { input.read(packetBuffer) }.getOrElse { -1 }
            if (length <= 0) {
                if (length < 0) break
                continue
            }

            txBytes.addAndGet(length.toLong())
            packetsHandled.incrementAndGet()

            // Parse IPv4 packets only
            val version = (packetBuffer[0].toInt() shr 4) and 0x0F
            if (version != 4 || length < 28) continue

            val ihlBytes = (packetBuffer[0].toInt() and 0x0F) * 4
            if (ihlBytes < 20 || length < ihlBytes + 8) continue

            val protocol = packetBuffer[9].toInt() and 0xFF
            if (protocol != 17) continue // UDP only

            val dstPort = ((packetBuffer[ihlBytes + 2].toInt() and 0xFF) shl 8) or
                (packetBuffer[ihlBytes + 3].toInt() and 0xFF)
            if (dstPort != 53) continue // DNS queries

            val udpLength = ((packetBuffer[ihlBytes + 4].toInt() and 0xFF) shl 8) or
                (packetBuffer[ihlBytes + 5].toInt() and 0xFF)
            val dnsOffset = ihlBytes + 8
            val dnsLength = (udpLength - 8).coerceAtMost(length - dnsOffset)
            if (dnsLength <= 0) continue

            val srcIp = packetBuffer.copyOfRange(12, 16)
            val dstIp = packetBuffer.copyOfRange(16, 20)
            val srcPortBytes = packetBuffer.copyOfRange(ihlBytes, ihlBytes + 2)
            val dstPortBytes = packetBuffer.copyOfRange(ihlBytes + 2, ihlBytes + 4)
            val dnsPayload = packetBuffer.copyOfRange(dnsOffset, dnsOffset + dnsLength)

            val responseDns = forwardDnsQuery(upstreamAddr, dnsPayload) ?: continue
            val replyPacket = buildIpv4UdpReply(
                origSrcIp = srcIp,
                origDstIp = dstIp,
                origSrcPort = srcPortBytes,
                origDstPort = dstPortBytes,
                payload = responseDns
            )

            synchronized(output) {
                runCatching {
                    output.write(replyPacket)
                    output.flush()
                    rxBytes.addAndGet(replyPacket.size.toLong())
                    packetsHandled.incrementAndGet()
                }
            }
        }
    }

    private fun forwardDnsQuery(upstreamAddr: InetAddress, query: ByteArray): ByteArray? {
        return runCatching {
            DatagramSocket().use { socket ->
                vpnService.protect(socket)
                socket.soTimeout = 2500
                val requestPacket = DatagramPacket(query, query.size, upstreamAddr, 53)
                socket.send(requestPacket)

                val recvBuf = ByteArray(4096)
                val responsePacket = DatagramPacket(recvBuf, recvBuf.size)
                socket.receive(responsePacket)
                recvBuf.copyOf(responsePacket.length)
            }
        }.getOrNull()
    }

    private fun buildIpv4UdpReply(
        origSrcIp: ByteArray,
        origDstIp: ByteArray,
        origSrcPort: ByteArray,
        origDstPort: ByteArray,
        payload: ByteArray
    ): ByteArray {
        val udpLength = 8 + payload.size
        val totalLength = 20 + udpLength
        val packet = ByteArray(totalLength)
        val buffer = ByteBuffer.wrap(packet)

        // IPv4 Header (20 bytes)
        buffer.put(0x45.toByte()) // Version 4, IHL 5
        buffer.put(0x00.toByte()) // TOS
        buffer.putShort(totalLength.toShort())
        buffer.putShort(0x1A2B.toShort()) // Identification
        buffer.putShort(0x4000.toShort()) // Don't fragment
        buffer.put(64.toByte()) // TTL
        buffer.put(17.toByte()) // UDP
        buffer.putShort(0.toShort()) // Checksum placeholder
        buffer.put(origDstIp) // Swap: server becomes source
        buffer.put(origSrcIp) // Swap: client becomes destination

        // Compute IPv4 header checksum
        val checksum = computeIpv4Checksum(packet, 20)
        packet[10] = ((checksum ushr 8) and 0xFF).toByte()
        packet[11] = (checksum and 0xFF).toByte()

        // UDP Header (8 bytes)
        buffer.position(20)
        buffer.put(origDstPort) // Swap ports
        buffer.put(origSrcPort)
        buffer.putShort(udpLength.toShort())
        buffer.putShort(0.toShort()) // Checksum 0 (optional for IPv4 UDP)
        buffer.put(payload)

        return packet
    }

    private fun computeIpv4Checksum(header: ByteArray, length: Int): Int {
        var sum = 0
        var i = 0
        while (i < length) {
            if (i == 10) {
                i += 2
                continue
            }
            val word = ((header[i].toInt() and 0xFF) shl 8) or (header[i + 1].toInt() and 0xFF)
            sum += word
            if ((sum and -0x10000) != 0) {
                sum = (sum and 0xFFFF) + (sum ushr 16)
            }
            i += 2
        }
        while ((sum ushr 16) != 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return sum.inv() and 0xFFFF
    }
}
