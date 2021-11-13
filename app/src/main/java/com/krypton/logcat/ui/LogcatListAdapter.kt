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

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.krypton.logcat.R

import com.krypton.logcat.data.LogInfo

class LogcatListAdapter : ListAdapter<LogInfo, LogcatListViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogcatListViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.logcat_list_item, parent, false)
        return LogcatListViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LogcatListViewHolder, position: Int) {
        val logInfo = getItem(position)
        with(holder) {
            pidView.text = logInfo.pid.toString()
            timestampView.text = logInfo.timestamp
            tagView.text = logInfo.tag
            levelView.text = logInfo.level.toLetter()
            messageView.text = logInfo.message
            itemView.setOnClickListener { holder.toggleExpandedState() }
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<LogInfo>() {
            override fun areItemsTheSame(oldItem: LogInfo, newItem: LogInfo) =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: LogInfo, newItem: LogInfo) =
                oldItem == newItem
        }
    }
}