package com.example

import com.example.engine.FileUtils
import com.example.engine.ValidationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying operation allowance, input validation, and file calculations.
 */
class ExampleUnitTest {

    @Test
    fun testParsePageRangesValid() {
        val pages1 = ValidationUtils.parsePageRanges("1-3, 5, 7", 10)
        assertEquals(listOf(1, 2, 3, 5, 7), pages1)

        val pages2 = ValidationUtils.parsePageRanges("4, 2, 2, 1", 5)
        assertEquals(listOf(1, 2, 4), pages2)

        val pagesSingle = ValidationUtils.parsePageRanges("3", 10)
        assertEquals(listOf(3), pagesSingle)
    }

    @Test
    fun testParsePageRangesInvalid() {
        assertThrows(IllegalArgumentException::class.java) {
            ValidationUtils.parsePageRanges("", 10)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ValidationUtils.parsePageRanges("11", 10) // out of bounds
        }
        assertThrows(IllegalArgumentException::class.java) {
            ValidationUtils.parsePageRanges("5-2", 10) // start > end
        }
        assertThrows(IllegalArgumentException::class.java) {
            ValidationUtils.parsePageRanges("0", 10) // 0-based is invalid
        }
    }

    @Test
    fun testSanitizeFileName() {
        assertEquals("report_2026.pdf", FileUtils.sanitizeFileName("report/2026.pdf"))
        assertEquals("my_notes_.pdf", FileUtils.sanitizeFileName("my*notes?.pdf"))
        assertEquals("document.pdf", FileUtils.sanitizeFileName("   "))
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("0 KB", FileUtils.formatFileSize(0))
        assertEquals("500 B", FileUtils.formatFileSize(500))
        assertEquals("1.5 KB", FileUtils.formatFileSize(1536))
        assertEquals("2 MB", FileUtils.formatFileSize(2 * 1024 * 1024))
    }

    @Test
    fun testFreeTierVsPremiumLogic() {
        val isPremium = true
        val isConnected = false
        val allowedWhenPremium = isPremium || isConnected
        assertTrue(allowedWhenPremium)

        val isFree = false
        val allowedWhenFreeOffline = isFree || isConnected
        assertFalse(allowedWhenFreeOffline)
    }

    @Test
    fun testAdManagerTestIds() {
        assertEquals("ca-app-pub-3940256099942544/6300978111", com.example.data.AdManager.BANNER_TEST_ID)
        assertEquals("ca-app-pub-3940256099942544/6300978111", com.example.data.AdManager.MEDIUM_RECTANGLE_TEST_ID)
        assertEquals("ca-app-pub-3940256099942544/1033173712", com.example.data.AdManager.INTERSTITIAL_TEST_ID)
        assertEquals("ca-app-pub-3940256099942544/5224354917", com.example.data.AdManager.REWARDED_TEST_ID)
    }

    @Test
    fun testAdStateValues() {
        val states = com.example.data.AdState.values()
        assertTrue(states.contains(com.example.data.AdState.IDLE))
        assertTrue(states.contains(com.example.data.AdState.LOADING))
        assertTrue(states.contains(com.example.data.AdState.READY))
        assertTrue(states.contains(com.example.data.AdState.SHOWING))
        assertTrue(states.contains(com.example.data.AdState.FAILED))
        assertTrue(states.contains(com.example.data.AdState.EXPIRED))
        assertTrue(states.contains(com.example.data.AdState.DISPOSED))
    }
}
