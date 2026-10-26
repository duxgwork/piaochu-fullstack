package com.piaochu.app.data

import android.content.Context
import com.piaochu.app.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** 仅调试包读取可修改的地址与代理；正式包始终使用构建时指定的 HTTPS 地址。 */
class NetworkSettings(context: Context) {
    private val preferences = context.getSharedPreferences("debug_network", Context.MODE_PRIVATE)

    fun read(): NetworkConfig = if (BuildConfig.DEBUG) {
        NetworkConfig(
            preferences.getString("baseUrl", null) ?: BuildConfig.API_BASE_URL,
            preferences.getString("proxyHost", "").orEmpty(),
            preferences.getInt("proxyPort", 8888),
        )
    } else NetworkConfig(BuildConfig.API_BASE_URL)

    fun save(config: NetworkConfig) {
        check(BuildConfig.DEBUG) { "正式包不提供联调设置" }
        validate(config)
        val saved = preferences.edit().putString("baseUrl", normalizedUrl(config.baseUrl))
            .putString("proxyHost", config.proxyHost.trim()).putInt("proxyPort", config.proxyPort).commit()
        check(saved) { "接口设置保存失败，请重试" }
    }

    companion object {
        fun normalizedUrl(value: String): String = value.trim().trimEnd('/') + "/"

        fun validate(config: NetworkConfig) {
            val url = normalizedUrl(config.baseUrl).toHttpUrlOrNull()
                ?: throw IllegalArgumentException("接口地址必须是完整的 http:// 或 https:// 地址")
            require(url.username.isEmpty() && url.password.isEmpty()) { "接口地址中不能包含账号密码" }
            require(url.encodedPath == "/" && url.query == null && url.fragment == null) {
                "请填写服务根地址，例如 http://10.0.2.2:8090/，不要附加 /api/orders"
            }
            require(config.proxyHost.isBlank() ||
                (!config.proxyHost.contains("://") && !config.proxyHost.contains('/') && !config.proxyHost.contains(':'))) {
                "代理主机填写 IPv4 或域名，例如 10.0.2.2；端口单独填写"
            }
            require(config.proxyHost.isBlank() || config.proxyPort in 1..65535) { "代理端口范围为 1–65535" }
        }
    }
}

data class NetworkConfig(val baseUrl: String, val proxyHost: String = "", val proxyPort: Int = 8888)
