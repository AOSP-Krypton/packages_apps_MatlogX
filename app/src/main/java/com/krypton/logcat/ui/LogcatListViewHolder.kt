/*
 * Copyright (C) 2021 AOSP-Krypton Project
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

package com.krypton.logcat.ui

import android.view.View
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import com.krypton.logcat.R

class LogcatListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val pidView: TextView = itemView.findViewById(R.id.pid)
    val timestampView: TextView = itemView.findViewById(R.id.timestamp)
    val tagView: TextView = itemView.findViewById(R.id.tag)
    val levelView: TextView = itemView.findViewById(R.id.level)
    val messageView: TextView = itemView.findViewById(R.id.message)

    private var isExpanded = false

    init {
        updateView()
    }

    fun toggleExpandedState() {
        isExpanded = !isExpanded
        updateView()
    }

    private fun updateView() {
        pidView.visibility = if (isExpanded) View.VISIBLE else View.GONE
        timestampView.visibility = if (isExpanded) View.VISIBLE else View.GONE
        tagView.isSingleLine = !isExpanded
        messageView.isSingleLine = !isExpanded
    }
}