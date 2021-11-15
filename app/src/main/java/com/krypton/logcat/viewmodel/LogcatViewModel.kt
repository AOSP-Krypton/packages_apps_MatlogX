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
    private var job: Job? = null

    init {
        // Start reading on init
        startReadingLogcat()
    }

    /**
     * Get a [LiveData] for view to observe.
     *
     * @return [LiveData] of a [List] of [LogInfo].
     */
    fun getLogcatLiveData(): LiveData<List<LogInfo>> = logcatLiveData

    /**
     * Subscribe to the [LogInfo] flow from [LogcatRepository].
     * TODO Make this function public when floating action button code is implemented
     */
    private fun startReadingLogcat() {
        cancellationSignal = CancellationSignal()
        job = viewModelScope.launch {
            logcatRepository.getLogcatStream(cancellationSignal).collect {
                logList.add(it)
                logcatLiveData.value = logList.toList()
            }
        }
    }

    /**
     * Terminate any existing subscriptions that are
     * subscribed to the [LogInfo] flow from [LogcatRepository].
     * TODO Make this function public when floating action button code is implemented
     */
    private fun stopReadingLogcat() {
        cancellationSignal.cancel()
        job?.cancel()
    }

    override fun onCleared() {
        stopReadingLogcat()
        super.onCleared()
    }
}