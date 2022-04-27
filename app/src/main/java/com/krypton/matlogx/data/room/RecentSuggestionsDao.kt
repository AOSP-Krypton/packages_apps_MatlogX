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

package com.krypton.matlogx.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSuggestionsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSearchQuery(recentSearchEntity: RecentSearchEntity)

    @Query("SELECT `query` FROM recent_search_table ORDER BY timestamp DESC")
    fun getRecentSearchQueriesSorted(): Flow<List<String>>

    @Query("DELETE FROM recent_search_table WHERE `query` is :query")
    fun clearRecentSearchQuery(query: String)

    @Query("DELETE FROM recent_search_table")
    fun clearAllRecentSearchQueries()
}