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

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController

import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.krypton.matlogx.ui.Routes
import com.krypton.matlogx.viewmodels.LogcatViewModel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class TopBarState(
    private val logcatViewModel: LogcatViewModel,
    private val navHostController: NavHostController
) {

    val searchSuggestions: Flow<List<String>>
        get() = logcatViewModel.searchSuggestions

    val logcatUiUpdatePaused: StateFlow<Boolean>
        get() = logcatViewModel.logcatUiUpdatePaused

    fun toggleLogcatFlowState() {
        logcatViewModel.toggleLogcatFlowState()
    }

    fun clearLogs() {
        logcatViewModel.clearLogs()
    }

    fun openSettings() {
        navHostController.navigate(Routes.SETTINGS)
    }

    fun handleSearch(query: String) {
        if (query.isNotBlank()) {
            logcatViewModel.saveRecentSearchQuery(query)
        }
        logcatViewModel.handleSearch(query)
    }

    fun clearSearch() {
        logcatViewModel.handleSearch(null)
    }

    fun clearRecentSearch(query: String) {
        logcatViewModel.clearRecentSearch(query)
    }

    fun clearAllRecentSearches() {
        logcatViewModel.clearAllRecentSearches()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun rememberTopBarState(
    logcatViewModel: LogcatViewModel,
    navHostController: NavHostController = rememberAnimatedNavController()
) = remember(logcatViewModel) {
    TopBarState(logcatViewModel, navHostController)
}