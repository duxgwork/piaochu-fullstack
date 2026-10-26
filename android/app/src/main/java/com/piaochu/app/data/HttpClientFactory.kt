package com.piaochu.app.data

import android.util.Log
import com.piaochu.app.BuildConfig
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.UUID
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

/**
 * 保留 OkHttp 的系统证书和主机名校验。HTTPS 抓包通过 debug CA 配置实现，
 * 不安装信任所有证书的 TrustManager，也不设置恒为 true 的 HostnameVerifier。
 */
object HttpClientFactory {
    fun create(config: NetworkConfig, token: () -> String? = { null }): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        // 写操作遇到断线时结果可能未知，不自动重发创建请求以免产生重复待办。
        .retryOnConnectionFailure(false)
        .apply {
            if (BuildConfig.DEBUG && config.proxyHost.isNotBlank()) {
                // 延后代理域名解析，避免在 Activity 创建客户端时触发主线程 DNS 请求。
                proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(config.proxyHost.trim(), config.proxyPort)))
            }
            // 未指定代理时沿用系统 ProxySelector，支持 Wi-Fi 手动代理与 Android Studio 检查器。
        }
        .addInterceptor { chain ->
            val requestId = UUID.randomUUID().toString()
            val builder = chain.request().newBuilder().header("Accept", "application/json").header("X-Request-Id", requestId)
            token()?.let { builder.header("Authorization", "Bearer $it") }
            val request = builder.build()
            if (BuildConfig.DEBUG) Log.d("PiaochuNetwork", "--> ${request.method} ${request.url} id=$requestId")
            chain.proceed(request).also {
                if (BuildConfig.DEBUG) Log.d("PiaochuNetwork", "<-- ${it.code} id=${it.header("X-Request-Id")}")
            }
        }
        .build()
}
