package com.musx.a1.engine

import java.text.BreakIterator
import java.util.Locale

object SentenceProcessor {

    /**
     * Splits text into individual sentences using BreakIterator.
     */
    fun splitIntoSentences(text: String, locale: Locale = Locale.getDefault()): List<String> {
        val sentences = mutableListOf<String>()
        val boundary = BreakIterator.getSentenceInstance(locale)
        boundary.setText(text)

        var start = boundary.first()
        var end = boundary.next()

        while (end != BreakIterator.DONE) {
            val sentence = text.substring(start, end).trim()
            if (sentence.isNotEmpty()) {
                sentences.add(sentence)
            }
            start = end
            end = boundary.next()
        }

        return sentences
    }
}
