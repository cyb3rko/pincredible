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

import android.content.Context
import android.content.Intent
import android.os.Process.killProcess
import android.util.Log
import kotlin.system.exitProcess

internal class ExceptionHandler(val context: Context) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        Intent(context, UncaughtExceptionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_STACKTRACE, Log.getStackTraceString(e))
            context.startActivity(this)
        }
        killProcess(android.os.Process.myPid())
        exitProcess(1)
    }

    companion object {
        const val EXTRA_STACKTRACE = "throwable"
    }
}
