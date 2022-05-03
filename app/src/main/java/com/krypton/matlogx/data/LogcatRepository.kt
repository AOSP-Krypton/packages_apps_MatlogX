/*
 * Copyright (C) 2021-2022 AOSP-Krypton Project
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

package com.krypton.matlogx.data

import android.content.Context
import android.net.Uri

import com.krypton.matlogx.data.settings.settingsDataStore

import dagger.hilt.android.qualifiers.ApplicationContext

import java.io.File

import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

@Singleton
class LogcatRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val logFileManager: LogFileManager
) {

    private val settingsDataStore = context.settingsDataStore
    private val contentResolver = context.contentResolver

    private val _recordingLogs = MutableStateFlow(false)
    val recordingLogs: StateFlow<Boolean>
        get() = _recordingLogs

    val recordLogErrorChannel = Channel<Throwable>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * Get an asynchronous stream of [LogInfo].
     *
     * @param tags a list of string that will be used to print only
     *             logs with those string as tags.
     * @param logLevel the level of log below which logs should be omitted.
     * @return a flow of [LogInfo].
     */
    suspend fun getLogcatStream(
        tags: List<String>?,
        logLevel: String,
    ): Flow<LogInfo> {
        return LogcatReader.readAsFlow(getLogcatArgs(), tags, logLevel)
            .flowOn(Dispatchers.IO)
    }

    /**
     * Get current logs as a list.
     *
     * @param tags a list of string that will be used to print only
     *             logs with those string as tags.
     * @param logLevel the level of log below which logs should be omitted.
     * @return the logs as a [List] of [LogInfo].
     */
    suspend fun getLogsAsList(
        tags: List<String>?,
        logLevel: String,
    ): List<LogInfo> =
        withContext(Dispatchers.IO) {
            LogcatReader.getRawLogs(getLogcatArgs(), tags, logLevel)
                .split("\n")
                .filter { it.isNotBlank() }
                .map { LogInfo.fromLine(it) }
        }

    /**
     * Saves given list of [LogInfo] as a zip file.
     *
     * @param tags a list of string that will be used to print only
     *             logs with those string as tags.
     * @param logLevel the level of log below which logs should be omitted.
     * @param includeDeviceInfo whether to include device info inside the zip.
     * @return a result with the [File] (or an exception if failed) that was saved.
     */
    suspend fun saveLogAsZip(
        tags: List<String>?,
        logLevel: String,
        includeDeviceInfo: Boolean,
    ): Result<Uri> =
        withContext(Dispatchers.IO) {
            logFileManager.saveZip(
                LogcatReader.getRawLogs(getLogcatArgs(), tags, logLevel),
                includeDeviceInfo,
            )
        }

    suspend fun recordLogs() {
        _recordingLogs.value = true
        val recordingFileUriResult = withContext(Dispatchers.IO) {
            logFileManager.getNewRecordingFileUri()
        }
        recordingFileUriResult.onFailure {
            recordLogErrorChannel.send(it)
            _recordingLogs.value = false
            return
        }
        val outputStreamResult = withContext(Dispatchers.IO) {
            runCatching {
                contentResolver.openOutputStream(recordingFileUriResult.getOrThrow(), "wa")
            }
        }
        outputStreamResult.onFailure {
            recordLogErrorChannel.send(it)
            _recordingLogs.value = false
            return
        }
        val outputStream = outputStreamResult.getOrNull() ?: run {
            recordLogErrorChannel.send(Throwable("Failed to open output stream"))
            _recordingLogs.value = false
            return
        }
        outputStream.bufferedWriter().use { writer ->
            val writeBuffer = settingsDataStore.data.map { it.writeBufferSize }.first()
            val logLevel = settingsDataStore.data.map { it.logLevel }.first()
            val bufferedLogs = mutableListOf<String>()
            try {
                LogcatReader.readRawLogsAsFlow(
                    getLogcatArgs(),
                    null /* tags aren't supported yet*/,
                    logLevel
                ).flowOn(Dispatchers.IO)
                    .collect {
                        bufferedLogs.add(it)
                        if (bufferedLogs.size >= writeBuffer) {
                            bufferedLogs.forEach { log ->
                                writer.write(log)
                                writer.newLine()
                            }
                            writer.flush()
                            bufferedLogs.clear()
                        }
                    }
            } catch (e: Throwable) {
                recordLogErrorChannel.send(e)
                _recordingLogs.value = false
                return
            }
        }
    }

    fun stopRecordingLogs() {
        _recordingLogs.value = false
    }

    private suspend fun getLogcatArgs(): Map<String, String?> {
        val buffers = settingsDataStore.data.map { it.logcatBuffers }.first()
        return mapOf(LogcatReader.OPTION_BUFFER to buffers)
    }

    fun getSavedLogsDirectoryUri(): Result<Uri> = logFileManager.getLogDirUri()
}