package io.github.alexistrejo.pimienta.pos.data.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Encrypts device refresh tokens with an Android Keystore AES key.
class DeviceCredentials(context: Context) {
    private val prefs = context.getSharedPreferences("pos-device-credentials", Context.MODE_PRIVATE)
    private val keyAlias = "pimienta-pos-device"
    private val key: SecretKey
        get() {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!store.containsAlias(keyAlias)) {
                val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                generator.init(KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
                generator.generateKey()
            }
            return (store.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
        }
    fun save(access: String, refresh: String) { prefs.edit().putString("access", encrypt(access)).putString("refresh", encrypt(refresh)).apply() }
    fun access(): String? = prefs.getString("access", null)?.let(::decrypt)
    fun refresh(): String? = prefs.getString("refresh", null)?.let(::decrypt)
    fun clear() { prefs.edit().clear().apply() }
    private fun encrypt(value: String): String { val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key); return Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8)), Base64.NO_WRAP) }
    private fun decrypt(value: String): String { val raw = Base64.decode(value, Base64.NO_WRAP); val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, raw.copyOfRange(0, 12))); return String(cipher.doFinal(raw.copyOfRange(12, raw.size)), StandardCharsets.UTF_8) }
}
