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

package de.cyb3rko.pincredible.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.core.content.res.ResourcesCompat
import de.cyb3rko.backpack.fragments.BackpackAnalysisFragment
import de.cyb3rko.backpack.fragments.BackpackSettingsFragment
import de.cyb3rko.backpack.interfaces.BackpackSettingsView
import de.cyb3rko.pincredible.R

internal class SettingsFragment : BackpackSettingsFragment(), BackpackSettingsView {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        bindInterface(this)
        super.onCreatePreferences(savedInstanceState, rootKey)
    }

    override fun getPreferences(): Int {
        return R.xml.preferences
    }

    override fun getPackageMainActivity(): String {
        return "de.cyb3rko.pincredible.MainActivity"
    }

    override fun getAppName(): String {
        return getString(R.string.app_name)
    }

    override fun getAppIcon(): Drawable {
        return ResourcesCompat.getDrawable(resources, R.mipmap.ic_launcher, null)!!
    }

    override fun getAnalysisFragment(): BackpackAnalysisFragment {
        return AnalysisFragment()
    }
}
