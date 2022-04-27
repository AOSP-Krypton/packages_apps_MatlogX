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

import java.io.InputStream

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A utility object to reads lines from system logcat.
 */
object LogcatReader {

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

    /**
     * Read logcat with the given command line args.
     *
     * @param args command line arguments for logcat.
     * @param tags a list of string tags to filter the logs.
     * @param query string to filter the logs.
     * @param logLevel the log level below which the logs should be discarded.
     * @return a [Flow] of [LogInfo].
     */
    fun readAsFlow(
        args: Map<String, String?>? = null,
        tags: List<String>? = null,
        query: String?,
        logLevel: String,
    ): Flow<LogInfo> {
        return flow {
            getInputStream(args, tags, query, logLevel).bufferedReader().use {
                while (true) {
                    runCatching {
                        it.readLine()
                    }.getOrNull()?.let { line ->
                        emit(LogInfo.fromLine(line))
                    }
                }
            }
        }
    }

    /**
     * Get a snapshot of current logcat stream.
     *
     * @param args command line arguments for logcat.
     * @param tags a list of string tags to filter the logs.
     * @param query string to filter the logs.
     * @param logLevel the log level below which the logs should be discarded.
     * @return current system logs joined to a string.
     */
    fun getRawLogs(
        args: Map<String, String?>? = null,
        tags: List<String>? = null,
        query: String?,
        logLevel: String,
    ): String {
        return getInputStream(args, tags, query, logLevel, dump = true).bufferedReader().use { br ->
            br.readText()
        }
    }

    private fun getInputStream(
        args: Map<String, String?>? = null,
        tags: List<String>? = null,
        query: String?,
        logLevel: String,
        dump: Boolean = false,
    ): InputStream {
        val argsList = mutableListOf(
            LOGCAT_BIN,
            "*:$logLevel",
            "--format=time"
        )
        // Append args
        args?.forEach { (k, v) ->
            argsList.add(k)
            argsList.add(v ?: "")
        }
        // Append tags
        if (tags != null) {
            argsList.add(OPTION_DEFAULT_SILENT)
            tags.forEach { argsList.add(it) }
        }
        // Filter based on query
        if (query?.isNotBlank() == true) {
            argsList.add("| grep -i $query")
        }
        // Dump and close stream if specified
        if (dump) {
            argsList.add(OPTION_DUMP)
        }
        val process = ProcessBuilder("/bin/sh", "-c", argsList.joinToString(" ")).start()
        process.outputStream.close()
        return process.inputStream
    }
}