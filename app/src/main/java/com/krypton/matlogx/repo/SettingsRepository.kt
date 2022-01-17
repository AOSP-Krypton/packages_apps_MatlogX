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

package com.krypton.matlogx.repo

import android.content.Context

import com.krypton.matlogx.data.settingsDataStore

import dagger.hilt.android.qualifiers.ApplicationContext

import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val settingsDataStore = context.settingsDataStore

    /**
     * Get the logcat buffers to include in logs.
     *
     * @return the buffers separated by a ",".
     */
    fun getLogcatBuffers(): Flow<String> = settingsDataStore.data.map {
        it.logcatBuffers
    }

    /**
     * Save the newly selected logcat buffers to settings.
     *
     * @param buffers the buffers separated by a ",".
     */
    suspend fun setLogcatBuffers(buffers: String) {
        settingsDataStore.updateData {
            it.toBuilder()
                .setLogcatBuffers(buffers)
                .build()
        }
    }

    /**
     * Get the user selected limit for number of log lines to keep
     * at a time to prevent OOM errors.
     *
     * @return number of lines to keep as a [Flow].
     */
    fun getLogcatSizeLimit(): Flow<Int> = settingsDataStore.data.map {
        it.logSizeLimit
    }

    /**
     * Set the logcat size limit.
     *
     * @param limit the new limit to save.
     */
    suspend fun setLogcatSizeLimit(limit: Int) {
        settingsDataStore.updateData {
            it.toBuilder()
                .setLogSizeLimit(limit)
                .build()
        }
    }

    /**
     * Get the user selected log level.
     *
     * @return the persisted log level as a [Flow].
     */
    fun getLogLevel(): Flow<String> = settingsDataStore.data.map {
        it.logLevel
    }

    /**
     * Set the log level to filter logs
     *
     * @param value the log level to persist
     */
    suspend fun setLogLevel(value: String) {
        settingsDataStore.updateData {
            it.toBuilder()
                .setLogLevel(value)
                .build()
        }
    }

    /**
     * Whether to include device information in logs.
     *
     * @return the saved value as a [Flow].
     */
    fun getIncludeDeviceInfo(): Flow<Boolean> = settingsDataStore.data.map {
        it.includeDeviceInfo
    }

    /**
     * Save include device information preference.
     *
     * @param include the value to save.
     */
    suspend fun setIncludeDeviceInfo(include: Boolean) {
        settingsDataStore.updateData {
            it.toBuilder()
                .setIncludeDeviceInfo(include)
                .build()
        }
    }

    /**
     * Whether to expand log message by default.
     *
     * @return the saved value as a [Flow].
     */
    fun getExpandedByDefault(): Flow<Boolean> = settingsDataStore.data.map {
        it.expandedByDefault
    }

    /**
     * Save whether to expand log message by default.
     *
     * @param expanded the value to save.
     */
    suspend fun setExpandedByDefault(expanded: Boolean) {
        settingsDataStore.updateData {
            it.toBuilder()
                .setExpandedByDefault(expanded)
                .build()
        }
    }
}