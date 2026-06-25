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
            val title = file.name?.removeSuffix(".pdf") ?: "Unknown"

            val totalPages = pdfParser.getPageCount(path)

            val bookId = repository.insertBook(Book(
                title = title,
                filePath = path,
                totalPages = totalPages,
                coverImage = null
            ))

            // Detect and save chapters
            val detectedChapters = ChapterDetector.detect(context, path)
            if (detectedChapters.isNotEmpty()) {
                val chapters = detectedChapters.mapIndexed { index, meta ->
                    Chapter(
                        bookId = bookId,
                        title = meta.title,
                        startPage = meta.startPage,
                        endPage = if (index + 1 < detectedChapters.size) {
                            detectedChapters[index + 1].startPage
                        } else {
                            totalPages
                        }
                    )
                }
                repository.insertChapters(chapters)
            }
        }
    }
}
