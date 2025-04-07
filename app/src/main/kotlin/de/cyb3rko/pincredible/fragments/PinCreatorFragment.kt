/*
 * Copyright (c) 2023-2024 Cyb3rKo
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

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import de.cyb3rko.backpack.crypto.CryptoManager
import de.cyb3rko.backpack.crypto.CryptoManager.EnDecryptionException
import de.cyb3rko.backpack.modals.ErrorDialog
import de.cyb3rko.backpack.modals.InputDialog
import de.cyb3rko.backpack.utils.ObjectSerializer
import de.cyb3rko.backpack.utils.Safe
import de.cyb3rko.backpack.utils.Vibration
import de.cyb3rko.backpack.utils.hide
import de.cyb3rko.backpack.utils.show
import de.cyb3rko.pincredible.R
import de.cyb3rko.pincredible.SettingsActivity
import de.cyb3rko.pincredible.data.Cell
import de.cyb3rko.pincredible.data.PinTable
import de.cyb3rko.pincredible.databinding.FragmentPinCreatorBinding
import de.cyb3rko.pincredible.utils.BackupHandler.pinDir
import de.cyb3rko.pincredible.utils.BackupHandler.pinListFile
import de.cyb3rko.pincredible.views.CoordinateViewManager
import java.io.File
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PinCreatorFragment : Fragment() {
    private var _binding: FragmentPinCreatorBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var myContext: Context
    private val pinTable by lazy { PinTable() }
    private var clickedCell: Cell? = null
    private val addedIndices by lazy { mutableSetOf<Int>() }
    private val vibrator by lazy { Vibration.getVibrator(myContext) }
    private val colorBlindAlternative by lazy {
        Safe.getBoolean(SettingsActivity.KEY_COLOR_BLIND, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPinCreatorBinding.inflate(inflater, container, false)
        myContext = requireContext()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tableView.colorizeRandom(pinTable, colorBlindAlternative)
        setTableClickListeners()
        setButtonClickListeners()
        setFabClickListener()

        CoordinateViewManager.initializeViews(
            binding.coordinatesRow1,
            binding.coordinatesCol1,
            binding.coordinatesCol2
        )
    }

    private fun setTableClickListeners() {
        var currentBackgroundInt: Int
        binding.tableView.iterate { tableView, row, column ->
            val cell = tableView.getCell(row, column)
            cell.setOnClickListener {
                Vibration.vibrateTick(vibrator)
                clickedCell?.let { safeClickedCell ->
                    tableView.unselect(safeClickedCell, colorBlindAlternative)
                    if (safeClickedCell.view == it) {
                        clickedCell = null
                        binding.buttonContainer.hide()
                        return@setOnClickListener
                    }
                }

                currentBackgroundInt = pinTable.getBackground(row, column)
                tableView.select(cell, currentBackgroundInt, colorBlindAlternative)
                clickedCell = Cell(cell, row, column, currentBackgroundInt)
                shuffleButtonDigits()
                binding.buttonContainer.show()
            }
        }
    }

    private fun setButtonClickListeners() {
        var clickedCellView: TextView
        binding.run {
            setOf(
                button1, button2, button3, button4, button5, button6, button7, button8, button9,
                button0
            ).forEach { button ->
                button.setOnClickListener {
                    clickedCell?.let {
                        buttonContainer.hide()

                        clickedCellView = it.view
                        clickedCellView.text = button.text
                        tableView.unselect(it, colorBlindAlternative)
                        val number = clickedCellView.text.toString().toInt()
                        pinTable.put(it.row, it.column, number)
                        addedIndices.add(it.row * 7 + it.column)
                        clickedCell = null
                        if (pinTable.isFilled()) fab.show()
                    }
                }
            }
            buttonGenerate.setOnClickListener {
                binding.tableView.colorizeRandom(pinTable, colorBlindAlternative)
            }
            buttonFill.setOnClickListener {
                fillTable()
            }
            buttonClear.setOnClickListener {
                clearTable()
            }
        }
    }

    private fun setFabClickListener() {
        binding.fab.setOnClickListener {
            InputDialog.show(
                myContext,
                R.string.dialog_name_title,
                R.string.dialog_name_hint,
                maxInputLength = 30
            ) { dialog, inputLayout, input ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val hash = CryptoManager.xxHash(input)
                        val saved = savePinFile(hash)
                        if (saved) {
                            savePinName(input)
                            Vibration.vibrateDoubleClick(vibrator)
                            withContext(Dispatchers.Main) {
                                dialog.dismiss()
                                findNavController().navigate(
                                    PinCreatorFragmentDirections.pinCreatorToHome()
                                )
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                inputLayout.error = getString(R.string.dialog_name_error_exist)
                            }
                        }
                    } catch (e: EnDecryptionException) {
                        Log.d("CryptoManager", e.customStacktrace)
                        withContext(Dispatchers.Main) {
                            dialog.dismiss()
                            ErrorDialog.show(myContext, e)
                        }
                    }
                }
            }
        }
    }

    private fun fillTable() {
        pinTable.fill(addedIndices)
        binding.tableView.fill(pinTable)
        binding.fab.show()
    }

    private fun clearTable() {
        binding.fab.hide()
        pinTable.resetDigits()
        binding.tableView.clear()
        addedIndices.clear()
    }

    @Throws(EnDecryptionException::class)
    private suspend fun savePinFile(hash: String): Boolean {
        val newPinFile = File(myContext.pinDir(), "p$hash")
        return if (!newPinFile.exists()) {
            newPinFile.createNewFile()
            val bytes = pinTable.toBytes()
            CryptoManager.encrypt(bytes, newPinFile)
            Log.d("PINcredible", "New PIN - Hash:$hash")
            true
        } else {
            false
        }
    }

    @Throws(EnDecryptionException::class)
    private fun savePinName(name: String) {
        val pinsFile = myContext.pinListFile()

        if (!pinsFile.exists()) {
            Log.d("PINcredible", "Creating new PINs file")
            pinsFile.createNewFile()
            CryptoManager.encrypt(
                ObjectSerializer.serialize(setOf(name)),
                pinsFile
            )
        } else {
            Log.d("PINcredible", "Appending PIN to PINs file")
            CryptoManager.appendStrings(pinsFile, name)
        }
    }

    private fun shuffleButtonDigits() {
        if (!Safe.getBoolean(SettingsActivity.KEY_BUTTON_RANDOMIZER, false)) return
        val digits = MutableList(10) { it }.apply {
            shuffle(SecureRandom())
        }
        binding.run {
            setOf(
                button1, button2, button3, button4, button5, button6, button7, button8, button9,
                button0
            ).forEach { button ->
                button.text = digits.removeFirst().toString()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
