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

package com.krypton.matlogx.services

import android.service.quicksettings.TileService
import android.widget.Toast

import androidx.appcompat.app.AlertDialog

import com.krypton.matlogx.R
import com.krypton.matlogx.repo.LogcatRepository
import com.krypton.matlogx.repo.SettingsRepository

import dagger.hilt.android.AndroidEntryPoint

import javax.inject.Inject

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LogTileService : TileService() {

    @Inject
    lateinit var logcatRepository: LogcatRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private lateinit var serviceScope: CoroutineScope

    private var toast: Toast? = null

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(Dispatchers.Main)
    }

    override fun onStartListening() {
        qsTile.apply {
            subtitle = getString(R.string.save_logs)
            updateTile()
        }
    }

    override fun onClick() {
        serviceScope.launch { showDialog() }
    }

    private suspend fun showDialog() {
        var includeDeviceInfo = settingsRepository.getIncludeDeviceInfo().first()
        val dialog = AlertDialog.Builder(this, R.style.Theme_MatLogX_AlertDialog)
            .setTitle(R.string.save_zip)
            .setCancelable(true)
            .setMultiChoiceItems(
                R.array.save_zip_items,
                booleanArrayOf(includeDeviceInfo)
            ) { _, which, checked ->
                includeDeviceInfo = which == 0 && checked
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                serviceScope.launch { saveLog(includeDeviceInfo) }
                dialog.dismiss()
            }
            .create()
        showDialog(dialog)
    }

    private suspend fun saveLog(includeDeviceInfo: Boolean) {
        val result = logcatRepository.saveLogAsZip(
            null, /* tags aren't supported yet */
            null,
            settingsRepository.getLogLevel().first(),
            includeDeviceInfo,
        )
        if (result.isSuccess) {
            toast(getString(R.string.log_saved_successfully))
        } else {
            toast(getString(R.string.failed_to_save_log, result.exceptionOrNull()))
        }
    }

    private fun toast(text: String) {
        toast?.cancel()
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT).also {
            it.show()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}