package com.example.ui.camera

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.json.JSONObject

data class PairingQrPayload(
    val ip: String,
    val port: Int = 8888,
    val code: String
) {
    fun toQrString(): String {
        val json = JSONObject()
        json.put("app", "sharescreen")
        json.put("ip", ip)
        json.put("port", port)
        json.put("code", code)
        return json.toString()
    }

    companion object {
        fun parse(raw: String): PairingQrPayload? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null

            // 1. Try parsing JSON
            try {
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    val json = JSONObject(trimmed)
                    val ip = json.optString("ip", "")
                    val port = json.optInt("port", 8888)
                    val code = json.optString("code", "")
                    if (code.isNotEmpty()) {
                        return PairingQrPayload(
                            ip = ip.ifEmpty { "127.0.0.1" },
                            port = port,
                            code = code
                        )
                    }
                }
            } catch (_: Exception) {}

            // 2. Try parsing URI schema: sharescreen://pair?ip=...&code=...&port=...
            if (trimmed.startsWith("sharescreen://") || trimmed.contains("pair?")) {
                try {
                    val uriString = if (trimmed.startsWith("sharescreen://")) trimmed else "sharescreen://$trimmed"
                    val androidUri = android.net.Uri.parse(uriString)
                    val ip = androidUri.getQueryParameter("ip") ?: ""
                    val code = androidUri.getQueryParameter("code") ?: ""
                    val port = androidUri.getQueryParameter("port")?.toIntOrNull() ?: 8888
                    if (code.isNotEmpty()) {
                        return PairingQrPayload(
                            ip = ip.ifEmpty { "127.0.0.1" },
                            port = port,
                            code = code
                        )
                    }
                } catch (_: Exception) {}
            }

            // 3. Try parsing IP:code or code
            if (trimmed.contains(":") && !trimmed.contains("http")) {
                val parts = trimmed.split(":")
                if (parts.size >= 2) {
                    val ip = parts[0].trim()
                    val code = parts[1].trim()
                    if (code.isNotEmpty()) {
                        return PairingQrPayload(ip = ip, port = 8888, code = code)
                    }
                }
            }

            // 4. If it's a 6-digit number or alphanumeric code
            if (trimmed.length in 4..12 && trimmed.all { it.isLetterOrDigit() }) {
                return PairingQrPayload(ip = "", port = 8888, code = trimmed)
            }

            return null
        }
    }
}

fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
    if (content.isBlank()) return null
    return try {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap
    } catch (_: Exception) {
        null
    }
}
