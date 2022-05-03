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

package com.krypton.matlogx.ui.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OverflowMenu(
    overflowIcon: @Composable () -> Unit,
    expanded: Boolean,
    onOverflowIconClicked: () -> Unit,
    onDismissRequest: () -> Unit,
    items: @Composable (ColumnScope.() -> Unit),
) {
    Box(Modifier.wrapContentSize(Alignment.TopEnd)) {
        IconButton(
            onClick = onOverflowIconClicked,
            content = overflowIcon
        )
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.wrapContentSize(Alignment.Center),
        content = items
    )
}

@Composable
fun MenuItem(
    title: String,
    iconContentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    painter: Painter? = null,
    imageVector: ImageVector? = null,
    onClick: () -> Unit = {},
) {
    DropdownMenuItem(
        modifier = modifier.padding(end = 24.dp),
        enabled = enabled,
        leadingIcon = {
            when {
                painter != null -> Icon(
                    painter = painter,
                    contentDescription = iconContentDescription
                )
                imageVector != null -> Icon(
                    imageVector = imageVector,
                    contentDescription = iconContentDescription
                )
                else -> Spacer(modifier = Modifier.size(24.dp))
            }
        },
        text = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        onClick = onClick,
    )
}
