package com.musx.a1.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class TextCleanerTest {

    @Test
    fun testCleanPageNumbers() {
        val input = "Header\nPage 1 of 10\nContent starts here.\n- 5 -\nMore content.\n123\nFooter"
        val expected = "Header Content starts here. More content. Footer"
        assertEquals(expected, TextCleaner.clean(input))
    }

    @Test
    fun testNormalizeWhitespace() {
        val input = "This  is    a \n test   with many  spaces."
        val expected = "This is a test with many spaces."
        assertEquals(expected, TextCleaner.clean(input))
    }

    @Test
    fun testFixBrokenLines() {
        val input = "This is a sentence that is split\nacross two lines."
        val expected = "This is a sentence that is split across two lines."
        assertEquals(expected, TextCleaner.clean(input))
    }
}
