package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.ValidationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app_name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Universal PDF Utility", appName)
  }

  @Test
  fun `test page range parsing utility`() {
    val pages = ValidationUtils.parsePageRanges("1-3, 5, 7-8", maxPages = 10)
    assertEquals(listOf(1, 2, 3, 5, 7, 8), pages)
  }

  @Test
  fun `test sanitize filename`() {
    val sanitized = ValidationUtils.sanitizeFileName("My Report: 2026/08*Draft?.pdf")
    assertEquals("My_Report__2026_08_Draft_.pdf", sanitized)
  }
}
