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

import android.os.Build

/**
 * Utility class to get device information.
 */
object DeviceInfo {
    private val info = mapOf(
        "product" to Build.PRODUCT,
        "device" to Build.DEVICE,
        "model" to Build.MODEL,
        "manufacturer" to Build.MANUFACTURER,
        "id" to Build.ID,
        "board" to Build.BOARD,
        "display" to Build.DISPLAY,
        "fingerprint" to Build.FINGERPRINT,
        "hardware" to Build.HARDWARE,
        "supported_abis" to Build.SUPPORTED_ABIS.joinToString(", "),
        "build_type" to Build.TYPE,
        "build_tags" to Build.TAGS,
        "release_or_codename" to Build.VERSION.RELEASE_OR_CODENAME,
        "sdk_int" to Build.VERSION.SDK_INT.toString()
    )

    /**
     * Get device information as a [Map] of property to it's value.
     *
     * @return the device info map
     */
    fun get(): Map<String, String> = info

    /**
     * Flatten the device information into a string
     * separated by newline at the end of each property.
     *
     * @return the flattened string.
     */
    fun toRawString(): String = info.map { "${it.key} ${it.value}" }.joinToString("\n")
}