package com.heatnet.ui.screens

import android.Manifest

/** Runtime permission contract for reading Wi-Fi radio details on current Android releases. */
internal object WifiLocationPermissionPolicy {
    val requestPermissions: Array<String> = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    fun hasRequiredPermission(grants: Map<String, Boolean>): Boolean =
        grants[Manifest.permission.ACCESS_FINE_LOCATION] == true

    fun isApproximateOnly(grants: Map<String, Boolean>): Boolean =
        !hasRequiredPermission(grants) && grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
}
