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

package com.cyb3rko.pincredible.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cyb3rko.backpack.data.Serializable
import com.cyb3rko.backpack.fragments.BackpackAnalysisFragment
import com.cyb3rko.backpack.interfaces.BackpackAnalysis
import com.cyb3rko.pincredible.data.PinTable
import kotlin.random.Random

class AnalysisFragment : BackpackAnalysisFragment(), BackpackAnalysis {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindInterface(this)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun setRandomPattern(pinTable: PinTable) {
        repeat(PinTable.ROW_COUNT) { row ->
            repeat(PinTable.COLUMN_COUNT) { column ->
                pinTable.put(row, column, Random.nextInt(5))
            }
        }
    }

    override fun useRandom(): Boolean {
        return true
    }

    override fun getDemoData(): Serializable {
        val pinTable = PinTable().apply {
            fill()
            setRandomPattern(this)
        }
        return pinTable
    }
}
