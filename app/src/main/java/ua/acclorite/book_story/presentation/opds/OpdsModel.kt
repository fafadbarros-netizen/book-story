/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.opds

import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import ua.acclorite.book_story.R
import ua.acclorite.book_story.core.ui.UIText
import ua.acclorite.book_story.data.model.opds.OpdsEntry
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.repository.BookRepository
import ua.acclorite.book_story.domain.use_case.GetOpdsLibraryUseCase
import ua.acclorite.book_story.domain.use_case.book.AddBookUseCase
import ua.acclorite.book_story.data.settings.SettingsManager
import ua.acclorite.book_story.presentation.library.LibraryScreen
import java.io.File as JFile
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import javax.inject.Inject

@HiltViewModel
class OpdsModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getOpdsLibraryUseCase: GetOpdsLibraryUseCase,
    private val addBookUseCase: AddBookUseCase,
    private val bookRepository: BookRepository,
    private val settings: SettingsManager
) : ViewModel() {

    private val _state = MutableStateFlow(OpdsState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<OpdsEffect>()
    val effects = _effects.asSharedFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            settings.opdsCatalogUrl.flow.collectLatest { savedUrl ->
                val prevUrl = _state.value.configuredOpdsUrl
                _state.update { it.copy(configuredOpdsUrl = savedUrl) }
                if (savedUrl.isNotBlank() && (prevUrl != savedUrl || _state.value.entries.isEmpty())) {
                    loadLibrary(savedUrl)
                }
            }
        }
        refreshLocalLibraryTitles()
    }

    fun onEvent(event: OpdsEvent) {
        when (event) {
            is OpdsEvent.OnRefresh -> {
                val urlToLoad = _state.value.currentUrl.ifBlank {
                    _state.value.configuredOpdsUrl.ifBlank { null }
                }
                loadLibrary(urlToLoad)
                refreshLocalLibraryTitles()
            }

            is OpdsEvent.OnShowUrlConfigDialog -> {
                _state.update { it.copy(showUrlConfigDialog = event.show) }
            }

            is OpdsEvent.OnSaveOpdsUrl -> {
                val trimmedUrl = event.url.trim()
                settings.opdsCatalogUrl.update(trimmedUrl)
                _state.update {
                    it.copy(
                        configuredOpdsUrl = trimmedUrl,
                        showUrlConfigDialog = false,
                        breadcrumbs = emptyList()
                    )
                }
                loadLibrary(trimmedUrl.ifBlank { null })
            }

            is OpdsEvent.OnOpenFolder -> {
                val nextUrl = event.entry.subsectionUrl ?: return
                val currentBreadcrumb = OpdsBreadcrumb(
                    title = _state.value.title,
                    url = _state.value.currentUrl
                )
                _state.update {
                    it.copy(
                        breadcrumbs = it.breadcrumbs + currentBreadcrumb,
                        title = event.entry.title,
                        searchQuery = "",
                        showSearch = false
                    )
                }
                loadLibrary(nextUrl)
            }

            is OpdsEvent.OnNavigateBack -> {
                val breadcrumbs = _state.value.breadcrumbs
                if (breadcrumbs.isNotEmpty()) {
                    val lastBreadcrumb = breadcrumbs.last()
                    _state.update {
                        it.copy(
                            breadcrumbs = breadcrumbs.dropLast(1),
                            title = lastBreadcrumb.title,
                            searchQuery = "",
                            showSearch = false
                        )
                    }
                    loadLibrary(lastBreadcrumb.url.ifBlank { null })
                }
            }

            is OpdsEvent.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = event.query) }
            }

            is OpdsEvent.OnFilterByTag -> {
                _state.update {
                    it.copy(
                        searchQuery = event.query,
                        showSearch = true,
                        selectedBookForPreview = null
                    )
                }
            }

            is OpdsEvent.OnToggleSearch -> {
                _state.update {
                    it.copy(
                        showSearch = event.show,
                        searchQuery = if (!event.show) "" else it.searchQuery
                    )
                }
            }

            is OpdsEvent.OnSelectBookForPreview -> {
                _state.update { it.copy(selectedBookForPreview = event.entry) }
            }

            is OpdsEvent.OnDownloadOrReadBook -> {
                downloadBook(event.entry, openReader = true)
            }

            is OpdsEvent.OnDownloadBookOnly -> {
                downloadBook(event.entry, openReader = false)
            }

            is OpdsEvent.OnBatchDownloadSeries -> {
                batchDownloadSeries(event.entry)
            }
        }
    }

    private fun refreshLocalLibraryTitles() {
        viewModelScope.launch(Dispatchers.IO) {
            bookRepository.searchBooks("").onSuccess { books ->
                _state.update {
                    it.copy(
                        localLibraryBookTitles = books.map { b -> b.title.lowercase().trim() }.toSet()
                    )
                }
            }
        }
    }

    private fun loadLibrary(url: String? = null) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null, currentUrl = url.orEmpty()) }

            getOpdsLibraryUseCase(url).fold(
                onSuccess = { feedResult ->
                    _state.update {
                        it.copy(
                            title = feedResult.title.ifBlank { it.title },
                            entries = feedResult.entries,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { exception ->
                    _state.update {
                        it.copy(
                            entries = emptyList(),
                            isLoading = false,
                            error = exception.message ?: "Erro ao carregar catálogo OPDS"
                        )
                    }
                }
            )
        }
    }

    private fun downloadBook(entry: OpdsEntry, openReader: Boolean) {
        val downloadUrl = entry.downloadUrl ?: return
        val bookIdKey = entry.id.ifBlank { entry.title }

        if (_state.value.downloadingBookIds.contains(bookIdKey)) return

        viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(downloadingBookIds = it.downloadingBookIds + bookIdKey)
            }

            try {
                // Check if already in DB
                val existing = bookRepository.searchBooks(entry.title).getOrNull()
                    ?.firstOrNull { it.title.equals(entry.title, ignoreCase = true) }
                if (existing != null) {
                    _state.update {
                        it.copy(
                            downloadingBookIds = it.downloadingBookIds - bookIdKey,
                            selectedBookForPreview = null
                        )
                    }
                    if (openReader) {
                        _effects.emit(OpdsEffect.OnNavigateToReader(existing.id))
                    } else {
                        _effects.emit(OpdsEffect.ShowToast("Este livro já está na sua biblioteca!"))
                    }
                    return@launch
                }

                val booksDir = JFile(context.filesDir, "opds_books").apply { mkdirs() }
                val safeId = Math.abs((entry.id.ifBlank { entry.title }).hashCode())
                val sanitizedTitle = entry.title.replace(Regex("[^a-zA-Z0-9]"), "_").take(30)
                val destFile = JFile(booksDir, "${sanitizedTitle}_${safeId}.epub")

                if (!destFile.exists() || destFile.length() == 0L) {
                    val httpUrl = downloadUrl.toHttpUrlOrNull()
                    val request = if (httpUrl != null) {
                        Request.Builder().url(httpUrl).build()
                    } else {
                        Request.Builder().url(downloadUrl).build()
                    }

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        destFile.delete()
                        throw Exception("HTTP ${response.code}")
                    }

                    val body = response.body ?: run {
                        destFile.delete()
                        throw Exception("Resposta vazia")
                    }

                    destFile.outputStream().use { out ->
                        body.byteStream().use { it.copyTo(out) }
                    }
                }

                if (!destFile.exists() || destFile.length() == 0L) {
                    destFile.delete()
                    throw Exception("Arquivo baixado está vazio")
                }

                // Parse EPUB metadata and cover directly with ZipFile
                val (bookTitle, bookAuthor, bookDescription, epubCover) = parseEpub(destFile, entry)
                val coverImage = epubCover ?: fetchCoverFallback(entry)

                val bookToAdd = Book(
                    title = bookTitle,
                    author = bookAuthor,
                    description = bookDescription,
                    scrollIndex = 0,
                    scrollOffset = 0,
                    progress = 0f,
                    filePath = destFile.absolutePath,
                    lastOpened = null,
                    categories = emptyList(),
                    coverImage = null
                )

                addBookUseCase(bookToAdd, coverImage)
                LibraryScreen.refreshListChannel.trySend(0)

                // Refresh local library book titles reactive state
                refreshLocalLibraryTitles()

                // Find the inserted book by file path
                val inserted = bookRepository.searchBooks(bookTitle).getOrNull()
                    ?.firstOrNull { it.filePath == destFile.absolutePath }
                    ?: bookRepository.searchBooks(bookTitle).getOrNull()?.firstOrNull()

                _state.update {
                    it.copy(
                        downloadingBookIds = it.downloadingBookIds - bookIdKey,
                        selectedBookForPreview = null
                    )
                }

                if (openReader && inserted != null) {
                    _effects.emit(OpdsEffect.OnNavigateToReader(inserted.id))
                } else {
                    _effects.emit(OpdsEffect.ShowToast("\"${entry.title}\" adicionado à biblioteca!"))
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        downloadingBookIds = it.downloadingBookIds - bookIdKey,
                        selectedBookForPreview = null
                    )
                }
                _effects.emit(OpdsEffect.ShowToast("Erro ao baixar: ${e.message}"))
            }
        }
    }

    private fun batchDownloadSeries(entry: OpdsEntry) {
        val seriesUrl = entry.subsectionUrl ?: return
        if (_state.value.batchDownloadingSeriesUrls.contains(seriesUrl)) return

        viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(batchDownloadingSeriesUrls = it.batchDownloadingSeriesUrls + seriesUrl)
            }
            _effects.emit(OpdsEffect.ShowToast("Baixando saga: \"${entry.title}\"..."))

            try {
                val feedResult = getOpdsLibraryUseCase(seriesUrl).getOrThrow()
                val books = feedResult.entries.filter { !it.isFolder && !it.downloadUrl.isNullOrBlank() }

                var downloadedCount = 0
                for (bookEntry in books) {
                    val alreadyInLibrary = _state.value.localLibraryBookTitles.contains(
                        bookEntry.title.lowercase().trim()
                    )
                    if (!alreadyInLibrary) {
                        try {
                            downloadBookInternal(bookEntry)
                            downloadedCount++
                        } catch (e: Exception) {
                            // continue downloading remaining books in saga
                        }
                    }
                }

                refreshLocalLibraryTitles()
                LibraryScreen.refreshListChannel.trySend(0)
                _state.update {
                    it.copy(batchDownloadingSeriesUrls = it.batchDownloadingSeriesUrls - seriesUrl)
                }
                _effects.emit(
                    OpdsEffect.ShowToast("Saga \"${entry.title}\": $downloadedCount livro(s) baixados com sucesso!")
                )
            } catch (e: Exception) {
                _state.update {
                    it.copy(batchDownloadingSeriesUrls = it.batchDownloadingSeriesUrls - seriesUrl)
                }
                _effects.emit(OpdsEffect.ShowToast("Erro ao baixar saga: ${e.message}"))
            }
        }
    }

    private suspend fun downloadBookInternal(entry: OpdsEntry) {
        val downloadUrl = entry.downloadUrl ?: return
        val booksDir = JFile(context.filesDir, "opds_books").apply { mkdirs() }
        val safeId = Math.abs((entry.id.ifBlank { entry.title }).hashCode())
        val sanitizedTitle = entry.title.replace(Regex("[^a-zA-Z0-9]"), "_").take(30)
        val destFile = JFile(booksDir, "${sanitizedTitle}_${safeId}.epub")

        if (!destFile.exists() || destFile.length() == 0L) {
            val httpUrl = downloadUrl.toHttpUrlOrNull()
            val request = if (httpUrl != null) {
                Request.Builder().url(httpUrl).build()
            } else {
                Request.Builder().url(downloadUrl).build()
            }
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            val body = response.body ?: throw Exception("Resposta vazia")
            destFile.outputStream().use { out -> body.byteStream().use { it.copyTo(out) } }
        }

        val (bookTitle, bookAuthor, bookDescription, epubCoverInternal) = parseEpub(destFile, entry)
        val coverImage = epubCoverInternal ?: fetchCoverFallback(entry)
        val bookToAdd = Book(
            title = bookTitle,
            author = bookAuthor,
            description = bookDescription,
            scrollIndex = 0,
            scrollOffset = 0,
            progress = 0f,
            filePath = destFile.absolutePath,
            lastOpened = null,
            categories = emptyList(),
            coverImage = null
        )
        addBookUseCase(bookToAdd, coverImage)
    }

    private fun fetchCoverFallback(entry: OpdsEntry): android.graphics.Bitmap? {
        val coverUrl = entry.coverUrl?.takeIf { it.isNotBlank() } ?: return null
        return try {
            val request = Request.Builder().url(coverUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private data class EpubMeta(
        val title: String,
        val author: UIText,
        val description: String?,
        val cover: android.graphics.Bitmap?
    )

    private fun parseEpub(file: JFile, entry: OpdsEntry): EpubMeta {
        return try {
            ZipFile(file).use { zip ->
                val opfEntry = zip.entries().asSequence()
                    .find { it.name.endsWith(".opf", ignoreCase = true) }

                if (opfEntry == null) {
                    return EpubMeta(
                        title = entry.title,
                        author = UIText.StringValue(entry.author.orEmpty()),
                        description = entry.summary,
                        cover = null
                    )
                }

                val opfContent = zip.getInputStream(opfEntry).bufferedReader().use { it.readText() }
                val document = Jsoup.parse(opfContent, Parser.xmlParser())

                val title = document.select("metadata > dc|title").text().trim()
                    .ifBlank { entry.title }

                val authorText = document.select("metadata > dc|creator").text().trim()
                val author: UIText = if (authorText.isBlank()) {
                    UIText.StringResource(R.string.unknown_author)
                } else {
                    UIText.StringValue(authorText)
                }

                val description = Jsoup.parse(
                    document.select("metadata > dc|description").text()
                ).text().ifBlank { null }

                // Extract cover
                val opfDir = opfEntry.name.substringBeforeLast("/", "").let {
                    if (it.isEmpty()) "" else "$it/"
                }
                val coverImagePath = document
                    .select("metadata > meta[name=cover]")
                    .attr("content")
                    .let { coverId ->
                        if (coverId.isNotBlank()) {
                            document.select("manifest > item[id=$coverId]").attr("href")
                                .takeIf { it.isNotBlank() }
                        } else null
                    }
                    ?: document.select("manifest > item[media-type*=image]")
                        .firstOrNull()?.attr("href")

                val cover = coverImagePath?.let { path ->
                    val decoded = try {
                        URLDecoder.decode(path, StandardCharsets.UTF_8.name())
                    } catch (e: Exception) { path }

                    val fullPath = if (decoded.startsWith("/")) decoded.trimStart('/') else "$opfDir$decoded"
                    val coverEntry = zip.entries().asSequence().find { it.name == fullPath }
                        ?: zip.entries().asSequence().find { it.name.endsWith(decoded) }
                    coverEntry?.let { ce ->
                        BitmapFactory.decodeStream(zip.getInputStream(ce))
                    }
                }

                EpubMeta(title, author, description, cover)
            }
        } catch (e: Exception) {
            EpubMeta(
                title = entry.title,
                author = UIText.StringValue(entry.author.orEmpty()),
                description = entry.summary,
                cover = null
            )
        }
    }
}
