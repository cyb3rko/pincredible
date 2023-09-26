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

package com.cyb3rko.pincredible.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import com.cyb3rko.pincredible.R
import com.google.android.material.textview.MaterialTextView

class PinTableCoordinate(
    context: Context,
    attrs: AttributeSet
) : MaterialTextView(context, attrs) {
    init {
        gravity = Gravity.CENTER
        setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            resources.getDimension(R.dimen.table_coordinate_text_size)
        )
        setTextColor(resources.getColor(R.color.table_coordinate_text, null))
    }
}
