/*
 * Copyright (C) 2022 AOSP-Krypton Project
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

package com.krypton.matlogx.data

import com.krypton.matlogx.data.room.AppDatabase
import com.krypton.matlogx.data.room.RecentSearchEntity

import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Singleton
class AppRepository @Inject constructor(appDatabase: AppDatabase) {

    private val recentSuggestionsDao = appDatabase.recentSuggestionsDao()

    val searchSuggestions: Flow<List<String>>
        get() = recentSuggestionsDao.getRecentSearchQueriesSorted()

    suspend fun saveRecentSearchQuery(query: String) {
        withContext(Dispatchers.IO) {
            recentSuggestionsDao.insertSearchQuery(
                RecentSearchEntity(
                    query,
                    System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun clearRecentSearchQuery(query: String) {
        withContext(Dispatchers.IO) {
            recentSuggestionsDao.clearRecentSearchQuery(query)
        }
    }

    suspend fun clearAllRecentSearchQueries() {
        withContext(Dispatchers.IO) {
            recentSuggestionsDao.clearAllRecentSearchQueries()
        }
    }
}