package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object FormatUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceAtMost(4)
        val unit = listOf("B", "KB", "MB", "GB", "TB")[exp]
        val value = bytes / 1024.0.pow(exp.toDouble())
        return String.format(Locale("pt", "AO"), "%.2f %s", value, unit)
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return "0 KB/s"
        return "${formatBytes(bytesPerSec)}/s"
    }

    fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatDateTime(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return "Nunca"
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "PT"))
        return formatter.format(Date(timestampMillis))
    }

    fun formatTimeShort(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return "—"
        val formatter = SimpleDateFormat("HH:mm:ss", Locale("pt", "PT"))
        return formatter.format(Date(timestampMillis))
    }
}
