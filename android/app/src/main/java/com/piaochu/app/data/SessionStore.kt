package com.piaochu.app.data
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.GsonBuilder
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
/** 使用系统 Keystore 的 AES-GCM 加密会话；不同接口地址分别保存，切换后端不会携带另一环境的令牌。 */
class SessionStore(context:Context,baseUrl:String) {
    private val prefs=context.getSharedPreferences("encrypted_sessions",Context.MODE_PRIVATE)
    private val slot=MessageDigest.getInstance("SHA-256").digest(NetworkSettings.normalizedUrl(baseUrl).toByteArray()).joinToString(""){"%02x".format(it)}
    private val gson=GsonBuilder().disableJdkUnsafe().create()
    private val alias="com.piaochu.app.session.v1"
    var session:AuthDto?=read();private set
    private fun key():SecretKey {
        val ks=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        (ks.getKey(alias,null) as? SecretKey)?.let{return it}
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply{
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun read():AuthDto?=runCatching{
        val bytes=Base64.decode(prefs.getString(slot,null)?:return null,Base64.NO_WRAP)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(0,12)))
        gson.fromJson(String(cipher.doFinal(bytes.copyOfRange(12,bytes.size))),AuthDto::class.java)
    }.getOrElse{prefs.edit().remove(slot).apply();null}
    fun save(value:AuthDto) {
        value.toUser();require(!value.token.isNullOrBlank()){"登录响应缺少令牌"}
        val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key())
        val encoded=Base64.encodeToString(cipher.iv+cipher.doFinal(gson.toJson(value).toByteArray()),Base64.NO_WRAP)
        check(prefs.edit().putString(slot,encoded).commit()){"登录保存失败，请重试"};session=value
    }
    fun clear(){session=null;prefs.edit().remove(slot).commit()}
}
