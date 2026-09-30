package com.example

import com.example.data.remote.model.CompanionMessage
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {

    @Test
    fun testHandshakeRequestSerialization() {
        val req = CompanionMessage.HandshakeRequest(
            deviceId = "dev-123",
            deviceName = "Rohit Phone",
            role = "MONITORED",
            pairingCode = "482719"
        )
        val jsonStr = req.toJson()
        val parsed = CompanionMessage.HandshakeRequest.fromJson(JSONObject(jsonStr))

        assertEquals("dev-123", parsed.deviceId)
        assertEquals("Rohit Phone", parsed.deviceName)
        assertEquals("MONITORED", parsed.role)
        assertEquals("482719", parsed.pairingCode)
    }

    @Test
    fun testHandshakeResponseSerialization() {
        val resp = CompanionMessage.HandshakeResponse(
            success = true,
            deviceId = "server-456",
            deviceName = "Host Device",
            token = "sec_token_abc",
            message = "Pairing successful"
        )
        val jsonStr = resp.toJson()
        val parsed = CompanionMessage.HandshakeResponse.fromJson(JSONObject(jsonStr))

        assertTrue(parsed.success)
        assertEquals("server-456", parsed.deviceId)
        assertEquals("sec_token_abc", parsed.token)
    }

    @Test
    fun testCommandSerialization() {
        val cmd = CompanionMessage.Command(
            action = CompanionMessage.CMD_START_SCREEN,
            payload = "HIGH"
        )
        val jsonStr = cmd.toJson()
        val parsed = CompanionMessage.Command.fromJson(JSONObject(jsonStr))

        assertEquals(CompanionMessage.CMD_START_SCREEN, parsed.action)
        assertEquals("HIGH", parsed.payload)
    }

    @Test
    fun testPairingQrPayloadParsing() {
        val payload = com.example.ui.camera.PairingQrPayload(
            ip = "192.168.1.50",
            port = 8888,
            code = "998877"
        )
        val qrString = payload.toQrString()
        val parsed = com.example.ui.camera.PairingQrPayload.parse(qrString)
        org.junit.Assert.assertNotNull(parsed)
        assertEquals("192.168.1.50", parsed?.ip)
        assertEquals(8888, parsed?.port)
        assertEquals("998877", parsed?.code)

        val uriParsed = com.example.ui.camera.PairingQrPayload.parse("sharescreen://pair?ip=10.0.0.5&code=123456&port=8888")
        org.junit.Assert.assertNotNull(uriParsed)
        assertEquals("10.0.0.5", uriParsed?.ip)
        assertEquals("123456", uriParsed?.code)

        val rawCodeParsed = com.example.ui.camera.PairingQrPayload.parse("654321")
        org.junit.Assert.assertNotNull(rawCodeParsed)
        assertEquals("654321", rawCodeParsed?.code)
    }

    @Test
    fun testGenerateQrBitmap() {
        val bitmap = com.example.ui.camera.generateQrBitmap("sharescreen-test-content", 200)
        org.junit.Assert.assertNotNull(bitmap)
        assertEquals(200, bitmap?.width)
        assertEquals(200, bitmap?.height)
    }
}
