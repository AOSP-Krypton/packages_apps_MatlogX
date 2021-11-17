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

package com.krypton.matlogx.reader

import android.os.CancellationSignal

import com.krypton.matlogx.data.LogInfo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * A reader class to reads lines from system logcat.
 */
// Ignore this, flow context is of IO Dispatcher and yet Android Studio is not able to sense it
@Suppress("BlockingMethodInNonBlockingContext")
class LogcatReader {

    /**
     * Read logcat with the given command line args.
     *
     * @param terminateSignal signal to call cancel when reading should be terminated safely.
     * @return a [Flow] of [LogInfo].
     */
    fun read(
        terminateSignal: CancellationSignal,
        args: Map<String, String?>? = null,
        tags: List<String>? = null,
    ): Flow<LogInfo> {
        val flattenedArgs = flattenArgsToString(args)
        val flattenedTags = flattenTagsToString(tags)
        val process: Process =
            Runtime.getRuntime().exec("$LOGCAT_BIN$flattenedArgs$flattenedTags")
        val bufferedReader = process.inputStream.bufferedReader()
        return flow {
            bufferedReader.use {
                while (!terminateSignal.isCanceled) {
                    it.readLine().takeIf { line ->
                        line != null && line.isNotBlank()
                    }?.let { line ->
                        getLogInfo(line)?.let { info -> emit(info) }
                    }
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Get the number of logs from all buffers.
     *
     * @return an [Int] type value of the total number of logs.
     */
    suspend fun getSize(): Int {
        val process: Process =
            Runtime.getRuntime().exec("$LOGCAT_BIN ${Args.OPTION_STATISTICS}")
        val bufferedReader = process.inputStream.bufferedReader()
        return withContext(Dispatchers.IO) {
            val statLine = bufferedReader.lines()
                .filter {
                    it != null && it.isNotBlank()
                }.filter {
                    // We want this line                                       This here is the total number of logs
                    // Total 1689561/12962 891650/6756 0/0 181653/1846 2762864/21564
                    it.startsWith("Total")
                }.findFirst().get()
            if (statLine.isNotBlank()) {
                statLine.substringAfterLast("/").toInt()
            } else {
                0
            }
        }
    }

    /**
     * Command line options and supported values for logcat binary.
     * There are many other options besides those given here, these
     * are the only one's being used right now.
     * Use of these options can be seen with logcat --help command.
     */
    object Args {
        const val OPTION_BUFFER = "-b"
        const val BUFFER_ALL = "all"

        const val OPTION_STATISTICS = "-S"
    }

    companion object {
        private const val LOGCAT_BIN = "logcat"

        private fun flattenArgsToString(args: Map<String, String?>?): String =
            args?.map { " ${it.key} ${it.value ?: ""}" }?.fold("", { r, t -> r + t }) ?: ""

        private fun flattenTagsToString(tags: List<String>?): String =
            tags?.fold(" '*:S", { r, t -> "$r $t" }) ?: ""

        private fun getLogInfo(logLine: String): LogInfo? {
            // Filter event separators
            if (logLine.startsWith("-")) {
                return LogInfo(message = logLine)
            }
            // Example elements: 11-13,22:52:06.633,pid,uid,level,TAG,some_message
            val list = logLine.split(" ").filter { it.isNotBlank() }
            // We shouldn't bother about logs separating events
            if (list.size < 5) {
                return null
            }
            return LogInfo(
                pid = list[2].toInt(),
                timestamp = "${list[0]} ${list[1]}",
                tag = list[5],
                level = getLevelFromString(list[4]),
                message = logLine.substringAfter(": "),
            )
        }

        private fun getLevelFromString(level: String): LogInfo.Level =
            LogInfo.Level.values().first { it.toChar() == level }
    }
}