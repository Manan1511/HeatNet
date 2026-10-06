package com.heatnet

import android.app.Application
import com.heatnet.ui.AppContainer

class HeatNetApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
