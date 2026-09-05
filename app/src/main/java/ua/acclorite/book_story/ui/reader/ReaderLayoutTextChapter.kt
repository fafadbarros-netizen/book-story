/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.domain.model.reader.ReaderText.Chapter
import ua.acclorite.book_story.presentation.reader.model.ReaderTextAlignment
import ua.acclorite.book_story.ui.common.components.common.StyledText

@Composable
fun LazyItemScope.ReaderLayoutTextChapter(
    chapter: Chapter,
    chapterTitleAlignment: ReaderTextAlignment,
    fontColor: Color,
    sidePadding: Dp,
    highlightedReading: Boolean,
    highlightedReadingThickness: FontWeight
) {
    val cleanTitle = chapter.title.trim()
    val isGenericTitle = cleanTitle.isBlank() ||
        cleanTitle.equals("Image", ignoreCase = true) ||
        cleanTitle.equals("Img", ignoreCase = true) ||
        cleanTitle.equals("Cover", ignoreCase = true) ||
        cleanTitle.equals("Capa", ignoreCase = true) ||
        cleanTitle.equals("Folha de rosto", ignoreCase = true) ||
        cleanTitle.equals("Title Page", ignoreCase = true) ||
        cleanTitle.equals("Landmarks", ignoreCase = true) ||
        cleanTitle.equals("Sumário", ignoreCase = true) ||
        cleanTitle.equals("Sumario", ignoreCase = true) ||
        cleanTitle.equals("Table of Contents", ignoreCase = true) ||
        cleanTitle.equals("Índice", ignoreCase = true) ||
        cleanTitle.equals("Indice", ignoreCase = true)

    if (isGenericTitle) {
        return
    }

    Column(
        Modifier
            .animateItem(
                fadeInSpec = null,
                fadeOutSpec = null
            )
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(22.dp))

        StyledText(
            text = buildAnnotatedString { append(chapter.title) },
            modifier = Modifier
                .padding(horizontal = sidePadding)
                .fillMaxWidth(),
            style = (if (!chapter.nested) MaterialTheme.typography.headlineMedium
            else MaterialTheme.typography.headlineSmall)
                .copy(
                    color = fontColor,
                    textAlign = chapterTitleAlignment.textAlignment
                ),
            highlightText = highlightedReading,
            highlightThickness = highlightedReadingThickness
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = fontColor.copy(0.4f))
        Spacer(modifier = Modifier.height(16.dp))
    }
}