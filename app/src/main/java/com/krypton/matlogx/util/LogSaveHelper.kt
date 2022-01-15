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

package com.krypton.matlogx.util

import com.krypton.matlogx.data.Result

import java.io.File
import java.io.OutputStream

object LogSaveHelper {
    private const val DEVICE_INFO_FILE = "device_info.txt"

    /**
     * Saves logs to a zip file, optionally including device info.
     *
     * @param fileUtil the [FileUtil] that helps with various file operations.
     * @param log the log content to save.
     * @param includeDeviceInfo whether to include device info.
     * @param timestamp timestamp to use for the log file name.
     * @param outputStream the output stream to write the zip file into.
     * @return a [Result] indicating whether the operation was successful or not,
     *         type parameter represents the saved file.
     */
    fun saveZip(
        fileUtil: FileUtil, log: String,
        includeDeviceInfo: Boolean,
        timestamp: String,
        outputStream: OutputStream,
    ): Result<File> {
        val logResult =
            fileUtil.writeToFile("$timestamp.log", log)
        if (logResult.isFailure) {
            return Result.failure(Exception("Failed to write log to file, ${logResult.exceptionOrNull()?.message}"))
        }
        val zipResult: Result<File>
        if (includeDeviceInfo) {
            val deviceInfoResult =
                fileUtil.writeToFile(DEVICE_INFO_FILE, DeviceInfo.toRawString())
            if (deviceInfoResult.isFailure) {
                return Result.failure(Exception("Failed to write device info, ${deviceInfoResult.exceptionOrNull()?.message}"))
            }
            zipResult = fileUtil.zip(
                deviceInfoResult.getOrThrow(),
                logResult.getOrThrow(),
                name = "tmp.zip"
            )
        } else {
            zipResult = fileUtil.zip(
                logResult.getOrThrow(),
                name = "tmp.zip"
            )
        }
        if (zipResult.isFailure) {
            return Result.failure(Exception("Failed to zip the file, ${zipResult.exceptionOrNull()?.message}"))
        }
        val fileResult = fileUtil.writeToStreamAndClose(zipResult.getOrThrow(), outputStream)
        fileUtil.clearCache()
        return fileResult
    }
}