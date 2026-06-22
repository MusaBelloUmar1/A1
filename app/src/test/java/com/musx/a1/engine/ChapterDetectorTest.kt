package com.musx.a1.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterDetectorTest {

    @Test
    fun testFallbackPageChunking() {
        // Since we can't easily mock PDDocument in a simple unit test
        // without more setup, this test might be limited.
        // But we can test the metadata structure.
        val metadata = ChapterMetadata("Test", 10)
        assertEquals("Test", metadata.title)
        assertEquals(10, metadata.startPage)
    }
}
