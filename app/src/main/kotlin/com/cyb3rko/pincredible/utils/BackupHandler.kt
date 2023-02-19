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
import androidx.activity.result.ActivityResultLauncher
import com.cyb3rko.pincredible.R
import com.cyb3rko.pincredible.crypto.CryptoManager
import com.cyb3rko.pincredible.data.PinTable
import com.cyb3rko.pincredible.modals.ErrorDialog
import com.cyb3rko.pincredible.modals.PasswordDialog
import com.cyb3rko.pincredible.modals.ProgressDialog
import java.io.File
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date

internal object BackupHandler {
    private const val MULTI_BACKUP_FILE = ".pinc"
    private const val INTEGRITY_CHECK = "INTGRTY"
    private const val OVERHEAD_SIZE = INTEGRITY_CHECK.length + 1

    @SuppressLint("SimpleDateFormat")
    fun initiateBackup(context: Context, launcher: ActivityResultLauncher<Intent>) {
        val fileList = context.fileList()
        if (fileList.size > 1) {
            val numberOfPins = fileList.size - 1
            val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss")
                .format(Date(System.currentTimeMillis()))
            val fileName = "PINs[$numberOfPins]-$timestamp$MULTI_BACKUP_FILE"
            showFileCreator(launcher, fileName)
        }
    }

    fun runBackup(context: Context, uri: Uri) {
        PasswordDialog.show(
            context,
            R.string.dialog_backup_title
        ) { dialog, inputLayout, input ->
            if (input.isNotEmpty() && input.length in 10..100) {
                dialog.dismiss()
                runExport(context, uri, CryptoManager.shaHash(input))
            } else {
                inputLayout.error = context.getString(R.string.dialog_name_error_length, 10, 100)
            }
        }
    }

    private fun runExport(context: Context, uri: Uri, hash: String) {
        val progressDialog = ProgressDialog().apply {
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
                val pins = mutableListOf<BackupPinTable>()
                context.filesDir.listFiles()?.forEach {
                    if (it.name.startsWith("p") && it.name != CryptoManager.PINS_FILE) {
                        val bytes = CryptoManager.decrypt(it)
                        val version = bytes.last()
                        val pinTable = ObjectSerializer.deserialize(bytes.withoutLast()) as PinTable
                        pins.add(BackupPinTable(pinTable, version, it.name))
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

                val nameFile = File(context.filesDir, CryptoManager.PINS_FILE)
                val names = ObjectSerializer.deserialize(
                    CryptoManager.decrypt(nameFile)
                ) as Set<String>

                val bytes = ObjectSerializer.serialize(BackupStructure(pins.toSet(), names))
                val version = CryptoManager.BACKUP_CRYPTO_ITERATION.toByte()
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
        showFilePicker(launcher)
    }

    fun restoreBackup(
        context: Context,
        uri: Uri,
        showError: Boolean = false,
        onFinished: () -> Unit
    ) {
        PasswordDialog.show(
            context,
            R.string.dialog_backup_title,
            showError
        ) { dialog, inputLayout, input ->
            if (input.isNotEmpty() && input.length in 10..100) {
                dialog.dismiss()
                doRestoreBackup(context, uri, CryptoManager.shaHash(input), onFinished)
                onFinished()
            } else {
                inputLayout.error = context.getString(R.string.dialog_name_error_length, 10, 100)
            }
        }
    }

    private fun doRestoreBackup(
        context: Context,
        uri: Uri,
        hash: String,
        onFinished: () -> Unit
    ) {
        val progressDialog = ProgressDialog().apply {
            show(
                context,
                titleRes = R.string.dialog_export_title,
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
            val backup = ObjectSerializer.deserialize(
                bytes.withoutLastN(OVERHEAD_SIZE)
            ) as BackupStructure
            progressBar.progress = 50
            progressNote.text = context.getString(R.string.dialog_import_state_saving, 50)

            val nameFile = File(context.filesDir, CryptoManager.PINS_FILE)
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
                R.string.dialog_import_state_finished,
                imports,
                backup.pins.size
            )
            progressDialog.dialogReference.setCancelable(true)
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

    private fun showFileCreator(
        launcher: ActivityResultLauncher<Intent>,
        fileName: String
    ) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        launcher.launch(intent)
    }

    private fun showFilePicker(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        launcher.launch(intent)
    }

    class BackupPinTable(
        val pinTable: PinTable,
        val siid: Byte,
        val fileName: String
    ) : Serializable

    class BackupStructure(
        val pins: Set<BackupPinTable>,
        val names: Set<String>
    ) : Serializable
}
