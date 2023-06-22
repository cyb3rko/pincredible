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

import android.content.Context
import com.cyb3rko.backpack.crypto.CryptoManager
import com.cyb3rko.backpack.utils.ObjectSerializer
import com.cyb3rko.pincredible.data.PinTable
import java.io.File
import kotlin.random.Random

internal object DebugUtils {
    fun demoData(context: Context) {
        val names = setOf(
            "American Express",
            "Backup Number Alarm System",
            "Girlfriend's Banking Card",
            "SIM Card"
        )
        names.forEach {
            val newPinFile = File(context.filesDir, "p${CryptoManager.xxHash(it)}")
            if (!newPinFile.exists()) {
                newPinFile.createNewFile()
                val pinTable = PinTable().apply { fill() }
                repeat(PinTable.ROW_COUNT) { row ->
                    repeat(PinTable.COLUMN_COUNT) { column ->
                        pinTable.putBackground(row, column, Random.nextInt(5))
                    }
                }
                val bytes = ObjectSerializer.serialize(pinTable)
                val version = BackupHandler.PIN_CRYPTO_ITERATION.toByte()
                CryptoManager.encrypt(bytes.plus(version), newPinFile)
            }
        }

        val pinsFile = File(context.filesDir, BackupHandler.PINS_FILE)
        if (!pinsFile.exists()) {
            pinsFile.createNewFile()
            CryptoManager.encrypt(ObjectSerializer.serialize(names), pinsFile)
        } else {
            CryptoManager.appendStrings(pinsFile, *names.toTypedArray())
        }
    }
}
