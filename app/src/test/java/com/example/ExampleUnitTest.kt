package com.example

import com.example.data.model.PantryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ExampleUnitTest {
    @Test
    fun testPantryItemExpiringSoonCalculation() {
        val now = System.currentTimeMillis()
        val expiringItem = PantryItem(
            name = "Milk",
            expiryDateMillis = now + TimeUnit.DAYS.toMillis(2)
        )
        assertTrue(expiringItem.isExpiringSoon())
        assertFalse(expiringItem.isExpired())

        val freshItem = PantryItem(
            name = "Canned Corn",
            expiryDateMillis = now + TimeUnit.DAYS.toMillis(40)
        )
        assertFalse(freshItem.isExpiringSoon())
        assertFalse(freshItem.isExpired())

        val expiredItem = PantryItem(
            name = "Old Yogurt",
            expiryDateMillis = now - TimeUnit.DAYS.toMillis(1)
        )
        assertFalse(expiredItem.isExpiringSoon())
        assertTrue(expiredItem.isExpired())
    }

    @Test
    fun testConsumedItemNotExpiringSoon() {
        val now = System.currentTimeMillis()
        val consumedItem = PantryItem(
            name = "Eggs",
            expiryDateMillis = now + TimeUnit.DAYS.toMillis(1),
            isConsumed = true
        )
        assertFalse(consumedItem.isExpiringSoon())
        assertFalse(consumedItem.isExpired())
    }
}
