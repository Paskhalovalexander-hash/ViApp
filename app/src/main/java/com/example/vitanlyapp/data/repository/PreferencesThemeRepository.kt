package com.example.vitanlyapp.data.repository

import android.content.Context
import com.example.vitanlyapp.domain.model.ThemeMode
import com.example.vitanlyapp.domain.repository.ThemeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

private const val PREFS_NAME = "vitanly_prefs"
private const val KEY_THEME_MODE = "theme_mode"

class PreferencesThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : ThemeRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())

    override val themeMode: Flow<ThemeMode> = _themeMode.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    private fun loadThemeMode(): ThemeMode {
        return prefs.getString(KEY_THEME_MODE, ThemeMode.MATTE_DARK.name)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.MATTE_DARK
    }
}
