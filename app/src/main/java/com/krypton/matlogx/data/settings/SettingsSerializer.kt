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

package com.krypton.matlogx.data.settings

import android.content.Context

import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore

import java.io.InputStream
import java.io.OutputStream

object SettingsSerializer : Serializer<Settings> {
    override val defaultValue: Settings = Settings.newBuilder()
        .setLogcatBuffers(SettingsDefaults.DEFAULT_BUFFERS)
        .setLogSizeLimit(SettingsDefaults.LOG_SIZE_DEFAULT)
        .setLogLevel(SettingsDefaults.LOG_LEVEL_DEFAULT)
        .setIncludeDeviceInfo(SettingsDefaults.INCLUDE_DEVICE_INFO_DEFAULT)
        .setExpandedByDefault(SettingsDefaults.EXPANDED_DEFAULT)
        .setTextSize(SettingsDefaults.DEFAULT_TEXT_SIZE)
        .setWriteBufferSize(SettingsDefaults.DEFAULT_WRITE_BUFFER_SIZE)
        .build()

    override suspend fun readFrom(input: InputStream): Settings {
        return runCatching {
            Settings.parseFrom(input)
        }.getOrThrow()
    }

    override suspend fun writeTo(
        t: Settings,
        output: OutputStream
    ) {
        runCatching {
            t.writeTo(output)
        }.getOrThrow()
    }
}

val Context.settingsDataStore: DataStore<Settings> by dataStore(
    fileName = "settings",
    serializer = SettingsSerializer
)