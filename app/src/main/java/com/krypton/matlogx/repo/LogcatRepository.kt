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
        val args = mapOf<String, String?>(
            LogcatReader.OPTION_BUFFER to settingsHelper.getLogcatBuffers().joinToString(",")
        )
        val logcatReader = LogcatReader()
        return logcatReader.read(
            args = args,
            tags = null,
            query = query,
            logLevel = getLogLevel(),
        )
    }

    /**
     * Get an estimated size of current logcat stream.
     *
     * @param query optional string to filter logs.
     * @return the size of the stream.
     */
    fun getLogcatSize(query: String?): Int {
        val args = mapOf<String, String?>(
            LogcatReader.OPTION_BUFFER to settingsHelper.getLogcatBuffers().joinToString(",")
        )
        val logcatReader = LogcatReader()
        return logcatReader.getSize(
            args = args,
            tags = null,
            query,
            getLogLevel(),
        )
    }

    /**
     * Get the user selected limit for number of log lines to keep
     * at a time to prevent OOM errors.
     *
     * @return number of lines to keep.
     */
    fun getLogcatSizeLimit(): Int = settingsHelper.getLogSizeLimit()

    /**
     * Get the user selected log level.
     *
     * @return the persisted log level.
     */
    fun getLogLevel(): String = settingsHelper.getLogLevel()

    /**
     * Set the log level to filter logs
     *
     * @param value the log level to persist
     */
    fun setLogLevel(value: String) {
        settingsHelper.setLogLevel(value)
    }

    /**
     * Registers a listener to get notified when settings change
     * that require clients of [getLogcatStream] to re-start the coroutines.
     *
     * @param listener the callback that will be invoked when settings change.
     */
    fun registerConfigurationChangeListener(listener: () -> Unit) {
        settingsHelper.registerConfigurationChangeListener(listener)
    }
}