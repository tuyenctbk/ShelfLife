package com.example.ui.engagement

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.TimeUnit

/**
 * Snapshot of app usage and engagement metrics collected locally.
 */
data class AppUsageStats(
    val launchCount: Int,
    val rescuedCount: Int,
    val itemsAddedCount: Int,
    val recipesCookedCount: Int,
    val daysSinceInstall: Int,
    val hasRated: Boolean,
    val hasShared: Boolean,
    val neverRate: Boolean,
    val ratingDismissCount: Int,
    val shareDismissCount: Int,
    val updateDismissCount: Int,
    val lastDismissedUpdateVersion: String?
)

/**
 * Types of auto popup suggestions evaluated by [EngagementManager].
 */
enum class SuggestionType {
    NONE,
    UPDATE,
    RATE,
    SHARE
}

/**
 * Manages app usage metrics and intelligently orchestrates auto popup suggestions
 * (Update, Rate, and Share) so they appear at optimal milestones without annoying the user.
 */
class EngagementManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("shelflife_engagement_prefs", Context.MODE_PRIVATE)

    // In-memory guard: strictly at most ONE auto popup suggestion per app session/process
    @Volatile
    private var sessionPromptShown: Boolean = false

    companion object {
        // App Usage Keys
        private const val KEY_FIRST_INSTALL_TIME = "key_first_install_time"
        private const val KEY_LAUNCH_COUNT = "key_launch_count"
        private const val KEY_RESCUED_COUNT = "key_rescued_count"
        private const val KEY_ITEMS_ADDED_COUNT = "key_items_added_count"
        private const val KEY_RECIPES_COOKED_COUNT = "key_recipes_cooked_count"

        // Global Anti-Nagging Guard
        private const val KEY_LAST_ANY_POPUP_TIMESTAMP = "key_last_any_popup_timestamp"

        // Rating Keys
        private const val KEY_LAST_RATING_PROMPT = "key_last_rating_prompt"
        private const val KEY_HAS_RATED = "key_has_rated"
        private const val KEY_RATING_DISMISS_COUNT = "key_rating_dismiss_count"
        private const val KEY_NEVER_RATE = "key_never_rate"

        // Share Keys
        private const val KEY_LAST_SHARE_PROMPT = "key_last_share_prompt"
        private const val KEY_HAS_SHARED = "key_has_shared"
        private const val KEY_SHARE_DISMISS_COUNT = "key_share_dismiss_count"

        // Update Keys
        private const val KEY_LAST_UPDATE_PROMPT = "key_last_update_prompt"
        private const val KEY_UPDATE_DISMISS_COUNT = "key_update_dismiss_count"
        private const val KEY_DISMISSED_UPDATE_VERSION = "key_dismissed_update_version"
        private const val KEY_LATEST_AVAILABLE_VERSION = "key_latest_available_version"

        // Strict Anti-Nagging Cooldowns & Limits
        val GLOBAL_COOLDOWN_MS = TimeUnit.DAYS.toMillis(3)       // 3 days between ANY auto popup
        val COOLDOWN_DAYS_RATING = TimeUnit.DAYS.toMillis(7)     // 7 days between rating prompts
        val COOLDOWN_DAYS_SHARE = TimeUnit.DAYS.toMillis(14)     // 14 days between share prompts
        val COOLDOWN_DAYS_UPDATE = TimeUnit.DAYS.toMillis(7)     // 7 days between update prompts

        const val MAX_RATING_DISMISSALS = 3
        const val MAX_SHARE_DISMISSALS = 2
        const val MAX_UPDATE_DISMISSALS = 3
        const val MIN_LAUNCHES_BEFORE_AUTO_POPUP = 2
    }

    // ==========================================
    // Usage Tracking
    // ==========================================

    fun recordLaunch() {
        val now = System.currentTimeMillis()
        if (prefs.getLong(KEY_FIRST_INSTALL_TIME, 0L) == 0L) {
            prefs.edit().putLong(KEY_FIRST_INSTALL_TIME, now).apply()
        }
        val current = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        prefs.edit().putInt(KEY_LAUNCH_COUNT, current + 1).apply()
    }

    fun recordRescuedItem() {
        val current = prefs.getInt(KEY_RESCUED_COUNT, 0)
        prefs.edit().putInt(KEY_RESCUED_COUNT, current + 1).apply()
    }

    fun recordItemAdded() {
        val current = prefs.getInt(KEY_ITEMS_ADDED_COUNT, 0)
        prefs.edit().putInt(KEY_ITEMS_ADDED_COUNT, current + 1).apply()
    }

    fun recordRecipeCooked() {
        val current = prefs.getInt(KEY_RECIPES_COOKED_COUNT, 0)
        prefs.edit().putInt(KEY_RECIPES_COOKED_COUNT, current + 1).apply()
    }

    /**
     * Inspect usage metrics collected so far.
     */
    fun getUsageStats(): AppUsageStats {
        val firstInstall = prefs.getLong(KEY_FIRST_INSTALL_TIME, System.currentTimeMillis())
        val days = ((System.currentTimeMillis() - firstInstall) / TimeUnit.DAYS.toMillis(1)).toInt().coerceAtLeast(0)
        return AppUsageStats(
            launchCount = prefs.getInt(KEY_LAUNCH_COUNT, 0),
            rescuedCount = prefs.getInt(KEY_RESCUED_COUNT, 0),
            itemsAddedCount = prefs.getInt(KEY_ITEMS_ADDED_COUNT, 0),
            recipesCookedCount = prefs.getInt(KEY_RECIPES_COOKED_COUNT, 0),
            daysSinceInstall = days,
            hasRated = prefs.getBoolean(KEY_HAS_RATED, false),
            hasShared = prefs.getBoolean(KEY_HAS_SHARED, false),
            neverRate = prefs.getBoolean(KEY_NEVER_RATE, false),
            ratingDismissCount = prefs.getInt(KEY_RATING_DISMISS_COUNT, 0),
            shareDismissCount = prefs.getInt(KEY_SHARE_DISMISS_COUNT, 0),
            updateDismissCount = prefs.getInt(KEY_UPDATE_DISMISS_COUNT, 0),
            lastDismissedUpdateVersion = prefs.getString(KEY_DISMISSED_UPDATE_VERSION, null)
        )
    }

    // ==========================================
    // Smart Timing & Optimization Engine
    // ==========================================

    /**
     * Evaluates whether an auto popup suggestion is qualified right now.
     * Enforces:
     * 1. At most 1 suggestion per app session.
     * 2. Global cooldown between ANY suggestion (at least 3 days).
     * 3. User experience threshold (not on first day/launch).
     * 4. Priority order: Update -> Rate -> Share.
     */
    fun checkNextAutoSuggestion(
        currentVersion: String,
        latestVersion: String? = getLatestAvailableVersion()
    ): SuggestionType {
        // Rule 1: Never interrupt more than once in the same session
        if (sessionPromptShown) return SuggestionType.NONE

        // Rule 2: Don't bother users on their initial exploratory launches
        val launches = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        if (launches < MIN_LAUNCHES_BEFORE_AUTO_POPUP) return SuggestionType.NONE

        // Rule 3: Enforce global cooldown across all auto popups
        val lastAnyPopup = prefs.getLong(KEY_LAST_ANY_POPUP_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        if (now - lastAnyPopup < GLOBAL_COOLDOWN_MS) return SuggestionType.NONE

        // Priority 1: Check for App Update
        if (!latestVersion.isNullOrBlank() && shouldShowUpdatePrompt(currentVersion, latestVersion, isManual = false)) {
            return SuggestionType.UPDATE
        }

        // Priority 2: Check for Rating suggestion (positive sentiment moment)
        if (shouldShowRatingPrompt(isManual = false)) {
            return SuggestionType.RATE
        }

        // Priority 3: Check for Share suggestion (community impact milestone)
        if (shouldShowSharePrompt(isManual = false)) {
            return SuggestionType.SHARE
        }

        return SuggestionType.NONE
    }

    /**
     * Mark that a suggestion was displayed to the user.
     * Locks the session guard and updates the global cooldown timer.
     */
    fun markSuggestionShown(type: SuggestionType, targetVersion: String? = null) {
        sessionPromptShown = true
        val now = System.currentTimeMillis()
        val editor = prefs.edit().putLong(KEY_LAST_ANY_POPUP_TIMESTAMP, now)

        when (type) {
            SuggestionType.UPDATE -> {
                editor.putLong(KEY_LAST_UPDATE_PROMPT, now)
            }
            SuggestionType.RATE -> {
                editor.putLong(KEY_LAST_RATING_PROMPT, now)
            }
            SuggestionType.SHARE -> {
                editor.putLong(KEY_LAST_SHARE_PROMPT, now)
            }
            SuggestionType.NONE -> Unit
        }
        editor.apply()
    }

    // ==========================================
    // Specific Suggestion Criteria
    // ==========================================

    /**
     * Evaluates rating prompt eligibility.
     */
    fun shouldShowRatingPrompt(isManual: Boolean = false): Boolean {
        if (prefs.getBoolean(KEY_HAS_RATED, false)) return false
        if (prefs.getBoolean(KEY_NEVER_RATE, false)) return false

        val dismissCount = prefs.getInt(KEY_RATING_DISMISS_COUNT, 0)
        if (dismissCount >= MAX_RATING_DISMISSALS) return false

        if (!isManual) {
            val launches = prefs.getInt(KEY_LAUNCH_COUNT, 0)
            val rescued = prefs.getInt(KEY_RESCUED_COUNT, 0)
            val added = prefs.getInt(KEY_ITEMS_ADDED_COUNT, 0)

            // Value threshold: user has rescued >= 2 items, added >= 5 items, or launched >= 4 times
            val hasMetValueThreshold = rescued >= 2 || added >= 5 || launches >= 4
            if (!hasMetValueThreshold) return false

            val lastPrompt = prefs.getLong(KEY_LAST_RATING_PROMPT, 0L)
            val now = System.currentTimeMillis()
            if (now - lastPrompt < COOLDOWN_DAYS_RATING) return false
        }

        return true
    }

    fun markRatingPromptShown() {
        markSuggestionShown(SuggestionType.RATE)
    }

    fun onUserRated() {
        prefs.edit().putBoolean(KEY_HAS_RATED, true).apply()
    }

    fun onUserDismissedRating(neverAskAgain: Boolean) {
        val dismissCount = prefs.getInt(KEY_RATING_DISMISS_COUNT, 0)
        val now = System.currentTimeMillis()
        prefs.edit()
            .putInt(KEY_RATING_DISMISS_COUNT, dismissCount + 1)
            .putBoolean(KEY_NEVER_RATE, neverAskAgain)
            .putLong(KEY_LAST_RATING_PROMPT, now)
            .putLong(KEY_LAST_ANY_POPUP_TIMESTAMP, now)
            .apply()
    }

    /**
     * Evaluates share prompt eligibility.
     */
    fun shouldShowSharePrompt(isManual: Boolean = false): Boolean {
        if (prefs.getBoolean(KEY_HAS_SHARED, false)) return false

        val dismissCount = prefs.getInt(KEY_SHARE_DISMISS_COUNT, 0)
        if (dismissCount >= MAX_SHARE_DISMISSALS) return false

        if (!isManual) {
            val rescued = prefs.getInt(KEY_RESCUED_COUNT, 0)
            val launches = prefs.getInt(KEY_LAUNCH_COUNT, 0)

            // Impact milestone: user rescued >= 4 items, or rescued >= 2 with >= 6 launches
            val hasMetMilestone = rescued >= 4 || (rescued >= 2 && launches >= 6)
            if (!hasMetMilestone) return false

            val lastPrompt = prefs.getLong(KEY_LAST_SHARE_PROMPT, 0L)
            val now = System.currentTimeMillis()
            if (now - lastPrompt < COOLDOWN_DAYS_SHARE) return false
        }

        return true
    }

    fun markSharePromptShown() {
        markSuggestionShown(SuggestionType.SHARE)
    }

    fun onUserShared() {
        prefs.edit().putBoolean(KEY_HAS_SHARED, true).apply()
    }

    fun onUserDismissedShare() {
        val dismissCount = prefs.getInt(KEY_SHARE_DISMISS_COUNT, 0)
        val now = System.currentTimeMillis()
        prefs.edit()
            .putInt(KEY_SHARE_DISMISS_COUNT, dismissCount + 1)
            .putLong(KEY_LAST_SHARE_PROMPT, now)
            .putLong(KEY_LAST_ANY_POPUP_TIMESTAMP, now)
            .apply()
    }

    /**
     * Evaluates update prompt eligibility.
     */
    fun shouldShowUpdatePrompt(
        currentVersion: String,
        latestVersion: String,
        isManual: Boolean = false
    ): Boolean {
        if (latestVersion.isBlank()) return false
        if (!isNewerVersion(currentVersion, latestVersion)) return false

        if (!isManual) {
            // If the user already dismissed this specific version, do not nag them again for it
            val dismissedVersion = prefs.getString(KEY_DISMISSED_UPDATE_VERSION, null)
            if (dismissedVersion == latestVersion) return false

            val dismissCount = prefs.getInt(KEY_UPDATE_DISMISS_COUNT, 0)
            if (dismissCount >= MAX_UPDATE_DISMISSALS) return false

            val lastPrompt = prefs.getLong(KEY_LAST_UPDATE_PROMPT, 0L)
            val now = System.currentTimeMillis()
            if (now - lastPrompt < COOLDOWN_DAYS_UPDATE) return false
        }

        return true
    }

    fun onUserDismissedUpdate(latestVersion: String) {
        val dismissCount = prefs.getInt(KEY_UPDATE_DISMISS_COUNT, 0)
        val now = System.currentTimeMillis()
        prefs.edit()
            .putString(KEY_DISMISSED_UPDATE_VERSION, latestVersion)
            .putInt(KEY_UPDATE_DISMISS_COUNT, dismissCount + 1)
            .putLong(KEY_LAST_UPDATE_PROMPT, now)
            .putLong(KEY_LAST_ANY_POPUP_TIMESTAMP, now)
            .apply()
    }

    fun onUserUpdated(latestVersion: String) {
        prefs.edit()
            .putString(KEY_DISMISSED_UPDATE_VERSION, latestVersion)
            .putLong(KEY_LAST_UPDATE_PROMPT, System.currentTimeMillis())
            .apply()
    }

    // ==========================================
    // Remote Version Configuration & Helpers
    // ==========================================

    fun getLatestAvailableVersion(): String? {
        return prefs.getString(KEY_LATEST_AVAILABLE_VERSION, null)
    }

    fun setLatestAvailableVersion(version: String?) {
        prefs.edit().putString(KEY_LATEST_AVAILABLE_VERSION, version).apply()
    }

    fun resetSessionPrompt() {
        sessionPromptShown = false
    }

    /**
     * Compares semantic version strings (e.g., "1.1" > "1.0", "1.0.1" > "1.0").
     */
    fun isNewerVersion(current: String, latest: String): Boolean {
        try {
            val currParts = current.split(".").mapNotNull { it.trim().toIntOrNull() }
            val latestParts = latest.split(".").mapNotNull { it.trim().toIntOrNull() }
            if (currParts.isEmpty() || latestParts.isEmpty()) {
                return current != latest
            }
            val maxLen = maxOf(currParts.size, latestParts.size)
            for (i in 0 until maxLen) {
                val c = currParts.getOrElse(i) { 0 }
                val l = latestParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            return false
        } catch (_: Exception) {
            return current != latest
        }
    }
}
