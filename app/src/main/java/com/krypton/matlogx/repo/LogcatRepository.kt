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
import android.net.Uri

import com.krypton.matlogx.data.LogInfo
import com.krypton.matlogx.data.Result
import com.krypton.matlogx.data.settingsDataStore
import com.krypton.matlogx.reader.LogcatReader
import com.krypton.matlogx.util.FileUtil
import dagger.hilt.android.qualifiers.ApplicationContext

import java.io.File

import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class LogcatRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val fileUtil: FileUtil,
) {

    private val settingsDataStore = context.settingsDataStore

    /**
     * Get an asynchronous stream of [LogInfo].
     *
     * @param tags a list of string that will be used to print only
     *             logs with those string as tags.
     * @param query string to filter the logs with.
     * @param logLevel the level of log below which logs should be omitted.
     * @return a flow of [LogInfo].
     */
    suspend fun getLogcatStream(
        tags: List<String>?,
        query: String?,
        logLevel: String,
    ): Flow<LogInfo> {
        return LogcatReader.read(getLogcatArgs(), tags, query, logLevel)
    }

    /**
     * Get an estimated size of current logcat stream.
     *
     * @param tags a list of string that will be used to print only
     *             logs with those string as tags.
     * @param query string to filter the logs with.
     * @param logLevel the level of log below which logs should be omitted.
     * @return the size of the stream.
     */
    suspend fun getLogcatSize(
        tags: List<String>?,
        query: String?,
        logLevel: String,
    ): Int {
        return LogcatReader.getSize(getLogcatArgs(), tags, query, logLevel)
    }

    /**
     * Saves given list of [LogInfo] as a zip file.
     *
     * @param tags a list of string that will be used to print only
     *             logs with those string as tags.
     * @param query string to filter the logs with.
     * @param logLevel the level of log below which logs should be omitted.
     * @param includeDeviceInfo whether to include device info inside the zip.
     * @return a result with the [File] (or an exception if failed) that was saved.
     */
    suspend fun saveLogAsZip(
        tags: List<String>?,
        query: String?,
        logLevel: String,
        includeDeviceInfo: Boolean,
    ): Result<Uri> {
        val logs = LogcatReader.getRawLogs(getLogcatArgs(), tags, query, logLevel)
        val result = withContext(Dispatchers.IO) {
            fileUtil.saveZip(
                logs,
                includeDeviceInfo,
            )
        }
        return result
    }

    private suspend fun getLogcatArgs(): Map<String, String?> {
        val buffers = settingsDataStore.data.map { it.logcatBuffers }.first()
        return mapOf(LogcatReader.OPTION_BUFFER to buffers)
    }
}