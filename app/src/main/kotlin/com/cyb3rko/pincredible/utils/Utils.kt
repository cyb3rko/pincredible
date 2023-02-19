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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.TableLayout
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.cyb3rko.pincredible.R
import com.cyb3rko.pincredible.data.PinTable
import com.google.android.material.R as MaterialR
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// TableLayout extension functions

internal fun TableLayout.iterate(action: (view: TableLayout, row: Int, column: Int) -> Unit) {
    repeat(PinTable.ROW_COUNT) { row ->
        repeat(PinTable.COLUMN_COUNT) { column ->
            action(this, row, column)
        }
    }
}

// Context extension functions

internal fun Context.showDialog(
    title: String,
    message: CharSequence,
    icon: Int?,
    action: () -> Unit = {},
    actionMessage: String = "",
    cancelable: Boolean = true
) {
    val builder = MaterialAlertDialogBuilder(
        this,
        MaterialR.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
    )
        .setTitle(title)
        .setMessage(message)
        .setCancelable(cancelable)

    if (icon != null) {
        builder.setIcon(ResourcesCompat.getDrawable(resources, icon, theme))
    }

    if (actionMessage.isNotBlank()) {
        builder.setPositiveButton(actionMessage) { _, _ ->
            action()
        }
    }
    builder.show()
}

internal fun Context.showToast(message: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, length).show()
}

internal fun Context.storeToClipboard(label: String, text: String) {
    val clip = ClipData.newPlainText(label, text)
    (this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        .setPrimaryClip(clip)
}

internal fun Context.openUrl(url: String, label: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        this.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        this.storeToClipboard(label, url)
        this.showToast(getString(R.string.toast_url_failed), Toast.LENGTH_LONG)
    }
}

// Fragment extension functions

internal fun Fragment.openUrl(url: String, label: String) {
    this.requireContext().openUrl(url, label)
}

// View extension functions

internal fun View.show() {
    this.visibility = View.VISIBLE
}

internal fun View.hide() {
    this.visibility = View.GONE
}

// ByteArray extension functions

internal fun ByteArray.nthLast(n: Int) = this[this.size - n]

internal fun ByteArray.lastN(n: Int) = this.copyOfRange(this.size - n, this.size)

internal fun ByteArray.withoutLast() = this.copyOfRange(0, this.size - 1)

internal fun ByteArray.withoutLastN(n: Int) = this.copyOfRange(0, this.size - n)
