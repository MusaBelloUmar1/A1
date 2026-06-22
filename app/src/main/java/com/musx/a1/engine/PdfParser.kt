package com.musx.a1.engine

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

class PdfParser(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    fun extractTextFromPage(uriString: String, pageIndex: Int): String? {
        val uri = Uri.parse(uriString)
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    val stripper = PDFTextStripper()
                    stripper.startPage = pageIndex + 1
                    stripper.endPage = pageIndex + 1
                    stripper.getText(document)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getPageCount(uriString: String): Int {
        val uri = Uri.parse(uriString)
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    document.numberOfPages
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
