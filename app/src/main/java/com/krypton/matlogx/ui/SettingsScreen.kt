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

package com.krypton.matlogx.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource

import com.google.accompanist.systemuicontroller.SystemUiController
import com.krypton.matlogx.R
import com.krypton.matlogx.ui.preferences.*
import com.krypton.matlogx.ui.widgets.CollapsingToolbarScreen
import com.krypton.matlogx.viewmodels.SettingsViewModel

import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    systemUiController: SystemUiController,
    onBackPressed: () -> Unit,
) {
    val isSystemInDarkTheme = isSystemInDarkTheme()
    CollapsingToolbarScreen(
        title = stringResource(R.string.settings),
        backButtonContentDescription = stringResource(R.string.settings_back_button_content_desc),
        onBackButtonPressed = onBackPressed,
        onStatusBarColorUpdateRequest = {
            systemUiController.setStatusBarColor(
                color = it,
                darkIcons = !isSystemInDarkTheme
            )
        }
    ) {
        item {
            PreferenceGroupHeader(title = stringResource(id = R.string.appearance))
        }
        item {
            val textSize = settingsViewModel.textSize.collectAsState(initial = 0)
            ListPreference(
                title = stringResource(id = R.string.text_size),
                entries = listOf(
                    Entry(stringResource(id = R.string.small), 8),
                    Entry(stringResource(id = R.string.medium), 12),
                    Entry(stringResource(id = R.string.large), 16),
                ),
                value = textSize.value,
                onEntrySelected = {
                    settingsViewModel.setTextSize(it)
                }
            )
        }
        item {
            val expanded = settingsViewModel.expandedByDefault.collectAsState(initial = false)
            CheckBoxPreference(
                title = stringResource(id = R.string.expand_logs),
                summary = stringResource(id = R.string.expand_logs_summary),
                checked = expanded.value,
                onCheckedChange = {
                    settingsViewModel.setExpandedByDefault(it)
                }
            )
        }
        item {
            PreferenceGroupHeader(title = stringResource(id = R.string.configuration))
        }
        item {
            val buffers = stringArrayResource(id = R.array.buffers).toList()
            val selectedBuffers = settingsViewModel.logcatBuffers.map { it.split(",") }
                .collectAsState(initial = emptyList())
            MultiSelectListPreference(
                title = stringResource(id = R.string.log_buffers),
                summary = selectedBuffers.value.joinToString(","),
                entries = buffers.map { Entry(it, it) },
                values = selectedBuffers.value,
                onValuesUpdated = {
                    settingsViewModel.setLogcatBuffers(it.joinToString(","))
                },
                onDismissListener = {}
            )
        }
        item {
            val limit = settingsViewModel.logcatSizeLimit.collectAsState(0)
            EditTextPreference(
                title = stringResource(id = R.string.log_display_limit),
                summary = if (limit.value == 0)
                    stringResource(id = R.string.log_display_limit_summary_no_limit)
                else
                    stringResource(
                        id = R.string.log_display_limit_summary_placeholder,
                        limit.value
                    ),
                value = limit.value.toString(),
                onValueSelected = {
                    settingsViewModel.setLogcatSizeLimit(it.toInt())
                }
            )
        }
    }
}