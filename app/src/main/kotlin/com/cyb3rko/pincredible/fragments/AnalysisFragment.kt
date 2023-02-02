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

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cyb3rko.pincredible.R
import com.cyb3rko.pincredible.crypto.CryptoManager
import com.cyb3rko.pincredible.crypto.CryptoManager.EnDecryptionException
import com.cyb3rko.pincredible.data.PinTable
import com.cyb3rko.pincredible.databinding.FragmentAnalysisBinding
import com.cyb3rko.pincredible.modals.ErrorDialog
import com.cyb3rko.pincredible.utils.ObjectSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class AnalysisFragment : Fragment() {
    private var _binding: FragmentAnalysisBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var myContext: Context

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        myContext = requireContext()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CryptoManager.hash("") // initialize hash mechanism
        binding.hashingCard.setOnClickListener { runHash() }

        binding.encryptionCard.setOnClickListener { runEncryption() }
        binding.decryptionCard.setOnClickListener { runDecryption() }

        val sRP = getSecureRandomProvider()
        binding.randomSource.text = getString(
            R.string.analysis_provider_info,
            sRP.name,
            sRP.version.toString(),
            sRP.info
        )

        val kP = getKeyStoreProvider()
        binding.keystoreProvider.text = getString(
            R.string.analysis_provider_info,
            kP.name,
            kP.version.toString(),
            kP.info
        )

        lifecycleScope.launch {
            lifecycleScope.launch((Dispatchers.Main)) {
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun runHash() {
        val time = measureTime { CryptoManager.hash("This is a test") }
        binding.hashingSpeed.text = getString(
            R.string.analysis_hashing_result,
            time.toString()
        )
    }

    private fun setRandomPattern(pinTable: PinTable) {
        repeat(PinTable.ROW_COUNT) { row ->
            repeat(PinTable.COLUMN_COUNT) { column ->
                pinTable.put(row, column, Random.nextInt(5))
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun runEncryption() {
        try {
            val pinTable = PinTable()
            pinTable.fill()
            setRandomPattern(pinTable)
            val tempFile = File(myContext.filesDir, "enc-test")
            tempFile.createNewFile()
            val time = measureTime {
                CryptoManager.encrypt(ObjectSerializer.serialize(pinTable), tempFile)
            }
            tempFile.delete()

            binding.encryptionSpeed.text = getString(
                R.string.analysis_encryption_result,
                time.toString()
            )
        } catch (e: EnDecryptionException) {
            Log.d("CryptoManager", e.customStacktrace)
            ErrorDialog.show(myContext, e)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun runDecryption() {
        try {
            val pinTable = PinTable()
            pinTable.fill()
            setRandomPattern(pinTable)
            val tempFile = File(myContext.filesDir, "dec-test")
            tempFile.createNewFile()
            CryptoManager.encrypt(ObjectSerializer.serialize(pinTable), tempFile)
            val time = measureTime { CryptoManager.decrypt(tempFile) }
            tempFile.delete()

            binding.decryptionSpeed.text = getString(
                R.string.analysis_decryption_result,
                time.toString()
            )
        } catch (e: EnDecryptionException) {
            Log.d("CryptoManager", e.customStacktrace)
            ErrorDialog.show(myContext, e)
        }
    }

    private fun getKeyStoreProvider() = KeyStore.getInstance("AndroidKeyStore").provider

    private fun getSecureRandomProvider() = SecureRandom().provider

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
