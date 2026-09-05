/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.model.opds

data class OpdsEntry(
    val id: String,
    val title: String,
    val author: String?,
    val summary: String?,           // clean synopsis (header stripped)
    val coverUrl: String?,          // full-resolution cover (for detail view)
    val thumbnailUrl: String?,      // thumbnail for grid (faster load)
    val downloadUrl: String?,
    val subsectionUrl: String? = null,
    val type: String?,
    val isFolder: Boolean = !subsectionUrl.isNullOrBlank(),
    // Rich metadata parsed from server
    val rating: String? = null,         // e.g. "4.8/5.0"
    val ratingCount: String? = null,    // e.g. "2.415 avaliações"
    val saga: String? = null,           // e.g. "Três Coroas Negras #1"
    val publisher: String? = null,      // from <dc:publisher> or summary header
    val releaseDate: String? = null,    // e.g. "15 de setembro de 2017"
    val genres: List<String> = emptyList() // from <category> tags
)
