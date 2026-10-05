package com.heatnet.measurement.probe

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network

internal data class IcmpRoute(
    val hostAddress: String,
    val interfaceName: String,
)

internal fun interface IcmpRouteResolver {
    fun resolve(network: Network, host: String): IcmpRoute?
}

internal class AndroidIcmpRouteResolver(context: Context) : IcmpRouteResolver {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun resolve(network: Network, host: String): IcmpRoute? = runCatching {
        val interfaceName = connectivityManager.getLinkProperties(network)?.interfaceName ?: return null
        val hostAddress = network.getAllByName(host).firstOrNull()?.hostAddress ?: return null
        IcmpRoute(hostAddress, interfaceName)
    }.getOrNull()
}
