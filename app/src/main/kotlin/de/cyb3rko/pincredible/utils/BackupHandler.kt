/*
 * Copyright (c) 2023-2025 Cyb3rKo
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

package de.cyb3rko.pincredible.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleCoroutineScope
import de.cyb3rko.backpack.crypto.CryptoManager
import de.cyb3rko.backpack.crypto.CryptoManager.Hash
import de.cyb3rko.backpack.data.Serializable
import de.cyb3rko.backpack.managers.StorageManager
import de.cyb3rko.backpack.modals.ErrorDialog
import de.cyb3rko.backpack.modals.PasswordDialog
import de.cyb3rko.backpack.modals.ProgressDialog
import de.cyb3rko.backpack.modals.VersionNotSupportedDialog
import de.cyb3rko.backpack.utils.ObjectSerializer
import de.cyb3rko.backpack.utils.dateNow
import de.cyb3rko.backpack.utils.lastN
import de.cyb3rko.backpack.utils.logD
import de.cyb3rko.backpack.utils.toFormattedString
import de.cyb3rko.backpack.utils.withoutLastN
import de.cyb3rko.pincredible.R
import de.cyb3rko.pincredible.data.PinTable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal object BackupHandler {
    private enum class BackupType {
        SINGLE_PIN,
        MULTI_PIN,
        UNKNOWN
    }

    private const val TAG = "PINcredible-Backup"

    private const val PINS_FILE = "pins"
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
        logD(TAG, "Initiated single backup: initially $fileName")
        StorageManager.launchFileCreator(launcher, fileName)
    }

    @SuppressLint("SimpleDateFormat")
    fun initiateBackup(context: Context, launcher: ActivityResultLauncher<Intent>) {
        val fileList = context.pinDir().listFiles()
        if (fileList == null || fileList.isEmpty()) return

        val timestamp = dateNow().toFormattedString()
        val fileName = "PINs[${fileList.size}]-$timestamp$MULTI_BACKUP_FILE"
        logD(TAG, "Initiated full backup: initially $fileName")
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
                    initialNote = context.getString(R.string.dialog_export_message)
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
        logD(TAG, "Running single export")
        try {
            val bytes = singleBackup.toBytes()
            CryptoManager.encrypt(
                bytes.plus(INTEGRITY_CHECK.encodeToByteArray()),
                context.contentResolver.openOutputStream(uri),
                hash
            )
            withContext(Dispatchers.Main) {
                progressDialog.complete(context.getString(R.string.dialog_export_finished))
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
        logD(TAG, "Running full export")
        try {
            val fileList = context.pinDir().listFiles()
            if (fileList == null || fileList.isEmpty()) return

            val progressStep = 50 / (fileList.size)
            val pins = mutableListOf<MultiBackupPinTable>()
            var message: String
            fileList.forEach {
                if (!it.name.startsWith("p") || it.name.contains(".")) return@forEach

                val bytes = CryptoManager.decrypt(it)
                val pinTable = PinTable().loadFromBytes(bytes) as PinTable
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
            CryptoManager.encrypt(
                bytes.plus(INTEGRITY_CHECK.encodeToByteArray()),
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
        logD(TAG, "Initiated backup restore")
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
        logD(TAG, "Running single backup restore")
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

                val backup = SingleBackupStructure().loadFromBytes(
                    bytes.withoutLastN(OVERHEAD_SIZE - 1)
                ) as SingleBackupStructure?
                if (backup == null) {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        VersionNotSupportedDialog.show(context)
                    }
                    return@launch
                }
                logD(TAG, "Single backup version ${backup.getVersion()} found")
                withContext(Dispatchers.Main) {
                    progressDialog.updateAbsolute(50)
                    progressDialog.updateText(
                        context.getString(R.string.dialog_import_state_saving, 50)
                    )
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
                    progressDialog.updateText(
                        context.getString(R.string.dialog_import_state_saving, 75)
                    )
                }

                val fileHash = CryptoManager.xxHash(backup.name)
                val saved = savePinFile(context, "p$fileHash", backup.pinTable)
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
        logD(TAG, "Running full backup restore")
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

                val backup = MultiBackupStructure().loadFromBytes(
                    bytes.withoutLastN(OVERHEAD_SIZE - 1)
                ) as MultiBackupStructure?
                if (backup == null) {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        VersionNotSupportedDialog.show(context)
                    }
                    return@launch
                }
                logD(TAG, "Full backup version ${backup.getVersion()} found")
                withContext(Dispatchers.Main) {
                    progressDialog.updateAbsolute(50)
                    progressDialog.updateText(
                        context.getString(R.string.dialog_import_state_saving, 50)
                    )
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
                        it.pinTable
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
        pinTable: PinTable
    ): Boolean {
        val newPinFile = File(context.pinDir(), fileName)
        return if (!newPinFile.exists()) {
            newPinFile.createNewFile()
            val bytes = pinTable.toBytes()
            CryptoManager.encrypt(bytes, newPinFile)
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

        override suspend fun loadFromBytes(bytes: ByteArray): Serializable? {
            logD(TAG, "Found ${bytes.size} bytes for $CLASS_NAME(616+n)")
            ByteArrayInputStream(bytes).use {
                val version = it.read()
                logD(TAG, "Found $CLASS_NAME v$version")
                if (version > getVersion()) {
                    logD(TAG, "$CLASS_NAME version not supported")
                    return null
                }
                logD(TAG, "Size of $CLASS_NAME-version: ${Byte.SIZE_BYTES} bytes")
                val buffer = ByteArray(PinTable.SIZE)
                it.read(buffer)
                logD(TAG, "Size of $CLASS_NAME-pinTable: ${buffer.size} bytes")
                pinTable = PinTable().loadFromBytes(buffer) as PinTable
                val tempName = it.readBytes()
                logD(TAG, "Size of $CLASS_NAME-name: ${tempName.size} bytes")
                name = tempName.decodeToString()
            }
            return this
        }

        override suspend fun toBytes(): ByteArray {
            val stream = ByteArrayOutputStream()
            stream.use {
                val version = byteArrayOf(getVersion())
                logD(TAG, "Size of $CLASS_NAME-version: ${version.size} bytes")
                it.write(version)
                val byteArray = pinTable.toBytes()
                logD(TAG, "Size of $CLASS_NAME-pinTable: ${byteArray.size} bytes")
                it.write(byteArray)
                val name = name.encodeToByteArray()
                logD(TAG, "Size of $CLASS_NAME-name: ${name.size} bytes")
                it.write(name)
            }
            val output = stream.toByteArray()
            logD(TAG, "Size of $CLASS_NAME(616+n): ${output.size} bytes")
            return output
        }

        override suspend fun getVersion(): Byte = 0

        companion object {
            private const val CLASS_NAME = "SingleBackupStructure"
            private const val TAG = "PINcredible-SBS"
        }
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

        override suspend fun loadFromBytes(bytes: ByteArray): Serializable? {
            logD(TAG, "Found ${bytes.size} bytes for $CLASS_NAME(???)")
            ByteArrayInputStream(bytes).use {
                val version = it.read()
                logD(TAG, "Found $CLASS_NAME v$version")
                if (version > getVersion()) {
                    logD(TAG, "$CLASS_NAME version not supported")
                    return null
                }
                logD(TAG, "Size of $CLASS_NAME-version: ${Byte.SIZE_BYTES} bytes")
                val buffer = ByteArray(PinTable.SIZE)
                it.read(buffer)
                logD(TAG, "Size of $CLASS_NAME-pinTable: ${buffer.size} bytes")
                pinTable = PinTable().loadFromBytes(buffer) as PinTable
                var tempFileName = it.readBytes()
                logD(TAG, "Size of $CLASS_NAME-fileName: ${tempFileName.size} bytes")
                fileName = tempFileName.decodeToString()
            }
            return this
        }

        override suspend fun toBytes(): ByteArray {
            val stream = ByteArrayOutputStream()
            stream.use {
                val version = byteArrayOf(getVersion())
                logD(TAG, "Size of $CLASS_NAME-version: ${version.size} bytes")
                it.write(version)
                val byteArray = pinTable.toBytes()
                logD(TAG, "Size of $CLASS_NAME-pinTable: ${byteArray.size} bytes")
                it.write(byteArray)
                val fileName = fileName.encodeToByteArray()
                logD(TAG, "Size of $CLASS_NAME-fileName: ${fileName.size} bytes")
                it.write(fileName)
            }
            val output = stream.toByteArray()
            logD(TAG, "Size of $CLASS_NAME(???): ${output.size} bytes")
            return output
        }

        override suspend fun getVersion(): Byte = 0

        companion object {
            private const val CLASS_NAME = "MultiBackupPinTable"
            private const val TAG = "PINcredible-MBPT"
        }
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
        override suspend fun loadFromBytes(bytes: ByteArray): Serializable? {
            logD(TAG, "Found ${bytes.size} bytes for $CLASS_NAME(???)")
            ByteArrayInputStream(bytes).use {
                val version = it.read()
                logD(TAG, "Found $CLASS_NAME v$version")
                if (version > getVersion()) {
                    logD(TAG, "$CLASS_NAME version not supported")
                    return null
                }
                logD(TAG, "Size of $CLASS_NAME-version: ${Byte.SIZE_BYTES} bytes")
                val pinCount = it.read()
                logD(TAG, "Parsing $pinCount PINs")
                val pinBuffer = mutableListOf<MultiBackupPinTable>()
                val pinSizeBytes = ByteArray(2)
                var pinSize: Short
                repeat(pinCount) { i ->
                    it.read(pinSizeBytes)
                    pinSize = ByteBuffer.wrap(pinSizeBytes).short
                    val buffer = ByteArray(pinSize.toInt())
                    it.read(buffer)
                    logD(TAG, "Size of $CLASS_NAME-pins[$i]: ${buffer.size} bytes")
                    pinBuffer.add(
                        MultiBackupPinTable().loadFromBytes(buffer) as MultiBackupPinTable
                    )
                }
                pins = pinBuffer.toSet()
                val tempNames = it.readBytes()
                logD(TAG, "Size of $CLASS_NAME-names: ${tempNames.size} bytes")
                names = ObjectSerializer.deserialize(tempNames) as Set<String>
            }
            return this
        }

        override suspend fun toBytes(): ByteArray {
            val stream = ByteArrayOutputStream()
            stream.use {
                val version = byteArrayOf(getVersion())
                logD(TAG, "Size of $CLASS_NAME-version: ${version.size} bytes")
                it.write(version)
                val pinCount = pins.size.toByte()
                logD(TAG, "Parsing $pinCount PINs")
                it.write(byteArrayOf(pinCount))
                pins.forEachIndexed { i, pin ->
                    val byteArray = pin.toBytes()
                    logD(TAG, "Size of $CLASS_NAME-pins[$i]: ${byteArray.size} bytes")
                    it.write(ByteBuffer.allocate(2).putShort(byteArray.size.toShort()).array())
                    it.write(byteArray)
                }
                val names = ObjectSerializer.serialize(names)
                logD(TAG, "Size of $CLASS_NAME-names: ${names.size} bytes")
                it.write(names)
            }
            val output = stream.toByteArray()
            logD(TAG, "Size of $CLASS_NAME(???): ${output.size} bytes")
            return output
        }

        override suspend fun getVersion(): Byte = 0

        companion object {
            private const val CLASS_NAME = "MultiBackupStructure"
            private const val TAG = "PINcredible-MBS"
        }
    }
}
