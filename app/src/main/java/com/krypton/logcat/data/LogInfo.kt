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

package com.krypton.logcat.data

/**
 * Data class representing a line from logcat.
 *
 * @property pid process id of the program that logged this entry.
 * @property timestamp time (format MM-dd HH:MM:ss.SSS) at which this entry was logged.
 * @property tag the log tag of this entry.
 * @property level the log level of this entry (one of [Level]).
 * @property message the message that was logged.
 */
data class LogInfo(
    val pid: Int = -1,
    val timestamp: String = "",
    val tag: String = "",
    val level: Level = Level.UNKNOWN,
    val message: String = "",
) {
    enum class Level {
        // For logs with only messages
        UNKNOWN {
            override fun toChar() = ""
        },
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL;

        open fun toChar() = name[0].toString()
    }

    fun hasOnlyMessage() = level == Level.UNKNOWN
}