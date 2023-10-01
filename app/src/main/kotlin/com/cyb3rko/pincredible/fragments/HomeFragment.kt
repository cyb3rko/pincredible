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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cyb3rko.backpack.crypto.CryptoManager
import com.cyb3rko.backpack.crypto.CryptoManager.EnDecryptionException
import com.cyb3rko.backpack.data.BuildInfo
import com.cyb3rko.backpack.fragments.BackpackMainFragment
import com.cyb3rko.backpack.interfaces.BackpackMainView
import com.cyb3rko.backpack.modals.ErrorDialog
import com.cyb3rko.backpack.utils.ObjectSerializer
import com.cyb3rko.backpack.utils.Vibration
import com.cyb3rko.backpack.utils.hide
import com.cyb3rko.backpack.utils.show
import com.cyb3rko.pincredible.BuildConfig
import com.cyb3rko.pincredible.MainActivity
import com.cyb3rko.pincredible.R
import com.cyb3rko.pincredible.SettingsActivity
import com.cyb3rko.pincredible.databinding.FragmentHomeBinding
import com.cyb3rko.pincredible.recycler.PinAdapter
import com.cyb3rko.pincredible.utils.BackupHandler
import com.cyb3rko.pincredible.utils.DebugUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HomeFragment : BackpackMainFragment(), BackpackMainView {
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var adapter: PinAdapter

    private val fileCreatorResultLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                BackupHandler.runBackup(myContext, uri, true, lifecycleScope)
            }
        }

    private val filePickerResultLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                BackupHandler.restoreBackup(myContext, uri, lifecycleScope) {
                    readAndShowPins()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        bindInterface(this)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        adapter = PinAdapter {
            Vibration.vibrateClick(vibrator)
            hideSubtitle()
            findNavController().navigate(HomeFragmentDirections.homeToPinviewer(it))
        }
        binding.recycler.layoutManager = LinearLayoutManager(myContext)
        binding.recycler.adapter = adapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        readAndShowPins()

        binding.fab.setOnClickListener {
            Vibration.vibrateDoubleClick(vibrator)
            hideSubtitle()
            findNavController().navigate(HomeFragmentDirections.homeToPincreator())
        }
        binding.backupFab.apply {
            setOnOpen {
                binding.fab.hide()
            }
            setOnClose {
                binding.fab.show()
            }
            setOnExport {
                BackupHandler.initiateBackup(myContext, fileCreatorResultLauncher)
            }
            setOnImport {
                BackupHandler.initiateRestoreBackup(filePickerResultLauncher)
            }
        }
        if (BuildConfig.DEBUG) {
            binding.fab.setOnLongClickListener {
                DebugUtils.demoData(myContext)
                requireActivity().finish()
                startActivity(Intent(myContext, MainActivity::class.java))
                true
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (requireActivity() as MainActivity).showSubtitle()
    }

    private fun hideSubtitle() {
        (requireActivity() as MainActivity).showSubtitle(false)
    }

    private fun readAndShowPins() {
        lifecycleScope.launch(Dispatchers.IO) {
            val pins: List<String>
            try {
                pins = retrievePins()
                Log.d("PINcredible", "${pins.size} PINs found")
                withContext(Dispatchers.Main) {
                    showSavedPins(pins)
                }
            } catch (e: EnDecryptionException) {
                Log.d("CryptoManager", e.customStacktrace)
                withContext(Dispatchers.Main) {
                    binding.progressBar.hide()
                    ErrorDialog.show(myContext, e)
                }
            }
        }
    }

    @Throws(EnDecryptionException::class)
    private fun retrievePins(): List<String> {
        val pinsFile = File(myContext.filesDir, BackupHandler.PINS_FILE)
        return if (pinsFile.exists()) {
            @Suppress("UNCHECKED_CAST")
            (ObjectSerializer.deserialize(CryptoManager.decrypt(pinsFile)) as Set<String>).toList()
        } else {
            listOf()
        }
    }

    private fun showSavedPins(pins: List<String>) {
        binding.progressBar.hide()
        if (pins.isNotEmpty()) {
            binding.emptyHintContainer.hide()
            adapter.submitList(pins.sorted())
            binding.chip.run {
                text = getString(R.string.home_found_pins, pins.size)
                show()
            }
        } else {
            binding.emptyHintContainer.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getSettingsIntent(): Intent {
        return Intent(myContext, SettingsActivity::class.java)
    }

    override fun getAnalysisNavigation(): NavDirections {
        return HomeFragmentDirections.homeToAnalysis()
    }

    override fun getGithubLink(): Int {
        return R.string.github_link
    }

    override fun getBuildInfo(): BuildInfo {
        return BuildInfo(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, BuildConfig.BUILD_TYPE)
    }

    override fun getIconCredits(): Int {
        return R.string.icon_credits
    }
}
