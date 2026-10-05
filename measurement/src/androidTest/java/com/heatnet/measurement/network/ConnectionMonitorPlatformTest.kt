package com.heatnet.measurement.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectionMonitorPlatformTest {
    @Test
    fun callbackCanStartAndStopRepeatedly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val monitor = ConnectionMonitor(context)

        monitor.start()
        monitor.start()
        monitor.stop()
        monitor.stop()
    }
}
