package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिंदी"),
    BENGALI("bn", "বাংলা")
}

class SettingsRepository(context: Context) {
    companion object {
        private const val PREFS_NAME = "pdf_utility_settings"
        private const val KEY_THEME = "app_theme_mode"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_FIRST_LAUNCH = "first_launch_done"
        private const val KEY_GUIDE_VIDEOS_ENABLED = "guide_videos_enabled"
        private const val KEY_GUIDE_VIDEO_ASKED = "guide_video_asked"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        try {
            val savedTheme = prefs.getString(KEY_THEME, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
            AppThemeMode.valueOf(savedTheme)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(!prefs.getBoolean(KEY_FIRST_LAUNCH, false))
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    private val _guideVideosEnabled = MutableStateFlow(prefs.getBoolean(KEY_GUIDE_VIDEOS_ENABLED, false))
    val guideVideosEnabled: StateFlow<Boolean> = _guideVideosEnabled.asStateFlow()

    private val _isGuideVideoPreferenceAsked = MutableStateFlow(prefs.getBoolean(KEY_GUIDE_VIDEO_ASKED, false))
    val isGuideVideoPreferenceAsked: StateFlow<Boolean> = _isGuideVideoPreferenceAsked.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    fun setGuideVideosEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_GUIDE_VIDEOS_ENABLED, enabled)
            .putBoolean(KEY_GUIDE_VIDEO_ASKED, true)
            .apply()
        _guideVideosEnabled.value = enabled
        _isGuideVideoPreferenceAsked.value = true
    }

    fun completeFirstLaunch(enableGuideVideos: Boolean? = null) {
        val editor = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, true)
        if (enableGuideVideos != null) {
            editor.putBoolean(KEY_GUIDE_VIDEOS_ENABLED, enableGuideVideos)
            editor.putBoolean(KEY_GUIDE_VIDEO_ASKED, true)
            _guideVideosEnabled.value = enableGuideVideos
            _isGuideVideoPreferenceAsked.value = true
        }
        editor.apply()
        _isFirstLaunch.value = false
    }
}
