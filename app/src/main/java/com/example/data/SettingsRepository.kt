package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.theme.AppColorTheme
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
        private const val KEY_COLOR_THEME = "app_color_theme"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_FIRST_LAUNCH = "first_launch_done"
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

    private val _colorTheme = MutableStateFlow(
        try {
            val savedColorTheme = prefs.getString(KEY_COLOR_THEME, AppColorTheme.PROFESSIONAL_BLUE.id)
            AppColorTheme.fromId(savedColorTheme)
        } catch (_: Exception) {
            AppColorTheme.PROFESSIONAL_BLUE
        }
    )
    val colorTheme: StateFlow<AppColorTheme> = _colorTheme.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(!prefs.getBoolean(KEY_FIRST_LAUNCH, false))
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    fun setColorTheme(theme: AppColorTheme) {
        prefs.edit().putString(KEY_COLOR_THEME, theme.id).apply()
        _colorTheme.value = theme
    }

    fun completeFirstLaunch() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, true).apply()
        _isFirstLaunch.value = false
    }
}
