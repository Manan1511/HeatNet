package com.heatnet.ui.screens

import android.Manifest
import com.heatnet.measurement.model.ConnectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPermissionPolicyTest {
    @Test
    fun wifiSessionRequestsLocationUntilPrecisePermissionIsGranted() {
        assertTrue(WifiLocationPermissionPolicy.shouldRequestFor(ConnectionType.WIFI, hasFineLocation = false))
        assertFalse(WifiLocationPermissionPolicy.shouldRequestFor(ConnectionType.WIFI, hasFineLocation = true))
    }

    @Test
    fun mobileAndUnselectedSessionsDoNotRequestLocation() {
        assertFalse(WifiLocationPermissionPolicy.shouldRequestFor(ConnectionType.MOBILE, hasFineLocation = false))
        assertFalse(WifiLocationPermissionPolicy.shouldRequestFor(null, hasFineLocation = false))
    }

    @Test
    fun requestIncludesFineAndCoarseTogether() {
        assertEquals(
            setOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            WifiLocationPermissionPolicy.requestPermissions.toSet(),
        )
    }

    @Test
    fun approximateOnlyGrantDoesNotUnlockWifiMeasurements() {
        val grants = mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to false,
            Manifest.permission.ACCESS_COARSE_LOCATION to true,
        )

        assertFalse(WifiLocationPermissionPolicy.hasRequiredPermission(grants))
        assertTrue(WifiLocationPermissionPolicy.isApproximateOnly(grants))
    }

    @Test
    fun preciseGrantUnlocksWifiMeasurements() {
        val grants = mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to true,
            Manifest.permission.ACCESS_COARSE_LOCATION to true,
        )

        assertTrue(WifiLocationPermissionPolicy.hasRequiredPermission(grants))
        assertFalse(WifiLocationPermissionPolicy.isApproximateOnly(grants))
    }
}
