/*
 * Copyright (c) 2023-2025 Cyb3rKo
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

package de.cyb3rko.pincredible.data

import android.util.Log
import de.cyb3rko.backpack.crypto.CryptoManager
import de.cyb3rko.backpack.data.Serializable
import de.cyb3rko.backpack.utils.ObjectSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

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
                    row[columnIndex] = CryptoManager.getSecureRandom(10)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun loadFromBytes(bytes: ByteArray): Serializable? {
        ByteArrayInputStream(bytes).use {
            val version = it.read()
            Log.d("PINcredible", "Found PinTable v$version")
            if (version > getVersion()) {
                Log.d("PINcredible", "PinTable version not supported")
                return null
            }
            val buffer = ByteArray(307)
            it.read(buffer)
            data = ObjectSerializer.deserialize(buffer) as Array<IntArray>
            it.read(buffer)
            pattern = ObjectSerializer.deserialize(buffer) as Array<IntArray>
        }
        return this
    }

    override suspend fun toBytes(): ByteArray {
        val stream = ByteArrayOutputStream()
        stream.use {
            it.write(byteArrayOf(getVersion()))
            var byteArray = ObjectSerializer.serialize(data)
            Log.d("PINcredible", "Size of PinTable-data: ${byteArray.size}")
            it.write(byteArray)
            byteArray = ObjectSerializer.serialize(pattern)
            Log.d("PINcredible", "Size of PinTable-pattern: ${byteArray.size}")
            it.write(byteArray)
        }
        return stream.toByteArray()
    }

    override suspend fun getVersion(): Byte = 0

    companion object {
        const val ROW_COUNT = 7
        const val COLUMN_COUNT = 7
        const val SIZE = 615
    }
}
