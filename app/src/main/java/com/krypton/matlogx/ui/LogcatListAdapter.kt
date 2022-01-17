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

import androidx.recyclerview.widget.RecyclerView

import com.krypton.matlogx.R
import com.krypton.matlogx.data.LogcatListData

class LogcatListAdapter(context: Context) : RecyclerView.Adapter<LogcatListViewHolder>() {

    private val layoutInflater = LayoutInflater.from(context)
    private var list = emptyList<LogcatListData>()

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
        return LogcatListViewHolder(
            layoutInflater.inflate(
                R.layout.logcat_list_item,
                parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: LogcatListViewHolder, position: Int) {
        val data = list[position]
        holder.apply {
            setData(data)
            if (!data.logInfo.hasOnlyMessage()) {
                val color = colorMap[data.logInfo.level]
                if (color != null) {
                    levelView.setBackgroundColor(color.first)
                    levelView.setTextColor(color.second)
                }
            }
        }
    }

    override fun getItemCount(): Int = list.size

    /**
     * Since this adapter is only meant to submit empty list /
     * a new list that is just the old list + new elements /
     * a new list of same size which is just left shifted, we
     * can skip diffing list and do things in a memory efficient way.
     *
     * @param newList the new list to be submitted.
     */
    fun submitList(newList: List<LogcatListData>) {
        when {
            newList.size == list.size -> {
                if (newList.isEmpty()) return // If empty list is submitted more than once.
                list = newList
                notifyItemRemoved(0)
                notifyItemInserted(newList.size)
            }
            newList.size > list.size -> {
                val startIndex = list.size
                val sizeDiff = newList.size - startIndex
                list = newList
                notifyItemRangeInserted(startIndex, sizeDiff)
            }
            newList.isEmpty() -> {
                val count = list.size
                list = newList
                notifyItemRangeRemoved(0, count)
            }
        }
    }
}