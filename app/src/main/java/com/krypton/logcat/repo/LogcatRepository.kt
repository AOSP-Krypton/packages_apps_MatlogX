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

package com.krypton.logcat.repo

import com.krypton.logcat.data.LogInfo

import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Singleton
class LogcatRepository @Inject constructor() {

    /**
     * Get a stream of logs from logcat
     */
    fun getLogcatStream(): Flow<LogInfo> = flow {
        // TODO: remove this dummy simulation once logcat is actually fetched
        kotlinx.coroutines.delay(3000L)
        emit(LogInfo(
            1000,
            "2021",
            "AReallyLongTag",
            LogInfo.Level.VERBOSE,
            "Something that is not lorem ipsum",
        ))
    }
}