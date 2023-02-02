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
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import kotlin.experimental.and
import kotlin.jvm.Throws

internal object CryptoManager {
    private const val KEYSTORE_ALIAS = "iamsecure"
    private const val ENC_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val ENC_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
    private const val ENC_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
    private const val ENC_TRANSFORMATION = "$ENC_ALGORITHM/$ENC_BLOCK_MODE/$ENC_PADDING"

    private val secureRandom by lazy { SecureRandom() }

    // Hashing

    fun hash(plaintext: String): String {
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
    private fun getEncryptCipher(): Cipher {
        return Cipher.getInstance(ENC_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getKey())
        }
    }

    @Throws(
        KeyStoreException::class,
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class
    )
    private fun getDecryptCipherForIv(iv: ByteArray): Cipher {
        return Cipher.getInstance(ENC_TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getKey(), IvParameterSpec(iv))
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
        val encryptCipher: Cipher
        try {
            encryptCipher = getEncryptCipher()
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
        FileOutputStream(file).use {
            it.write(encryptCipher.iv.size)
            it.write(encryptCipher.iv)
            it.write(encryptedBytes.size)
            it.write(encryptedBytes)
        }
        return encryptedBytes
    }

    @Throws(EnDecryptionException::class)
    fun decrypt(file: File): ByteArray {
        return FileInputStream(file).use {
            val ivSize = it.read()
            val iv = ByteArray(ivSize)
            it.read(iv)

            val encryptedBytesSize = it.read()
            val encryptedBytes = ByteArray(encryptedBytesSize)
            it.read(encryptedBytes)

            val decryptCipher: Cipher
            try {
                decryptCipher = getDecryptCipherForIv(iv)
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
            decryptCipher.doFinal(encryptedBytes)
        }
    }

    @Throws(EnDecryptionException::class)
    fun appendString(file: File, newString: String) {
        @Suppress("UNCHECKED_CAST")
        var data = ObjectSerializer.deserialize(decrypt(file)) as Set<String>
        data = data.plus(newString)
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
