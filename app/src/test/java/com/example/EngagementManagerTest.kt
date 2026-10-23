package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.engagement.EngagementManager
import com.example.ui.engagement.SuggestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EngagementManagerTest {

    private lateinit var context: Context
    private lateinit var engagementManager: EngagementManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear shared preferences for clean test environment
        val prefs = context.getSharedPreferences("shelflife_engagement_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        engagementManager = EngagementManager(context)
    }

    @Test
    fun testUsageTrackingMetrics() {
        engagementManager.recordLaunch()
        engagementManager.recordLaunch()
        engagementManager.recordItemAdded()
        engagementManager.recordItemAdded()
        engagementManager.recordItemAdded()
        engagementManager.recordRescuedItem()
        engagementManager.recordRecipeCooked()

        val stats = engagementManager.getUsageStats()
        assertEquals(2, stats.launchCount)
        assertEquals(3, stats.itemsAddedCount)
        assertEquals(1, stats.rescuedCount)
        assertEquals(1, stats.recipesCookedCount)
        assertFalse(stats.hasRated)
        assertFalse(stats.hasShared)
        assertFalse(stats.neverRate)
        assertEquals(0, stats.ratingDismissCount)
        assertEquals(0, stats.shareDismissCount)
        assertEquals(0, stats.updateDismissCount)
    }

    @Test
    fun testFirstImpressionProtection_noSuggestionOnFirstLaunch() {
        engagementManager.recordLaunch()
        engagementManager.recordRescuedItem()
        engagementManager.recordRescuedItem()

        // Even though rescued threshold is met, launchCount is 1 (< 2)
        val suggestion = engagementManager.checkNextAutoSuggestion(currentVersion = "1.0")
        assertEquals(SuggestionType.NONE, suggestion)
    }

    @Test
    fun testRateSuggestionQualifiesAfterMilestone() {
        engagementManager.recordLaunch()
        engagementManager.recordLaunch() // launch count = 2
        engagementManager.recordRescuedItem()
        engagementManager.recordRescuedItem() // rescued count = 2

        val suggestion = engagementManager.checkNextAutoSuggestion(currentVersion = "1.0")
        assertEquals(SuggestionType.RATE, suggestion)
    }

    @Test
    fun testSessionGuard_onlyOneAutoPopupPerSession() {
        engagementManager.recordLaunch()
        engagementManager.recordLaunch()
        engagementManager.recordRescuedItem()
        engagementManager.recordRescuedItem()

        // First check qualifies for Rate
        val firstSuggestion = engagementManager.checkNextAutoSuggestion(currentVersion = "1.0")
        assertEquals(SuggestionType.RATE, firstSuggestion)

        // Mark shown in current session
        engagementManager.markSuggestionShown(SuggestionType.RATE)

        // Subsequent check in the same session must be NONE
        val secondSuggestion = engagementManager.checkNextAutoSuggestion(currentVersion = "1.0")
        assertEquals(SuggestionType.NONE, secondSuggestion)
    }

    @Test
    fun testGlobalCooldown_suppressesAutoPopupsForThreeDays() {
        engagementManager.recordLaunch()
        engagementManager.recordLaunch()
        engagementManager.recordRescuedItem()
        engagementManager.recordRescuedItem()

        // Show suggestion and dismiss
        engagementManager.markSuggestionShown(SuggestionType.RATE)
        engagementManager.onUserDismissedRating(neverAskAgain = false)

        // Reset session to simulate new app launch 1 hour later
        engagementManager.resetSessionPrompt()

        // Still blocked by 3-day global cooldown
        val suggestion = engagementManager.checkNextAutoSuggestion(currentVersion = "1.0")
        assertEquals(SuggestionType.NONE, suggestion)
    }

    @Test
    fun testNeverAskAgain_permanentlySuppressesRating() {
        engagementManager.recordLaunch()
        engagementManager.recordLaunch()
        engagementManager.recordRescuedItem()
        engagementManager.recordRescuedItem()

        engagementManager.onUserDismissedRating(neverAskAgain = true)
        assertTrue(engagementManager.getUsageStats().neverRate)
        assertFalse(engagementManager.shouldShowRatingPrompt(isManual = false))
        assertFalse(engagementManager.shouldShowRatingPrompt(isManual = true))
    }

    @Test
    fun testMaxDismissals_permanentlySuppressesRating() {
        engagementManager.onUserDismissedRating(neverAskAgain = false)
        engagementManager.onUserDismissedRating(neverAskAgain = false)
        engagementManager.onUserDismissedRating(neverAskAgain = false) // 3 times

        assertEquals(3, engagementManager.getUsageStats().ratingDismissCount)
        assertFalse(engagementManager.shouldShowRatingPrompt(isManual = false))
    }

    @Test
    fun testUserRated_permanentlySuppressesRating() {
        engagementManager.onUserRated()
        assertTrue(engagementManager.getUsageStats().hasRated)
        assertFalse(engagementManager.shouldShowRatingPrompt(isManual = false))
    }

    @Test
    fun testShareSuggestionQualifiesAfterHighImpact() {
        // User already rated
        engagementManager.onUserRated()

        engagementManager.recordLaunch()
        engagementManager.recordLaunch()
        repeat(4) { engagementManager.recordRescuedItem() } // rescued count = 4

        val suggestion = engagementManager.checkNextAutoSuggestion(currentVersion = "1.0")
        assertEquals(SuggestionType.SHARE, suggestion)

        engagementManager.onUserShared()
        assertTrue(engagementManager.getUsageStats().hasShared)
        assertFalse(engagementManager.shouldShowSharePrompt(isManual = false))
    }

    @Test
    fun testUpdateSuggestionPriorityAndDismissal() {
        engagementManager.recordLaunch()
        engagementManager.recordLaunch()
        engagementManager.setLatestAvailableVersion("1.2")

        // Update is highest priority
        val suggestion = engagementManager.checkNextAutoSuggestion(currentVersion = "1.0")
        assertEquals(SuggestionType.UPDATE, suggestion)

        // User dismisses version 1.2
        engagementManager.onUserDismissedUpdate("1.2")
        engagementManager.resetSessionPrompt()

        // Same version 1.2 is now permanently suppressed
        val nextSuggestion = engagementManager.checkNextAutoSuggestion(currentVersion = "1.0", latestVersion = "1.2")
        assertFalse(nextSuggestion == SuggestionType.UPDATE)

        // But when a newer version 1.3 is released, update prompt is available again after cooldown
        val prefs = context.getSharedPreferences("shelflife_engagement_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("key_last_any_popup_timestamp", 0L).putLong("key_last_update_prompt", 0L).apply()
        val newerSuggestion = engagementManager.checkNextAutoSuggestion(currentVersion = "1.0", latestVersion = "1.3")
        assertEquals(SuggestionType.UPDATE, newerSuggestion)
    }

    @Test
    fun testSemanticVersionComparison() {
        assertTrue(engagementManager.isNewerVersion("1.0", "1.1"))
        assertTrue(engagementManager.isNewerVersion("1.0", "1.0.1"))
        assertTrue(engagementManager.isNewerVersion("1.9.9", "2.0"))
        assertTrue(engagementManager.isNewerVersion("1.0.0", "1.0.1"))

        assertFalse(engagementManager.isNewerVersion("1.0", "1.0"))
        assertFalse(engagementManager.isNewerVersion("1.1", "1.0"))
        assertFalse(engagementManager.isNewerVersion("2.0", "1.9.9"))
    }
}
