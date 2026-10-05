package com.heatnet.measurement.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.heatnet.measurement.model.ConnectionType

enum class PermissionOutcome {
    GRANTED,
    DENIED,
}

/** Reports permission readiness only; the consuming app remains responsible for requesting it. */
class PermissionGate private constructor(
    private val isFineLocationGranted: () -> Boolean,
) {
    constructor(context: Context) : this({
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    })

    fun check(type: ConnectionType): PermissionOutcome =
        if (type == ConnectionType.MOBILE || isFineLocationGranted()) {
            PermissionOutcome.GRANTED
        } else {
            PermissionOutcome.DENIED
        }

    internal companion object {
        fun fromChecker(isFineLocationGranted: () -> Boolean) = PermissionGate(isFineLocationGranted)
    }
}
