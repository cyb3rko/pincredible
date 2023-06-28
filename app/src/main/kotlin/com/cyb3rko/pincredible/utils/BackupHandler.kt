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
import java.io.File

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

    @SuppressLint("SimpleDateFormat")
    fun initiateSingleBackup(hash: String, launcher: ActivityResultLauncher<Intent>) {
        val timestamp = dateNow().toFormattedString()
        val fileName = "PIN-${hash.take(8)}-$timestamp$SINGLE_BACKUP_FILE"
        Log.d("PINcredible", "Initiated single backup: initially $fileName")
        StorageManager.launchFileCreator(launcher, fileName)
    }

    @SuppressLint("SimpleDateFormat")
    fun initiateBackup(context: Context, launcher: ActivityResultLauncher<Intent>) {
        val fileList = context.fileList()
        if (fileList.size > 1) {
            val numberOfPins = fileList.size - 1
            val timestamp = dateNow().toFormattedString()
            val fileName = "PINs[$numberOfPins]-$timestamp$MULTI_BACKUP_FILE"
            Log.d("PINcredible", "Initiated full backup: initially $fileName")
            StorageManager.launchFileCreator(launcher, fileName)
        }
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
            val bytes = ObjectSerializer.serialize(singleBackup)
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
            val fileList = context.fileList()
            if (fileList.size > 1) {
                val progressStep = 50 / (fileList.size - 1)
                val pins = mutableListOf<MultiBackupPinTable>()
                var message: String
                context.filesDir.listFiles()?.forEach {
                    if (it.name.startsWith("p") && it.name != PINS_FILE) {
                        val bytes = CryptoManager.decrypt(it)
                        val version = bytes.last()
                        val pinTable = ObjectSerializer.deserialize(bytes.withoutLast()) as PinTable
                        pins.add(MultiBackupPinTable(pinTable, version, it.name))
                        withContext(Dispatchers.Main) {
                            progressDialog.updateRelative(progressStep)
                            message = context.getString(
                                R.string.dialog_export_state_retrieving,
                                progressDialog.getProgress()
                            )
                            progressDialog.updateText(message)
                        }
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

                val nameFile = File(context.filesDir, PINS_FILE)

                @Suppress("UNCHECKED_CAST")
                val names = ObjectSerializer.deserialize(
                    CryptoManager.decrypt(nameFile)
                ) as Set<String>

                val bytes = ObjectSerializer.serialize(MultiBackupStructure(pins.toSet(), names))
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
                doRestoreSingleBackup(context, uri, input, onFinished)
            } else if (backupType == BackupType.MULTI_PIN) {
                doRestoreMultiBackup(context, uri, input, onFinished)
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

        try {
            val bytes = CryptoManager.decrypt(
                context.contentResolver.openInputStream(uri),
                input
            )
            progressDialog.updateAbsolute(25)
            progressDialog.updateText(
                context.getString(R.string.dialog_import_state_retrieving, 25)
            )

            if (bytes.isEmpty() ||
                bytes.lastN(OVERHEAD_SIZE - 1).decodeToString() != INTEGRITY_CHECK
            ) {
                progressDialog.dismiss()
                // Show password dialog again, but with error message
                restoreBackup(context, uri, true, onFinished)
                return
            }

            val version = bytes.nthLast(OVERHEAD_SIZE)
            Log.d("PINcredible Backup", "Single backup version $version found")
            val backup = ObjectSerializer.deserialize(
                bytes.withoutLastN(OVERHEAD_SIZE)
            ) as SingleBackupStructure
            progressDialog.updateAbsolute(50)
            progressDialog.updateText(context.getString(R.string.dialog_import_state_saving, 50))

            val nameFile = File(context.filesDir, PINS_FILE)
            if (nameFile.exists()) {
                CryptoManager.appendStrings(nameFile, backup.name)
            } else {
                nameFile.createNewFile()
                CryptoManager.encrypt(ObjectSerializer.serialize(backup.name), nameFile)
            }
            progressDialog.updateAbsolute(75)
            progressDialog.updateText(context.getString(R.string.dialog_import_state_saving, 75))

            val fileHash = CryptoManager.xxHash(backup.name)
            val saved = savePinFile(context, "p$fileHash", backup.pinTable, backup.siid)
            val messageRes = if (saved) {
                R.string.dialog_single_import_state_finished
            } else {
                R.string.dialog_single_import_state_cancelled
            }
            progressDialog.complete(context.getString(messageRes))
            onFinished()
        } catch (e: Exception) {
            e.printStackTrace()
            progressDialog.cancel()
            ErrorDialog.show(context, e, R.string.dialog_import_error)
        }
    }

    private fun doRestoreMultiBackup(
        context: Context,
        uri: Uri,
        input: String,
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

        try {
            val bytes = CryptoManager.decrypt(
                context.contentResolver.openInputStream(uri),
                input
            )
            progressDialog.updateAbsolute(25)
            progressDialog.updateText(
                context.getString(R.string.dialog_import_state_retrieving, 25)
            )

            if (bytes.isEmpty() ||
                bytes.lastN(OVERHEAD_SIZE - 1).decodeToString() != INTEGRITY_CHECK
            ) {
                progressDialog.dismiss()
                // Show password dialog again, but with error message
                restoreBackup(context, uri, true, onFinished)
                return
            }

            val version = bytes.nthLast(OVERHEAD_SIZE)
            Log.d("PINcredible Backup", "Full backup version $version found")
            val backup = ObjectSerializer.deserialize(
                bytes.withoutLastN(OVERHEAD_SIZE)
            ) as MultiBackupStructure
            progressDialog.updateAbsolute(50)
            progressDialog.updateText(context.getString(R.string.dialog_import_state_saving, 50))

            val nameFile = File(context.filesDir, PINS_FILE)
            if (nameFile.exists()) {
                CryptoManager.appendStrings(nameFile, *backup.names.toTypedArray())
            } else {
                nameFile.createNewFile()
                CryptoManager.encrypt(
                    ObjectSerializer.serialize(backup.names),
                    nameFile
                )
            }
            var message: String
            val progressStep = 50 / backup.pins.size
            var imports = 0
            backup.pins.forEach {
                val imported = savePinFile(context, it.fileName, it.pinTable, it.siid)
                if (imported) imports += 1
                progressDialog.updateRelative(progressStep)
                message = context.getString(
                    R.string.dialog_import_state_saving,
                    progressDialog.getProgress() + progressStep
                )
                progressDialog.updateText(message)
            }
            message = context.getString(
                R.string.dialog_multi_import_state_finished,
                imports,
                backup.pins.size
            )
            progressDialog.complete(message)
            onFinished()
        } catch (e: Exception) {
            e.printStackTrace()
            progressDialog.cancel()
            ErrorDialog.show(context, e, R.string.dialog_import_error)
        }
    }

    @Throws(CryptoManager.EnDecryptionException::class)
    private fun savePinFile(
        context: Context,
        fileName: String,
        pinTable: PinTable,
        version: Byte
    ): Boolean {
        val newPinFile = File(context.filesDir, fileName)
        return if (!newPinFile.exists()) {
            newPinFile.createNewFile()
            val bytes = ObjectSerializer.serialize(pinTable)
            CryptoManager.encrypt(bytes.plus(version), newPinFile)
            true
        } else {
            false
        }
    }

    class SingleBackupStructure(
        val pinTable: PinTable,
        val siid: Byte,
        val name: String
    ) : Serializable() {
        companion object {
            private const val serialVersionUID = -2831435563176726440

            fun getSerialUID() = serialVersionUID
        }
    }

    class MultiBackupPinTable(
        val pinTable: PinTable,
        val siid: Byte,
        val fileName: String
    ) : Serializable() {
        companion object {
            private const val serialVersionUID = 8205381827686169546

            fun getSerialUID() = serialVersionUID
        }
    }

    class MultiBackupStructure(
        val pins: Set<MultiBackupPinTable>,
        val names: Set<String>
    ) : Serializable() {
        companion object {
            private const val serialVersionUID = 3259317506686259398

            fun getSerialUID() = serialVersionUID
        }
    }
}
