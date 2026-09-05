/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.theme.model.HorizontalAlignment

@Composable
fun ReaderProgressBar(
    progress: String,
    chapterTitle: String = "",
    progressBarPadding: Dp,
    progressBarAlignment: HorizontalAlignment,
    progressBarFontSize: TextUnit,
    fontColor: Color,
    sidePadding: Dp
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = sidePadding,
                vertical = progressBarPadding
            )
    ) {
        DisableSelection {
            if (chapterTitle.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StyledText(
                        text = chapterTitle,
                        style = LocalTextStyle.current.copy(
                            color = fontColor.copy(alpha = 0.65f),
                            fontSize = progressBarFontSize
                        ),
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(end = 12.dp)
                    )

                    StyledText(
                        text = progress,
                        style = LocalTextStyle.current.copy(
                            color = fontColor.copy(alpha = 0.85f),
                            fontSize = progressBarFontSize
                        ),
                        maxLines = 1
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = progressBarAlignment.alignment
                ) {
                    StyledText(
                        text = progress,
                        style = LocalTextStyle.current.copy(
                            color = fontColor.copy(alpha = 0.85f),
                            fontSize = progressBarFontSize
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}