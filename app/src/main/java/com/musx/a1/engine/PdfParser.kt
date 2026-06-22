package com.musx.a1.engine

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

class PdfParser(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    fun extractTextFromPage(filePath: String, pageIndex: Int): String? {
        val file = File(filePath)
        if (!file.exists()) return null

        return try {
            PDDocument.load(file).use { document ->
                val stripper = PDFTextStripper()
                stripper.startPage = pageIndex + 1
                stripper.endPage = pageIndex + 1
                stripper.getText(document)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getPageCount(filePath: String): Int {
        val file = File(filePath)
        if (!file.exists()) return 0
        return try {
            PDDocument.load(file).use { document ->
                document.numberOfPages
            }
        } catch (e: Exception) {
            0
        }
    }
}
