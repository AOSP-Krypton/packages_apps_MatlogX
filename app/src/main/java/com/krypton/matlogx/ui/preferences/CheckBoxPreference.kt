/*
 * Copyright (C) 2022 AOSP-Krypton Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package com.krypton.matlogx.ui.preferences

import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckBoxPreference(
    title: String,
    summary: String? = null,
    clickable: Boolean = true,
    onClick: () -> Unit = {},
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    Preference(
        title = title,
        summary = summary,
        clickable = clickable,
        onClick = onClick,
        endWidget = {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}