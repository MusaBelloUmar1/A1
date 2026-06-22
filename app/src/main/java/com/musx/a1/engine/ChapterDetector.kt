package com.musx.a1.engine

import com.musx.a1.data.entity.Chapter
import java.io.File
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem

object ChapterDetector {

    /**
     * Detects chapters in a PDF file.
     * Priorities: 1. PDF Bookmarks, 2. Page chunking (fallback).
     */
    fun detect(filePath: String): List<ChapterMetadata> {
        val file = File(filePath)
        if (!file.exists()) return emptyList()

        return try {
            PDDocument.load(file).use { document ->
                val outline = document.documentCatalog.documentOutline
                if (outline != null) {
                    val chapters = mutableListOf<ChapterMetadata>()
                    var current = outline.firstChild
                    while (current != null) {
                        val page = document.documentCatalog.pages.indexOf(current.destination?.let {
                            // Simplified page detection from destination
                            // In real scenarios, this needs more robust handling
                            null
                        } ?: current.findDestinationPage(document))

                        chapters.add(ChapterMetadata(current.title, if (page == -1) 0 else page))
                        current = current.nextSibling
                    }
                    if (chapters.isNotEmpty()) return chapters
                }

                // Fallback: Page chunking
                val totalPages = document.numberOfPages
                val chapters = mutableListOf<ChapterMetadata>()
                for (i in 0 until totalPages step 10) {
                    chapters.add(ChapterMetadata("Part ${i / 10 + 1}", i))
                }
                chapters
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class ChapterMetadata(val title: String, val startPage: Int)
