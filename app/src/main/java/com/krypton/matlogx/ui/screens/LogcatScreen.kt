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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

import com.krypton.matlogx.R
import com.krypton.matlogx.data.LogInfo
import com.krypton.matlogx.data.LogcatListData
import com.krypton.matlogx.ui.states.LogcatScreenState

import kotlinx.coroutines.launch

sealed interface FABState {
    object Gone : FABState
    object Up : FABState
    object Down : FABState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogcatScreen(
    logcatScreenState: LogcatScreenState,
    onQuitAppRequest: () -> Unit
) {
    var showLogLevelDialog by remember { mutableStateOf(false) }
    if (showLogLevelDialog) {
        val logLevel by logcatScreenState.logLevel.collectAsState(0)
        LogLevelDialog(
            currentLevel = logLevel,
            onDismissRequest = { showLogLevelDialog = false },
            onLogLevelSelected = {
                showLogLevelDialog = false
                logcatScreenState.setLogLevel(it)
            }
        )
    }
    var showZipSaveDialog by remember { mutableStateOf(false) }
    if (showZipSaveDialog) {
        val includeDeviceInfo by logcatScreenState.includeDeviceInfo.collectAsState(false)
        ZipSaveAndShareDialog(
            title = stringResource(id = R.string.save_zip),
            includeDeviceInfo = includeDeviceInfo,
            onIncludeDeviceInfoRequest = {
                logcatScreenState.setIncludeDeviceInfo(it)
            },
            onDismissRequest = {
                showZipSaveDialog = false
            },
            onConfirmRequest = {
                showZipSaveDialog = false
                logcatScreenState.saveLogs()
            }
        )
    }
    var showShareZipDialog by remember { mutableStateOf(false) }
    if (showShareZipDialog) {
        val includeDeviceInfo by logcatScreenState.includeDeviceInfo.collectAsState(false)
        ZipSaveAndShareDialog(
            title = stringResource(id = R.string.save_zip),
            includeDeviceInfo = includeDeviceInfo,
            onIncludeDeviceInfoRequest = {
                logcatScreenState.setIncludeDeviceInfo(it)
            },
            onDismissRequest = {
                showShareZipDialog = false
            },
            onConfirmRequest = {
                showShareZipDialog = false
                logcatScreenState.shareLogs()
            }
        )
    }
    Scaffold(
        topBar = {
            TopBar(
                logcatScreenState.topBarState,
                onShowLogLevelMenuRequest = {
                    showLogLevelDialog = true
                },
                onSaveLogsRequest = {
                    showZipSaveDialog = true
                },
                onShareLogsRequest = {
                    showShareZipDialog = true
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = logcatScreenState.snackbarHostState)
        }
    ) { paddingValues ->
        if (!logcatScreenState.hasReadLogsPermission) {
            PermissionDialog(onQuitAppRequest = onQuitAppRequest) {
                logcatScreenState.copyCommand()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val loadingData by logcatScreenState.loadingData.collectAsState(false)
            if (loadingData) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                )
            } else {
                val logcatList by logcatScreenState.logcatList.collectAsState(emptyList())
                val listState = rememberLazyListState()
                var fabState by remember { mutableStateOf<FABState>(FABState.Gone) }
                LaunchedEffect(logcatList) {
                    if (logcatList.isNotEmpty()) {
                        listState.animateScrollToItem(logcatList.lastIndex)
                        // Make sure to hide FAB when auto scrolling
                        // as nested scroll connection won't know about internal
                        // scrolls
                        if (fabState !is FABState.Gone) {
                            fabState = FABState.Gone
                        }
                    }
                }
                val isLastItemVisible =
                    (listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.size) == logcatList.size
                if (listState.firstVisibleItemIndex == 0 ||
                    isLastItemVisible &&
                    (fabState !is FABState.Gone)
                ) {
                    fabState = FABState.Gone
                }
                val nestedScrollConnection = remember {
                    object : NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            if (consumed.y > 0f) {
                                fabState = FABState.Up
                            } else if (consumed.y < 0f) {
                                fabState = FABState.Down
                            }
                            return super.onPostScroll(consumed, available, source)
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                    state = listState
                ) {
                    itemsIndexed(logcatList) { index, item ->
                        LogItem(
                            item = item,
                            onExpansionChanged = {
                                logcatScreenState.expandItemAtIndex(index, it)
                            },
                        )
                    }
                }
                ScrollFAB(
                    fabState = fabState,
                    onClick = {
                        if (fabState is FABState.Up) {
                            logcatScreenState.coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        } else if (fabState is FABState.Down) {
                            logcatScreenState.coroutineScope.launch {
                                listState.animateScrollToItem(logcatList.lastIndex)
                            }
                        }
                        fabState = FABState.Gone
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(32.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogLevelDialog(
    currentLevel: Int,
    onDismissRequest: () -> Unit,
    onLogLevelSelected: (Int) -> Unit
) {
    val levels = stringArrayResource(id = R.array.log_levels)
    var selectedLogLevel by remember { mutableStateOf(levels[currentLevel]) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onLogLevelSelected(levels.indexOf(selectedLogLevel))
                },
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true),
        title = {
            Text(text = stringResource(id = R.string.log_level))
        },
        text = {
            Column {
                levels.forEach { level ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            enabled = true,
                            onClick = {
                                selectedLogLevel = level
                            }
                        )
                    ) {
                        RadioButton(
                            selected = selectedLogLevel == level,
                            onClick = {
                                selectedLogLevel = level
                            }
                        )
                        Text(level, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZipSaveAndShareDialog(
    title: String,
    includeDeviceInfo: Boolean,
    onIncludeDeviceInfoRequest: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit
) {
    AlertDialog(
        shape = RoundedCornerShape(32.dp),
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirmRequest) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true),
        title = {
            Text(text = title)
        },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeDeviceInfo, onCheckedChange = onIncludeDeviceInfoRequest)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(id = R.string.include_device_info),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    )
}

@Composable
fun PermissionDialog(
    onQuitAppRequest: () -> Unit,
    onCopyRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onQuitAppRequest,
        confirmButton = {
            TextButton(onClick = onQuitAppRequest) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onCopyRequest) {
                Text(text = stringResource(id = android.R.string.copy))
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Text(text = stringResource(id = R.string.grant_permissions))
        },
        text = {
            Text(text = stringResource(id = R.string.how_to_grant))
        }
    )
}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun LogItem(item: LogcatListData, onExpansionChanged: (Boolean) -> Unit) {
    val textSize = item.textSize.toFloat()
    val hasOnlyMessage = item.logInfo.hasOnlyMessage()
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.clickable(
            enabled = true,
            onClick = {
                onExpansionChanged(!item.isExpanded)
            },
        )
    ) {
        if (item.isExpanded && !hasOnlyMessage) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    modifier = Modifier.weight(.25f),
                    text = item.logInfo.pid.toString(),
                    fontSize = TextUnit(textSize, TextUnitType.Sp)
                )
                Text(
                    modifier = Modifier.weight(.75f),
                    text = item.logInfo.time.toString(),
                    fontSize = TextUnit(textSize, TextUnitType.Sp)
                )
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            if (!hasOnlyMessage) {
                Text(
                    modifier = Modifier.weight(.25f),
                    text = item.logInfo.tag.toString(),
                    maxLines = if (item.isExpanded) Int.MAX_VALUE else 1,
                    fontSize = TextUnit(textSize, TextUnitType.Sp),
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.logInfo.level.toString(),
                    modifier = Modifier
                        .weight(.05f)
                        .padding(horizontal = 2.dp),
                    fontSize = TextUnit(textSize, TextUnitType.Sp),
                    textAlign = TextAlign.Center
                )
            }
            Text(
                modifier = Modifier.weight(if (hasOnlyMessage) 1f else .75f),
                text = item.logInfo.message.toString(),
                maxLines = if (item.isExpanded) Int.MAX_VALUE else 1,
                fontSize = TextUnit(textSize, TextUnitType.Sp),
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview
@Composable
fun PreviewLogItem() {
    LogItem(
        item = LogcatListData(
            logInfo = LogInfo(
                1000,
                "26-05 12:55:47",
                "AReallyLongTagForTesting",
                'V',
                "This is some log. More logs. More and more logs. Now something else. More logs."
            ),
            isExpanded = true,
            textSize = 12
        ),
        onExpansionChanged = {}
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ScrollFAB(
    fabState: FABState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = fabState !is FABState.Gone,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            AnimatedContent(targetState = fabState) {
                when (it) {
                    is FABState.Down -> {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(id = R.string.scroll_down_fab_content_desc),
                        )
                    }
                    is FABState.Up -> {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = stringResource(id = R.string.scroll_up_fab_content_desc),
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}