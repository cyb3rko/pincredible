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
import androidx.navigation.fragment.findNavController
import com.cyb3rko.pincredible.KEY_BUTTON_RANDOMIZER
import com.cyb3rko.pincredible.R
import com.cyb3rko.pincredible.crypto.CryptoManager
import com.cyb3rko.pincredible.crypto.CryptoManager.EnDecryptionException
import com.cyb3rko.pincredible.data.Cell
import com.cyb3rko.pincredible.data.PinTable
import com.cyb3rko.pincredible.databinding.FragmentPinCreatorBinding
import com.cyb3rko.pincredible.modals.ErrorDialog
import com.cyb3rko.pincredible.modals.InputDialog
import com.cyb3rko.pincredible.utils.ObjectSerializer
import com.cyb3rko.pincredible.utils.Safe
import com.cyb3rko.pincredible.utils.Vibration
import com.cyb3rko.pincredible.utils.hide
import com.cyb3rko.pincredible.utils.iterate
import com.cyb3rko.pincredible.utils.show
import java.io.File
import java.security.SecureRandom
import kotlin.jvm.Throws
import kotlin.random.Random

class PinCreatorFragment : Fragment() {
    private var _binding: FragmentPinCreatorBinding? = null
    private lateinit var myContext: Context

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val pinTable by lazy { PinTable() }
    private var clickedCell: Cell? = null
    private val vibrator by lazy { Vibration.getVibrator(myContext) }

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
        colorTableView()
        setTableClickListeners()
        setButtonClickListeners()
        setFabClickListener()
    }

    private fun colorTableView() {
        var randomIndex: Int
        var randomBackgroundInt: Int
        var randomBackground: Drawable
        binding.tableLayout.table.iterate { view, row, column ->
            randomIndex = Random.nextInt(5)
            randomBackgroundInt = when (randomIndex) {
                0 -> R.drawable.cell_shape_cyan
                1 -> R.drawable.cell_shape_green
                2 -> R.drawable.cell_shape_orange
                3 -> R.drawable.cell_shape_red
                4 -> R.drawable.cell_shape_yellow
                else -> -1 // Not possible to appear
            }
            randomBackground = ResourcesCompat.getDrawable(
                resources,
                randomBackgroundInt,
                myContext.theme
            )!!
            (view[row] as TableRow)[column].background = randomBackground
            pinTable.putBackground(row, column, randomIndex)
        }
    }

    private fun setTableClickListeners() {
        var currentBackgroundInt: Int
        var selectedBackgroundInt: Int
        binding.tableLayout.table.iterate { view, row, column ->
            (view[row] as TableRow)[column].setOnClickListener {
                Vibration.vibrateTick(vibrator)
                clickedCell?.let { cell ->
                    revertSelectedBackground(cell)
                    if (cell.view == it) {
                        clickedCell = null
                        binding.buttonContainer.hide()
                        return@setOnClickListener
                    }
                }

                currentBackgroundInt = pinTable.getBackground(row, column)
                selectedBackgroundInt = when (currentBackgroundInt) {
                    0 -> R.drawable.cell_shape_cyan_selected
                    1 -> R.drawable.cell_shape_green_selected
                    2 -> R.drawable.cell_shape_orange_selected
                    3 -> R.drawable.cell_shape_red_selected
                    4 -> R.drawable.cell_shape_yellow_selected
                    else -> -1 // Not possible to appear
                }
                (it as TextView).background = ResourcesCompat.getDrawable(
                    resources,
                    selectedBackgroundInt,
                    myContext.theme
                )!!
                clickedCell = Cell(it, row, column, currentBackgroundInt)
                shuffleButtonDigits()
                binding.buttonContainer.show()
            }
        }
    }

    private fun revertSelectedBackground(cell: Cell) {
        val backgroundInt = when (cell.background) {
            0 -> R.drawable.cell_shape_cyan
            1 -> R.drawable.cell_shape_green
            2 -> R.drawable.cell_shape_orange
            3 -> R.drawable.cell_shape_red
            4 -> R.drawable.cell_shape_yellow
            else -> -1 // Not possible to appear
        }
        cell.view.background = ResourcesCompat.getDrawable(
            resources,
            backgroundInt,
            myContext.theme
        )!!
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
                        revertSelectedBackground(it)
                        pinTable.put(
                            it.row,
                            it.column,
                            clickedCellView.text.toString().toInt()
                        )
                        clickedCell = null
                        if (pinTable.isFilled()) fab.show()
                    }
                }
            }
            buttonGenerate.setOnClickListener {
                colorTableView()
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
                R.string.dialog_name_hint
            ) { dialog, inputLayout, input ->
                if (input.isNotEmpty() && input.length <= 30) {
                    try {
                        val hash = CryptoManager.xxHash(input)
                        val saved = savePinFile(hash)
                        if (saved) {
                            savePinName(input)
                            Vibration.vibrateDoubleClick(vibrator)
                            dialog.dismiss()
                            findNavController().navigate(
                                PinCreatorFragmentDirections.pinCreatorToHome()
                            )
                        } else {
                            inputLayout.error = getString(R.string.dialog_name_error_exist)
                        }
                    } catch (e: EnDecryptionException) {
                        Log.d("CryptoManager", e.customStacktrace)
                        dialog.dismiss()
                        ErrorDialog.show(myContext, e)
                    }
                } else {
                    inputLayout.error = getString(R.string.dialog_name_error_length, 1, 30)
                }
            }
        }
    }

    private fun fillTable() {
        pinTable.fill()
        binding.tableLayout.table.iterate { view, row, column ->
            ((view[row] as TableRow)[column] as TextView).text = pinTable.get(row, column)
        }
        binding.fab.show()
    }

    private fun clearTable() {
        binding.fab.hide()
        pinTable.reset()
        binding.tableLayout.table.iterate { view, row, column ->
            ((view[row] as TableRow)[column] as TextView).text = null
        }
    }

    @Throws(EnDecryptionException::class)
    private fun savePinFile(hash: String): Boolean {
        val newPinFile = File(myContext.filesDir, "p$hash")
        return if (!newPinFile.exists()) {
            newPinFile.createNewFile()
            val bytes = ObjectSerializer.serialize(pinTable)
            val version = CryptoManager.PIN_CRYPTO_ITERATION.toByte()
            CryptoManager.encrypt(bytes.plus(version), newPinFile)
            true
        } else {
            false
        }
    }

    @Throws(EnDecryptionException::class)
    private fun savePinName(name: String) {
        val pinsFile = File(myContext.filesDir, CryptoManager.PINS_FILE)

        if (!pinsFile.exists()) {
            pinsFile.createNewFile()
            CryptoManager.encrypt(
                ObjectSerializer.serialize(setOf(name)),
                pinsFile
            )
        } else {
            CryptoManager.appendStrings(pinsFile, name)
        }
    }

    private fun shuffleButtonDigits() {
        if (!Safe.getBoolean(myContext, KEY_BUTTON_RANDOMIZER, false)) return
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
