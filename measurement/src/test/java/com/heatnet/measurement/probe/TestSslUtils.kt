package com.heatnet.measurement.probe

import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate

internal object TestSslUtils {
    val handshakeCertificates: HandshakeCertificates by lazy {
        val localhostCertificate = HeldCertificate.Builder()
            .addSubjectAlternativeName("localhost")
            .addSubjectAlternativeName("127.0.0.1")
            .build()
        HandshakeCertificates.Builder()
            .heldCertificate(localhostCertificate)
            .addTrustedCertificate(localhostCertificate.certificate)
            .build()
    }

    fun configureHttps(server: MockWebServer): OkHttpClient {
        server.useHttps(handshakeCertificates.sslSocketFactory())
        return createTrustingClient()
    }

    fun createTrustingClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .sslSocketFactory(handshakeCertificates.sslSocketFactory(), handshakeCertificates.trustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }
}
