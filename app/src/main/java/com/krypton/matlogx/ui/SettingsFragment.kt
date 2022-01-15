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

import androidx.preference.EditTextPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceFragmentCompat

import com.krypton.matlogx.R
import com.krypton.matlogx.util.SettingsHelper

import dagger.hilt.android.AndroidEntryPoint

import javax.inject.Inject

@AndroidEntryPoint(PreferenceFragmentCompat::class)
class SettingsFragment : Hilt_SettingsFragment() {

    @Inject
    lateinit var settingsHelper: SettingsHelper

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey)
        setupBufferPreference()
        setupDisplayLimitPreference()
    }

    private fun setupBufferPreference() {
        findPreference<MultiSelectListPreference>(BUFFER_KEY)?.also {
            val buffers = settingsHelper.getLogcatBuffers()
            it.summary = buffers.joinToString(", ")
            it.values = buffers
            it.setOnPreferenceChangeListener { _, newValue ->
                @Suppress("UNCHECKED_CAST")
                val newBuffers = newValue as Set<String>
                settingsHelper.setLogcatBuffers(newBuffers)
                it.summary = newBuffers.joinToString(", ")
                true
            }
        }
    }

    private fun setupDisplayLimitPreference() {
        findPreference<EditTextPreference>(LOG_DISPLAY_LIMIT_KEY)?.also {
            it.summary = getString(
                R.string.log_display_limit_summary_placeholder,
                settingsHelper.getLogSizeLimit()
            )
            it.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            it.setOnPreferenceChangeListener { preference, newValue ->
                if (newValue is String) {
                    settingsHelper.setLogSizeLimit(newValue.toInt())
                    preference.summary = getString(R.string.log_display_limit_summary_placeholder, newValue.toInt())
                }
                true
            }
        }
    }

    companion object {
        private const val BUFFER_KEY = "buffer"
        private const val LOG_DISPLAY_LIMIT_KEY = "log_display_limit"
    }
}