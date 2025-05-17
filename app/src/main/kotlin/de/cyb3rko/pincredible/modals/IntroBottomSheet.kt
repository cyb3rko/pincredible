/*
 * Copyright (c) 2025 Cyb3rKo
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

package de.cyb3rko.pincredible.modals

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.cyb3rko.backpack.utils.Safe
import de.cyb3rko.backpack.utils.openUrl
import de.cyb3rko.pincredible.R
import de.cyb3rko.pincredible.databinding.BottomsheetIntroBinding

class IntroBottomSheet : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = BottomsheetIntroBinding.inflate(inflater)
        binding.title.text = getString(R.string.bottomsheet_intro_title)
        binding.notice.text = getString(R.string.bottomsheet_intro_notice)
        binding.openButton.setOnClickListener {
            openUrl(getString(R.string.intro_link), LINK_LABEL)
        }
        binding.copyButton.setOnClickListener {
            val clipboard = this.activity?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
            if (clipboard == null) return@setOnClickListener
            clipboard.setPrimaryClip(
                ClipData.newPlainText(LINK_LABEL, getString(R.string.intro_link))
            )
        }
        Safe.writeBoolean(Safe.KEY_INTRO, false)
        return binding.root
    }

    companion object {
        const val TAG = "Notice Bottom Sheet"
        private const val LINK_LABEL = "Introduction YouTube video"
    }
}
