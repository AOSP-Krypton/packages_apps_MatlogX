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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.krypton.matlogx.data.AppRepository
import com.krypton.matlogx.data.LogcatListData
import com.krypton.matlogx.data.LogcatRepository
import com.krypton.matlogx.data.settings.SettingsRepository

import dagger.hilt.android.lifecycle.HiltViewModel

import javax.inject.Inject

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class LogcatViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val logcatRepository: LogcatRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _logcatList = MutableStateFlow(emptyList<LogcatListData>())
    val logcatList: StateFlow<List<LogcatListData>>
        get() = _logcatList

    private val _loadingData = MutableStateFlow(true)
    val loadingData: StateFlow<Boolean>
        get() = _loadingData

    private val logList = mutableListOf<LogcatListData>()
    private var job: Job? = null

    private var cachedQuery: String? = null

    private val logLevelMap = ArrayMap<String, Int>(6).apply {
        put("V", 0)
        put("I", 1)
        put("D", 2)
        put("W", 3)
        put("E", 4)
        put("F", 5)
    }

    private var isExpanded = false
    private var textSize = 0

    private val listMutex = Mutex()

    val searchSuggestions: Flow<List<String>>
        get() = appRepository.searchSuggestions

    private val _logcatFlowSuspended = MutableStateFlow(false)
    val logcatStreamPaused: StateFlow<Boolean>
        get() = _logcatFlowSuspended

    val includeDeviceInfo: Flow<Boolean>
        get() = settingsRepository.getIncludeDeviceInfo()

    private val _logLevel: Flow<String>
        get() = settingsRepository.getLogLevel()

    val logLevel: Flow<Int>
        get() = _logLevel.map { logLevelMap[it] ?: 0 }

    val recordingLogs: StateFlow<Boolean>
        get() = logcatRepository.recordingLogs

    fun init() {
        viewModelScope.launch {
            isExpanded = settingsRepository.getExpandedByDefault().first()
            textSize = settingsRepository.getTextSize().first()
            observeExpandedState()
            observeTextSize()
            observeLogLevel()
            observeLogSizeLimit()
            observeBuffers()
            startJob()
        }
    }

    private fun observeExpandedState() {
        viewModelScope.launch {
            settingsRepository.getExpandedByDefault().filterNot { isExpanded == it }.collectLatest {
                isExpanded = it
                listMutex.withLock {
                    logList.replaceAll { data ->
                        data.copy(isExpanded = isExpanded)
                    }
                }
                notifyDataChanged()
            }
        }
    }

    private fun observeTextSize() {
        viewModelScope.launch {
            settingsRepository.getTextSize().filterNot { textSize == it }.collectLatest {
                textSize = it
                listMutex.withLock {
                    logList.replaceAll { data ->
                        data.copy(textSize = textSize)
                    }
                }
                notifyDataChanged()
            }
        }
    }

    private fun observeLogLevel() {
        viewModelScope.launch {
            settingsRepository.getLogLevel().collect {
                _logcatFlowSuspended.value = false
                restartLogcat()
            }
        }
    }

    private fun observeLogSizeLimit() {
        viewModelScope.launch {
            settingsRepository.getLogcatSizeLimit().collect {
                _logcatFlowSuspended.value = false
                restartLogcat()
            }
        }
    }

    private fun observeBuffers() {
        viewModelScope.launch {
            settingsRepository.getLogcatBuffers().collect {
                _logcatFlowSuspended.value = false
                restartLogcat()
            }
        }
    }

    fun handleSearch(query: String?) {
        if (cachedQuery == query) return
        cachedQuery = query
        viewModelScope.launch {
            notifyDataChanged()
        }
    }

    private fun restartLogcat() {
        cancelJob()
        startJob()
    }

    /**
     * Save logcat to a zip file.
     */
    suspend fun saveLogAsZip() =
        logcatRepository.saveLogAsZip(
            null, /* Tags isn't supported yet */
            _logLevel.first(),
            includeDeviceInfo.first(),
        )

    /**
     * Update and save the log level.
     *
     * @param level the new log level.
     */
    fun setLogLevel(level: Int) {
        val index = logLevelMap.indexOfValue(level).takeIf { it != -1 } ?: return
        viewModelScope.launch {
            settingsRepository.setLogLevel(logLevelMap.keyAt(index))
        }
    }

    /**
     * Update and save setting indicating whether or not to
     * include device info.
     *
     * @param includeDeviceInfo the value of the setting.
     */
    fun setIncludeDeviceInfo(includeDeviceInfo: Boolean) {
        viewModelScope.launch {
            settingsRepository.setIncludeDeviceInfo(includeDeviceInfo)
        }
    }

    /**
     * Clear all cached logs.
     */
    fun clearLogs() {
        viewModelScope.launch {
            listMutex.withLock {
                if (logList.isNotEmpty()) {
                    logList.clear()
                }
            }
            notifyDataChanged()
        }
    }

    fun expandItemAtIndex(index: Int, expanded: Boolean) {
        viewModelScope.launch {
            listMutex.withLock {
                logList.getOrNull(index)?.copy(isExpanded = expanded)?.let {
                    logList.removeAt(index)
                    logList.add(index, it)
                }
            }
            notifyDataChanged()
        }
    }

    private fun startJob() {
        // Make sure to cancel current active job
        job?.cancel()
        job = viewModelScope.launch {
            listMutex.withLock {
                logList.clear()
            }
            val logLevel = settingsRepository.getLogLevel().first()
            val logs = withContext(Dispatchers.Default) {
                logcatRepository.getLogsAsList(
                    null /* Tags isn't supported yet */,
                    logLevel
                ).toMutableList()
            }
            val sizeLimit = settingsRepository.getLogcatSizeLimit().first()
            val logsToDrop = (logs.size - sizeLimit).coerceAtLeast(0)
            withContext(Dispatchers.Default) {
                listMutex.withLock {
                    logList.addAll(
                        logs.subList(
                            logsToDrop,
                            logs.lastIndex
                        ).filter {
                            it.hasString(cachedQuery)
                        }.map {
                            LogcatListData(it, isExpanded, textSize)
                        }
                    )
                }
            }
            // Let's free some memory
            logs.clear()
            _loadingData.value = false
            notifyDataChanged()
            delay(1000)
            logcatRepository.getLogcatStream(
                null /* Tags isn't supported yet */,
                logLevel
            ).drop(logsToDrop)
                .map {
                    LogcatListData(it, isExpanded, textSize)
                }
                .collect {
                    listMutex.withLock {
                        logList.add(it)
                        if (logList.size > sizeLimit) logList.removeFirstOrNull()
                    }
                    viewModelScope.launch {
                        delay(50)
                        notifyDataChanged()
                    }
                }
        }
    }

    private suspend fun notifyDataChanged() {
        withContext(Dispatchers.Default) {
            listMutex.withLock {
                _logcatList.value = logList.filter {
                    it.logInfo.hasString(cachedQuery)
                }
            }
        }
    }

    private fun cancelJob() {
        job?.cancel()
        clearLogs()
        _loadingData.value = true
    }

    fun saveRecentSearchQuery(query: String) {
        viewModelScope.launch {
            appRepository.saveRecentSearchQuery(query)
        }
    }

    fun toggleLogcatFlowState() {
        _logcatFlowSuspended.value = !_logcatFlowSuspended.value
        if (_logcatFlowSuspended.value) {
            job?.cancel()
        } else {
            startJob()
        }
    }

    fun clearRecentSearch(query: String) {
        viewModelScope.launch {
            appRepository.clearRecentSearchQuery(query)
        }
    }

    fun clearAllRecentSearches() {
        viewModelScope.launch {
            appRepository.clearAllRecentSearchQueries()
        }
    }

    fun getSavedLogsDirectoryUri(): Result<Uri> {
        return logcatRepository.getSavedLogsDirectoryUri()
    }
}