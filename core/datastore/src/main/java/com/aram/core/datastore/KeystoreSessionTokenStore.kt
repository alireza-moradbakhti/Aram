package com.aram.core.datastore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Stores `expiry|token` as AES/GCM ciphertext. The key lives in the
 * AndroidKeyStore and is created on first use; a decrypt failure (for example a
 * wiped keystore) clears the entry instead of crashing sign-in.
 */
class KeystoreSessionTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) : SessionTokenStore {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    override suspend fun read(): SessionToken? = mutex.withLock {
        val encoded = preferences.getString(KEY_SESSION, null) ?: return@withLock null
        val plain = runCatching { decrypt(encoded) }.getOrElse {
            preferences.edit().clear().apply()
            return@withLock null
        }
        val separator = plain.indexOf(PAYLOAD_SEPARATOR)
        val expiresAt = plain.take(separator).toLongOrNull()
        if (separator < 0 || expiresAt == null) {
            preferences.edit().clear().apply()
            return@withLock null
        }
        SessionToken(plain.substring(separator + 1), expiresAt)
    }

    override suspend fun write(token: SessionToken) {
        val plain = "${token.expiresAtEpochMillis}$PAYLOAD_SEPARATOR${token.value}"
        val encoded = mutex.withLock { encrypt(plain) }
        preferences.edit().putString(KEY_SESSION, encoded).apply()
    }

    override suspend fun clear() {
        mutex.withLock {
            preferences.edit().clear().apply()
        }
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val payload = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        require(payload.size > GCM_IV_BYTES) { "Session payload is too short." }
        val iv = payload.copyOfRange(0, GCM_IV_BYTES)
        val body = payload.copyOfRange(GCM_IV_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(body).toString(Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES_NAME = "aram_secure_session"
        const val KEY_SESSION = "session"
        const val KEY_ALIAS = "aram_session_token"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val KEY_BITS = 256
        const val PAYLOAD_SEPARATOR = '|'
    }
}
