/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.opds

import ua.acclorite.book_story.data.model.opds.OpdsEntry

sealed class OpdsEvent {
    data object OnRefresh : OpdsEvent()
    data class OnOpenFolder(val entry: OpdsEntry) : OpdsEvent()
    data class OnSelectBookForPreview(val entry: OpdsEntry?) : OpdsEvent()
    data class OnDownloadOrReadBook(val entry: OpdsEntry) : OpdsEvent()
    data class OnDownloadBookOnly(val entry: OpdsEntry) : OpdsEvent()
    data class OnBatchDownloadSeries(val entry: OpdsEntry) : OpdsEvent()
    data object OnNavigateBack : OpdsEvent()
    data class OnSearchQueryChange(val query: String) : OpdsEvent()
    data class OnFilterByTag(val query: String) : OpdsEvent()
    data class OnToggleSearch(val show: Boolean) : OpdsEvent()
    data class OnShowUrlConfigDialog(val show: Boolean) : OpdsEvent()
    data class OnSaveOpdsUrl(val url: String) : OpdsEvent()
}
