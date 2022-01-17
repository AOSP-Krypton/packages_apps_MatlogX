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

package com.krypton.matlogx.data

/**
 * Wrapper class around [LogInfo] representing an item
 * in logcat list view.
 *
 * @property logInfo
 * @property isExpanded
 */
data class LogcatListData(
    val logInfo: LogInfo,
    /**
     * Whether full log message including PID and timestamp should be shown.
     */
    var isExpanded: Boolean = false,
)