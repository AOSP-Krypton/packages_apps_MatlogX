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

import android.os.Bundle
import android.text.InputType

import androidx.fragment.app.activityViewModels
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceFragmentCompat

import com.krypton.matlogx.R
import com.krypton.matlogx.viewmodel.SettingsViewModel

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    private val settingsViewModel: SettingsViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey)
        setupExpandLogsPreference()
        setupBufferPreference()
        setupDisplayLimitPreference()
    }

    private fun setupExpandLogsPreference() {
        val preference = findPreference<CheckBoxPreference>(EXPAND_LOGS_KEY)?.also {
            it.setOnPreferenceChangeListener { _, newValue ->
                settingsViewModel.setExpandedByDefault(newValue as Boolean)
                true
            }
        }
        settingsViewModel.expandedByDefault.observe(this) {
            preference?.isChecked = it
        }
    }

    private fun setupBufferPreference() {
        val preference = findPreference<MultiSelectListPreference>(BUFFER_KEY)?.also {
            it.setOnPreferenceChangeListener { preference, newValue ->
                @Suppress("UNCHECKED_CAST")
                val newBuffers = (newValue as Set<String>).joinToString(",")
                settingsViewModel.setLogcatBuffers(newBuffers)
                preference.summary = newBuffers
                true
            }
        }
        settingsViewModel.logcatBuffers.observe(this) {
            preference?.apply {
                summary = it
                values = it.split(",").toSet()
            }
        }
    }

    private fun setupDisplayLimitPreference() {
        val preference = findPreference<EditTextPreference>(LOG_DISPLAY_LIMIT_KEY)?.also {
            it.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            it.setOnPreferenceChangeListener { preference, newValue ->
                if (newValue is String) {
                    val limit = newValue.toInt()
                    settingsViewModel.setLogcatSizeLimit(limit)
                    preference.summary =
                        if (limit == 0) getString(R.string.log_display_limit_summary_no_limit) else getString(
                            R.string.log_display_limit_summary_placeholder,
                            newValue.toInt()
                        )
                }
                true
            }
        }
        settingsViewModel.logcatSizeLimit.observe(this) {
            preference?.apply {
                summary = if (it == 0) getString(R.string.log_display_limit_summary_no_limit)
                    else getString(R.string.log_display_limit_summary_placeholder, it)
                text = it.toString()
            }
        }
    }

    companion object {
        private const val EXPAND_LOGS_KEY = "expand_logs"
        private const val BUFFER_KEY = "buffer"
        private const val LOG_DISPLAY_LIMIT_KEY = "log_display_limit"
    }
}