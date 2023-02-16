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
import com.google.android.material.dialog.MaterialAlertDialogBuilder

internal object AcceptDialog {
    fun show(
        context: Context,
        @StringRes titleRes: Int,
        message: String,
        onAccepted: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(titleRes))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onAccepted()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }
}
