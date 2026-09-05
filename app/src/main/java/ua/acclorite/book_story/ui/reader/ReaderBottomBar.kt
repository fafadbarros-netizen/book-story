/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.domain.model.reader.ReaderText.Chapter
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.presentation.reader.model.Checkpoint
import ua.acclorite.book_story.ui.common.components.common.IconButton
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.common.helpers.noRippleClickable
import ua.acclorite.book_story.ui.theme.readerBarsColor
import kotlin.math.roundToInt

@Composable
fun ReaderBottomBar(
    book: Book,
    text: List<ReaderText>,
    currentChapter: Chapter?,
    listState: LazyListState,
    lockMenu: Boolean,
    checkpoints: List<Checkpoint>,
    bottomBarPadding: Dp,
    restoreCheckpoint: (ReaderEvent.OnRestoreCheckpoint) -> Unit,
    scroll: (ReaderEvent.OnScroll) -> Unit,
    changeProgress: (ReaderEvent.OnChangeProgress) -> Unit,
    scrollToChapter: (ReaderEvent.OnScrollToChapter) -> Unit
) {
    val currentIndex by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex
        }
    }
    val checkpointsProgress = rememberCheckpointsProgress(
        checkpoints = checkpoints,
        text = text,
        currentIndex = currentIndex
    )
    val currentCheckpoint = rememberCurrentCheckpoint(
        checkpoints = checkpoints,
        currentIndex = currentIndex
    )

    val chapters = remember(text) { text.filterIsInstance<Chapter>() }
    val currentChapterIndex = remember(chapters, currentChapter) {
        if (currentChapter != null) chapters.indexOf(currentChapter) else -1
    }
    val previousChapter = remember(chapters, currentChapterIndex) {
        if (currentChapterIndex > 0) chapters[currentChapterIndex - 1]
        else null
    }
    val nextChapter = remember(chapters, currentChapterIndex) {
        if (currentChapterIndex != -1 && currentChapterIndex < chapters.lastIndex) {
            chapters[currentChapterIndex + 1]
        } else if (currentChapterIndex == -1 && chapters.isNotEmpty()) {
            chapters.first()
        } else {
            null
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.readerBarsColor)
            .noRippleClickable(onClick = {})
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 8.dp + bottomBarPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Chapter Title and Progress indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (currentChapter != null) {
                StyledText(
                    text = currentChapter.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .basicMarquee()
                )
            }

            val chapterCountText = if (currentChapterIndex != -1 && chapters.isNotEmpty()) {
                "Capítulo ${currentChapterIndex + 1} de ${chapters.size} • "
            } else ""
            val progressPercentage = "${(book.progress * 100).roundToInt()}%"

            StyledText(
                text = "$chapterCountText$progressPercentage lido",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // Slider Row with Previous & Next Chapter Skip buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                icon = Icons.Default.SkipPrevious,
                contentDescription = R.string.previous_chapter,
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primary,
                enabled = previousChapter != null && !lockMenu,
                disableOnClick = false
            ) {
                previousChapter?.let {
                    scrollToChapter(ReaderEvent.OnScrollToChapter(it))
                }
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                ReaderBottomBarSlider(
                    book = book,
                    lockMenu = lockMenu,
                    listState = listState,
                    scroll = scroll,
                    changeProgress = changeProgress
                )
                ReaderBottomBarCheckpointsIndicator(
                    checkpointsProgress = checkpointsProgress
                )
            }

            IconButton(
                icon = Icons.Default.SkipNext,
                contentDescription = R.string.next_chapter,
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primary,
                enabled = nextChapter != null && !lockMenu,
                disableOnClick = false
            ) {
                nextChapter?.let {
                    scrollToChapter(ReaderEvent.OnScrollToChapter(it))
                }
            }
        }

        // Quick Navigation Step Row (Voltar, Retornar ao Ponto, Avançar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = {
                    val step = 0.02f
                    val newProgress = (book.progress - step).coerceAtLeast(0f)
                    val total = listState.layoutInfo.totalItemsCount
                    val newIndex = if (total > 0) (total * newProgress).toInt() else 0
                    scroll(ReaderEvent.OnScroll(newProgress))
                    changeProgress(
                        ReaderEvent.OnChangeProgress(
                            progress = newProgress,
                            firstVisibleItemIndex = newIndex,
                            firstVisibleItemOffset = 0
                        )
                    )
                },
                enabled = !lockMenu && book.progress > 0.005f,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                StyledText(
                    stringResource(R.string.step_backward),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            if (currentCheckpoint != null) {
                FilledTonalButton(
                    onClick = {
                        restoreCheckpoint(ReaderEvent.OnRestoreCheckpoint(currentCheckpoint))
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    StyledText(
                        stringResource(R.string.restore_reading_position),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            FilledTonalButton(
                onClick = {
                    val step = 0.02f
                    val newProgress = (book.progress + step).coerceAtMost(1f)
                    val total = listState.layoutInfo.totalItemsCount
                    val newIndex = if (total > 0) (total * newProgress).toInt() else 0
                    scroll(ReaderEvent.OnScroll(newProgress))
                    changeProgress(
                        ReaderEvent.OnChangeProgress(
                            progress = newProgress,
                            firstVisibleItemIndex = newIndex,
                            firstVisibleItemOffset = 0
                        )
                    )
                },
                enabled = !lockMenu && book.progress < 0.995f,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                StyledText(
                    stringResource(R.string.step_forward),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun rememberCheckpointsProgress(
    checkpoints: List<Checkpoint>,
    text: List<ReaderText>,
    currentIndex: Int
): List<Float> {
    return remember(checkpoints, text, currentIndex) {
        if (text.isEmpty()) return@remember emptyList()

        val progressScale = 0.987f
        checkpoints.mapNotNull { checkpoint ->
            if (checkpoint.index == currentIndex) return@mapNotNull null
            (checkpoint.index / text.lastIndex.toFloat()) * progressScale
        }
    }
}

@Composable
private fun rememberCurrentCheckpoint(
    checkpoints: List<Checkpoint>,
    currentIndex: Int
): Checkpoint? {
    return remember(checkpoints, currentIndex) {
        checkpoints.lastOrNull { checkpoint ->
            when {
                checkpoint.index > currentIndex -> true
                checkpoint.index < currentIndex -> true
                else -> false
            }
        }
    }
}

@Composable
private fun ReaderBottomBarSlider(
    book: Book,
    lockMenu: Boolean,
    listState: LazyListState,
    scroll: (ReaderEvent.OnScroll) -> Unit,
    changeProgress: (ReaderEvent.OnChangeProgress) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging = interactionSource.collectIsDraggedAsState()
    val animatedProgress = animateFloatAsState(book.progress)
    val progress = remember(isDragging.value, book.progress, animatedProgress.value) {
        if (isDragging.value) book.progress
        else animatedProgress.value
    }

    Slider(
        value = progress,
        enabled = !lockMenu,
        onValueChange = {
            if (listState.layoutInfo.totalItemsCount > 0) {
                scroll(ReaderEvent.OnScroll(it))
                changeProgress(
                    ReaderEvent.OnChangeProgress(
                        progress = it,
                        firstVisibleItemIndex = (listState.layoutInfo.totalItemsCount * it).toInt(),
                        firstVisibleItemOffset = 0
                    )
                )
            }
        },
        interactionSource = interactionSource,
        colors = SliderDefaults.colors(
            inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(0.15f),
            disabledActiveTrackColor = MaterialTheme.colorScheme.primary,
            disabledThumbColor = MaterialTheme.colorScheme.primary,
            disabledInactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(0.15f),
        )
    )
}

@Composable
private fun ReaderBottomBarCheckpointsIndicator(checkpointsProgress: List<Float>) {
    checkpointsProgress.forEach { checkpoint ->
        Row {
            Spacer(modifier = Modifier.fillMaxWidth(checkpoint))
            Box(
                Modifier
                    .width(4.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(0.5.dp))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(0.7f))
            )
        }
    }
}