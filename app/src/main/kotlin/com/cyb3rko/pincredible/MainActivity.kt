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
import androidx.navigation.findNavController
import androidx.viewbinding.ViewBinding
import com.cyb3rko.backpack.activities.BackpackMainActivity
import com.cyb3rko.backpack.interfaces.BackpackMain
import com.cyb3rko.backpack.modals.NoticeBottomSheet
import com.cyb3rko.backpack.utils.Safe
import com.cyb3rko.pincredible.databinding.ActivityMainBinding
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : BackpackMainActivity(), BackpackMain {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).asContentView()
        findNavController(R.id.nav_host_fragment_content_main).apply()
        bindInterface(this)
    }

    override fun onResume() {
        super.onResume()

        val noticeKey = "${KEY_BREAKING_RELEASE_REQUIRED}_${getVersionName()}"
        if (Safe.getBoolean(noticeKey, true)) {
            val title = getString(R.string.bottomdialog_title)
            val notice = getString(R.string.bottomdialog_notice)
            NoticeBottomSheet(title, notice) {
                Safe.writeBoolean(noticeKey, false)
            }.show(supportFragmentManager, NoticeBottomSheet.TAG)
        }
    }

    override fun getBinding(): ViewBinding {
        return binding
    }

    override fun getToolbar(): MaterialToolbar {
        return binding.toolbar
    }

    override fun getVersionName(): String {
        return getString(R.string.version_name)
    }

    override fun getGitHubLink(): String {
        return getString(R.string.github_link)
    }

    companion object {
        const val KEY_BREAKING_RELEASE_REQUIRED = "breaking_notice_required"
    }
}
