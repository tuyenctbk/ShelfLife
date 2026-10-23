package com.example.ui.engagement

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.TimeUnit

class EngagementManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("shelflife_engagement_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAUNCH_COUNT = "key_launch_count"
        private const val KEY_RESCUED_COUNT = "key_rescued_count"
        private const val KEY_LAST_RATING_PROMPT = "key_last_rating_prompt"
        private const val KEY_HAS_RATED = "key_has_rated"
        private const val KEY_RATING_DISMISS_COUNT = "key_rating_dismiss_count"
        private const val KEY_NEVER_RATE = "key_never_rate"

        private const val KEY_LAST_SHARE_PROMPT = "key_last_share_prompt"
        private const val KEY_HAS_SHARED = "key_has_shared"

        private const val KEY_DISMISSED_UPDATE_VERSION = "key_dismissed_update_version"

        private val COOLDOWN_DAYS_RATING = TimeUnit.DAYS.toMillis(7)
        private val COOLDOWN_DAYS_SHARE = TimeUnit.DAYS.toMillis(10)
    }

    fun recordLaunch() {
        val current = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        prefs.edit().putInt(KEY_LAUNCH_COUNT, current + 1).apply()
    }

    fun recordRescuedItem() {
        val current = prefs.getInt(KEY_RESCUED_COUNT, 0)
        prefs.edit().putInt(KEY_RESCUED_COUNT, current + 1).apply()
    }

    fun shouldShowRatingPrompt(): Boolean {
        if (prefs.getBoolean(KEY_HAS_RATED, false)) return false
        if (prefs.getBoolean(KEY_NEVER_RATE, false)) return false

        val dismissCount = prefs.getInt(KEY_RATING_DISMISS_COUNT, 0)
        if (dismissCount >= 3) return false

        val launches = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        val rescued = prefs.getInt(KEY_RESCUED_COUNT, 0)

        // Only show if user had positive experience (>= 3 items rescued OR >= 5 launches)
        val hasMetThreshold = rescued >= 3 || launches >= 5
        if (!hasMetThreshold) return false

        val lastPrompt = prefs.getLong(KEY_LAST_RATING_PROMPT, 0L)
        val now = System.currentTimeMillis()
        return (now - lastPrompt) > COOLDOWN_DAYS_RATING
    }

    fun markRatingPromptShown() {
        prefs.edit().putLong(KEY_LAST_RATING_PROMPT, System.currentTimeMillis()).apply()
    }

    fun onUserRated() {
        prefs.edit().putBoolean(KEY_HAS_RATED, true).apply()
    }

    fun onUserDismissedRating(neverAskAgain: Boolean) {
        val dismissCount = prefs.getInt(KEY_RATING_DISMISS_COUNT, 0)
        prefs.edit()
            .putInt(KEY_RATING_DISMISS_COUNT, dismissCount + 1)
            .putBoolean(KEY_NEVER_RATE, neverAskAgain)
            .putLong(KEY_LAST_RATING_PROMPT, System.currentTimeMillis())
            .apply()
    }

    fun shouldShowSharePrompt(): Boolean {
        if (prefs.getBoolean(KEY_HAS_SHARED, false)) return false

        val rescued = prefs.getInt(KEY_RESCUED_COUNT, 0)
        // Show after user rescued 5 items (significant milestone)
        if (rescued < 5) return false

        val lastPrompt = prefs.getLong(KEY_LAST_SHARE_PROMPT, 0L)
        val now = System.currentTimeMillis()
        return (now - lastPrompt) > COOLDOWN_DAYS_SHARE
    }

    fun markSharePromptShown() {
        prefs.edit().putLong(KEY_LAST_SHARE_PROMPT, System.currentTimeMillis()).apply()
    }

    fun onUserShared() {
        prefs.edit().putBoolean(KEY_HAS_SHARED, true).apply()
    }

    fun onUserDismissedShare() {
        prefs.edit().putLong(KEY_LAST_SHARE_PROMPT, System.currentTimeMillis()).apply()
    }

    fun shouldShowUpdatePrompt(currentVersion: String, latestVersion: String): Boolean {
        if (currentVersion == latestVersion) return false
        val dismissedVersion = prefs.getString(KEY_DISMISSED_UPDATE_VERSION, "")
        return dismissedVersion != latestVersion
    }

    fun onUserDismissedUpdate(latestVersion: String) {
        prefs.edit().putString(KEY_DISMISSED_UPDATE_VERSION, latestVersion).apply()
    }
}
