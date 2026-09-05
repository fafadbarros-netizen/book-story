/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case

import ua.acclorite.book_story.data.model.opds.OpdsFeedResult
import ua.acclorite.book_story.data.repository.OpdsRepository
import javax.inject.Inject

class GetOpdsLibraryUseCase @Inject constructor(
    private val opdsRepository: OpdsRepository
) {
    suspend operator fun invoke(url: String? = null): Result<OpdsFeedResult> {
        return if (url.isNullOrBlank()) {
            opdsRepository.getDefaultLibrary()
        } else {
            opdsRepository.getLibrary(url)
        }
    }
}
