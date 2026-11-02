package com.smartexambox.security.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurityKeystoreManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        ensureMasterKey()
    }

    private fun ensureMasterKey() {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encryptString(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        // Combine IV (12 bytes for GCM) + CipherText
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decryptString(encryptedBase64: String): String? {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val iv = ByteArray(12)
            val cipherText = ByteArray(combined.size - 12)
            System.arraycopy(combined, 0, iv, 0, 12)
            System.arraycopy(combined, 12, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val plainBytes = cipher.doFinal(cipherText)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Store App PIN using SHA-256 with Cryptographic Salt.
     * Plaintext PIN is NEVER stored.
     */
    fun savePin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hash = hashPinWithSalt(pin, salt)

        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, saltBase64)
            .putBoolean(KEY_PIN_CONFIGURED, true)
            .apply()
    }

    fun isPinConfigured(): Boolean {
        return prefs.getBoolean(KEY_PIN_CONFIGURED, false)
    }

    fun verifyPin(pinInput: String): Boolean {
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val savedSaltBase64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val salt = Base64.decode(savedSaltBase64, Base64.NO_WRAP)
        val computedHash = hashPinWithSalt(pinInput, salt)
        return computedHash == savedHash
    }

    private fun hashPinWithSalt(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "SmartExamBoxMasterKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        private const val PREFS_NAME = "smart_exam_secure_prefs"
        private const val KEY_PIN_HASH = "sec_pin_hash"
        private const val KEY_PIN_SALT = "sec_pin_salt"
        private const val KEY_PIN_CONFIGURED = "sec_pin_configured"

        @Volatile
        private var instance: SecurityKeystoreManager? = null

        fun getInstance(context: Context): SecurityKeystoreManager {
            return instance ?: synchronized(this) {
                instance ?: SecurityKeystoreManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
