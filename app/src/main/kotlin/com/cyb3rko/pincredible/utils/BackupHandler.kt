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

package com.cyb3rko.pincredible.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleCoroutineScope
import com.cyb3rko.backpack.crypto.CryptoManager
import com.cyb3rko.backpack.crypto.CryptoManager.Hash
import com.cyb3rko.backpack.data.Serializable
import com.cyb3rko.backpack.managers.StorageManager
import com.cyb3rko.backpack.modals.ErrorDialog
import com.cyb3rko.backpack.modals.PasswordDialog
import com.cyb3rko.backpack.modals.ProgressDialog
import com.cyb3rko.backpack.utils.ObjectSerializer
import com.cyb3rko.backpack.utils.dateNow
import com.cyb3rko.backpack.utils.lastN
import com.cyb3rko.backpack.utils.nthLast
import com.cyb3rko.backpack.utils.toFormattedString
import com.cyb3rko.backpack.utils.withoutLast
import com.cyb3rko.backpack.utils.withoutLastN
import com.cyb3rko.pincredible.R
import com.cyb3rko.pincredible.data.PinTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer

internal object BackupHandler {
    const val PIN_CRYPTO_ITERATION = 0

    // v1: Use Argon2 instead of SHA-512
    private const val SINGLE_BACKUP_CRYPTO_ITERATION = 1

    // v1: Use Argon2 instead of SHA-512
    private const val MULTI_BACKUP_CRYPTO_ITERATION = 1

    private enum class BackupType {
        SINGLE_PIN, MULTI_PIN, UNKNOWN
    }
    const val PINS_FILE = "pins"
    private const val SINGLE_BACKUP_FILE = ".pin"
    private const val MULTI_BACKUP_FILE = ".pinc"
    private const val INTEGRITY_CHECK = "INTGRTY"
    private const val OVERHEAD_SIZE = INTEGRITY_CHECK.length + 1

    fun Context.pinDir() = File("${this.filesDir}/p/").apply {
        mkdir()
    }

    fun Context.pinListFile() = File("${this.filesDir}/$PINS_FILE")

    @SuppressLint("SimpleDateFormat")
    fun initiateSingleBackup(hash: String, launcher: ActivityResultLauncher<Intent>) {
        val timestamp = dateNow().toFormattedString()
        val fileName = "PIN-${hash.take(8)}-$timestamp$SINGLE_BACKUP_FILE"
        Log.d("PINcredible", "Initiated single backup: initially $fileName")
        StorageManager.launchFileCreator(launcher, fileName)
    }

    @SuppressLint("SimpleDateFormat")
    fun initiateBackup(context: Context, launcher: ActivityResultLauncher<Intent>) {
        val fileList = context.pinDir().listFiles()
        if (fileList == null || fileList.isEmpty()) return

        val timestamp = dateNow().toFormattedString()
        val fileName = "PINs[${fileList.size}]-$timestamp$MULTI_BACKUP_FILE"
        Log.d("PINcredible", "Initiated full backup: initially $fileName")
        StorageManager.launchFileCreator(launcher, fileName)
    }

    fun runBackup(
        context: Context,
        uri: Uri,
        multiPin: Boolean,
        coroutineScope: LifecycleCoroutineScope,
        singleBackup: SingleBackupStructure? = null
    ) {
        PasswordDialog.show(context, R.string.dialog_backup_title) { input ->
            val progressDialog = ProgressDialog(true).apply {
                show(
                    context,
                    titleRes = R.string.dialog_export_title,
                    initialNote = context.getString(R.string.dialog_single_export_message)
                )
            }
            coroutineScope.launch(Dispatchers.IO) {
                if (!multiPin) {
                    runSingleExport(
                        context,
                        uri,
                        CryptoManager.argon2Hash(input),
                        singleBackup!!,
                        progressDialog
                    )
                } else {
                    runExport(
                        context,
                        uri,
                        CryptoManager.argon2Hash(input),
                        progressDialog
                    )
                }
            }
        }
    }

    private suspend fun runSingleExport(
        context: Context,
        uri: Uri,
        hash: Hash,
        singleBackup: SingleBackupStructure,
        progressDialog: ProgressDialog
    ) {
        Log.d("PINcredible", "Running single export")
        try {
            val bytes = singleBackup.toBytes()
            val version = SINGLE_BACKUP_CRYPTO_ITERATION.toByte()
            CryptoManager.encrypt(
                bytes.plus(version).plus(INTEGRITY_CHECK.encodeToByteArray()),
                context.contentResolver.openOutputStream(uri),
                hash
            )
            withContext(Dispatchers.Main) {
                progressDialog.complete(context.getString(R.string.dialog_single_export_finished))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                progressDialog.cancel()
                ErrorDialog.show(context, e, R.string.dialog_export_error)
            }
        }
    }

    private suspend fun runExport(
        context: Context,
        uri: Uri,
        hash: Hash,
        progressDialog: ProgressDialog
    ) {
        Log.d("PINcredible", "Running full export")
        try {
            val fileList = context.pinDir().listFiles()
            if (fileList == null || fileList.isEmpty()) return

            val progressStep = 50 / (fileList.size)
            val pins = mutableListOf<MultiBackupPinTable>()
            var message: String
            fileList.forEach {
                if (!it.name.startsWith("p") || it.name.contains(".")) return@forEach

                val bytes = CryptoManager.decrypt(it)
                val pinTable = PinTable().loadFromBytes(bytes.withoutLast()) as PinTable
                pins.add(MultiBackupPinTable(pinTable, it.name))
                withContext(Dispatchers.Main) {
                    progressDialog.updateRelative(progressStep)
                    message = context.getString(
                        R.string.dialog_export_state_retrieving,
                        progressDialog.getProgress()
                    )
                    progressDialog.updateText(message)
                }
            }
            withContext(Dispatchers.Main) {
                progressDialog.updateAbsolute(50)
                message = context.getString(
                    R.string.dialog_export_state_saving,
                    50
                )
                progressDialog.updateText(message)
            }

            val nameFile = context.pinListFile()

            @Suppress("UNCHECKED_CAST")
            val names = ObjectSerializer.deserialize(CryptoManager.decrypt(nameFile)) as Set<String>
            val bytes = MultiBackupStructure(pins.toSet(), names).toBytes()
            val version = MULTI_BACKUP_CRYPTO_ITERATION.toByte()
            CryptoManager.encrypt(
                bytes
                    .plus(version)
                    .plus(INTEGRITY_CHECK.encodeToByteArray()),
                context.contentResolver.openOutputStream(uri),
                hash
            )
            withContext(Dispatchers.Main) {
                progressDialog.complete(
                    context.getString(R.string.dialog_export_state_finished)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                progressDialog.cancel()
                ErrorDialog.show(context, e, R.string.dialog_export_error)
            }
        }
    }

    fun initiateRestoreBackup(launcher: ActivityResultLauncher<Intent>) {
        Log.d("PINcredible", "Initiated backup restore")
        StorageManager.launchFileSelector(launcher)
    }

    fun restoreBackup(
        context: Context,
        uri: Uri,
        lifecycleScope: LifecycleCoroutineScope,
        showError: Boolean = false,
        onFinished: () -> Unit
    ) {
        val backupType = getBackupType(context, uri)
        if (backupType == BackupType.UNKNOWN) {
            ErrorDialog.showCustom(
                context,
                R.string.dialog_import_error,
                R.string.dialog_import_error_file_format_message
            )
            return
        }

        PasswordDialog.show(
            context,
            R.string.dialog_backup_title,
            showError
        ) { input ->
            if (backupType == BackupType.SINGLE_PIN) {
                doRestoreSingleBackup(context, uri, input, lifecycleScope, onFinished)
            } else if (backupType == BackupType.MULTI_PIN) {
                doRestoreMultiBackup(context, uri, input, lifecycleScope, onFinished)
            }
        }
    }

    private fun getBackupType(context: Context, uri: Uri): BackupType {
        val fileName = StorageManager.getUriFileName(context, uri) ?: return BackupType.UNKNOWN
        return if (fileName.endsWith(".pin")) {
            BackupType.SINGLE_PIN
        } else if (fileName.endsWith(".pinc")) {
            BackupType.MULTI_PIN
        } else {
            BackupType.UNKNOWN
        }
    }

    private fun doRestoreSingleBackup(
        context: Context,
        uri: Uri,
        input: String,
        lifecycleScope: LifecycleCoroutineScope,
        onFinished: () -> Unit
    ) {
        Log.d("PINcredible", "Running single backup restore")
        val progressDialog = ProgressDialog(false).apply {
            show(
                context,
                titleRes = R.string.dialog_import_title,
                initialNote = context.getString(R.string.dialog_import_state_retrieving, 0)
            )
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bytes = CryptoManager.decrypt(
                    context.contentResolver.openInputStream(uri),
                    input
                )
                val invalidBytes = bytes.isEmpty() ||
                    bytes.lastN(OVERHEAD_SIZE - 1).decodeToString() != INTEGRITY_CHECK

                withContext(Dispatchers.Main) {
                    progressDialog.updateAbsolute(25)
                    progressDialog.updateText(
                        context.getString(R.string.dialog_import_state_retrieving, 25)
                    )
                    if (invalidBytes) {
                        progressDialog.dismiss()
                        // Show password dialog again, but with error message
                        restoreBackup(context, uri, lifecycleScope, true, onFinished)
                    }
                }
                if (invalidBytes) return@launch

                val version = bytes.nthLast(OVERHEAD_SIZE)
                Log.d("PINcredible Backup", "Single backup version $version found")
                val backup = SingleBackupStructure().loadFromBytes(
                    bytes.withoutLastN(OVERHEAD_SIZE)
                ) as SingleBackupStructure
                withContext(Dispatchers.Main) {
                    progressDialog.updateAbsolute(50)
                    progressDialog.updateText(context.getString(R.string.dialog_import_state_saving, 50))
                }

                val nameFile = context.pinListFile()
                if (nameFile.exists()) {
                    CryptoManager.appendStrings(nameFile, backup.name)
                } else {
                    nameFile.createNewFile()
                    CryptoManager.encrypt(ObjectSerializer.serialize(backup.name), nameFile)
                }
                withContext(Dispatchers.Main) {
                    progressDialog.updateAbsolute(75)
                    progressDialog.updateText(context.getString(R.string.dialog_import_state_saving, 75))
                }

                val fileHash = CryptoManager.xxHash(backup.name)
                val saved = savePinFile(context, "p$fileHash", backup.pinTable, backup.getVersion())
                val messageRes = if (saved) {
                    R.string.dialog_single_import_state_finished
                } else {
                    R.string.dialog_single_import_state_cancelled
                }
                withContext(Dispatchers.Main) {
                    progressDialog.complete(context.getString(messageRes))
                    onFinished()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressDialog.cancel()
                    ErrorDialog.show(context, e, R.string.dialog_import_error)
                }
            }
        }
    }

    private fun doRestoreMultiBackup(
        context: Context,
        uri: Uri,
        input: String,
        lifecycleScope: LifecycleCoroutineScope,
        onFinished: () -> Unit
    ) {
        Log.d("PINcredible", "Running full backup restore")
        val progressDialog = ProgressDialog(false).apply {
            show(
                context,
                titleRes = R.string.dialog_import_title,
                initialNote = context.getString(R.string.dialog_import_state_retrieving, 0)
            )
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bytes = CryptoManager.decrypt(
                    context.contentResolver.openInputStream(uri),
                    input
                )
                val invalidBytes = bytes.isEmpty() ||
                    bytes.lastN(OVERHEAD_SIZE - 1).decodeToString() != INTEGRITY_CHECK

                withContext(Dispatchers.Main) {
                    progressDialog.updateAbsolute(25)
                    progressDialog.updateText(
                        context.getString(R.string.dialog_import_state_retrieving, 25)
                    )
                    if (invalidBytes) {
                        progressDialog.dismiss()
                        // Show password dialog again, but with error message
                        restoreBackup(context, uri, lifecycleScope, true, onFinished)
                    }
                }
                if (invalidBytes) return@launch

                val version = bytes.nthLast(OVERHEAD_SIZE)
                Log.d("PINcredible Backup", "Full backup version $version found")
                val backup = MultiBackupStructure().loadFromBytes(
                    bytes.withoutLastN(OVERHEAD_SIZE)
                ) as MultiBackupStructure
                withContext(Dispatchers.Main) {
                    progressDialog.updateAbsolute(50)
                    progressDialog.updateText(context.getString(R.string.dialog_import_state_saving, 50))
                }

                val nameFile = context.pinListFile()
                if (nameFile.exists()) {
                    CryptoManager.appendStrings(nameFile, *backup.names.toTypedArray())
                } else {
                    nameFile.createNewFile()
                    CryptoManager.encrypt(ObjectSerializer.serialize(backup.names), nameFile)
                }
                var message: String
                val progressStep = 50 / backup.pins.size
                var imports = 0
                backup.pins.forEach {
                    val imported = savePinFile(
                        context,
                        it.fileName,
                        it.pinTable,
                        it.pinTable.getVersion()
                    )
                    if (imported) imports += 1
                    withContext(Dispatchers.Main) {
                        progressDialog.updateRelative(progressStep)
                        message = context.getString(
                            R.string.dialog_import_state_saving,
                            progressDialog.getProgress() + progressStep
                        )
                        progressDialog.updateText(message)
                    }
                }
                withContext(Dispatchers.Main) {
                    message = context.getString(
                        R.string.dialog_multi_import_state_finished,
                        imports,
                        backup.pins.size
                    )
                    progressDialog.complete(message)
                }
                onFinished()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressDialog.cancel()
                    ErrorDialog.show(context, e, R.string.dialog_import_error)
                }
            }
        }
    }

    @Throws(CryptoManager.EnDecryptionException::class)
    private suspend fun savePinFile(
        context: Context,
        fileName: String,
        pinTable: PinTable,
        version: Byte
    ): Boolean {
        val newPinFile = File(context.pinDir(), fileName)
        return if (!newPinFile.exists()) {
            newPinFile.createNewFile()
            val bytes = pinTable.toBytes()
            CryptoManager.encrypt(bytes.plus(version), newPinFile)
            true
        } else {
            false
        }
    }

    class SingleBackupStructure() : Serializable() {
        constructor(
            pinTable: PinTable,
            name: String
        ) : this() {
            this.pinTable = pinTable
            this.name = name
        }

        lateinit var pinTable: PinTable
        lateinit var name: String

        override suspend fun loadFromBytes(bytes: ByteArray): Serializable {
            ByteArrayInputStream(bytes).use {
                val version = it.read()
                Log.d("PINcredible", "Found SingleBackupStructure v$version")
                val buffer = ByteArray(PinTable.SIZE)
                it.read(buffer)
                pinTable = PinTable().loadFromBytes(buffer) as PinTable
                name = it.readBytes().decodeToString()
            }
            return this
        }

        override suspend fun toBytes(): ByteArray {
            val stream = ByteArrayOutputStream()
            stream.use {
                it.write(byteArrayOf(getVersion()))
                val byteArray = pinTable.toBytes()
                Log.d("PINcredible", "Size of SingleBackupStructure-pinTable: ${byteArray.size}")
                it.write(byteArray)
                it.write(name.encodeToByteArray())
            }
            return stream.toByteArray()
        }

        override suspend fun getVersion(): Byte = 0
    }

    class MultiBackupPinTable() : Serializable() {
        constructor(
            pinTable: PinTable,
            fileName: String
        ) : this() {
            this.pinTable = pinTable
            this.fileName = fileName
        }

        lateinit var pinTable: PinTable
        lateinit var fileName: String

        override suspend fun loadFromBytes(bytes: ByteArray): Serializable {
            ByteArrayInputStream(bytes).use {
                val version = it.read()
                Log.d("PINcredible", "Found MultiBackupPinTable v$version")
                val buffer = ByteArray(PinTable.SIZE)
                it.read(buffer)
                pinTable = PinTable().loadFromBytes(buffer) as PinTable
                fileName = it.readBytes().decodeToString()
            }
            return this
        }

        override suspend fun toBytes(): ByteArray {
            val stream = ByteArrayOutputStream()
            stream.use {
                it.write(byteArrayOf(getVersion()))
                val byteArray = pinTable.toBytes()
                Log.d("PINcredible", "Size of MultiBackupPinTable-pinTable: ${byteArray.size}")
                it.write(byteArray)
                it.write(fileName.encodeToByteArray())
            }
            return stream.toByteArray()
        }

        override suspend fun getVersion(): Byte = 0
    }

    class MultiBackupStructure() : Serializable() {
        constructor(
            pins: Set<MultiBackupPinTable>,
            names: Set<String>
        ) : this() {
            this.pins = pins
            this.names = names
        }

        lateinit var pins: Set<MultiBackupPinTable>
        lateinit var names: Set<String>

        @Suppress("UNCHECKED_CAST")
        override suspend fun loadFromBytes(bytes: ByteArray): Serializable {
            ByteArrayInputStream(bytes).use { stream ->
                val version = stream.read()
                Log.d("PINcredible", "Found MultiBackupStrucutre v$version")
                val pinCount = stream.read()
                val pinBuffer = mutableListOf<MultiBackupPinTable>()
                val pinSizeBytes = ByteArray(2)
                var pinSize: Short
                repeat(pinCount) {
                    stream.read(pinSizeBytes)
                    pinSize = ByteBuffer.wrap(pinSizeBytes).short
                    val buffer = ByteArray(pinSize.toInt())
                    stream.read(buffer)
                    pinBuffer.add(
                        MultiBackupPinTable().loadFromBytes(buffer) as MultiBackupPinTable
                    )
                }
                pins = pinBuffer.toSet()
                names = ObjectSerializer.deserialize(stream.readBytes()) as Set<String>
            }
            return this
        }

        override suspend fun toBytes(): ByteArray {
            val stream = ByteArrayOutputStream()
            stream.use {
                it.write(byteArrayOf(getVersion()))
                it.write(byteArrayOf(pins.size.toByte()))
                pins.forEach { pin ->
                    val byteArray = pin.toBytes()
                    Log.d(
                        "PINcredible",
                        "Size of MultiBackupStructure-MultiBackupPinTable: ${byteArray.size}"
                    )
                    it.write(ByteBuffer.allocate(2).putShort(byteArray.size.toShort()).array())
                    it.write(byteArray)
                }
                it.write(ObjectSerializer.serialize(names))
            }
            return stream.toByteArray()
        }

        override suspend fun getVersion(): Byte = 0
    }
}
