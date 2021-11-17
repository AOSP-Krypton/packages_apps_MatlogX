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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView

import com.krypton.logcat.R
import com.krypton.logcat.data.LogInfo

class LogcatListAdapter(context: Context) : RecyclerView.Adapter<LogcatListViewHolder>() {

    private var list: List<LogInfo> = emptyList()

    // Background & Foreground color maps for different log levels
    private val bgColorMap = mapOf(
        LogInfo.Level.VERBOSE to context.getColor(R.color.background_verbose),
        LogInfo.Level.DEBUG to context.getColor(R.color.background_debug),
        LogInfo.Level.INFO to context.getColor(R.color.background_info),
        LogInfo.Level.WARN to context.getColor(R.color.background_warn),
        LogInfo.Level.ERROR to context.getColor(R.color.background_error),
        LogInfo.Level.FATAL to context.getColor(R.color.background_fatal),
    )

    private val fgColorMap = mapOf(
        LogInfo.Level.VERBOSE to context.getColor(R.color.foreground_verbose),
        LogInfo.Level.DEBUG to context.getColor(R.color.foreground_debug),
        LogInfo.Level.INFO to context.getColor(R.color.foreground_info),
        LogInfo.Level.WARN to context.getColor(R.color.foreground_warn),
        LogInfo.Level.ERROR to context.getColor(R.color.foreground_error),
        LogInfo.Level.FATAL to context.getColor(R.color.foreground_fatal),
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogcatListViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.logcat_list_item, parent, false)
        return LogcatListViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LogcatListViewHolder, position: Int) {
        val logInfo = getItem(position)
        with(holder) {
            if (logInfo.level == LogInfo.Level.UNKNOWN) {
                pidView.visibility = View.GONE
                timestampView.visibility = View.GONE
                tagView.visibility = View.GONE
                levelView.visibility = View.GONE
            } else {
                pidView.text = logInfo.pid.toString()
                timestampView.text = logInfo.timestamp
                tagView.text = logInfo.tag
                levelView.text = logInfo.level.toChar()
                levelView.setBackgroundColor(getLogLevelBgColor(logInfo.level))
                levelView.setTextColor(getLogLevelFgColor(logInfo.level))
                itemView.setOnClickListener { holder.toggleExpandedState() }
            }
            messageView.text = logInfo.message
        }
    }

    override fun getItemCount() = list.size

    /**
     * Submit a [List] of [LogInfo] to be displayed in the list view.
     * TODO Update the list diffing algorithm when search support is added.
     *
     * @param list the list of [LogInfo].
     */
    fun submitList(list: List<LogInfo>) {
        val currentSize = this.list.size
        val sizeDiff = list.size - currentSize
        if (sizeDiff > 0) {
            this.list = list
            notifyItemRangeInserted(currentSize, sizeDiff)
        }
    }

    private fun getItem(position: Int) = list[position]

    private fun getLogLevelBgColor(level: LogInfo.Level): Int = bgColorMap[level]!!

    private fun getLogLevelFgColor(level: LogInfo.Level): Int = fgColorMap[level]!!
}