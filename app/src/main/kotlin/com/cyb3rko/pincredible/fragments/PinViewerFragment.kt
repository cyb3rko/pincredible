/*
 * Copyright (c) 2022-2024 Cyb3rKo
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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.cyb3rko.backpack.crypto.CryptoManager
import com.cyb3rko.backpack.crypto.CryptoManager.EnDecryptionException
import com.cyb3rko.backpack.modals.AcceptDialog
import com.cyb3rko.backpack.modals.ErrorDialog
import com.cyb3rko.backpack.utils.Safe
import com.cyb3rko.backpack.utils.Vibration
import com.cyb3rko.pincredible.R
import com.cyb3rko.pincredible.SettingsActivity
import com.cyb3rko.pincredible.data.PinTable
import com.cyb3rko.pincredible.databinding.FragmentPinViewerBinding
import com.cyb3rko.pincredible.utils.BackupHandler
import com.cyb3rko.pincredible.utils.BackupHandler.SingleBackupStructure
import com.cyb3rko.pincredible.utils.BackupHandler.pinDir
import com.cyb3rko.pincredible.utils.BackupHandler.pinListFile
import com.cyb3rko.pincredible.utils.TableScreenshotHandler
import com.cyb3rko.pincredible.views.CoordinateViewManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.Float.min
import kotlin.properties.Delegates

class PinViewerFragment : Fragment() {
    private var _binding: FragmentPinViewerBinding? = null

    // This property is only valid between onCreateView and onDestroyView.

    private val binding get() = _binding!!

    private lateinit var myContext: Context
    private val vibrator by lazy { Vibration.getVibrator(myContext) }
    private val args: PinViewerFragmentArgs by navArgs()
    private val hash by lazy { CryptoManager.xxHash(args.pin) }
    private lateinit var pinTable: PinTable
    private var siid by Delegates.notNull<Byte>()
    private val colorBlindAlternative by lazy {
        Safe.getBoolean(SettingsActivity.KEY_COLOR_BLIND, false)
    }

    private val imageCreatorResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                AcceptDialog.show(
                    myContext,
                    R.string.dialog_image_title,
                    getString(
                        R.string.dialog_image_message,
                        TableScreenshotHandler.getFileName(
                            myContext,
                            uri
                        )
                    )
                ) {
                    generateAndExportImage(uri, args.pin)
                }
            }
        }

    private val fileCreatorResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                val singleBackup = SingleBackupStructure(pinTable, args.pin)
                BackupHandler.runBackup(myContext, uri, false, lifecycleScope, singleBackup)
            }
        }

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

        binding.hashView.text = getString(R.string.viewer_hash, hash.take(10))
        binding.pinNameView.text = args.pin

        CoordinateViewManager.initializeViews(
            binding.coordinatesRow1,
            binding.coordinatesCol1,
            binding.coordinatesCol2
        )

        lifecycleScope.launch {
            loadDataIntoTable()
        }

        binding.saveImageButton.setOnClickListener {
            TableScreenshotHandler.initiateTableImage(
                imageCreatorResultLauncher,
                "${args.pin}.jpg"
            )
        }

        binding.deleteButton.setOnClickListener {
            MaterialAlertDialogBuilder(myContext)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setPositiveButton(R.string.dialog_delete_button1) { _, _ ->
                    val file = File(myContext.pinDir(), "p$hash")
                    if (file.exists()) {
                        val deletionSuccess = file.delete()
                        if (deletionSuccess) {
                            Vibration.vibrateDoubleClick(vibrator)
                            CryptoManager.removeString(
                                myContext.pinListFile(),
                                args.pin
                            )
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

        binding.exportFab.setOnClickListener {
            BackupHandler.initiateSingleBackup(hash, fileCreatorResultLauncher)
        }
    }

    private suspend fun loadDataIntoTable() {
        try {
            pinTable = decryptData(hash)
            withContext(Dispatchers.Main) {
                binding.progressBar.hide()
                binding.tableView.colorize(pinTable, colorBlindAlternative)
                binding.tableView.fill(pinTable)
            }
            binding.exportFab.show()
        } catch (e: EnDecryptionException) {
            Log.d("CryptoManager", e.customStacktrace)
            withContext(Dispatchers.Main) {
                binding.progressBar.hide()
            }
        }
    }

    @Throws(EnDecryptionException::class)
    private suspend fun decryptData(hash: String): PinTable {
        val file = File(myContext.pinDir(), "p$hash")
        val pinTable = PinTable().loadFromBytes(CryptoManager.decrypt(file)) as PinTable
        siid = pinTable.getVersion()
        @SuppressLint("SetTextI18n")
        binding.siidView.text = "SIID: $siid"
        Log.d("PINcredible", "PIN - Hash:$hash, version:$siid")
        return pinTable
    }

    /**
     * Adds a white frame, a PINcredible watermark and a title text to the original bitmap.
     *
     * The text size is dynamically adjusted to fit within the frame.
     */
    private fun frameAndTitleBitmap(bitmap: Bitmap, title: String): Bitmap {
        val frameThickness = 20
        val titlePadding = bitmap.height / 20
        val watermarkPadding = bitmap.height / 40
        val paint = Paint()

        // Draws from top to bottom
        var currentY = 0f

        // Dynamically adjust the text size to fit within the frame
        val maxTextWidth = bitmap.width - (2 * frameThickness)
        val textWidth = paint.measureText(title) // Measure the text width
        paint.textSize *= (maxTextWidth / textWidth) // Adjust the text size to fit the text width
        paint.textSize = min(
            paint.textSize,
            bitmap.height / 15f
        ) // Limit the text size to 1/15 of the bitmap height

        // Calculate the new size for the bitmap with frame and watermark space
        val newBitmapWidth = bitmap.width + (frameThickness * 2)
        val newBitmapHeight = bitmap.height + (frameThickness * 2)
            + paint.textSize.toInt() + titlePadding + watermarkPadding

        // Create a new bitmap with the new size
        val framedBitmap =
            Bitmap.createBitmap(newBitmapWidth, newBitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(framedBitmap)

        // Draw the white frame
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, newBitmapWidth.toFloat(), newBitmapHeight.toFloat(), paint)

        // Draw the title
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(title, newBitmapWidth / 2f, paint.textSize + titlePadding / 2f, paint)
        currentY += titlePadding + paint.textSize

        // Draw the original bitmap onto the new bitmap with the frame
        canvas.drawBitmap(bitmap, frameThickness.toFloat(), (currentY), paint)
        currentY += bitmap.height

        // Draw the watermark
        paint.color = Color.DKGRAY
        paint.textSize = bitmap.width / 40f
        context?.let {
            canvas.drawText(
                it.getString(R.string.export_watermark),
                newBitmapWidth / 2f,
                currentY + paint.textSize + watermarkPadding / 2,
                paint
            )
        }
        currentY += watermarkPadding + paint.textSize

        return framedBitmap
    }

    private fun generateAndExportImage(uri: Uri, title: String) {
        val tableView = binding.tableView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bitmap = Bitmap.createBitmap(
                tableView.width,
                tableView.height,
                Bitmap.Config.ARGB_8888
            )
            val locationOfViewInWindow = IntArray(2)
            tableView.getLocationInWindow(locationOfViewInWindow)
            try {
                PixelCopy.request(
                    requireActivity().window,
                    Rect(
                        locationOfViewInWindow[0],
                        locationOfViewInWindow[1],
                        locationOfViewInWindow[0] + tableView.width,
                        locationOfViewInWindow[1] + tableView.height
                    ),
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            saveImage(frameAndTitleBitmap(bitmap, title), uri)
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        } else {
            TableScreenshotHandler.generateTableCacheCopy(tableView) {
                if (it != null) {
                    saveImage(frameAndTitleBitmap(it, title), uri)
                }
            }
        }
    }

    private fun saveImage(bitmap: Bitmap, uri: Uri) {
        myContext.contentResolver.openOutputStream(uri).use {
            if (it != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
