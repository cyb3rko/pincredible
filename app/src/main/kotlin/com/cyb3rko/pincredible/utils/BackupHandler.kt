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
import com.cyb3rko.backpack.crypto.CryptoManager
import com.cyb3rko.backpack.data.Serializable
import com.cyb3rko.backpack.managers.StorageManager
import com.cyb3rko.backpack.modals.ErrorDialog
import com.cyb3rko.backpack.utils.dateNow
import com.cyb3rko.backpack.utils.lastN
import com.cyb3rko.backpack.utils.nthLast
import com.cyb3rko.backpack.utils.toFormattedString
import com.cyb3rko.backpack.utils.withoutLast
import com.cyb3rko.backpack.utils.withoutLastN
import com.cyb3rko.pincredible.R
import com.cyb3rko.pincredible.data.PinTable
import com.cyb3rko.pincredible.modals.PasswordDialog
import com.cyb3rko.pincredible.modals.ProgressDialog
import java.io.File

internal object BackupHandler {
    const val PIN_CRYPTO_ITERATION = 0
    private const val SINGLE_BACKUP_CRYPTO_ITERATION = 0
    private const val MULTI_BACKUP_CRYPTO_ITERATION = 0
    const val PINS_FILE = "pins"

    private enum class BackupType {
        SINGLE_PIN, MULTI_PIN, UNKNOWN
    }
    private const val SINGLE_BACKUP_FILE = ".pin"
    private const val MULTI_BACKUP_FILE = ".pinc"
    private const val INTEGRITY_CHECK = "INTGRTY"
    private const val OVERHEAD_SIZE = INTEGRITY_CHECK.length + 1

    @SuppressLint("SimpleDateFormat")
    fun initiateSingleBackup(hash: String, launcher: ActivityResultLauncher<Intent>) {
        val timestamp = dateNow().toFormattedString()
        val fileName = "PIN-${hash.take(8)}-$timestamp$SINGLE_BACKUP_FILE"
        StorageManager.launchFileCreator(launcher, fileName)
    }

    @SuppressLint("SimpleDateFormat")
    fun initiateBackup(context: Context, launcher: ActivityResultLauncher<Intent>) {
        val fileList = context.fileList()
        if (fileList.size > 1) {
            val numberOfPins = fileList.size - 1
            val timestamp = dateNow().toFormattedString()
            val fileName = "PINs[$numberOfPins]-$timestamp$MULTI_BACKUP_FILE"
            StorageManager.launchFileCreator(launcher, fileName)
        }
    }

    fun runBackup(
        context: Context,
        uri: Uri,
        multiPin: Boolean,
        singleBackup: SingleBackupStructure? = null
    ) {
        PasswordDialog.show(
            context,
            R.string.dialog_backup_title
        ) { dialog, inputLayout, input ->
            if (input.isNotEmpty() && input.length in 10..100) {
                dialog.dismiss()
                if (!multiPin) {
                    runSingleExport(context, uri, CryptoManager.shaHash(input), singleBackup!!)
                } else {
                    runExport(context, uri, CryptoManager.shaHash(input))
                }
            } else {
                inputLayout.error = context.getString(R.string.dialog_name_error_length, 10, 100)
            }
        }
    }

    private fun runSingleExport(
        context: Context,
        uri: Uri,
        hash: String,
        singleBackup: SingleBackupStructure
    ) {
        val progressDialog = ProgressDialog(true).apply {
            show(
                context,
                titleRes = R.string.dialog_export_title,
                initialNote = context.getString(R.string.dialog_single_export_message)
            )
        }
        val progressBar = progressDialog.binding.progressBar
        val progressNote = progressDialog.binding.progressNote

        try {
            val bytes = ObjectSerializer.serialize(singleBackup)
            val version = SINGLE_BACKUP_CRYPTO_ITERATION.toByte()
            CryptoManager.encrypt(
                bytes.plus(version).plus(INTEGRITY_CHECK.encodeToByteArray()),
                context.contentResolver.openOutputStream(uri),
                hash.take(32)
            )

            progressBar.isIndeterminate = false
            progressBar.progress = 100
            progressNote.text = context.getString(R.string.dialog_single_export_finished)
            progressDialog.dialogReference.setCancelable(true)
        } catch (e: Exception) {
            e.printStackTrace()
            progressDialog.dialogReference.cancel()
            ErrorDialog.show(context, e, R.string.dialog_export_error)
        }
    }

    private fun runExport(context: Context, uri: Uri, hash: String) {
        val progressDialog = ProgressDialog(false).apply {
            show(
                context,
                titleRes = R.string.dialog_export_title,
                initialNote = context.getString(R.string.dialog_export_state_retrieving, 0)
            )
        }
        val progressBar = progressDialog.binding.progressBar
        val progressNote = progressDialog.binding.progressNote

        try {
            val fileList = context.fileList()
            if (fileList.size > 1) {
                val progressStep = 50 / (fileList.size - 1)
                val pins = mutableListOf<MultiBackupPinTable>()
                context.filesDir.listFiles()?.forEach {
                    if (it.name.startsWith("p") && it.name != PINS_FILE) {
                        val bytes = CryptoManager.decrypt(it)
                        val version = bytes.last()
                        val pinTable = ObjectSerializer.deserialize(bytes.withoutLast()) as PinTable
                        pins.add(MultiBackupPinTable(pinTable, version, it.name))
                        progressBar.progress = progressBar.progress + progressStep
                        progressNote.text = context.getString(
                            R.string.dialog_export_state_retrieving,
                            progressBar.progress
                        )
                    }
                }
                progressBar.progress = 50
                progressNote.text = context.getString(
                    R.string.dialog_export_state_saving,
                    50
                )

                val nameFile = File(context.filesDir, PINS_FILE)
                @Suppress("UNCHECKED_CAST")
                val names = ObjectSerializer.deserialize(
                    CryptoManager.decrypt(nameFile)
                ) as Set<String>

                val bytes = ObjectSerializer.serialize(MultiBackupStructure(pins.toSet(), names))
                val version = MULTI_BACKUP_CRYPTO_ITERATION.toByte()
                CryptoManager.encrypt(
                    bytes.plus(version).plus(INTEGRITY_CHECK.encodeToByteArray()),
                    context.contentResolver.openOutputStream(uri),
                    hash.take(32)
                )

                progressBar.progress = 100
                progressNote.text = context.getString(R.string.dialog_export_state_finished)
                progressDialog.dialogReference.setCancelable(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            progressDialog.dialogReference.cancel()
            ErrorDialog.show(context, e, R.string.dialog_export_error)
        }
    }

    fun initiateRestoreBackup(launcher: ActivityResultLauncher<Intent>) {
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
        ) { dialog, inputLayout, input ->
            if (input.isNotEmpty() && input.length in 10..100) {
                dialog.dismiss()
                if (backupType == BackupType.SINGLE_PIN) {
                    doRestoreSingleBackup(context, uri, CryptoManager.shaHash(input), onFinished)
                } else if (backupType == BackupType.MULTI_PIN) {
                    doRestoreMultiBackup(context, uri, CryptoManager.shaHash(input), onFinished)
                }
            } else {
                inputLayout.error = context.getString(R.string.dialog_name_error_length, 10, 100)
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
        hash: String,
        onFinished: () -> Unit
    ) {
        val progressDialog = ProgressDialog(false).apply {
            show(
                context,
                titleRes = R.string.dialog_import_title,
                initialNote = context.getString(R.string.dialog_import_state_retrieving, 0)
            )
        }
        val progressBar = progressDialog.binding.progressBar
        val progressNote = progressDialog.binding.progressNote

        try {
            val bytes = CryptoManager.decrypt(
                context.contentResolver.openInputStream(uri),
                hash.take(32)
            )
            progressBar.progress = 25
            progressNote.text = context.getString(R.string.dialog_import_state_retrieving, 25)

            if (bytes.isEmpty() ||
                bytes.lastN(OVERHEAD_SIZE - 1).decodeToString() != INTEGRITY_CHECK
            ) {
                progressDialog.dialogReference.dismiss()
                // Show password dialog again, but with error message
                restoreBackup(context, uri, true, onFinished)
                return
            }

            val version = bytes.nthLast(OVERHEAD_SIZE)
            Log.d("PINcredible Backup", "Backup version $version found")
            val backup = ObjectSerializer.deserialize(
                bytes.withoutLastN(OVERHEAD_SIZE)
            ) as SingleBackupStructure
            progressBar.progress = 50
            progressNote.text = context.getString(R.string.dialog_import_state_saving, 50)

            val nameFile = File(context.filesDir, PINS_FILE)
            if (nameFile.exists()) {
                CryptoManager.appendStrings(nameFile, backup.name)
            } else {
                nameFile.createNewFile()
                CryptoManager.encrypt(ObjectSerializer.serialize(backup.name), nameFile)
            }
            progressBar.progress = 75
            progressNote.text = context.getString(R.string.dialog_import_state_saving, 75)

            val fileHash = CryptoManager.xxHash(backup.name)
            val saved = savePinFile(context, "p$fileHash", backup.pinTable, backup.siid)
            progressBar.progress = 100
            if (saved) {
                progressNote.text = context.getString(R.string.dialog_single_import_state_finished)
            } else {
                progressNote.text = context.getString(R.string.dialog_single_import_state_cancelled)
            }
            progressDialog.dialogReference.setCancelable(true)
            onFinished()
        } catch (e: Exception) {
            e.printStackTrace()
            progressDialog.dialogReference.cancel()
            ErrorDialog.show(context, e, R.string.dialog_import_error)
        }
    }

    private fun doRestoreMultiBackup(
        context: Context,
        uri: Uri,
        hash: String,
        onFinished: () -> Unit
    ) {
        val progressDialog = ProgressDialog(false).apply {
            show(
                context,
                titleRes = R.string.dialog_import_title,
                initialNote = context.getString(R.string.dialog_import_state_retrieving, 0)
            )
        }
        val progressBar = progressDialog.binding.progressBar
        val progressNote = progressDialog.binding.progressNote

        try {
            val bytes = CryptoManager.decrypt(
                context.contentResolver.openInputStream(uri),
                hash.take(32)
            )
            progressBar.progress = 25
            progressNote.text = context.getString(R.string.dialog_import_state_retrieving, 25)

            if (bytes.isEmpty() ||
                bytes.lastN(OVERHEAD_SIZE - 1).decodeToString() != INTEGRITY_CHECK
            ) {
                progressDialog.dialogReference.dismiss()
                // Show password dialog again, but with error message
                restoreBackup(context, uri, true, onFinished)
                return
            }

            val version = bytes.nthLast(OVERHEAD_SIZE)
            Log.d("PINcredible Backup", "Backup version $version found")
            val backup = ObjectSerializer.deserialize(
                bytes.withoutLastN(OVERHEAD_SIZE)
            ) as MultiBackupStructure
            progressBar.progress = 50
            progressNote.text = context.getString(R.string.dialog_import_state_saving, 50)

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
            val progressStep = 50 / backup.pins.size
            var imports = 0
            backup.pins.forEach {
                val imported = savePinFile(context, it.fileName, it.pinTable, it.siid)
                if (imported) imports += 1
                progressBar.progress = progressBar.progress + progressStep
                progressNote.text = context.getString(
                    R.string.dialog_import_state_saving,
                    progressBar.progress + progressStep
                )
            }
            progressBar.progress = 100
            progressNote.text = context.getString(
                R.string.dialog_multi_import_state_finished,
                imports,
                backup.pins.size
            )
            progressDialog.dialogReference.setCancelable(true)
            onFinished()
        } catch (e: Exception) {
            e.printStackTrace()
            progressDialog.dialogReference.cancel()
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
