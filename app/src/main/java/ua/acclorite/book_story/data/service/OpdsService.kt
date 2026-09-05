/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.service

import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import ua.acclorite.book_story.data.model.opds.OpdsEntry
import ua.acclorite.book_story.data.model.opds.OpdsFeedResult
import java.io.Reader
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpdsService @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        const val RECOMMENDED_OPDS_URL = "https://my-rss-library.pages.dev/opds.xml?key=fabricio"
    }

    // Default fallback OPDS library URL
    val defaultOpdsUrl = RECOMMENDED_OPDS_URL

    suspend fun fetchOpdsFeed(url: String = defaultOpdsUrl): Result<OpdsFeedResult> {
        return try {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to fetch OPDS feed: HTTP ${response.code}"))
            }

            val body = response.body ?: return Result.failure(Exception("Empty response"))
            val feedResult = body.charStream().use { reader ->
                parseOpdsFeedStreaming(reader, url)
            }

            Result.success(feedResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resolveUrl(baseUrl: String, href: String?): String? {
        if (href.isNullOrBlank()) return null
        return try {
            val baseUri = URI(baseUrl)
            baseUri.resolve(href).toString()
        } catch (e: Exception) {
            href
        }
    }

    private fun parseOpdsFeedStreaming(reader: Reader, baseUrl: String): OpdsFeedResult {
        val factory = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val parser = factory.newPullParser()
        parser.setInput(reader)

        val entries = mutableListOf<OpdsEntry>()
        var feedTitle = "Minha Biblioteca Cloud"
        var inFeedTitle = false
        var inEntry = false

        var currentId = ""
        var currentTitle = ""
        var currentAuthor: String? = null
        var currentSummary: String? = null
        var currentCoverUrl: String? = null
        var currentThumbnailUrl: String? = null
        var currentDownloadUrl: String? = null
        var currentSubsectionUrl: String? = null
        var currentType: String? = null
        var currentPublisher: String? = null
        val currentCategories = mutableListOf<String>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val localName = parser.name.lowercase().substringAfterLast(":")
                    when {
                        localName == "title" -> {
                            if (!inEntry && !inFeedTitle) {
                                inFeedTitle = true
                                val text = parser.nextText().trim()
                                if (text.isNotBlank()) feedTitle = text
                            } else if (inEntry) {
                                currentTitle = parser.nextText().trim()
                            }
                        }

                        localName == "entry" -> {
                            inEntry = true
                            currentId = ""
                            currentTitle = ""
                            currentAuthor = null
                            currentSummary = null
                            currentCoverUrl = null
                            currentThumbnailUrl = null
                            currentDownloadUrl = null
                            currentSubsectionUrl = null
                            currentType = null
                            currentPublisher = null
                            currentCategories.clear()
                        }

                        localName == "id" -> {
                            if (inEntry) currentId = parser.nextText().trim()
                        }

                        localName == "name" -> {
                            if (inEntry && currentAuthor == null) {
                                currentAuthor = parser.nextText().trim()
                            }
                        }

                        localName == "author" -> {
                            if (inEntry && currentAuthor == null) {
                                val text = try { parser.nextText().trim() } catch (e: Exception) { "" }
                                if (text.isNotBlank()) currentAuthor = text
                            }
                        }

                        localName == "summary" || localName == "content" -> {
                            if (inEntry && currentSummary == null) {
                                currentSummary = parser.nextText().trim()
                            }
                        }

                        // dc:publisher — Dublin Core publisher tag
                        localName == "publisher" -> {
                            if (inEntry && currentPublisher == null) {
                                currentPublisher = try { parser.nextText().trim() } catch (e: Exception) { null }
                            }
                        }

                        // <category term="..." label="..."/>
                        localName == "category" -> {
                            if (inEntry) {
                                val label = parser.getAttributeValue(null, "label")
                                val term = parser.getAttributeValue(null, "term")
                                val value = (label ?: term)?.trim()
                                if (!value.isNullOrBlank()) currentCategories.add(value)
                            }
                        }

                        localName == "link" -> {
                            if (inEntry) {
                                val rel = (parser.getAttributeValue(null, "rel") ?: "").lowercase()
                                val href = parser.getAttributeValue(null, "href")
                                val type = parser.getAttributeValue(null, "type") ?: ""
                                val resolvedHref = resolveUrl(baseUrl, href)

                                when {
                                    rel.contains("acquisition") ||
                                            type.contains("application/epub") ||
                                            type.contains("application/pdf") -> {
                                        if (currentDownloadUrl == null && resolvedHref != null) {
                                            currentDownloadUrl = resolvedHref
                                            currentType = type
                                        }
                                    }

                                    rel.contains("thumbnail") -> {
                                        if (currentThumbnailUrl == null && resolvedHref != null) {
                                            currentThumbnailUrl = resolvedHref
                                        }
                                    }

                                    rel.contains("image") || rel.contains("cover") -> {
                                        if (currentCoverUrl == null && resolvedHref != null) {
                                            currentCoverUrl = resolvedHref
                                        }
                                    }

                                    rel.contains("subsection") ||
                                            type.contains("opds-catalog") -> {
                                        if (currentSubsectionUrl == null && resolvedHref != null) {
                                            currentSubsectionUrl = resolvedHref
                                            if (currentType == null) currentType = type
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    val localName = parser.name.lowercase().substringAfterLast(":")
                    if (localName == "entry") {
                        inEntry = false
                        if (currentTitle.isNotBlank()) {
                            val isFolder = !currentSubsectionUrl.isNullOrBlank() && currentDownloadUrl == null

                            // Parse rich header from summary: [★ 4.8/5.0 (2.415 avaliações) • Saga: X #1 • Editora: Y • Lançamento: Z]
                            val parsed = parseSummaryHeader(currentSummary)

                            // Separate saga categories from genre categories
                            val sagaLabel = currentCategories.firstOrNull { it.startsWith("Saga ") }
                            val genres = currentCategories.filter { !it.startsWith("Saga ") }

                            entries.add(
                                OpdsEntry(
                                    id = currentId.ifBlank { currentTitle },
                                    title = currentTitle,
                                    author = currentAuthor,
                                    summary = parsed.synopsis,
                                    coverUrl = currentCoverUrl ?: currentThumbnailUrl,
                                    thumbnailUrl = currentThumbnailUrl ?: currentCoverUrl,
                                    downloadUrl = currentDownloadUrl,
                                    subsectionUrl = currentSubsectionUrl,
                                    type = currentType,
                                    isFolder = isFolder,
                                    rating = parsed.rating,
                                    ratingCount = parsed.ratingCount,
                                    saga = parsed.saga ?: sagaLabel?.removePrefix("Saga "),
                                    publisher = currentPublisher ?: parsed.publisher,
                                    releaseDate = parsed.releaseDate,
                                    genres = genres
                                )
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return OpdsFeedResult(title = feedTitle, entries = entries)
    }

    private data class ParsedSummary(
        val rating: String?,
        val ratingCount: String?,
        val saga: String?,
        val publisher: String?,
        val releaseDate: String?,
        val synopsis: String?
    )

    /**
     * Parses the rich header format injected by the server into <summary>:
     * [★ 4.8/5.0 (2.415 avaliações) • Saga: Três Coroas Negras #1 • Editora: Paralela • Lançamento: 15 de setembro de 2017]
     * <newline>
     * Actual synopsis text…
     */
    private fun parseSummaryHeader(raw: String?): ParsedSummary {
        if (raw.isNullOrBlank()) return ParsedSummary(null, null, null, null, null, null)

        // Try to find the bracketed header at the start
        val headerRegex = Regex("""^\[([^\]]+)\]\s*""")
        val headerMatch = headerRegex.find(raw)

        if (headerMatch == null) {
            // No header — return raw as synopsis
            return ParsedSummary(null, null, null, null, null, raw.trim())
        }

        val headerContent = headerMatch.groupValues[1]
        val synopsis = raw.removePrefix(headerMatch.value).trim().ifBlank { null }

        // Parse individual fields from header (separated by " • ")
        var rating: String? = null
        var ratingCount: String? = null
        var saga: String? = null
        var publisher: String? = null
        var releaseDate: String? = null

        val parts = headerContent.split("•").map { it.trim() }
        for (part in parts) {
            when {
                part.startsWith("★") -> {
                    // e.g. "★ 4.8/5.0 (2.415 avaliações)"
                    val ratingMatch = Regex("""★\s*([\d.,]+/[\d.,]+)\s*\(([^)]+)\)""").find(part)
                    if (ratingMatch != null) {
                        rating = ratingMatch.groupValues[1]
                        ratingCount = ratingMatch.groupValues[2].trim()
                    } else {
                        rating = part.removePrefix("★").trim()
                    }
                }
                part.startsWith("Saga:") -> saga = part.removePrefix("Saga:").trim()
                part.startsWith("Editora:") -> publisher = part.removePrefix("Editora:").trim()
                part.startsWith("Lançamento:") -> releaseDate = part.removePrefix("Lançamento:").trim()
            }
        }

        return ParsedSummary(rating, ratingCount, saga, publisher, releaseDate, synopsis)
    }
}
