package com.example.vitanlyapp.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "vitanly_prefs"
private const val KEY_HAS_PROFILE = "has_profile"

/**
 * Быстрый кеш для проверки наличия профиля при запуске.
 * Позволяет избежать обращения к Room при холодном старте.
 */
@Singleton
class StartupPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Быстрая проверка наличия профиля (без Room запроса).
     */
    fun hasProfile(): Boolean {
        return prefs.getBoolean(KEY_HAS_PROFILE, false)
    }

    /**
     * Установить флаг наличия профиля.
     * Вызывать после успешного создания профиля.
     */
    fun setHasProfile(hasProfile: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_PROFILE, hasProfile).apply()
    }
}
