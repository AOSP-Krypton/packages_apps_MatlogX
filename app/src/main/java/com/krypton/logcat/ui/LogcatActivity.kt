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

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.krypton.logcat.R
import com.krypton.logcat.viewmodel.LogcatViewModel

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LogcatActivity : AppCompatActivity() {
    private val logcatViewModel: LogcatViewModel by viewModels()
    private lateinit var logcatListView: RecyclerView
    private lateinit var logcatListAdapter: LogcatListAdapter
    private lateinit var loadingProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logcat)
        setSupportActionBar(findViewById(R.id.toolbar))
        setupListView()

        loadingProgressBar = findViewById(R.id.loading_progress_bar)
        logcatViewModel.getLogcatLiveData().observe(this, {
            loadingProgressBar.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            logcatListAdapter.submitList(it)
        })
    }

    override fun onStart() {
        super.onStart()
        logcatViewModel.readLogcat()
    }

    private fun setupListView() {
        logcatListView = findViewById(R.id.log_list)
        logcatListView.layoutManager = LinearLayoutManager(this)
        logcatListAdapter = LogcatListAdapter()
        logcatListView.adapter = logcatListAdapter
    }
}