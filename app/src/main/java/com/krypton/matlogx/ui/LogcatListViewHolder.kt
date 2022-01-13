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

package com.krypton.matlogx.ui

import android.view.View
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import com.krypton.matlogx.R
import com.krypton.matlogx.data.LogInfo

class LogcatListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val pidView: TextView = itemView.findViewById(R.id.pid)
    private val timestampView: TextView = itemView.findViewById(R.id.timestamp)
    private val tagView: TextView = itemView.findViewById(R.id.tag)
    private val messageView: TextView = itemView.findViewById(R.id.message)
    val levelView: TextView = itemView.findViewById(R.id.level)

    private var isExpanded = false
    private var logInfo: LogInfo? = null

    init {
        updateView()
    }

    private fun updateView() {
        pidView.visibility = if (isExpanded && (logInfo?.hasOnlyMessage() != true)) View.VISIBLE else View.GONE
        timestampView.visibility = if (isExpanded && (logInfo?.hasOnlyMessage() != true)) View.VISIBLE else View.GONE
        tagView.isSingleLine = !isExpanded
        messageView.isSingleLine = !isExpanded
    }

    fun setLogInfo(logInfo: LogInfo) {
        if (logInfo.hasOnlyMessage()) {
            pidView.visibility = View.GONE
            timestampView.visibility = View.GONE
            tagView.visibility = View.GONE
            levelView.visibility = View.GONE
        } else {
            pidView.text = logInfo.pid.toString()
            timestampView.text = logInfo.timestamp
            tagView.text = logInfo.tag
            tagView.visibility = View.VISIBLE
            levelView.text = logInfo.level.toString()
            levelView.visibility = View.VISIBLE
            itemView.setOnClickListener {
                isExpanded = !isExpanded
                updateView()
            }
        }
        messageView.text = logInfo.message
        updateView()
    }
}