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

package com.krypton.matlogx.viewmodel

import android.util.ArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.krypton.matlogx.data.LogInfo
import com.krypton.matlogx.repo.LogcatRepository

import dagger.hilt.android.lifecycle.HiltViewModel

import java.util.LinkedList

import javax.inject.Inject

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch

@HiltViewModel
class LogcatViewModel @Inject constructor(
    private val logcatRepository: LogcatRepository
) : ViewModel() {

    private val logcatLiveData = MutableLiveData<List<LogInfo>>()
    private val logList = LinkedList<LogInfo>()
    private var job: Job? = null

    // Whether livedata should be updated when new log info is collected from repository
    var logcatUpdatePaused = false

    // Whether we should scroll to the bottom automatically
    // when new log info is added to the list.
    var autoScroll = true

    private var cachedQuery: String? = null

    private val logLevelMap = ArrayMap<String, Int>(6).apply {
        put("V", 0)
        put("I", 1)
        put("D", 2)
        put("W", 3)
        put("E", 4)
        put("F", 5)
    }

    var logLevel: Int = logLevelMap[logcatRepository.getLogLevel()]!!
        set(value) {
            field = value
            logcatRepository.setLogLevel(logLevelMap.keyAt(logLevelMap.indexOfValue(value)))
            job?.cancel()
            startJob()
        }

    init {
        // Start reading on init
        startJob()
    }

    /**
     * Get a [LiveData] for view to observe.
     *
     * @return [LiveData] of a [List] of [LogInfo].
     */
    fun getLogcatLiveData(): LiveData<List<LogInfo>> = logcatLiveData

    fun handleSearch(query: String?) {
        if (cachedQuery == query) return
        cachedQuery = query
        job?.cancel()
        startJob()
    }

    override fun onCleared() {
        cancelJob()
        super.onCleared()
    }

    private fun startJob() {
        job = viewModelScope.launch {

            // Start fresh on a new job
            logList.clear()
            logcatLiveData.value = emptyList()

            val limit = logcatRepository.getLogcatSizeLimit()
            val size = logcatRepository.getLogcatSize(cachedQuery)
            val actualLimit = minOf(limit, size)
            logcatRepository.getLogcatStream(cachedQuery).cancellable()
                .collectIndexed { index, logInfo ->
                    if (logList.size == actualLimit) {
                        logList.removeFirst()
                    }
                    logList.add(logInfo)
                    // This restriction is here so that the recycler view won't struggle
                    // when elements are pumped in one by one rapidly for the first time.
                    // Displaying a large set of logs first and then pushing the rest one by one
                    // is better.
                    if (!logcatUpdatePaused && ((index >= (actualLimit - 1)) || (cachedQuery?.isNotBlank() == true))) {
                        logcatLiveData.value = logList.toList()
                    }
                }
        }
    }

    private fun cancelJob() {
        if (job != null) {
            job?.cancel()
            job = null
        }
    }
}