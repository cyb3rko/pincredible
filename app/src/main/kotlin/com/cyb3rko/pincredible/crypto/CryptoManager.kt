/*
 * Copyright (c) 2023 Cyb3rKo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyb3rko.pincredible.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.cyb3rko.pincredible.BuildConfig
import com.cyb3rko.pincredible.crypto.xxhash3.XXH3_128
import com.cyb3rko.pincredible.utils.ObjectSerializer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

internal object CryptoManager {
    const val PIN_CRYPTO_ITERATION = 0
    const val SINGLE_BACKUP_CRYPTO_ITERATION = 0
    const val MULTI_BACKUP_CRYPTO_ITERATION = 0
    const val PINS_FILE = "pins"
    private const val KEYSTORE_ALIAS = "iamsecure"
    private const val ENC_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val ENC_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
    private const val ENC_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
    private const val ENC_TRANSFORMATION = "$ENC_ALGORITHM/$ENC_BLOCK_MODE/$ENC_PADDING"

    private val secureRandom by lazy { SecureRandom() }

    // Hashing

    fun xxHash(plaintext: String): String {
        val ciphertextBytes = XXH3_128().digest(plaintext.toByteArray())
        val sb = StringBuilder()
        for (i in ciphertextBytes.indices) {
            sb.append(((ciphertextBytes[i] and 0xff.toByte()) + 0x100).toString(16).substring(1))
        }
        val hash = sb.toString()
        if (BuildConfig.DEBUG) {
            Log.d("CryptoManager", "Hash: $hash")
        }
        return hash
    }

    fun shaHash(plaintext: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(plaintext.toByteArray(Charset.defaultCharset()))
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String {
        return joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
    }

    // Random

    fun getSecureRandom() = secureRandom.nextInt(10)

    // Encryption / Decryption

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    @Throws(
        KeyStoreException::class,
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class
    )
    private fun getEncryptCipher(key: String?): Cipher {
        val secretKey = if (key != null) {
            SecretKeySpec(key.toByteArray(), ENC_ALGORITHM)
        } else {
            null
        }
        return Cipher.getInstance(ENC_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey ?: getKey())
        }
    }

    @Throws(
        KeyStoreException::class,
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class
    )
    private fun getDecryptCipherForIv(iv: ByteArray, key: String?): Cipher {
        val secretKey = if (key != null) {
            SecretKeySpec(key.toByteArray(), ENC_ALGORITHM)
        } else {
            null
        }
        return Cipher.getInstance(ENC_TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey ?: getKey(), IvParameterSpec(iv))
        }
    }

    @Throws(
        KeyStoreException::class,
        NoSuchAlgorithmException::class
    )
    private fun getKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        return KeyGenerator.getInstance(ENC_ALGORITHM).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(ENC_BLOCK_MODE)
                    .setEncryptionPaddings(ENC_PADDING)
                    .setUserAuthenticationRequired(false)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
        }.generateKey()
    }

    @Throws(EnDecryptionException::class)
    fun encrypt(data: ByteArray, file: File): ByteArray {
        return doEncrypt(data, FileOutputStream(file), null)
    }

    @Throws(EnDecryptionException::class)
    fun encrypt(
        data: ByteArray,
        outputStream: OutputStream?,
        key: String
    ): ByteArray {
        return doEncrypt(data, outputStream as FileOutputStream, key)
    }

    @Throws(EnDecryptionException::class)
    private fun doEncrypt(
        data: ByteArray,
        outputStream: FileOutputStream,
        key: String? = null
    ): ByteArray {
        val encryptCipher: Cipher
        try {
            encryptCipher = getEncryptCipher(key)
        } catch (e: KeyStoreException) {
            throw EnDecryptionException(
                "The KeyStore access failed.",
                e.stackTraceToString()
            )
        } catch (e: NoSuchAlgorithmException) {
            throw EnDecryptionException(
                "The requested algorithm $ENC_ALGORITHM is not supported.",
                e.stackTraceToString()
            )
        } catch (e: NoSuchPaddingException) {
            throw EnDecryptionException(
                "The requested padding $ENC_PADDING is not supported.",
                e.stackTraceToString()
            )
        }

        val encryptedBytes = encryptCipher.doFinal(data)
        outputStream.use {
            it.write(encryptCipher.iv)
            it.write(encryptedBytes)
        }
        return encryptedBytes
    }

    @Throws(EnDecryptionException::class)
    fun decrypt(file: File): ByteArray {
        return doDecrypt(FileInputStream(file), null)
    }

    @Throws(EnDecryptionException::class)
    fun decrypt(inputStream: InputStream?, key: String): ByteArray {
        return doDecrypt(inputStream as FileInputStream, key)
    }

    @Throws(EnDecryptionException::class)
    private fun doDecrypt(inputStream: FileInputStream, key: String? = null): ByteArray {
        return inputStream.use {
            val iv = ByteArray(16)
            it.read(iv)
            val encryptedBytes = it.readBytes()

            val decryptCipher: Cipher
            try {
                decryptCipher = getDecryptCipherForIv(iv, key)
            } catch (e: KeyStoreException) {
                throw EnDecryptionException(
                    "The KeyStore access failed.",
                    e.stackTraceToString()
                )
            } catch (e: NoSuchAlgorithmException) {
                throw EnDecryptionException(
                    "The requested algorithm $ENC_ALGORITHM is not supported.",
                    e.stackTraceToString()
                )
            } catch (e: NoSuchPaddingException) {
                throw EnDecryptionException(
                    "The requested padding $ENC_PADDING is not supported.",
                    e.stackTraceToString()
                )
            }
            try {
                decryptCipher.doFinal(encryptedBytes)
            } catch (e: BadPaddingException) {
                if (e.stackTraceToString().contains("BAD_DECRYPT")) {
                    e.printStackTrace()
                    ByteArray(0)
                } else {
                    throw e
                }
            }
        }
    }

    @Throws(EnDecryptionException::class)
    fun appendStrings(file: File, vararg newStrings: String) {
        @Suppress("UNCHECKED_CAST")
        var data = ObjectSerializer.deserialize(decrypt(file)) as Set<String>
        newStrings.forEach {
            if (!data.contains(it)) {
                data = data.plus(it)
            }
        }
        encrypt(ObjectSerializer.serialize(data), file)
    }

    @Throws(EnDecryptionException::class)
    fun removeString(file: File, string: String) {
        @Suppress("UNCHECKED_CAST")
        var data = ObjectSerializer.deserialize(decrypt(file)) as Set<String>
        data = data.minus(string)
        encrypt(ObjectSerializer.serialize(data), file)
    }

    class EnDecryptionException(
        message: String,
        val customStacktrace: String
    ) : Exception(message)
}
