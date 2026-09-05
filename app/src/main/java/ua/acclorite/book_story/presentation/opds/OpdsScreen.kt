/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.opds

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.parcelize.Parcelize
import ua.acclorite.book_story.presentation.history.HistoryScreen
import ua.acclorite.book_story.presentation.navigator.Screen
import ua.acclorite.book_story.presentation.reader.ReaderScreen
import ua.acclorite.book_story.ui.common.helpers.showToast
import ua.acclorite.book_story.ui.navigator.LocalNavigator
import ua.acclorite.book_story.ui.opds.OpdsContent

@Parcelize
object OpdsScreen : Screen, Parcelable {

    @Composable
    override fun Content() {
        val viewModel: OpdsModel = hiltViewModel()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.current
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    is OpdsEffect.OnNavigateToReader -> {
                        HistoryScreen.insertHistoryChannel.trySend(effect.bookId)
                        navigator.push(ReaderScreen(effect.bookId))
                    }
                    is OpdsEffect.ShowToast -> {
                        effect.message.showToast(context)
                    }
                }
            }
        }

        BackHandler(enabled = state.breadcrumbs.isNotEmpty() || state.showSearch) {
            if (state.showSearch) {
                viewModel.onEvent(OpdsEvent.OnToggleSearch(false))
            } else {
                viewModel.onEvent(OpdsEvent.OnNavigateBack)
            }
        }

        OpdsContent(
            state = state,
            onEvent = viewModel::onEvent
        )
    }
}
