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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ListPreference(
    title: String,
    summary: String? = null,
    clickable: Boolean = true,
    onClick: (() -> Unit)? = null,
    entries: List<Entry<T>>,
    value: T?,
    onEntrySelected: (T) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {},
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
            shape = RoundedCornerShape(32.dp),
            title = {
                Text(text = title)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    entries.forEach {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    enabled = true,
                                    onClick = {
                                        showDialog = false
                                        onEntrySelected(it.value)
                                    },
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                onClick = {
                                    showDialog = false
                                    onEntrySelected(it.value)
                                },
                                selected = it.value == value
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(modifier = Modifier.weight(1f), text = it.name)
                        }
                    }
                }
            },
        )
    }
    Preference(
        title = title,
        summary = summary ?: entries.find { it.value == value }?.name,
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
fun PreviewListPreference() {
    ListPreference(title = "List preference",
        summary = "This is a list preference",
        entries = listOf(
            Entry("Entry 1", 0),
            Entry("Entry 2", 1),
            Entry("Entry 3", 2)
        ),
        value = 1,
        onEntrySelected = {}
    )
}