package com.heatnet.measurement.probe

import android.net.Network
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.OkHttpClient

/** Builds clients that keep both socket creation and DNS resolution on the captured Android route. */
class NetworkBoundHttpClientFactory {
    fun create(network: Network): OkHttpClient = OkHttpClient.Builder()
        .socketFactory(network.socketFactory)
        .dns(Dns { hostname -> network.getAllByName(hostname).toList() })
        .cookieJar(CookieJar.NO_COOKIES)
        .build()
}
