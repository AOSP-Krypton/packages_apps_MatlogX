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

package com.krypton.matlogx.ui

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.remember

import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.krypton.matlogx.ui.theme.LogcatTheme
import com.krypton.matlogx.viewmodels.SettingsViewModel

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LogcatTheme {
                val settingsViewModel = remember {
                    viewModels<SettingsViewModel>()
                }
                val systemUiController = rememberSystemUiController()
                SettingsScreen(
                    settingsViewModel = settingsViewModel.value,
                    systemUiController = systemUiController,
                    onBackPressed = {
                        onBackPressed()
                    }
                )
            }
        }
    }
}