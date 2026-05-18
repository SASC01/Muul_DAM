package com.example.muul.data.remote

import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.android.AndroidEngineConfig
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object SupabaseProvider {
    @OptIn(SupabaseInternal::class)
    val client = createSupabaseClient(
        // URL Corregida: Sin 'db.' para usar el API Gateway de Postgrest
        supabaseUrl = "https://bhqimnreziqhwihykbmw.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJocWltbnJlemlxaHdpaHlrYm13Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzkwMzY2NTQsImV4cCI6MjA5NDYxMjY1NH0.A27sSoMk6eB-Rp1jI23oiBLmLe7O1IBGD60PZRahyOA"
    ) {
        install(Postgrest)
        
        // Bypass de validación SSL para permitir la conexión en tu fecha de 2026
        httpConfig {
            engine {
                this as AndroidEngineConfig
                sslManager = { connection ->
                    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    })
                    val sc = SSLContext.getInstance("TLS")
                    sc.init(null, trustAllCerts, SecureRandom())
                    connection.sslSocketFactory = sc.socketFactory
                    connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
                }
            }
        }
    }
}
