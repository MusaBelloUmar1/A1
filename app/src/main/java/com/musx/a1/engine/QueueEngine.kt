package com.musx.a1.engine

import com.musx.a1.data.entity.Chapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class QueueEngine {
    private var chapters: List<Chapter> = emptyList()
    private var currentChapterIndex: Int = 0
    private var sentences: List<String> = emptyList()
    private var currentSentenceIndex: Int = 0

    private val _currentSentence = MutableStateFlow("")
    val currentSentence: StateFlow<String> = _currentSentence

    fun setQueue(chapters: List<Chapter>, startIndex: Int) {
        this.chapters = chapters
        this.currentChapterIndex = startIndex
    }

    fun setSentences(sentences: List<String>, startIndex: Int) {
        this.sentences = sentences
        this.currentSentenceIndex = startIndex
        updateCurrentSentence()
    }

    fun nextSentence(): String? {
        if (currentSentenceIndex < sentences.size - 1) {
            currentSentenceIndex++
            updateCurrentSentence()
            return sentences[currentSentenceIndex]
        }
        return null // End of chapter
    }

    fun previousSentence(): String? {
        if (currentSentenceIndex > 0) {
            currentSentenceIndex--
            updateCurrentSentence()
            return sentences[currentSentenceIndex]
        }
        return null // Beginning of chapter
    }

    fun nextChapter(): Int? {
        if (currentChapterIndex < chapters.size - 1) {
            currentChapterIndex++
            return currentChapterIndex
        }
        return null
    }

    fun previousChapter(): Int? {
        if (currentChapterIndex > 0) {
            currentChapterIndex--
            return currentChapterIndex
        }
        return null
    }

    fun getCurrentChapterIndex() = currentChapterIndex
    fun getCurrentSentenceIndex() = currentSentenceIndex

    private fun updateCurrentSentence() {
        if (currentSentenceIndex in sentences.indices) {
            _currentSentence.value = sentences[currentSentenceIndex]
        }
    }
}
