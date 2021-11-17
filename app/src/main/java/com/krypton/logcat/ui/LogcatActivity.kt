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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar

import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private lateinit var logcatLayoutManager: LinearLayoutManager
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var pauseButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logcat)
        setSupportActionBar(findViewById(R.id.toolbar))
        loadingProgressBar = findViewById(R.id.loading_progress_bar)
        pauseButton = findViewById<ImageButton>(R.id.pause_button).apply {
            setOnClickListener { logcatViewModel.togglePauseButtonState() }
        }
    }

    override fun onStart() {
        super.onStart()
        val status = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_LOGS)
        if (status == PackageManager.PERMISSION_DENIED) {
            showPermissionHelperDialog()
        } else {
            setupListView()
            logcatViewModel.getLogcatLiveData().observe(this) {
                loadingProgressBar.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
                logcatListAdapter.submitList(it)
                if (logcatViewModel.autoScroll) logcatListView.scrollToPosition(it.size - 1)
            }
            logcatViewModel.getPauseButtonState().observe(this) {
                if (it) {
                    pauseButton.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                } else {
                    pauseButton.setImageResource(R.drawable.ic_baseline_pause_24)
                    logcatListView.scrollToPosition(logcatListAdapter.itemCount - 1)
                }
            }
        }
    }

    private fun showPermissionHelperDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.grant_permissions)
            .setMessage(R.string.how_to_grant)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupListView() {
        logcatListAdapter = LogcatListAdapter(this)
        logcatLayoutManager = LinearLayoutManager(this)
        logcatListView = findViewById<RecyclerView>(R.id.log_list).apply {
            setHasFixedSize(true)
            layoutManager = logcatLayoutManager
            adapter = logcatListAdapter
            itemAnimator = null
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    logcatViewModel.autoScroll =
                        logcatLayoutManager.findLastCompletelyVisibleItemPosition() == logcatListAdapter.itemCount - 1
                }
            })
        }
    }
}