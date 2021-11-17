/*
 * Copyright (C) 2021 AOSP-Krypton Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.krypton.matlogx.repo

import android.os.CancellationSignal

import com.krypton.matlogx.data.LogInfo
import com.krypton.matlogx.reader.LogcatReader
import com.krypton.matlogx.util.SettingsHelper

import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.flow.Flow

@Singleton
class LogcatRepository @Inject constructor(
    private val settingsHelper: SettingsHelper,
) {

    /**
     * Get an asynchronous stream of [LogInfo].
     *
     * @param cancellationSignal to terminate the flow.
     * @return a flow of [LogInfo].
     */
    fun getLogcatStream(cancellationSignal: CancellationSignal): Flow<LogInfo> {
        val logcatReader = LogcatReader()
        return logcatReader.read(
            cancellationSignal,
            args = settingsHelper.getArgsFromUserSettings(),
            tags = null,
        )
    }

    /**
     * Query the total number of lines in the logcat buffer
     *
     * @return the total number of lines in all of the logcat buffer
     */
    suspend fun getCurrentLogcatSize(): Int = LogcatReader().getSize()
}