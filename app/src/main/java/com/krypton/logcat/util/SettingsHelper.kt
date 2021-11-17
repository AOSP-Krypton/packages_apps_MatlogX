package com.krypton.logcat.util

import com.krypton.logcat.reader.LogcatReader

import javax.inject.Inject

/**
 * Class that manages user preferences and
 * exposes helper APIs for clients to use.
 */
class SettingsHelper @Inject constructor() {

    /**
     * Get user selected arguments to pass as
     * an argument to [LogcatReader.read].
     * TODO fetch it from preferences when UI is set up for selecting args.
     *
     * @return map of options to it's values
     */
    fun getArgsFromUserSettings(): Map<String, String?> {
        return mapOf(
            LogcatReader.Args.OPTION_BUFFER to LogcatReader.Args.BUFFER_ALL,
        )
    }
}