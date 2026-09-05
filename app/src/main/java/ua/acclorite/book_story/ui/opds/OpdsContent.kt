/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.opds

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.presentation.opds.OpdsEvent
import ua.acclorite.book_story.presentation.opds.OpdsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun OpdsContent(
    state: OpdsState,
    onEvent: (OpdsEvent) -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoading,
        onRefresh = { onEvent(OpdsEvent.OnRefresh) }
    )
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.showSearch) {
        if (state.showSearch) focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            if (state.showSearch) {
                TopAppBar(
                    title = {
                        TextField(
                            value = state.searchQuery,
                            onValueChange = { onEvent(OpdsEvent.OnSearchQueryChange(it)) },
                            placeholder = { Text("Pesquisar livros ou autores...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { onEvent(OpdsEvent.OnToggleSearch(false)) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Fechar pesquisa"
                            )
                        }
                    },
                    actions = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onEvent(OpdsEvent.OnSearchQueryChange("")) }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpar")
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = state.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        if (state.breadcrumbs.isNotEmpty()) {
                            IconButton(onClick = { onEvent(OpdsEvent.OnNavigateBack) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Voltar"
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { onEvent(OpdsEvent.OnToggleSearch(true)) }) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Pesquisar")
                        }
                        IconButton(onClick = { onEvent(OpdsEvent.OnRefresh) }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Atualizar")
                        }
                        IconButton(onClick = { onEvent(OpdsEvent.OnShowUrlConfigDialog(true)) }) {
                            Icon(imageVector = Icons.Default.Dns, contentDescription = "Configurar Catálogo")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            when {
                state.error != null && state.entries.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = state.error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(onClick = { onEvent(OpdsEvent.OnRefresh) }) {
                                Text("Tentar novamente")
                            }
                        }
                    }
                }

                state.configuredOpdsUrl.isBlank() && !state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Nenhum catálogo configurado",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Adicione a URL do seu catálogo OPDS para sincronizar seus livros e coleções.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { onEvent(OpdsEvent.OnShowUrlConfigDialog(true)) }) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Configurar Catálogo Cloud")
                            }
                        }
                    }
                }

                state.filteredEntries.isEmpty() && !state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.searchQuery.isNotEmpty())
                                "Nenhum resultado para \"${state.searchQuery}\""
                            else "Nenhum item nesta seção",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    // Separate folders from books for mixed layout
                    val folders = state.filteredEntries.filter { it.isFolder }
                    val books = state.filteredEntries.filter { !it.isFolder }

                    // Responsive grid: 3 columns in portrait (3x3), 6 columns in landscape (3x6)
                    val configuration = LocalConfiguration.current
                    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    val gridColumns = if (isLandscape) 6 else 3

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Folders: full width rows
                        if (folders.isNotEmpty()) {
                            items(
                                count = folders.size,
                                key = { index -> "folder_${folders[index].id}_${folders[index].title}_$index" },
                                span = { GridItemSpan(gridColumns) }
                            ) { index ->
                                val entry = folders[index]
                                val isBatchDownloading = state.batchDownloadingSeriesUrls.contains(
                                    entry.subsectionUrl.orEmpty()
                                )
                                OpdsFolderItem(
                                    entry = entry,
                                    isBatchDownloading = isBatchDownloading,
                                    onOpenFolder = { onEvent(OpdsEvent.OnOpenFolder(entry)) },
                                    onBatchDownload = if (!entry.subsectionUrl.isNullOrBlank()) {
                                        { onEvent(OpdsEvent.OnBatchDownloadSeries(entry)) }
                                    } else null
                                )
                            }
                        }

                        // Books: 3-column grid
                        items(
                            count = books.size,
                            key = { index -> "book_${books[index].id}_${books[index].title}_$index" }
                        ) { index ->
                            val entry = books[index]
                            val isDownloading = state.downloadingBookIds.contains(
                                entry.id.ifBlank { entry.title }
                            )
                            val isInLibrary = state.localLibraryBookTitles.contains(
                                entry.title.lowercase().trim()
                            )
                            OpdsBookGridCell(
                                entry = entry,
                                isInLibrary = isInLibrary,
                                isDownloading = isDownloading,
                                onOpenPreview = { onEvent(OpdsEvent.OnSelectBookForPreview(entry)) }
                            )
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = state.isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Synopsis / Book Detail Preview BottomSheet
            state.selectedBookForPreview?.let { previewEntry ->
                val isInLibrary = state.localLibraryBookTitles.contains(
                    previewEntry.title.lowercase().trim()
                )
                val isDownloading = state.downloadingBookIds.contains(
                    previewEntry.id.ifBlank { previewEntry.title }
                )
                OpdsBookPreviewBottomSheet(
                    entry = previewEntry,
                    isInLibrary = isInLibrary,
                    isDownloading = isDownloading,
                    onDismiss = { onEvent(OpdsEvent.OnSelectBookForPreview(null)) },
                    onDownloadAndRead = { onEvent(OpdsEvent.OnDownloadOrReadBook(previewEntry)) },
                    onDownloadOnly = { onEvent(OpdsEvent.OnDownloadBookOnly(previewEntry)) },
                    onReadNow = { onEvent(OpdsEvent.OnDownloadOrReadBook(previewEntry)) },
                    onFilterByTag = { tag -> onEvent(OpdsEvent.OnFilterByTag(tag)) }
                )
            }

            // OPDS URL Configuration Dialog
            if (state.showUrlConfigDialog) {
                OpdsUrlConfigDialog(
                    currentUrl = state.configuredOpdsUrl,
                    onDismiss = { onEvent(OpdsEvent.OnShowUrlConfigDialog(false)) },
                    onSaveUrl = { onEvent(OpdsEvent.OnSaveOpdsUrl(it)) }
                )
            }
        }
    }
}
