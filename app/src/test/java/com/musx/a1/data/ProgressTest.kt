package com.musx.a1.data

import com.musx.a1.data.entity.Progress
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressTest {

    @Test
    fun testProgressDataClass() {
        val progress = Progress(
            bookId = 1,
            chapterIndex = 2,
            sentenceIndex = 3,
            percentage = 45.0f,
            lastPlayedAt = 123456789L
        )
        assertEquals(1L, progress.bookId)
        assertEquals(2, progress.chapterIndex)
        assertEquals(3, progress.sentenceIndex)
        assertEquals(45.0f, progress.percentage)
        assertEquals(123456789L, progress.lastPlayedAt)
    }
}
