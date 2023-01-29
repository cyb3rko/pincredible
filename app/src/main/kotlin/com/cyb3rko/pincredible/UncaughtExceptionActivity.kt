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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.cyb3rko.pincredible.databinding.ActivityUncaughtExceptionBinding
import com.cyb3rko.pincredible.utils.openUrl
import com.cyb3rko.pincredible.utils.showToast
import com.cyb3rko.pincredible.utils.storeToClipboard

class UncaughtExceptionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUncaughtExceptionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)

        binding = ActivityUncaughtExceptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val stacktrace = intent.getStringExtra(ExceptionHandler.EXTRA_STACKTRACE) ?: "no stacktrace"
        binding.stracktraceView.text = stacktrace
        binding.copyButton.setOnClickListener {
            storeToClipboard("Stacktrace", stacktrace)
            showToast(getString(R.string.toast_stacktrace), Toast.LENGTH_LONG)
        }
        binding.reportButton.setOnClickListener {
            openUrl(getString(R.string.github_link) + "/issues", "PINcredible GitHub")
        }
    }
}
