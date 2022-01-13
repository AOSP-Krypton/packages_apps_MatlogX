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
     * @param query string to filter the logs with
     * @return a flow of [LogInfo].
     */
    fun getLogcatStream(query: String?): Flow<LogInfo> {
        val logcatReader = LogcatReader()
        return logcatReader.read(
            args = settingsHelper.getLogcatArgs(),
            tags = null,
            query,
        )
    }

    /**
     * Get the user selected limit for number of log lines to keep
     * at a time to prevent OOM errors.
     *
     * @return number of lines to keep
     */
    fun getLogcatSizeLimit(): Int = settingsHelper.getLogSizeLimit()
}