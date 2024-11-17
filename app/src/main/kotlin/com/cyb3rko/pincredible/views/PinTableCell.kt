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

package com.cyb3rko.pincredible.views

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.MarginLayoutParams
import com.cyb3rko.pincredible.R
import com.google.android.material.textview.MaterialTextView

class PinTableCell(context: Context) : MaterialTextView(context) {
    init {
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.table_text_size))
        setTextColor(resources.getColor(R.color.table_cell_text, null))
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        (layoutParams as MarginLayoutParams).setMargins(6, 0, 6, 0)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredWidth)
    }
}
