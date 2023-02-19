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

package com.cyb3rko.pincredible.modals

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.cyb3rko.pincredible.R
import com.cyb3rko.pincredible.databinding.DialogPasswordBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

internal object PasswordDialog {
    fun show(
        context: Context,
        @StringRes title: Int,
        showError: Boolean = false,
        onSave: (
            dialog: AlertDialog,
            inputLayout: TextInputLayout,
            input: String
        ) -> Unit
    ) {
        val binding = DialogPasswordBinding.inflate((context as FragmentActivity).layoutInflater)
        if (showError) binding.inputLayout.error = context.getString(R.string.dialog_password_error)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(title))
            .setView(binding.root)
            .setPositiveButton(context.getString(R.string.dialog_input_button1), null)
            .setNegativeButton(context.getString(R.string.dialog_input_button2), null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val input = binding.input.text.toString()
                onSave(dialog, binding.inputLayout, input.trim())
            }
        }
        dialog.show()
    }
}
