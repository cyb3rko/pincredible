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

package com.cyb3rko.pincredible.data

import com.cyb3rko.backpack.crypto.CryptoManager
import com.cyb3rko.backpack.data.Serializable

internal class PinTable : Serializable() {
    private lateinit var data: Array<IntArray>
    private lateinit var pattern: Array<IntArray>

    init {
        resetDigits()
        resetPattern()
    }

    fun resetDigits() {
        data = Array(ROW_COUNT) { IntArray(COLUMN_COUNT) { -1 } }
    }

    private fun resetPattern() {
        pattern = Array(ROW_COUNT) { IntArray(COLUMN_COUNT) { -1 } }
    }

    fun isFilled(): Boolean {
        data.forEach {
            if (it.contains(-1)) return false
        }
        return true
    }

    fun put(row: Int, column: Int, value: Int) {
        if (row < 0 || row > ROW_COUNT) throw IllegalArgumentException("Invalid row index")
        if (column < 0 || column > COLUMN_COUNT) throw IllegalArgumentException("Invalid row index")
        data[row][column] = value
    }

    fun putBackground(row: Int, column: Int, background: Int) {
        if (row < 0 || row > ROW_COUNT) throw IllegalArgumentException("Invalid row index")
        if (column < 0 || column > COLUMN_COUNT) throw IllegalArgumentException("Invalid row index")
        pattern[row][column] = background
    }

    fun get(row: Int, column: Int) = data[row][column].toString()

    fun getBackground(row: Int, column: Int) = pattern[row][column]

    fun fill(ignoredIndices: Set<Int> = emptySet()) {
        data.forEachIndexed { rowIndex, row ->
            row.indices.forEach { columnIndex ->
                if (!ignoredIndices.contains(rowIndex * 7 + columnIndex)) {
                    row[columnIndex] = CryptoManager.getSecureRandom()
                }
            }
        }
    }

    companion object {
        const val ROW_COUNT = 7
        const val COLUMN_COUNT = 7
        private const val serialVersionUID = 5997637778385570065
    }
}
