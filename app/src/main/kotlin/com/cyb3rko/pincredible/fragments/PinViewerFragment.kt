/*
 * Copyright (c) 2022-2023 Cyb3rKo
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
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.cyb3rko.pincredible.R
import com.cyb3rko.pincredible.crypto.CryptoManager
import com.cyb3rko.pincredible.crypto.CryptoManager.EnDecryptionException
import com.cyb3rko.pincredible.data.PinTable
import com.cyb3rko.pincredible.databinding.FragmentPinViewerBinding
import com.cyb3rko.pincredible.modals.ErrorDialog
import com.cyb3rko.pincredible.utils.Vibration
import com.cyb3rko.pincredible.utils.iterate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PinViewerFragment : Fragment() {
    private var _binding: FragmentPinViewerBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var myContext: Context
    private val vibrator by lazy { Vibration.getVibrator(myContext) }
    private val args: PinViewerFragmentArgs by navArgs()
    private var hash = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPinViewerBinding.inflate(inflater, container, false)
        myContext = requireContext()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.pinNameView.text = args.pin

        lifecycleScope.launch {
            loadDataIntoTable()
        }

        binding.deleteButton.setOnClickListener {
            MaterialAlertDialogBuilder(myContext)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setPositiveButton(R.string.dialog_delete_button1) { _, _ ->
                    val file = File(myContext.filesDir, "p$hash")
                    if (file.exists()) {
                        val deletionSuccess = file.delete()
                        if (deletionSuccess) {
                            Vibration.vibrateDoubleClick(vibrator)
                            CryptoManager.removeString(File(myContext.filesDir, "pins"), args.pin)
                            findNavController().navigate(
                                PinViewerFragmentDirections.pinviewerToHome()
                            )
                        }
                    } else {
                        ErrorDialog.show(
                            myContext,
                            NoSuchFileException(
                                file,
                                reason = "The requested file could not be found."
                            ),
                            R.string.dialog_delete_error
                        )
                    }
                }
                .setNegativeButton(R.string.dialog_delete_button2, null)
                .show()
        }
    }

    private suspend fun loadDataIntoTable() {
        val timedValue = CryptoManager.hash(args.pin)
        hash = timedValue

        val sequences: Pair<String, String>
        try {
            sequences = decryptData(hash)
            withContext(Dispatchers.Main) {
                binding.progressBar.hide()
                colorTableView(sequences.first)
                fillTable(sequences.second)
            }
        } catch (e: EnDecryptionException) {
            Log.d("CryptoManager", e.customStacktrace)
            withContext(Dispatchers.Main) {
                binding.progressBar.hide()
            }
        }
    }

    private fun colorTableView(pattern: String) {
        var index: Int
        var backgroundInt: Int
        var background: Drawable
        binding.tableLayout.table.iterate { view, row, column ->
            index = row * 7 + column
            backgroundInt = when (pattern[index] - '0') {
                0 -> R.drawable.cell_shape_cyan
                1 -> R.drawable.cell_shape_green
                2 -> R.drawable.cell_shape_orange
                3 -> R.drawable.cell_shape_red
                4 -> R.drawable.cell_shape_yellow
                else -> -1 // Not possible to appear
            }
            background = ResourcesCompat.getDrawable(
                resources,
                backgroundInt,
                myContext.theme
            )!!
            (view[row] as TableRow)[column].background = background
        }
    }

    private fun fillTable(pinSequence: String) {
        var index: Int
        binding.tableLayout.table.iterate { view, row, column ->
            index = row * 7 + column
            ((view[row] as TableRow)[column] as TextView).text = pinSequence[index].toString()
        }
    }

    @Throws(EnDecryptionException::class)
    private fun decryptData(hash: String): Pair<String, String> {
        val file = File(myContext.filesDir, "p$hash")
        val secret = CryptoManager.decrypt(file)
        val triple = PinTable.extractData(secret)
        return triple.second to triple.third
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
