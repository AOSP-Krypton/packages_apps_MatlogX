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

import android.net.Uri
import android.util.ArrayMap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.krypton.matlogx.data.Event
import com.krypton.matlogx.data.LogcatListData
import com.krypton.matlogx.data.Result
import com.krypton.matlogx.repo.LogcatRepository
import com.krypton.matlogx.repo.SettingsRepository

import dagger.hilt.android.lifecycle.HiltViewModel

import javax.inject.Inject

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class LogcatViewModel @Inject constructor(
    private val logcatRepository: LogcatRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _logcatLiveData = MutableLiveData<List<LogcatListData>>()
    val logcatLiveData: LiveData<List<LogcatListData>> = _logcatLiveData
    private val logList = mutableListOf<LogcatListData>()
    private var job: Job? = null

    // Whether livedata should be updated when new log info is collected from repository
    var logcatUpdatePaused = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataChanged()
            }
        }

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

    private var logLevel: String = "V"
    var includeDeviceInfo = false
        private set
    private var sizeLimit = 0

    private val _logSaveResult = MutableLiveData<Event<Result<Uri>>>()
    val logSaveResult: LiveData<Event<Result<Uri>>> = _logSaveResult

    private var initDone = false

    var collectLogs = false
        set(value) {
            if (field != value) {
                field = value
                if (!value) {
                    cancelJob()
                    _logcatLiveData.value = emptyList()
                } else if (initDone) {
                    startJob()
                }
            }
        }

    private var isExpanded = false
    private val _expandLogsLiveData = MutableLiveData<Boolean>()
    val expandLogsLiveData: LiveData<Boolean> = _expandLogsLiveData

    private var textSize = 0
    private val _textSizeChangedLiveData = MutableLiveData<Event<Boolean>>()
    val textSizeChangedLiveData: LiveData<Event<Boolean>> = _textSizeChangedLiveData

    private var currentIndex = 0
    private var limit = 0

    init {
        viewModelScope.run {
            launch {
                logLevel = settingsRepository.getLogLevel().first()
                includeDeviceInfo = settingsRepository.getIncludeDeviceInfo().first()
                sizeLimit = settingsRepository.getLogcatSizeLimit().first()
                isExpanded = settingsRepository.getExpandedByDefault().first()
                textSize = settingsRepository.getTextSize().first()
                initDone = true
                startJob()
            }
            launch {
                settingsRepository.getLogLevel().collectLatest {
                    if (logLevel != it) {
                        logLevel = it
                        restartLogcat()
                    }
                }
            }
            launch {
                settingsRepository.getIncludeDeviceInfo().collectLatest { includeDeviceInfo = it }
            }
            launch {
                settingsRepository.getLogcatSizeLimit().collectLatest {
                    if (sizeLimit != it) {
                        sizeLimit = it
                        restartLogcat()
                    }
                }
            }
            launch {
                settingsRepository.getExpandedByDefault().collectLatest {
                    if (isExpanded != it) {
                        isExpanded = it
                        logList.forEach { data ->
                            data.isExpanded = isExpanded
                        }
                        notifyDataChanged()
                        _expandLogsLiveData.value = isExpanded
                    }
                }
            }
            launch {
                settingsRepository.getTextSize().collectLatest {
                    if (textSize != it) {
                        textSize = it
                        logList.forEach { data ->
                            data.textSize = textSize
                        }
                        notifyDataChanged()
                        _textSizeChangedLiveData.value = Event(true)
                    }
                }
            }
        }
    }

    fun handleSearch(query: String?) {
        if (cachedQuery == query) return
        cachedQuery = query
        restartLogcat()
    }

    /**
     * Save logcat to a zip file.
     */
    fun saveLogAsZip() {
        viewModelScope.launch {
            _logSaveResult.value = Event(
                logcatRepository.saveLogAsZip(
                    null, /* Tags isn't supported yet */
                    cachedQuery,
                    logLevel,
                    includeDeviceInfo,
                )
            )
        }
    }

    /**
     * Update and save the log level.
     *
     * @param level the new log level.
     */
    fun setLogLevel(level: Int) {
        val newLogLevel = logLevelMap.keyAt(logLevelMap.indexOfValue(level))
        if (newLogLevel == logLevel) return
        logLevel = newLogLevel
        viewModelScope.launch {
            settingsRepository.setLogLevel(logLevel)
            restartLogcat()
        }
    }

    /**
     * Get the log level.
     *
     * @return the log level.
     */
    fun getLogLevel(): Int = logLevelMap[logLevel] ?: 0

    /**
     * Update and save setting indicating whether or not to
     * include device info.
     *
     * @param include the value of the setting.
     */
    fun setIncludeDeviceInfo(include: Boolean) {
        if (includeDeviceInfo == include) return
        includeDeviceInfo = include
        viewModelScope.launch {
            settingsRepository.setIncludeDeviceInfo(includeDeviceInfo)
        }
    }

    /**
     * Toggle whether to expand log message by default.
     */
    fun toggleExpandedState() {
        viewModelScope.launch {
            settingsRepository.setExpandedByDefault(!isExpanded)
        }
    }

    private fun restartLogcat() {
        cancelJob()
        _logcatLiveData.value = emptyList()
        startJob()
    }

    private fun startJob() {
        if (!collectLogs || !initDone) return
        job = viewModelScope.launch {
            if (logList.isNotEmpty()) {
                // Start fresh on a new job
                logList.clear()
                _logcatLiveData.value = emptyList()
            }

            val size = logcatRepository.getLogcatSize(
                null /* Tags isn't supported yet */,
                cachedQuery,
                logLevel
            )
            limit = minOf(sizeLimit, size)
            logcatRepository.getLogcatStream(
                null /* Tags isn't supported yet */,
                cachedQuery,
                logLevel
            ).collectIndexed { index, logInfo ->
                currentIndex = index
                if (limit != 0 && logList.size == limit) {
                    logList.removeFirst()
                }
                val data = LogcatListData(logInfo, isExpanded, textSize)
                logList.add(data)
                notifyDataChanged()
            }
        }
    }

    private fun notifyDataChanged() {
        // This restriction is here so that the recycler view won't struggle
        // when elements are pumped in one by one rapidly for the first time.
        // Displaying a large set of logs first and then pushing the rest one by one
        // is better.
        if (!logcatUpdatePaused && ((currentIndex >= (limit - 1)) || (cachedQuery?.isNotBlank() == true))) {
            _logcatLiveData.value = logList.toList()
        }
    }

    private fun cancelJob() {
        if (job != null) {
            job?.cancel()
            job = null
        }
    }
}