package com.tripnext.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class TripSession(val apiUrl: String, val token: String, val userId: String, val name: String, val email: String)

class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("tripnext_session", Context.MODE_PRIVATE)
    fun load(): TripSession? {
        val encrypted = preferences.getString("token_encrypted", null) ?: return null
        val token = runCatching { decrypt(encrypted) }.getOrElse { clear(); return null }
        return TripSession(preferences.getString("api_url", "").orEmpty(), token, preferences.getString("user_id", "").orEmpty(), preferences.getString("name", "").orEmpty(), preferences.getString("email", "").orEmpty())
    }
    fun save(session: TripSession) = preferences.edit().putString("api_url", session.apiUrl).putString("token_encrypted", encrypt(session.token)).putString("user_id", session.userId).putString("name", session.name).putString("email", session.email).apply()
    fun clear() = preferences.edit().clear().apply()
    fun cursor(): Long = preferences.getLong("sync_cursor", 0)
    fun saveCursor(value: Long) = preferences.edit().putLong("sync_cursor", value).apply()
    fun version(tripId: String): Long = preferences.getLong("version:$tripId", 0)
    fun saveVersion(tripId: String, value: Long) = preferences.edit().putLong("version:$tripId", value).apply()
    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        return Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray()), Base64.NO_WRAP)
    }
    private fun decrypt(value: String): String {
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, 12)
        val encrypted = bytes.copyOfRange(12, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }
    private companion object { const val KEY_ALIAS = "tripnext_session_token_v1" }
}
