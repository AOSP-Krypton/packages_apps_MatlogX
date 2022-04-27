/*
 * Copyright (C) 2021-2022 AOSP-Krypton Project
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

package com.krypton.matlogx.viewmodels

import android.net.Uri
import android.util.ArrayMap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.krypton.matlogx.data.Event
import com.krypton.matlogx.data.LogcatListData
import com.krypton.matlogx.data.LogcatRepository
import com.krypton.matlogx.data.settings.SettingsRepository

import dagger.hilt.android.lifecycle.HiltViewModel

import javax.inject.Inject

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class LogcatViewModel @Inject constructor(
    private val logcatRepository: LogcatRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _logcatLiveData = MutableLiveData<List<LogcatListData>>()
    val logcatLiveData: LiveData<List<LogcatListData>> = _logcatLiveData

    private val _loadingProgressLiveData = MutableLiveData(Event(true))
    val loadingProgressLiveData: LiveData<Event<Boolean>> = _loadingProgressLiveData

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

    private var logcatBuffers: String = ""

    init {
        viewModelScope.launch {
            initSettings()
            startJob()
        }
        observeLogLevel()
        viewModelScope.launch {
            settingsRepository.getIncludeDeviceInfo().collectLatest { includeDeviceInfo = it }
        }
        observeLogSizeLimit()
        observeExpandedState()
        observeTextSize()
        observeBuffers()
    }

    private suspend fun initSettings() {
        logLevel = settingsRepository.getLogLevel().first()
        includeDeviceInfo = settingsRepository.getIncludeDeviceInfo().first()
        sizeLimit = settingsRepository.getLogcatSizeLimit().first()
        isExpanded = settingsRepository.getExpandedByDefault().first()
        textSize = settingsRepository.getTextSize().first()
        logcatBuffers = settingsRepository.getLogcatBuffers().first()
        initDone = true
    }

    private fun observeLogLevel() {
        viewModelScope.launch {
            settingsRepository.getLogLevel().collectLatest {
                if (logLevel != it && initDone) {
                    logLevel = it
                    restartLogcat()
                }
            }
        }
    }

    private fun observeLogSizeLimit() {
        viewModelScope.launch {
            settingsRepository.getLogcatSizeLimit().collectLatest {
                if (sizeLimit != it && initDone) {
                    sizeLimit = it
                    restartLogcat()
                }
            }
        }
    }

    private fun observeExpandedState() {
        viewModelScope.launch {
            settingsRepository.getExpandedByDefault().collectLatest {
                if (isExpanded != it && initDone) {
                    isExpanded = it
                    logList.forEach { data ->
                        data.isExpanded = isExpanded
                    }
                    notifyDataChanged()
                    _expandLogsLiveData.value = isExpanded
                }
            }
        }
    }

    private fun observeTextSize() {
        viewModelScope.launch {
            settingsRepository.getTextSize().collectLatest {
                if (textSize != it && initDone) {
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

    private fun observeBuffers() {
        viewModelScope.launch {
            settingsRepository.getLogcatBuffers().collectLatest {
                if (logcatBuffers != it && initDone) {
                    logcatBuffers = it
                    restartLogcat()
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
        if (!initDone) return
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
        if (!initDone || includeDeviceInfo == include) return
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

    /**
     * Clear all cached logs.
     */
    fun clearLogs() {
        if (logList.isNotEmpty()) {
            logList.clear()
            _logcatLiveData.value = emptyList()
        }
    }

    private fun restartLogcat() {
        cancelJob()
        startJob()
    }

    private fun startJob() {
        if (!collectLogs || !initDone) return
        job = viewModelScope.launch {
            val logs = logcatRepository.getLogsAsList(
                null /* Tags isn't supported yet */,
                cachedQuery,
                logLevel
            )
            val logSize = logs.size
            logList.addAll(
                if (sizeLimit < logSize) {
                    logs.subList(logSize - sizeLimit, logSize - 1)
                } else {
                    logs
                }.map {
                    LogcatListData(it, isExpanded, textSize)
                })
            if (_loadingProgressLiveData.value?.peek() == true)
                _loadingProgressLiveData.value = Event(false)
            notifyDataChanged()
            logcatRepository.getLogcatStream(
                null /* Tags isn't supported yet */,
                cachedQuery,
                logLevel
            ).drop(logSize).collect {
                logList.add(LogcatListData(it, isExpanded, textSize))
                if (logList.size > sizeLimit) logList.removeFirstOrNull()
                notifyDataChanged()
            }
        }
    }

    private fun notifyDataChanged() {
        if (!logcatUpdatePaused) {
            _logcatLiveData.value = logList.toList()
        }
    }

    private fun cancelJob() {
        if (job != null) {
            job?.cancel()
            job = null
        }
        clearLogs()
        _loadingProgressLiveData.value = Event(true)
    }
}