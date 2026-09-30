package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SecurityStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Share Screen: Parental Control App", appName)
    }

    @Test
    fun testSecurityStoragePinVerification() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val security = SecurityStorage(context)

        security.setPin("1234")
        assertTrue("PIN 1234 should verify successfully", security.verifyPin("1234"))
        assertFalse("Wrong PIN should fail", security.verifyPin("9999"))
    }

    @Test
    fun testPairingCodeGeneration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val security = SecurityStorage(context)

        val code = security.generatePairingCode()
        assertEquals(6, code.length)
        assertTrue(code.all { it.isDigit() })
    }

    @Test
    fun testAppScreenNavigationItemsNonNull() {
        val monitoredItems = com.example.ui.navigation.AppScreen.bottomNavItemsMonitored
        assertTrue("Monitored items should not be empty", monitoredItems.isNotEmpty())
        monitoredItems.forEach { screen ->
            org.junit.Assert.assertNotNull("Screen in monitored items must not be null", screen)
            assertTrue("Screen route must not be blank", screen.route.isNotBlank())
        }

        val viewerItems = com.example.ui.navigation.AppScreen.bottomNavItemsViewer
        assertTrue("Viewer items should not be empty", viewerItems.isNotEmpty())
        viewerItems.forEach { screen ->
            org.junit.Assert.assertNotNull("Screen in viewer items must not be null", screen)
            assertTrue("Screen route must not be blank", screen.route.isNotBlank())
        }
    }

    @Test
    fun testAuthenticationRepositoryResultStates() {
        val successResult = com.example.data.repository.AuthResult.Success("test_user_id")
        assertTrue(successResult is com.example.data.repository.AuthResult.Success)
        assertEquals("test_user_id", successResult.data)

        val errorResult = com.example.data.repository.AuthResult.Error("Test error message")
        assertTrue(errorResult is com.example.data.repository.AuthResult.Error)
        assertEquals("Test error message", errorResult.message)

        val cancelledResult = com.example.data.repository.AuthResult.Cancelled
        assertTrue(cancelledResult is com.example.data.repository.AuthResult.Cancelled)
    }

    @Test
    fun testBatteryManagerServiceStateFlows() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val batteryService = com.example.service.BatteryManagerService(context)

        val currentState = batteryService.getCurrentBatteryState()
        org.junit.Assert.assertNotNull(currentState)
        assertTrue(currentState.percentage in 0..100)
        org.junit.Assert.assertNotNull(currentState.healthStatus)
        org.junit.Assert.assertNotNull(currentState.chargingState)

        val percentageFlowValue = batteryService.batteryPercentage.value
        assertEquals(currentState.percentage, percentageFlowValue)

        val isChargingFlowValue = batteryService.isCharging.value
        assertEquals(currentState.isCharging, isChargingFlowValue)

        val healthFlowValue = batteryService.healthStatus.value
        assertEquals(currentState.healthStatus, healthFlowValue)

        val chargingStateFlowValue = batteryService.chargingState.value
        assertEquals(currentState.chargingState, chargingStateFlowValue)
    }
}
