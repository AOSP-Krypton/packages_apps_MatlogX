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

package com.krypton.logcat.viewmodel

import android.os.CancellationSignal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.krypton.logcat.data.LogInfo
import com.krypton.logcat.repo.LogcatRepository

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

import javax.inject.Inject

@HiltViewModel
class LogcatViewModel @Inject constructor(
    private val logcatRepository: LogcatRepository
) : ViewModel() {

    private val logcatLiveData = MutableLiveData<List<LogInfo>>()
    private val logList = mutableListOf<LogInfo>()
    private var cancellationSignal = CancellationSignal()
    private var job: Job

    // Whether livedata should be updated when new log info is collected from repository
    private var logcatUpdatePaused = false
    private val pauseButtonLiveData = MutableLiveData(logcatUpdatePaused)

    // Whether we should scroll to the bottom automatically
    // when new log info is added to the list
    var autoScroll = true

    init {
        // Start reading on init
        job = viewModelScope.launch {
            val size = logcatRepository.getCurrentLogcatSize()
            logcatRepository.getLogcatStream(cancellationSignal).collect {
                logList.add(it)
                if (!logcatUpdatePaused && logList.size > size) logcatLiveData.value = logList.toList()
            }
        }
    }

    /**
     * Get a [LiveData] for view to observe.
     *
     * @return [LiveData] of a [List] of [LogInfo].
     */
    fun getLogcatLiveData(): LiveData<List<LogInfo>> = logcatLiveData

    /**
     * Get the state of pause button as a [LiveData].
     * State is false if logcat stream is paused and vice versa
     *
     * @return [LiveData] of state variable.
     */
    fun getPauseButtonState(): LiveData<Boolean> = pauseButtonLiveData

    /**
     * Toggle the state of the pause button.
     */
    fun togglePauseButtonState() {
        logcatUpdatePaused = !logcatUpdatePaused
        pauseButtonLiveData.value = logcatUpdatePaused
    }

    override fun onCleared() {
        cancellationSignal.cancel()
        job.cancel()
        super.onCleared()
    }
}