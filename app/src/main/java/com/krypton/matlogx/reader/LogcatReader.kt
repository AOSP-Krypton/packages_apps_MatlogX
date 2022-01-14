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

import com.krypton.matlogx.data.LogInfo

import java.io.InputStream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

/**
 * A reader class to reads lines from system logcat.
 */
class LogcatReader {

    /**
     * Read logcat with the given command line args.
     *
     * @param args command line arguments for logcat.
     * @param tags a list of string tags to filter the logs.
     * @param query string to filter the logs.
     * @return a [Flow] of [LogInfo].
     */
    // Ignore this, flow context is of IO Dispatcher and yet Android Studio is not able to sense it
    @Suppress("BlockingMethodInNonBlockingContext")
    fun read(
        args: Map<String, String?>? = null,
        tags: List<String>? = null,
        query: String?,
        logLevel: String,
    ): Flow<LogInfo> {
        val argsList = mutableListOf(
            LOGCAT_BIN,
            "*:$logLevel",
            "--format=time,usec"
        )
        appendArgs(args, argsList)
        appendTags(tags, argsList)
        return flow {
            getInputStream(argsList).bufferedReader().use {
                while (currentCoroutineContext().isActive) {
                    it.readLine()?.takeIf { line ->
                        line.isNotBlank() && (query == null || line.contains(query, true))
                    }?.let { line ->
                        emit(getLogInfo(line))
                    }
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Get the current size of logcat stream.
     *
     * @param args command line arguments for logcat.
     * @param tags a list of string tags to filter the logs.
     * @param query string to filter the logs.
     * @return the current size of logs.
     */
    fun getSize(
        args: Map<String, String?>? = null,
        tags: List<String>? = null,
        query: String?,
        logLevel: String,
    ): Int {
        val argsList = mutableListOf(
            LOGCAT_BIN,
            "*:$logLevel",
        )
        appendArgs(args, argsList)
        appendTags(tags, argsList)
        argsList.add(OPTION_DUMP)
        return getInputStream(argsList).bufferedReader().use { br ->
            if (query?.isNotEmpty() == true) {
                br.lines().filter { it != null && it.contains(query) }.count()
            } else {
                br.lines().count()
            }
        }.toInt()
    }

    private fun getInputStream(commands: List<String>): InputStream {
        val process = ProcessBuilder(commands).start()
        process.outputStream.close()
        return process.inputStream
    }

    private fun appendArgs(
        args: Map<String, String?>?,
        list: MutableList<String>
    ): MutableList<String> {
        args?.forEach { (k, v) ->
            list.add(k)
            list.add(v ?: "")
        }
        return list
    }

    private fun appendTags(tags: List<String>?, list: MutableList<String>): MutableList<String> {
        if (tags != null) {
            list.add(OPTION_DEFAULT_SILENT)
            tags.forEach { list.add(it) }
        }
        return list
    }

    companion object {
        private const val LOGCAT_BIN = "logcat"

        /**
         * Command line options and supported values for logcat binary.
         * There are many other options besides those given here, these
         * are the only one's being used right now.
         * Use of these options can be seen with logcat --help command.
         */
        const val OPTION_BUFFER = "-b"

        //--Make it public if needed--//
        private const val OPTION_DEFAULT_SILENT = "-s"

        private const val OPTION_DUMP = "-d"

        private val timestampRegex =
            Regex("^[0-9]{2}-[0-9]{2}\\s[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{6}")
        private val pidRegex = Regex("\\(\\s*[0-9]+\\)")

        private fun getLogInfo(logLine: String): LogInfo {
            // Filter event separators
            if (logLine.startsWith("-")) {
                return LogInfo(message = logLine)
            }
            // Log format:
            // DD-MM HH:MM:SS.ssssss D/TAG( PID): message
            val metadata = logLine.substringBefore("/")
            val pid = pidRegex.find(logLine)?.value?.substringAfter("(")?.substringBefore(")")
                ?.trimStart()?.toShort()
                ?: -1
            return LogInfo(
                pid = pid,
                timestamp = timestampRegex.find(metadata)?.value ?: "",
                // Assuming that no one insane used ( in their tag
                tag = logLine.substringAfter("/").substringBefore("("),
                level = metadata.last(),
                message = logLine.substringAfter("):").trim(),
            )
        }
    }
}