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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun EditTextPreference(
    title: String,
    summary: String? = null,
    clickable: Boolean = true,
    onClick: (() -> Unit)? = null,
    value: String,
    onValueSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        var textValue by remember { mutableStateOf(value) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onValueSelected(textValue)
                }) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
            shape = RoundedCornerShape(32.dp),
            title = {
                Text(text = title)
            },
            text = {
                TextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                    },
                    singleLine = true
                )
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
fun PreviewEditTextPreference() {
    EditTextPreference(
        title = "Edit text preference",
        summary = "This is an edit text preference",
        value = "text",
        onValueSelected = {}
    )
}