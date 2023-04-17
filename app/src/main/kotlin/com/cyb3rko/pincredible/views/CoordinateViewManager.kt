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

package com.cyb3rko.pincredible.views

import android.content.Context
import android.widget.LinearLayout
import androidx.core.view.children
import com.cyb3rko.backpack.utils.show
import com.cyb3rko.pincredible.SettingsActivity
import com.cyb3rko.pincredible.databinding.TableCoordinatesHoriBinding
import com.cyb3rko.pincredible.databinding.TableCoordinatesVertBinding
import com.cyb3rko.pincredible.utils.Safe
import com.google.android.material.textview.MaterialTextView

internal object CoordinateViewManager {
    private enum class Orientation {
        HORIZONTAL, VERTICAL;
    }
    private enum class Frame(val pattern: String) {
        INDEX("12345671234567"),
        CHESS("ABCDEFG7654321"),
        CHESS_REVERSE_INDEX("ABCDEFG1234567")
    }

    fun initializeViews(
        context: Context,
        row: TableCoordinatesHoriBinding,
        col1: TableCoordinatesVertBinding,
        col2: TableCoordinatesVertBinding
    ) {
        if (Safe.getString(context, SettingsActivity.KEY_COORDINATE_FRAME, "-1") != "-1") {
            fillView(context, row)
            fillView(context, col1)
            fillView(context, col2)
            row.root.show()
            col1.root.show()
            col2.root.show()
        }
    }

    private fun fillView(context: Context, view: TableCoordinatesHoriBinding) {
        lowerFillView(context, view.root, Orientation.HORIZONTAL)
    }

    private fun fillView(context: Context, view: TableCoordinatesVertBinding) {
        lowerFillView(context, view.root, Orientation.VERTICAL)
    }

    private fun lowerFillView(
        context: Context,
        view: LinearLayout,
        orientation: Orientation
    ) {
        val setting = Safe.getString(context, SettingsActivity.KEY_COORDINATE_FRAME, "-1")!!
        val frame = when (setting.toInt()) {
            0 -> Frame.INDEX
            1 -> Frame.CHESS
            2 -> Frame.CHESS_REVERSE_INDEX
            else -> Frame.INDEX // shouldn't happen
        }.pattern

        view.children.forEachIndexed { index, textView ->
            (textView as MaterialTextView).text = if (orientation == Orientation.HORIZONTAL) {
                "${frame[index]}"
            } else {
                "${frame[index + 7]}"
            }
        }
    }
}
