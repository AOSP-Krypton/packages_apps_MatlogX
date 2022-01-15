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

import android.content.Context

import dagger.hilt.android.qualifiers.ApplicationContext

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class that can write files to application cache dir.
 */
@Singleton
class FileUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cacheDir = context.cacheDir

    /**
     * Write a string to a file.
     *
     * @param name the name of the file to create.
     * @param content the content to write to the file.
     * @return a [Result] indicating whether the write was success or not, including
     *         the file or an exception.
     */
    fun writeToFile(name: String, content: String): Result<File> {
        if (!cacheDir.isDirectory) {
            return Result.failure(IllegalStateException("Cache dir doesn't exist"))
        }
        val file = File(cacheDir, name)
        return try {
            FileOutputStream(file).bufferedWriter().use {
                it.write(content)
                it.flush()
            }
            Result.success(file)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    /**
     * Write a file to an output stream and closes it at the end.
     *
     * @param file the file to write into the output stream.
     * @param outputStream the stream to which the file should be written.
     * @return a [Result] indicating whether it was successful or not.
     */
    fun writeToStreamAndClose(file: File, outputStream: OutputStream): Result<File> =
        try {
            outputStream.use {
                it.write(Files.readAllBytes(file.toPath()))
                it.flush()
            }
            Result.success(file)
        } catch (e: IOException) {
            Result.failure(e)
        } finally {
            outputStream.close()
        }

    /**
     * Zip multiple files.
     *
     * @param files the files to zip.
     * @param name the file name for the zip file.
     * @return a [Result] indicating whether the write was success or not, including
     *         the file or an exception.
     */
    fun zip(vararg files: File, name: String): Result<File> {
        if (!cacheDir.isDirectory) {
            return Result.failure(IllegalStateException("Cache dir doesn't exist"))
        }
        val zipFile = File(cacheDir, name)
        return try {
            ZipOutputStream(FileOutputStream(zipFile)).use {
                files.forEach { file ->
                    it.putNextEntry(ZipEntry(file.name))
                    it.write(Files.readAllBytes(file.toPath()))
                    it.flush()
                    it.closeEntry()
                }
            }
            Result.success(zipFile)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    /**
     * Deletes all file inside [cacheDir]
     */
    fun clearCache() {
        if (cacheDir.isDirectory) {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }
}