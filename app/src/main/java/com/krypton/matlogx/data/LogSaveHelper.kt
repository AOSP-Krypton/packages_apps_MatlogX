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

import androidx.documentfile.provider.DocumentFile

import dagger.hilt.android.qualifiers.ApplicationContext

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class that can write files to application cache dir.
 */
@Singleton
class LogSaveHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Get uri to a new recording file.
     *
     * @return a [Result] holding a [Uri] to the file or an [Exception] on failure.
     */
    fun getNewRecordingFileUri(): Result<Uri> {
        val logDir = getLogDir().getOrElse {
            return Result.failure(it)
        }
        val recordingDir = (logDir.findFile(RECORDING_DIRECTORY_NAME)
            ?: logDir.createDirectory(RECORDING_DIRECTORY_NAME))
            ?: return Result.failure(Throwable("Unable to create directory for recording logs"))
        val timestamp = getTimestamp()
        val recordingFile = recordingDir.createFile("text/plain", timestamp + LOG_FILE_EXT)
            ?: return Result.failure(Throwable("Failed to create file for recording"))
        return Result.success(recordingFile.uri)
    }

    /**
     * Saves logs to a zip file, optionally including device info.
     *
     * @param log the log content to save.
     * @param includeDeviceInfo whether to include device info.
     * @return a [Result] indicating whether the operation was successful or not,
     *   data of [Result] is the uri of saved file.
     */
    fun saveZip(
        log: String,
        includeDeviceInfo: Boolean,
    ): Result<Uri> {
        val logDir = getLogDir().getOrElse {
            return Result.failure(it)
        }
        // Zip up all the contents
        val timestamp = getTimestamp()
        val contentsToZip = mutableMapOf<String, ByteArray>()
        contentsToZip[timestamp + LOG_FILE_EXT] = log.toByteArray(StandardCharsets.UTF_8)
        if (includeDeviceInfo) {
            contentsToZip[DEVICE_INFO_FILE] =
                DeviceInfo.toRawString().toByteArray(StandardCharsets.UTF_8)
        }
        val zipFile = logDir.createFile("application/zip", "$FILE_PREFIX-$timestamp")
            ?: return Result.failure(Throwable("Failed to create zip file"))
        return zip(contentsToZip, zipFile)
    }

    private fun getLogDir(): Result<DocumentFile> {
        // Create a sub directory inside the directory we have access.
        val persistedUris = context.contentResolver.persistedUriPermissions
        if (persistedUris.isEmpty())
            return Result.failure(IllegalStateException("Access to a directory in internal storage has not been given."))
        val treeUriPerm = persistedUris.first()
        if (!treeUriPerm.isReadPermission || !treeUriPerm.isWritePermission)
            return Result.failure(IllegalStateException("Does not have r/w permission"))
        val treeFile = DocumentFile.fromTreeUri(context, treeUriPerm.uri)
            ?: return Result.failure(Throwable("Unable to open document tree"))
        val subDir = (treeFile.findFile(DIRECTORY_NAME)
            ?: treeFile.createDirectory(DIRECTORY_NAME))
            ?: return Result.failure(Throwable("Unable to create sub directory"))
        return Result.success(subDir)
    }

    /**
     * Zip multiple files.
     *
     * @param fileMap the files to zip.
     * @param zipFile the file name for the zip file.
     * @return a [Result] indicating whether the write was successful or not.
     */
    private fun zip(fileMap: Map<String, ByteArray>, zipFile: DocumentFile): Result<Uri> {
        val uri = zipFile.uri
        val outputStream = context.contentResolver.openOutputStream(uri)
            ?: return Result.failure(Throwable("Unable to open OutputStream for zip file"))
        val zipResult = runCatching {
            ZipOutputStream(outputStream).use {
                fileMap.forEach { entry ->
                    it.putNextEntry(ZipEntry(entry.key))
                    it.write(entry.value)
                    it.flush()
                    it.closeEntry()
                }
            }
        }
        return if (zipResult.isSuccess) {
            Result.success(uri)
        } else {
            Result.failure(zipResult.exceptionOrNull() ?: Throwable("Failed to zip contents"))
        }
    }

    companion object {
        private const val DIRECTORY_NAME = "matlogx"
        private const val RECORDING_DIRECTORY_NAME = "recordings"

        private const val DEVICE_INFO_FILE = "device_info.txt"
        private const val FILE_PREFIX = "Logs"

        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
        private fun getTimestamp(): String {
            return LocalDateTime.now().format(dateTimeFormatter)
        }

        private const val LOG_FILE_EXT = ".log"
    }
}