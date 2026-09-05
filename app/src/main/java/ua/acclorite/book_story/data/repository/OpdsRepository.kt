/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.data.model.opds.OpdsFeedResult
import ua.acclorite.book_story.data.service.OpdsService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpdsRepository @Inject constructor(
    private val opdsService: OpdsService
) {
    suspend fun getDefaultLibrary(): Result<OpdsFeedResult> = withContext(Dispatchers.IO) {
        opdsService.fetchOpdsFeed()
    }

    suspend fun getLibrary(url: String): Result<OpdsFeedResult> = withContext(Dispatchers.IO) {
        opdsService.fetchOpdsFeed(url)
    }
}
