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
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        AppThemeMode.valueOf(prefs.getString(KEY_THEME, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name)
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(!prefs.getBoolean(KEY_FIRST_LAUNCH, false))
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    fun completeFirstLaunch() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, true).apply()
        _isFirstLaunch.value = false
    }
}
