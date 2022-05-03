/*
 * Copyright (C) 2022 AOSP-Krypton Project
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

package com.krypton.matlogx.ui.states

import android.Manifest
import android.content.ClipboardManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController

import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.krypton.matlogx.R
import com.krypton.matlogx.data.LogcatListData
import com.krypton.matlogx.viewmodels.LogcatViewModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LogcatScreenState(
    val topBarState: TopBarState,
    val snackbarHostState: SnackbarHostState,
    val coroutineScope: CoroutineScope,
    private val context: Context,
    private val logcatViewModel: LogcatViewModel
) {

    val hasReadLogsPermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_LOGS
        ) == PackageManager.PERMISSION_GRANTED

    val logcatList: Flow<List<LogcatListData>>
        get() = logcatViewModel.logcatList

    val logLevel: Flow<Int>
        get() = logcatViewModel.logLevel

    val loadingData: StateFlow<Boolean>
        get() = logcatViewModel.loadingData

    val includeDeviceInfo: Flow<Boolean>
        get() = logcatViewModel.includeDeviceInfo

    val logcatStreamPaused: Flow<Boolean>
        get() = logcatViewModel.logcatStreamPaused

    init {
        if (hasReadLogsPermission) {
            logcatViewModel.init()
        }
    }

    fun copyCommand() {
        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(
            ClipData(
                ClipDescription(context.getString(R.string.adb_command), arrayOf("text/plain")),
                ClipData.Item(context.getString(R.string.command))
            )
        )
        coroutineScope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.copied_to_clipboard))
        }
    }

    fun expandItemAtIndex(index: Int, expanded: Boolean) {
        logcatViewModel.expandItemAtIndex(index, expanded)
    }

    fun setLogLevel(level: Int) {
        logcatViewModel.setLogLevel(level)
    }

    fun setIncludeDeviceInfo(includeDeviceInfo: Boolean) {
        logcatViewModel.setIncludeDeviceInfo(includeDeviceInfo)
    }

    fun shareLogs() {
        coroutineScope.launch {
            val result = logcatViewModel.saveLogAsZip()
            if (result.isSuccess) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, result.getOrThrow())
                    type = "application/zip"
                }
                context.startActivity(
                    Intent.createChooser(
                        shareIntent,
                        context.getString(R.string.share_logs)
                    )
                )
            } else {
                snackbarHostState.showSnackbar(
                    result.exceptionOrNull()?.localizedMessage
                        ?: context.getString(R.string.failed_to_save_log)
                )
            }
        }
    }

    fun saveLogs() {
        coroutineScope.launch {
            val result = logcatViewModel.saveLogAsZip()
            if (result.isSuccess) {
                snackbarHostState.showSnackbar(context.getString(R.string.log_saved_successfully))
            } else {
                snackbarHostState.showSnackbar(
                    result.exceptionOrNull()?.localizedMessage
                        ?: context.getString(R.string.failed_to_save_log)
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun rememberLogcatScreenState(
    logcatViewModel: LogcatViewModel,
    navHostController: NavHostController = rememberAnimatedNavController(),
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
    topBarState: TopBarState = rememberTopBarState(
        logcatViewModel,
        navHostController,
        coroutineScope,
        snackbarHostState,
        context
    ),
) = remember(
    logcatViewModel,
    snackbarHostState,
    context
) {
    LogcatScreenState(
        topBarState,
        snackbarHostState,
        coroutineScope,
        context,
        logcatViewModel
    )
}