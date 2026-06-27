package com.musx.a1.engine.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.musx.a1.data.entity.Book
import com.musx.a1.data.entity.Chapter
import com.musx.a1.engine.PdfParser
import com.musx.a1.engine.ChapterDetector
import com.musx.a1.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FolderScanner(
    private val context: Context,
    private val repository: AppRepository,
    private val pdfParser: PdfParser
) {
    suspend fun scanFolder(folderUri: String) = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, Uri.parse(folderUri)) ?: return@withContext
        val files = root.listFiles()

        files.filter { it.name?.endsWith(".pdf", ignoreCase = true) == true }.forEach { file ->
            val path = file.uri.toString()

            // Skip if book already exists
            if (repository.getBookByPath(path) != null) return@forEach

            val title = file.name?.removeSuffix(".pdf") ?: "Unknown"
            val totalPages = pdfParser.getPageCount(path)

            val bookId = repository.insertBook(Book(
                title = title,
                filePath = path,
                totalPages = totalPages,
                coverImage = null
            ))

            // Detect and insert chapters
            val chaptersMetadata = ChapterDetector.detect(context, path)
            if (chaptersMetadata.isNotEmpty()) {
                val chapters = chaptersMetadata.mapIndexed { index, meta ->
                    val endPage = if (index + 1 < chaptersMetadata.size) {
                        chaptersMetadata[index + 1].startPage - 1
                    } else {
                        totalPages - 1
                    }
                    Chapter(
                        bookId = bookId,
                        title = meta.title,
                        startPage = meta.startPage,
                        endPage = endPage.coerceAtLeast(meta.startPage)
                    )
                }
                repository.insertChapters(chapters)
            }
        }
    }
}
