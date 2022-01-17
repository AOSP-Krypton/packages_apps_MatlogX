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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

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
    // Ignore this, flow context is of IO Dispatcher and yet Android Studio is not able to sense it
    @Suppress("BlockingMethodInNonBlockingContext")
    fun read(
        args: Map<String, String?>? = null,
        tags: List<String>? = null,
        query: String?,
        logLevel: String,
    ): Flow<LogInfo> {
        return flow {
            getInputStream(args, tags, query, logLevel).bufferedReader().use {
                while (true) {
                    it.readLine()?.let { line ->
                        emit(LogInfo.fromLine(line))
                    }
                }
            }
        }.flowOn(Dispatchers.IO)
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

    /**
     * Get the current size of logcat stream.
     *
     * @param args command line arguments for logcat.
     * @param tags a list of string tags to filter the logs.
     * @param query string to filter the logs.
     * * @param logLevel the log level below which the logs should be discarded.
     * @return the current size of logs.
     */
    fun getSize(
        args: Map<String, String?>? = null,
        tags: List<String>? = null,
        query: String?,
        logLevel: String,
    ): Int = getInputStream(args, tags, query, logLevel, dump = true).bufferedReader().use { br ->
        br.lines().count().toInt()
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
        appendArgs(args, argsList)
        appendTags(tags, argsList)
        if (query?.isNotBlank() == true) {
            argsList.add("| grep -i $query")
        }
        if (dump) {
            argsList.add(OPTION_DUMP)
        }
        val process = ProcessBuilder("/bin/sh", "-c", argsList.joinToString(" ")).start()
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
}