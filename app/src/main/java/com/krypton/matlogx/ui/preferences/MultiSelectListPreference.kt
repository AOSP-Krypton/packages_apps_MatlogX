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

package com.krypton.matlogx.ui.preferences

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MultiSelectListPreference(
    title: String,
    summary: String? = null,
    clickable: Boolean = true,
    onClick: (() -> Unit)? = null,
    entries: List<Entry<T>>,
    values: List<T> = emptyList(),
    onValuesUpdated: (List<T>) -> Unit,
    onDismissListener: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                onDismissListener()
            },
            confirmButton = {},
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
            shape = RoundedCornerShape(32.dp),
            title = {
                Text(text = title)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    entries.forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = values.contains(entry.value),
                                onCheckedChange = { checked ->
                                    onValuesUpdated(
                                        values.toMutableList().apply {
                                            if (checked) {
                                                add(entry.value)
                                            } else {
                                                remove(entry.value)
                                            }
                                        }.toList()
                                    )
                                },
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(modifier = Modifier.weight(1f), text = entry.name)
                        }
                    }
                }
            },
        )
    }
    Preference(
        title = title,
        summary = summary,
        clickable = clickable,
        onClick = {
            if (onClick == null) {
                showDialog = true
            } else {
                onClick()
            }
        },
    )
}

@Preview
@Composable
fun PreviewMultiSelectListPreference() {
    val selectedList = remember { mutableStateListOf(0, 1) }
    MultiSelectListPreference(title = "Multi select list preference",
        summary = "This is a multi select list preference",
        entries = listOf(
            Entry("Entry 1", 0),
            Entry("Entry 2", 1),
            Entry("Entry 3", 2)
        ),
        values = selectedList,
        onValuesUpdated = {},
        onDismissListener = {}
    )
}