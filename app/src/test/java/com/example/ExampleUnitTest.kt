package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying operation allowance and calculation logic.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
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
}
