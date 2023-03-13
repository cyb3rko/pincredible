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
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.get
import com.cyb3rko.pincredible.R
import com.cyb3rko.pincredible.data.Cell
import com.cyb3rko.pincredible.data.PinTable
import kotlin.random.Random

internal class PinTableView(
    private val context: Context,
    attrs: AttributeSet
) : TableLayout(context, attrs) {
    init {
        inflate(context, R.layout.table_view, this)
    }

    fun iterate(action: (view: PinTableView, row: Int, column: Int) -> Unit) {
        repeat(PinTable.ROW_COUNT) { row ->
            repeat(PinTable.COLUMN_COUNT) { column ->
                action(this, row, column)
            }
        }
    }

    fun getCell(row: Int, column: Int): TextView {
        return ((this[0] as TableLayout)[row] as TableRow)[column] as TextView
    }

    fun fill(pinTable: PinTable) {
        iterate { _, row, column ->
            getCell(row, column).text = pinTable.get(row, column)
        }
    }

    fun colorize(pinTable: PinTable) {
        var backgroundInt: Int
        iterate { _, row, column ->
            backgroundInt = when (pinTable.getBackground(row, column)) {
                0 -> R.drawable.cell_shape_cyan
                1 -> R.drawable.cell_shape_green
                2 -> R.drawable.cell_shape_orange
                3 -> R.drawable.cell_shape_red
                4 -> R.drawable.cell_shape_yellow
                else -> -1 // Not possible to appear
            }
            getCell(row, column).background = ResourcesCompat.getDrawable(
                resources,
                backgroundInt,
                context.theme
            )!!
        }
    }

    fun colorizeRandom(pinTable: PinTable) {
        var randomIndex: Int
        var randomBackgroundInt: Int
        var randomBackground: Drawable
        iterate { _, row, column ->
            randomIndex = Random.nextInt(5)
            randomBackgroundInt = when (randomIndex) {
                0 -> R.drawable.cell_shape_cyan
                1 -> R.drawable.cell_shape_green
                2 -> R.drawable.cell_shape_orange
                3 -> R.drawable.cell_shape_red
                4 -> R.drawable.cell_shape_yellow
                else -> -1 // Not possible to appear
            }
            randomBackground = ResourcesCompat.getDrawable(
                resources,
                randomBackgroundInt,
                context.theme
            )!!
            getCell(row, column).background = randomBackground
            pinTable.putBackground(row, column, randomIndex)
        }
    }

    fun select(cell: TextView, @ColorRes backgroundInt: Int) {
        val selectedBackgroundInt = when (backgroundInt) {
            0 -> R.drawable.cell_shape_cyan_selected
            1 -> R.drawable.cell_shape_green_selected
            2 -> R.drawable.cell_shape_orange_selected
            3 -> R.drawable.cell_shape_red_selected
            4 -> R.drawable.cell_shape_yellow_selected
            else -> -1 // Not possible to appear
        }
        cell.background = ResourcesCompat.getDrawable(
            resources,
            selectedBackgroundInt,
            context.theme
        )!!
    }

    fun unselect(cell: Cell) {
        val backgroundInt = when (cell.background) {
            0 -> R.drawable.cell_shape_cyan
            1 -> R.drawable.cell_shape_green
            2 -> R.drawable.cell_shape_orange
            3 -> R.drawable.cell_shape_red
            4 -> R.drawable.cell_shape_yellow
            else -> -1 // Not possible to appear
        }
        cell.view.background = ResourcesCompat.getDrawable(
            resources,
            backgroundInt,
            context.theme
        )!!
    }

    fun clear() {
        iterate { _, row, column ->
            getCell(row, column).text = null
        }
    }
}
