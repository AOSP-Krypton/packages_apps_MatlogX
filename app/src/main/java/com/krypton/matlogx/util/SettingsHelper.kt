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
import android.content.SharedPreferences

import com.krypton.matlogx.reader.LogcatReader
import dagger.hilt.android.qualifiers.ApplicationContext

import javax.inject.Inject

/**
 * Class that manages user preferences and
 * exposes helper APIs for clients to use.
 */
class SettingsHelper @Inject constructor(
    @ApplicationContext context: Context
) {

    private val sharedPreferences by lazy {
        context.getSharedPreferences(
            PREF_FILE_NAME,
            Context.MODE_PRIVATE
        )
    }

    /**
     * Get user selected arguments to pass as
     * an argument to [LogcatReader.read].
     *
     * @return map of options to it's values.
     */
    fun getLogcatArgs(): Map<String, String?> {
        return mapOf(
            LogcatReader.OPTION_BUFFER to sharedPreferences.getString(
                PREF_KEY_LOGCAT_BUFFER,
                LogcatReader.BUFFER_ALL
            ),
        )
    }

    /**
     * Get an [Int] type value of log size limit stored in [SharedPreferences]
     *
     * @return size limit stored in [SharedPreferences]
     */
    fun getLogSizeLimit() = sharedPreferences.getInt(PREF_KEY_LOG_SIZE_LIMIT, LOG_SIZE_DEFAULT)

    companion object {
        private const val PREF_FILE_NAME = "matlogx_shared_preferences"

        private const val PREF_KEY_LOGCAT_ARG_PREFIX = "key_logcat_arg_"
        private const val PREF_KEY_LOGCAT_BUFFER = "${PREF_KEY_LOGCAT_ARG_PREFIX}buffer"

        private const val PREF_KEY_LOG_SIZE_LIMIT = "key_log_size_limit"
        const val LOG_SIZE_DEFAULT = 1000
    }
}