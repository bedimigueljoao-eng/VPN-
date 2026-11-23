package com.example.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.local.ConnectivityLabReportEntity
import com.example.data.local.LabDestinationRecord
import com.example.model.ZeroBalanceClassification
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.OutputStream

object ReportExporter {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, LabDestinationRecord::class.java)
    private val destAdapter = moshi.adapter<List<LabDestinationRecord>>(listType)

    fun parseDestinations(json: String): List<LabDestinationRecord> {
        return runCatching { destAdapter.fromJson(json) ?: emptyList() }.getOrDefault(emptyList())
    }

    fun buildCsvContent(reports: List<ConnectivityLabReportEntity>): String {
        val sb = StringBuilder()
        sb.appendLine(
            "Data_Hora;Operadora;MCC_MNC;Transporte;Estado_Saldo;Classificacao;Estado_VPN;Servidor_VPN;IP_VPN;IP_Antes_VPN;IP_Depois_VPN;Consumo_Bytes;Resumo_Conectividade;Destinos;Conclusao_Tecnica"
        )
        reports.forEach { r ->
            val dests = parseDestinations(r.destinationsJson).joinToString(" | ") { d ->
                "${d.labelPt} (${d.host}:${d.port}): ${if (d.reachable) "${d.latencyMs ?: 0}ms" else "Bloqueado"}"
            }
            val classTitle = runCatching {
                ZeroBalanceClassification.valueOf(r.classification).titlePt
            }.getOrDefault(r.classification)

            val cols = listOf(
                FormatUtils.formatDateTime(r.executedAtMillis),
                r.carrierTested,
                r.detectedMccMnc,
                r.transportType,
                r.balanceState,
                classTitle,
                if (r.vpnActive) "Activa" else "Inactiva",
                r.vpnServerName,
                r.vpnIp,
                r.publicIpBefore,
                r.publicIpAfter,
                r.dataConsumedBytes.toString(),
                r.connectivitySummaryPt,
                dests,
                r.technicalConclusionPt
            ).map { escapeCsv(it) }
            sb.appendLine(cols.joinToString(";"))
        }
        return sb.toString()
    }

    fun exportCsv(outputStream: OutputStream, reports: List<ConnectivityLabReportEntity>) {
        val content = buildCsvContent(reports)
        outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())) // UTF-8 BOM para Excel
        outputStream.write(content.toByteArray(Charsets.UTF_8))
        outputStream.flush()
    }

    fun exportPdf(outputStream: OutputStream, reports: List<ConnectivityLabReportEntity>) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val maxLineWidth = pageWidth - (margin * 2)

        val headerBgPaint = Paint().apply { color = Color.parseColor("#0B1730") }
        val accentPaint = Paint().apply { color = Color.parseColor("#2388E8") }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#DAEBFD")
            textSize = 10f
        }
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#0B1730")
            textSize = 12f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 9.5f
        }
        val mutedPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 8.5f
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas: Canvas = page.canvas

        fun drawBanner(c: Canvas) {
            c.drawRect(0f, 0f, pageWidth.toFloat(), 76f, headerBgPaint)
            c.drawRect(0f, 72f, pageWidth.toFloat(), 76f, accentPaint)
            c.drawText("INTERNET VAULT · LABORATÓRIO DE CONECTIVIDADE", margin, 34f, titlePaint)
            c.drawText("Relatório Técnico Angola (Unitel 631-02 & Africell 631-05)", margin, 54f, subtitlePaint)
        }

        drawBanner(canvas)
        var y = 102f

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - margin) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                drawBanner(canvas)
                y = 102f
            }
        }

        fun drawWrapped(text: String, paint: Paint, lineHeight: Float = 13f) {
            val words = text.split(" ")
            val line = StringBuilder()
            for (word in words) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) > maxLineWidth) {
                    ensureSpace(lineHeight)
                    canvas.drawText(line.toString(), margin, y, paint)
                    y += lineHeight
                    line.clear()
                    line.append(word)
                } else {
                    if (line.isNotEmpty()) line.append(" ")
                    line.append(word)
                }
            }
            if (line.isNotEmpty()) {
                ensureSpace(lineHeight)
                canvas.drawText(line.toString(), margin, y, paint)
                y += lineHeight
            }
        }

        if (reports.isEmpty()) {
            drawWrapped("Sem relatórios registados para exportar.", bodyPaint)
        } else {
            reports.forEachIndexed { idx, r ->
                ensureSpace(140f)
                val classTitle = runCatching {
                    ZeroBalanceClassification.valueOf(r.classification).titlePt
                }.getOrDefault(r.classification)

                drawWrapped(
                    "Teste #${idx + 1} · ${r.carrierTested} (${r.detectedMccMnc}) · ${FormatUtils.formatDateTime(r.executedAtMillis)}",
                    sectionPaint,
                    16f
                )
                drawWrapped("Transporte: ${r.transportType} | Saldo informado: ${r.balanceState}", bodyPaint)
                drawWrapped("Classificação: $classTitle", bodyPaint)
                drawWrapped(
                    "VPN: ${if (r.vpnActive) "Activa" else "Inactiva"} (${r.vpnServerName} · IP VPN ${r.vpnIp}) | Consumo: ${FormatUtils.formatBytes(r.dataConsumedBytes)}",
                    bodyPaint
                )
                drawWrapped("IP Antes VPN: ${r.publicIpBefore}  ->  IP Depois VPN: ${r.publicIpAfter}", bodyPaint)
                drawWrapped("Resumo: ${r.connectivitySummaryPt}", mutedPaint)

                val dests = parseDestinations(r.destinationsJson)
                dests.forEach { d ->
                    val status = if (d.reachable) "Acessível (${d.latencyMs ?: 0} ms)" else "Bloqueado"
                    drawWrapped("  • ${d.labelPt} [${d.host}:${d.port}] -> $status", bodyPaint)
                }
                drawWrapped("Conclusão: ${r.technicalConclusionPt}", bodyPaint)
                y += 12f
            }
        }

        document.finishPage(page)
        document.writeTo(outputStream)
        outputStream.flush()
        document.close()
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\n", " ").replace("\r", " ")
        return if (clean.contains(";") || clean.contains("\"")) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else {
            clean
        }
    }
}
