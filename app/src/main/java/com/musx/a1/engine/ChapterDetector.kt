package com.musx.a1.engine

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination

object ChapterDetector {

    /**
     * Detects chapters in a PDF file using the document outline.
     */
    fun detect(context: Context, uriString: String): List<ChapterMetadata> {
        val uri = Uri.parse(uriString)
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    val outline = document.documentCatalog.documentOutline
                    val chapters = mutableListOf<ChapterMetadata>()

                    if (outline != null) {
                        var current = outline.firstChild
                        while (current != null) {
                            val destination = current.destination
                            val pageIndex: Int = if (destination is PDPageDestination) {
                                destination.retrievePageNumber()
                            } else {
                                val page = current.findDestinationPage(document)
                                document.documentCatalog.pages.indexOf(page)
                            }

                            chapters.add(ChapterMetadata(current.title, if (pageIndex < 0) 0 else pageIndex))
                            current = current.nextSibling
                        }
                    }

                    if (chapters.isEmpty()) {
                        // Fallback: Page chunking every 10 pages
                        val totalPages = document.numberOfPages
                        for (i in 0 until totalPages step 10) {
                            chapters.add(ChapterMetadata("Part ${i / 10 + 1}", i))
                        }
                    }
                    chapters
                }
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class ChapterMetadata(val title: String, val startPage: Int)
