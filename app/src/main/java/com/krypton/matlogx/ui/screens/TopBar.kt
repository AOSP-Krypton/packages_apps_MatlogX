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

package com.krypton.matlogx.ui.screens

import androidx.compose.animation.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.krypton.matlogx.R
import com.krypton.matlogx.ui.states.TopBarState
import com.krypton.matlogx.ui.widgets.MenuItem
import com.krypton.matlogx.ui.widgets.OverflowMenu
import com.krypton.matlogx.ui.widgets.SearchBar

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TopBar(
    topBarState: TopBarState,
    onShowLogLevelMenuRequest: () -> Unit,
    onSaveLogsRequest: () -> Unit,
    onShareLogsRequest: () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.primary
    val contentColorForContainer = contentColorFor(backgroundColor = containerColor)
    var searchBarExpanded by rememberSaveable { mutableStateOf(false) }
    SmallTopAppBar(
        title = {
            AnimatedContent(targetState = searchBarExpanded) { expanded ->
                if (expanded) {
                    val recentSearches by topBarState.searchSuggestions.collectAsState(emptyList())
                    SearchBar(
                        text = "",
                        hint = stringResource(id = R.string.search_hint),
                        history = recentSearches,
                        onSearchRequest = {
                            topBarState.handleSearch(it)
                        },
                        onDismissRequest = {
                            searchBarExpanded = false
                            topBarState.clearSearch()
                        },
                        onClearRecentQueryRequest = {
                            topBarState.clearRecentSearch(it)
                        },
                        onClearAllRecentQueriesRequest = {
                            topBarState.clearAllRecentSearches()
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            containerColor = Color.Transparent,
                            focusedLeadingIconColor = contentColorForContainer,
                            unfocusedLeadingIconColor = contentColorForContainer,
                            placeholderColor = contentColorForContainer,
                            textColor = contentColorForContainer,
                            cursorColor = contentColorForContainer,
                            focusedTrailingIconColor = contentColorForContainer,
                            unfocusedTrailingIconColor = contentColorForContainer,
                            unfocusedBorderColor = contentColorForContainer,
                            focusedBorderColor = contentColorForContainer
                        )
                    )
                } else {
                    Text(text = stringResource(id = R.string.app_name))
                }
            }
        },
        actions = {
            AnimatedVisibility(visible = !searchBarExpanded, enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = { searchBarExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.search_button_content_desc),
                        tint = contentColorForContainer
                    )
                }
            }
            IconButton(onClick = { topBarState.toggleLogcatFlowState() }) {
                val isPaused by topBarState.logcatUiUpdatePaused.collectAsState(false)
                Icon(
                    painter = painterResource(
                        if (isPaused)
                            R.drawable.ic_baseline_play_arrow_24
                        else
                            R.drawable.ic_baseline_pause_24
                    ),
                    contentDescription = stringResource(R.string.play_pause_button_content_desc),
                    tint = contentColorForContainer
                )
            }
            TopBarOverflowMenu(
                topBarState,
                contentColorForContainer,
                onShowLogLevelMenuRequest,
                onSaveLogsRequest,
                onShareLogsRequest
            )
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = containerColor,
            titleContentColor = contentColorForContainer
        )
    )
}

@Composable
fun TopBarOverflowMenu(
    topBarState: TopBarState,
    menuIconTint: Color,
    onShowLogLevelMenuRequest: () -> Unit,
    onSaveLogsRequest: () -> Unit,
    onShareLogsRequest: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    OverflowMenu(
        overflowIcon = {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = stringResource(R.string.menu_overflow_button),
                tint = menuIconTint
            )
        },
        expanded = menuExpanded,
        onOverflowIconClicked = {
            if (!menuExpanded) menuExpanded = true
        },
        onDismissRequest = {
            menuExpanded = false
        },
    ) {
        MenuItem(
            title = stringResource(id = R.string.clear_logs),
            iconContentDescription = stringResource(id = R.string.clear_logs_button_content_desc),
            iconImageVector = Icons.Filled.Clear,
            onClick = {
                menuExpanded = false
                topBarState.clearLogs()
            }
        )
        MenuItem(
            title = stringResource(id = R.string.share_logs),
            iconContentDescription = stringResource(id = R.string.share_button_content_desc),
            iconImageVector = Icons.Filled.Share,
            onClick = {
                menuExpanded = false
                onShareLogsRequest()
            }
        )
        MenuItem(
            title = stringResource(id = R.string.log_level),
            iconContentDescription = stringResource(id = R.string.log_level_button_content_desc),
            iconImageVector = Icons.Filled.List,
            onClick = {
                menuExpanded = false
                onShowLogLevelMenuRequest()
            }
        )
        MenuItem(
            title = stringResource(id = R.string.save_zip),
            iconContentDescription = stringResource(id = R.string.save_zip_button_content_desc),
            icon = painterResource(id = R.drawable.ic_baseline_folder_24),
            onClick = {
                menuExpanded = false
                onSaveLogsRequest()
            }
        )
        MenuItem(
            title = stringResource(id = R.string.settings),
            iconContentDescription = stringResource(id = R.string.clear_logs_button_content_desc),
            iconImageVector = Icons.Filled.Settings,
            onClick = {
                menuExpanded = false
                topBarState.openSettings()
            }
        )
    }
}