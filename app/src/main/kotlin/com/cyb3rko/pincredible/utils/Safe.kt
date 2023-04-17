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
import androidx.preference.PreferenceManager

internal object Safe {
    internal fun getBoolean(
        context: Context,
        key: String,
        default: Boolean
    ) = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, default)

    internal fun getString(
        context: Context,
        key: String,
        default: String
    ) = PreferenceManager.getDefaultSharedPreferences(context).getString(key, default)
}
