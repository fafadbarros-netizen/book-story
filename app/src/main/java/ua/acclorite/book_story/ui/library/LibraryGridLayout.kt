/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.library

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.presentation.library.model.SelectableBook
import ua.acclorite.book_story.ui.common.components.common.LazyVerticalGridWithScrollbar
import ua.acclorite.book_story.ui.common.data.ScrollbarData

@Composable
fun LibraryGridLayout(
    books: List<SelectableBook>,
    gridSize: Int,
    autoGridSize: Boolean,
    itemContent: @Composable (book: SelectableBook) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val defaultColumns = if (isLandscape) 6 else 3

    val columns = if (autoGridSize) {
        GridCells.Fixed(defaultColumns)
    } else {
        val baseSize = gridSize.coerceAtLeast(1)
        val adjustedSize = if (isLandscape) {
            if (baseSize <= 3) baseSize * 2 else (baseSize + 2).coerceAtMost(8)
        } else baseSize
        GridCells.Fixed(adjustedSize)
    }

    LazyVerticalGridWithScrollbar(
        columns = columns,
        modifier = Modifier.fillMaxSize(),
        scrollbarSettings = ScrollbarData.primaryScrollbar,
        contentPadding = PaddingValues(8.dp)
    ) {
        items(
            books,
            key = { it.data.id }
        ) { book ->
            Box(modifier = Modifier.animateItem()) {
                itemContent(book)
            }
        }
    }
}