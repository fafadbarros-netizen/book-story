/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.ui.theme.model.HorizontalAlignment

@Composable
fun LazyItemScope.ReaderLayoutTextImage(
    entry: ReaderText.Image,
    sidePadding: Dp,
    imagesCornersRoundness: Dp,
    imagesAlignment: HorizontalAlignment,
    imagesWidth: Float,
    imagesColorEffects: ColorFilter?
) {
    val horizontalPadding = if (imagesWidth >= 0.8f) (sidePadding * 0.2f).coerceAtLeast(0.dp) else sidePadding
    val scaleWidth = if (imagesWidth >= 0.8f) 1f else imagesWidth

    Box(
        modifier = Modifier
            .animateItem(
                fadeInSpec = null,
                fadeOutSpec = null
            )
            .padding(horizontal = horizontalPadding, vertical = 4.dp)
            .fillMaxWidth(),
        contentAlignment = imagesAlignment.alignment
    ) {
        Image(
            modifier = Modifier
                .clip(RoundedCornerShape(imagesCornersRoundness))
                .fillMaxWidth(scaleWidth),
            bitmap = entry.imageBitmap,
            contentDescription = null,
            colorFilter = imagesColorEffects,
            contentScale = ContentScale.FillWidth
        )
    }
}