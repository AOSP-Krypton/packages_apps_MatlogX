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

import android.content.Intent
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.krypton.matlogx.ui.screens.LogcatScreen
import com.krypton.matlogx.ui.screens.SettingsScreen
import com.krypton.matlogx.ui.states.rememberLogcatScreenState
import com.krypton.matlogx.ui.theme.LogcatTheme
import com.krypton.matlogx.viewmodels.LogcatViewModel
import com.krypton.matlogx.viewmodels.SettingsViewModel

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LogcatActivity : ComponentActivity() {

    private val documentTreeLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
            if (it == null) {
                finish()
                return@registerForActivityResult
            }
            val flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(it, flags)
        }

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LogcatTheme {
                val systemUiController = rememberSystemUiController()
                with(systemUiController) {
                    setStatusBarColor(MaterialTheme.colorScheme.primary)
                    setNavigationBarColor(MaterialTheme.colorScheme.surface)
                }
                val navHostController = rememberAnimatedNavController()
                AnimatedNavHost(
                    navController = navHostController,
                    startDestination = Routes.HOME
                ) {
                    composable(
                        Routes.HOME,
                        exitTransition = {
                            when (targetState.destination.route) {
                                Routes.SETTINGS -> slideOutOfContainer(
                                    AnimatedContentScope.SlideDirection.Start,
                                    tween(ANIMATION_DURATION)
                                )
                                else -> null
                            }
                        },
                        popEnterTransition = {
                            when (initialState.destination.route) {
                                Routes.SETTINGS -> slideIntoContainer(
                                    AnimatedContentScope.SlideDirection.End,
                                    tween(ANIMATION_DURATION)
                                )
                                else -> null
                            }
                        },
                    ) {
                        val logcatViewModel by viewModels<LogcatViewModel>()
                        val logcatScreenState = rememberLogcatScreenState(
                            logcatViewModel,
                            navHostController
                        )
                        LogcatScreen(
                            logcatScreenState,
                            onQuitAppRequest = {
                                finish()
                            },
                        )
                    }
                    composable(
                        Routes.SETTINGS,
                        enterTransition = {
                            when (initialState.destination.route) {
                                Routes.HOME -> slideIntoContainer(
                                    AnimatedContentScope.SlideDirection.Start,
                                    tween(ANIMATION_DURATION)
                                )
                                else -> null
                            }
                        },
                        popExitTransition = {
                            when (targetState.destination.route) {
                                Routes.HOME -> slideOutOfContainer(
                                    AnimatedContentScope.SlideDirection.End,
                                    tween(ANIMATION_DURATION)
                                )
                                else -> null
                            }
                        }
                    ) {
                        val settingsViewModel by viewModels<SettingsViewModel>()
                        SettingsScreen(
                            settingsViewModel = settingsViewModel,
                        ) {
                            navHostController.popBackStack()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val hasValidUriPerms = contentResolver.persistedUriPermissions.firstOrNull()?.let {
            it.isReadPermission && it.isWritePermission
        } == true
        if (!hasValidUriPerms) {
            documentTreeLauncher.launch(null)
        }
    }

    companion object {
        private const val ANIMATION_DURATION = 500
    }
}