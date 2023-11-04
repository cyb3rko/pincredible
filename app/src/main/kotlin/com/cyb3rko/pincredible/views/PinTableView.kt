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
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.TableLayout
import android.widget.TableRow
import androidx.annotation.ColorRes
import androidx.core.view.get
import com.cyb3rko.backpack.crypto.CryptoManager
import com.cyb3rko.backpack.utils.getDrawableCompat
import com.cyb3rko.pincredible.R
import com.cyb3rko.pincredible.data.Cell
import com.cyb3rko.pincredible.data.PinTable

internal class PinTableView(
    private val context: Context,
    attrs: AttributeSet
) : TableLayout(context, attrs) {
    init {
        isStretchAllColumns = true
        var row = PinTableRow(context)
        var currentRowIndex = 0
        var cell: PinTableCell
        iterate { _, rowIndex, _ ->
            if (currentRowIndex != rowIndex) {
                this.addView(row)
                row = PinTableRow(context)
                currentRowIndex += 1
            }
            cell = PinTableCell(context)
            row.addView(cell)
        }
        this.addView(row)
    }

    fun iterate(action: (view: PinTableView, row: Int, column: Int) -> Unit) {
        repeat(PinTable.ROW_COUNT) { row ->
            repeat(PinTable.COLUMN_COUNT) { column ->
                action(this, row, column)
            }
        }
    }

    fun getCell(row: Int, column: Int): PinTableCell {
        return (this[row] as TableRow)[column] as PinTableCell
    }

    fun fill(pinTable: PinTable) {
        var cell: PinTableCell
        iterate { _, row, column ->
            cell = getCell(row, column)
            cell.text = pinTable.get(row, column)
        }
    }

    fun colorize(pinTable: PinTable, colorBlindAlternative: Boolean) {
        var colorIndex: Int
        var backgroundInt: Int
        iterate { _, row, column ->
            colorIndex = pinTable.getBackground(row, column)
            backgroundInt = colorIndexToDrawableID(colorIndex, colorBlindAlternative)
            getCell(row, column).background = context.getDrawableCompat(backgroundInt)
        }
    }

    fun colorizeRandom(pinTable: PinTable, colorBlindAlternative: Boolean) {
        var randomIndex: Int
        var randomBackgroundInt: Int
        var randomBackground: Drawable
        iterate { _, row, column ->
            randomIndex = CryptoManager.getRandom(0, 5)
            randomBackgroundInt = colorIndexToDrawableID(randomIndex, colorBlindAlternative)
            randomBackground = context.getDrawableCompat(randomBackgroundInt)
            getCell(row, column).background = randomBackground
            pinTable.putBackground(row, column, randomIndex)
        }
    }

    fun select(cell: PinTableCell, @ColorRes backgroundInt: Int, colorBlindAlternative: Boolean) {
        val selectedBackgroundInt = colorIndexToSelectedDrawableID(
            backgroundInt,
            colorBlindAlternative
        )
        cell.background = context.getDrawableCompat(selectedBackgroundInt)
    }

    fun unselect(cell: Cell, colorBlindAlternative: Boolean) {
        val backgroundInt = colorIndexToDrawableID(cell.background, colorBlindAlternative)
        cell.view.background = context.getDrawableCompat(backgroundInt)
    }

    fun clear() {
        iterate { _, row, column ->
            getCell(row, column).text = null
        }
    }

    private fun colorIndexToDrawableID(index: Int, colorBlindAlternative: Boolean): Int {
        return when (if (!colorBlindAlternative) index else index + 5) {
            0 -> R.drawable.cell_shape_cyan
            1 -> R.drawable.cell_shape_green
            2 -> R.drawable.cell_shape_orange
            3 -> R.drawable.cell_shape_red
            4 -> R.drawable.cell_shape_yellow
            5 -> R.drawable.cell_shape_cb_blue
            6 -> R.drawable.cell_shape_cb_purple
            7 -> R.drawable.cell_shape_cb_orange
            8 -> R.drawable.cell_shape_cb_pink
            9 -> R.drawable.cell_shape_cb_yellow
            else -> -1 // Not possible to appear
        }
    }

    private fun colorIndexToSelectedDrawableID(index: Int, colorBlindAlternative: Boolean): Int {
        return when (if (!colorBlindAlternative) index else index + 5) {
            0 -> R.drawable.cell_shape_cyan_selected
            1 -> R.drawable.cell_shape_green_selected
            2 -> R.drawable.cell_shape_orange_selected
            3 -> R.drawable.cell_shape_red_selected
            4 -> R.drawable.cell_shape_yellow_selected
            5 -> R.drawable.cell_shape_cb_blue_selected
            6 -> R.drawable.cell_shape_cb_purple_selected
            7 -> R.drawable.cell_shape_cb_orange_selected
            8 -> R.drawable.cell_shape_cb_pink_selected
            9 -> R.drawable.cell_shape_cb_yellow_selected
            else -> -1 // Not possible to appear
        }
    }
}
