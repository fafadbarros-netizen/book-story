/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.opds

import ua.acclorite.book_story.data.model.opds.OpdsEntry
import java.text.Normalizer

data class OpdsBreadcrumb(
    val title: String,
    val url: String
)

data class OpdsState(
    val title: String = "Minha Biblioteca Cloud",
    val currentUrl: String = "",
    val breadcrumbs: List<OpdsBreadcrumb> = emptyList(),
    val entries: List<OpdsEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showSearch: Boolean = false,
    val searchQuery: String = "",
    val downloadingBookIds: Set<String> = emptySet(),
    val selectedBookForPreview: OpdsEntry? = null,
    val localLibraryBookTitles: Set<String> = emptySet(),
    val batchDownloadingSeriesUrls: Set<String> = emptySet(),
    val configuredOpdsUrl: String = "",
    val showUrlConfigDialog: Boolean = false
) {
    val filteredEntries: List<OpdsEntry>
        get() = if (searchQuery.isBlank()) {
            entries
        } else {
            val query = normalize(searchQuery.trim())
            entries.filter { entry ->
                normalize(entry.title).contains(query) ||
                    (entry.author?.let { normalize(it).contains(query) } == true) ||
                    (entry.summary?.let { normalize(it).contains(query) } == true) ||
                    (entry.saga?.let { normalize(it).contains(query) } == true) ||
                    entry.genres.any { normalize(it).contains(query) }
            }
        }

    private fun normalize(text: String): String {
        val nfd = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(nfd, "")
    }
}
