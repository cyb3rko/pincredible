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
import com.cyb3rko.pincredible.modals.PasswordDialog
import com.cyb3rko.pincredible.modals.ProgressDialog
import java.io.File
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date

internal object BackupHandler {
    @SuppressLint("SimpleDateFormat")
    fun initiateBackup(context: Context, launcher: ActivityResultLauncher<Intent>) {
        val fileList = context.fileList()
        if (fileList.size > 1) {
            val numberOfPins = fileList.size - 1
            val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss")
                .format(Date(System.currentTimeMillis()))
            val fileName = "PINs[$numberOfPins]-$timestamp.pinc"
            showFilePicker(launcher, fileName)
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
        val progressDialog = ProgressDialog.show(
            context,
            titleRes = R.string.dialog_export_title,
            initialNote = context.getString(R.string.dialog_export_state_retrieving, 0)
        )
        val progressBar = progressDialog.progressBar
        val progressNote = progressDialog.progressNote

        val fileList = context.fileList()
        if (fileList.size > 1) {
            val progressStep = 50 / (fileList.size - 1)
            val pins = mutableListOf<PinTable>()
            context.filesDir.listFiles()?.forEach {
                if (it.name.startsWith("p") && it.name != "pins") {
                    pins.add(ObjectSerializer.deserialize(CryptoManager.decrypt(it)) as PinTable)
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

            val nameFile = File(context.filesDir, "pins")
            val names = ObjectSerializer.deserialize(CryptoManager.decrypt(nameFile)) as Set<String>

            CryptoManager.encrypt(
                ObjectSerializer.serialize(BackupStructure(pins.toSet(), names)),
                context.contentResolver.openOutputStream(uri),
                hash.take(32)
            )

            progressBar.progress = 100
            progressNote.text = context.getString(R.string.dialog_export_state_finished)
        }
    }

    private fun showFilePicker(
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

    class BackupStructure(val pins: Set<PinTable>, val names: Set<String>) : Serializable
}
