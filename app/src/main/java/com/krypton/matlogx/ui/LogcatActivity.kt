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

import android.Manifest
import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.SearchView

import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.krypton.matlogx.R
import com.krypton.matlogx.provider.SuggestionProvider
import com.krypton.matlogx.viewmodel.LogcatViewModel

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LogcatActivity : AppCompatActivity() {

    private val logcatViewModel: LogcatViewModel by viewModels()
    private lateinit var logcatListView: RecyclerView
    private lateinit var logcatListAdapter: LogcatListAdapter
    private lateinit var logcatLayoutManager: LinearLayoutManager
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var searchView: SearchView
    private lateinit var toolbar: Toolbar
    private lateinit var topScrollButton: FloatingActionButton
    private lateinit var bottomScrollButton: FloatingActionButton

    private var scrolledToBottomInitial = false
    private var internalScroll = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logcat)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        loadingProgressBar = findViewById(R.id.loading_progress_bar)
        topScrollButton = findViewById<FloatingActionButton>(R.id.top_scroll_button).apply {
            setOnClickListener {
                hide()
                internalScroll = true
                logcatListView.scrollToPosition(0)
            }
        }
        bottomScrollButton = findViewById<FloatingActionButton>(R.id.bottom_scroll_button).apply {
            setOnClickListener {
                hide()
                if (logcatListAdapter.itemCount > 0) {
                    internalScroll = true
                    logcatListView.scrollToPosition(logcatListAdapter.itemCount - 1)
                }
            }
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
                if (logcatListAdapter.itemCount > 0 && (!scrolledToBottomInitial || logcatViewModel.autoScroll)) {
                    scrolledToBottomInitial = true
                    internalScroll = true
                    logcatListView.scrollToPosition(logcatListAdapter.itemCount - 1)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSearchIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.logcat_toolbar_menu, menu)
        menu.findItem(R.id.pause_button).setIcon(
            if (logcatViewModel.logcatUpdatePaused)
                R.drawable.ic_baseline_play_arrow_24
            else
                R.drawable.ic_baseline_pause_24
        )
        val searchManager = getSystemService(SearchManager::class.java)
        searchView = (menu.findItem(R.id.search_button).actionView as SearchView).also {
            it.setSearchableInfo(
                searchManager.getSearchableInfo(
                    componentName
                )
            )
            it.setOnCloseListener {
                toolbar.setTitle(R.string.app_name)
                logcatViewModel.handleSearch(null)
                false
            }
            it.setOnSearchClickListener {
                toolbar.title = null
            }
        }
        handleSearchIntent(intent)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.pause_button -> {
                logcatViewModel.logcatUpdatePaused = !logcatViewModel.logcatUpdatePaused
                if (logcatViewModel.logcatUpdatePaused) {
                    item.icon = ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_baseline_play_arrow_24,
                        null
                    )
                } else {
                    item.icon = ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_baseline_pause_24,
                        null
                    )
                    logcatListView.scrollToPosition(logcatListAdapter.itemCount - 1)
                }
                true
            }
            R.id.settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun handleSearchIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEARCH) return
        val query: String? = intent.getStringExtra(SearchManager.QUERY)
        searchView.setQuery(query, false)
        if (query?.isNotBlank() == true) {
            SearchRecentSuggestions(
                this,
                SuggestionProvider.AUTHORITY,
                SuggestionProvider.MODE
            ).saveRecentQuery(query, null)
        }
        logcatViewModel.handleSearch(query)
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
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    logcatViewModel.autoScroll =
                        newState == RecyclerView.SCROLL_STATE_IDLE && isLastItemVisible()
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (!internalScroll) {
                        if (dy < -2) {
                            if (isFirstItemVisible()) {
                                topScrollButton.hide()
                            } else {
                                topScrollButton.show()
                            }
                            bottomScrollButton.hide()
                        } else if (dy > 2) {
                            topScrollButton.hide()
                            if (isLastItemVisible()) {
                                bottomScrollButton.hide()
                            } else {
                                bottomScrollButton.show()
                            }
                        }
                    } else {
                        internalScroll = false
                    }
                }
            })
        }
    }

    private fun isFirstItemVisible() =
        logcatLayoutManager.findFirstCompletelyVisibleItemPosition() == 0

    private fun isLastItemVisible() =
        logcatLayoutManager.findLastCompletelyVisibleItemPosition() == logcatListAdapter.itemCount - 1
}