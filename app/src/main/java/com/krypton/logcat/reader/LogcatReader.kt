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

package com.krypton.logcat.reader

import android.os.CancellationSignal

import com.krypton.logcat.data.LogInfo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

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
        args: Map<Args, String?>? = null,
        tags: List<String>? = null,
    ): Flow<LogInfo> {
        val flattenedArgs = flattenArgsToString(args)
        val flattenedTags = flattenTagsToString(tags)
        val process: Process = Runtime.getRuntime().exec("$LOGCAT$flattenedArgs$flattenedTags")
        val bufferedReader = process.inputStream.bufferedReader()
        return flow {
            bufferedReader.use {
                while (!terminateSignal.isCanceled) {
                    it.readLine().takeIf { line -> line.isNotEmpty() }?.let { line ->
                        getLogInfo(line)?.let { info -> emit(info) }
                    }
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Enum class holding a set of standard arguments that can be passed to logcat.
     * Use of each argument is given in logcat --help man page.
     */
    enum class Args {
        BUFFER {
            override fun toString() = "-b"
        };
    }

    companion object {
        private const val LOGCAT = "logcat"

        private fun flattenArgsToString(args: Map<Args, String?>?): String =
            args?.map { " ${it.key} ${it.value ?: ""}" }?.fold("", { r, t -> r + t }) ?: ""

        private fun flattenTagsToString(tags: List<String>?): String =
            tags?.fold(" '*:S", { r, t -> "$r $t" }) ?: ""

        private fun getLogInfo(logLine: String): LogInfo? {
            // Example elements: 11-13,22:52:06.633,pid,uid,level,TAG,some_message
            val list = logLine.split(" ").filter { it.isNotBlank() }
            // We shouldn't bother about logs separating events
            if (list.size < 5) {
                return null
            }
            return LogInfo(
                list[2].toInt(),
                "${list[0]} ${list[1]}",
                list[5],
                getLevelFromString(list[4]),
                logLine.substringAfter(": "),
            )
        }

        private fun getLevelFromString(level: String): LogInfo.Level =
            LogInfo.Level.values().first { it.toChar() == level }
    }
}