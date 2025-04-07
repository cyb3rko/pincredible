/*
 * Copyright (c) 2024 Cyb3rKo
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

package de.cyb3rko.pincredible.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.cyb3rko.backpack.crypto.CryptoManager
import de.cyb3rko.backpack.utils.ObjectSerializer
import de.cyb3rko.pincredible.utils.DebugUtils
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class HomeViewModel : ViewModel() {
    private val _appsFlow = MutableStateFlow<List<String>>(listOf())
    val appsFlow: StateFlow<List<String>> = _appsFlow.asStateFlow()

    fun loadApps(pinsFile: File) {
        viewModelScope.launch {
            _appsFlow.value = if (pinsFile.exists()) {
                val rawList = ObjectSerializer.deserialize(CryptoManager.decrypt(pinsFile))
                @Suppress("UNCHECKED_CAST")
                (rawList as Set<String>).toList()
            } else {
                listOf()
            }
        }
    }

    fun loadDemoApps(pinDir: File, pinListFile: File) {
        viewModelScope.launch {
            DebugUtils.demoData(pinDir, pinListFile)
            val rawList = ObjectSerializer.deserialize(CryptoManager.decrypt(pinListFile))
            @Suppress("UNCHECKED_CAST")
            _appsFlow.value = (rawList as Set<String>).toList()
        }
    }
}
