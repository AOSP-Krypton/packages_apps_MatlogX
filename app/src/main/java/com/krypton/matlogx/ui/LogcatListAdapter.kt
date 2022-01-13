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

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

import com.krypton.matlogx.R
import com.krypton.matlogx.data.LogInfo

class LogcatListAdapter(context: Context) : ListAdapter<LogInfo, LogcatListViewHolder>(diffCallback) {

    private val layoutInflater = LayoutInflater.from(context)

    // Background & Foreground color map for different log levels
    private val colorMap = mapOf(
        'V' to Pair(
            context.getColor(R.color.background_verbose),
            context.getColor(R.color.foreground_verbose)
        ),
        'D' to Pair(
            context.getColor(R.color.background_debug),
            context.getColor(R.color.foreground_debug)
        ),
        'I' to Pair(
            context.getColor(R.color.background_info),
            context.getColor(R.color.foreground_info)
        ),
        'W' to Pair(
            context.getColor(R.color.background_warn),
            context.getColor(R.color.foreground_warn)
        ),
        'E' to Pair(
            context.getColor(R.color.background_error),
            context.getColor(R.color.foreground_error)
        ),
        'F' to Pair(
            context.getColor(R.color.background_fatal),
            context.getColor(R.color.foreground_fatal)
        ),
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogcatListViewHolder {
        return LogcatListViewHolder(layoutInflater.inflate(R.layout.logcat_list_item,
            parent, false))
    }

    override fun onBindViewHolder(holder: LogcatListViewHolder, position: Int) {
        val logInfo = getItem(position)
        holder.apply {
            setLogInfo(logInfo)
            if (!logInfo.hasOnlyMessage()) {
                val color = colorMap[logInfo.level]!!
                levelView.setBackgroundColor(color.first)
                levelView.setTextColor(color.second)
            }
        }
    }

    companion object {
        private val diffCallback = object: DiffUtil.ItemCallback<LogInfo>() {
            override fun areItemsTheSame(oldItem: LogInfo, newItem: LogInfo) =
                oldItem.timestamp == newItem.timestamp

            override fun areContentsTheSame(oldItem: LogInfo, newItem: LogInfo) =
                oldItem == newItem
        }
    }
}