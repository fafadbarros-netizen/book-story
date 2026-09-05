/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.opds

sealed class OpdsEffect {
    data class OnNavigateToReader(val bookId: Int) : OpdsEffect()
    data class ShowToast(val message: String) : OpdsEffect()
}
