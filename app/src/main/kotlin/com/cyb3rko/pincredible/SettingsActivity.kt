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

package com.cyb3rko.pincredible

import android.os.Bundle
import com.cyb3rko.backpack.activities.BackpackSettingsActivity
import com.cyb3rko.backpack.fragments.BackpackSettingsFragment
import com.cyb3rko.backpack.interfaces.BackpackSettings
import com.cyb3rko.pincredible.fragments.SettingsFragment

internal class SettingsActivity : BackpackSettingsActivity(), BackpackSettings {
    override fun onCreate(savedInstanceState: Bundle?) {
        bindInterface(this)
        super.onCreate(savedInstanceState)
    }

    override fun getPreferences(): Int {
        return R.xml.preferences
    }

    override fun getSettingsFragment(): BackpackSettingsFragment {
        return SettingsFragment()
    }

    companion object {
        const val KEY_BUTTON_RANDOMIZER = "button_randomizer"
        const val KEY_COORDINATE_FRAME = "coordinate_frame"
    }
}
