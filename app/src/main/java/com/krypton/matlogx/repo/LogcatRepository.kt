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

import android.content.Context
import android.widget.Toast

import com.krypton.matlogx.R
import com.krypton.matlogx.data.LogInfo
import com.krypton.matlogx.data.Result
import com.krypton.matlogx.reader.LogcatReader
import com.krypton.matlogx.util.FileUtil
import com.krypton.matlogx.util.LogSaveHelper
import com.krypton.matlogx.util.SettingsHelper

import dagger.hilt.android.qualifiers.ApplicationContext

import java.io.File
import java.io.OutputStream

import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Singleton
class LogcatRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsHelper: SettingsHelper,
    private val fileUtil: FileUtil,
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
        return LogcatReader.read(
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
        return LogcatReader.getSize(
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

    /**
     * Saves given list of [LogInfo] as a zip file.
     *
     * @param query optional string to filter logs.
     * @param includeDeviceInfo whether to include device info inside the zip.
     * @param outputStream the [OutputStream] to write to.
     * @return a result with the [File] (or an exception if failed) that was saved.
     */
    suspend fun saveLogAsZip(
        query: String?,
        includeDeviceInfo: Boolean,
        timestamp: String,
        outputStream: OutputStream,
    ): Result<File> {
        val args = mapOf<String, String?>(
            LogcatReader.OPTION_BUFFER to settingsHelper.getLogcatBuffers().joinToString(",")
        )
        val logs = LogcatReader.getRawLogs(
            args = args,
            tags = null,
            query,
            getLogLevel(),
        )
        val result = withContext(Dispatchers.IO) {
            LogSaveHelper.saveZip(
                fileUtil,
                logs,
                includeDeviceInfo,
                timestamp,
                outputStream
            )
        }
        if (result.isSuccess) {
            Toast.makeText(context, R.string.log_saved_successfully, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.failed_to_save_log, result.exceptionOrNull()?.message),
                Toast.LENGTH_SHORT
            ).show()
        }
        return result
    }

    /**
     * Whether to include device information in logs.
     *
     * @return the saved value.
     */
    fun getIncludeDeviceInfo(): Boolean {
        return settingsHelper.getIncludeDeviceInfo()
    }

    /**
     * Save include device information preference.
     *
     * @param include the value to save.
     */
    fun setIncludeDeviceInfo(include: Boolean) {
        settingsHelper.setIncludeDeviceInfo(include)
    }
}