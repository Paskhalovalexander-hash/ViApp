package com.example.vitanlyapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.vitanlyapp.data.local.dao.ChatMessageDao
import com.example.vitanlyapp.data.local.dao.DayEntryDao
import com.example.vitanlyapp.data.local.dao.UserProfileDao
import com.example.vitanlyapp.data.local.entity.ChatMessageEntity
import com.example.vitanlyapp.data.local.entity.DayEntryEntity
import com.example.vitanlyapp.data.local.entity.UserProfileEntity

/**
 * Room Database для VitanlyApp.
 *
 * Таблицы:
 * - user_profile: профиль пользователя (параметры для расчёта норм)
 * - day_entries: записи о еде за день
 * - chat_messages: история чата с AI
 *
 * Связи:
 * - UserProfile (1) → (N) DayEntry
 * - UserProfile (1) → (N) ChatMessage
 */
@Database(
    entities = [
        UserProfileEntity::class,
        DayEntryEntity::class,
        ChatMessageEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class VitanlyDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao

    abstract fun dayEntryDao(): DayEntryDao

    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        const val DATABASE_NAME = "vitanly_database"

        /**
         * Миграция 1 → 2: добавление emoji и mealSessionId в day_entries.
         * - emoji: для отображения иконки продукта (генерируется AI)
         * - mealSessionId: для группировки продуктов по приёмам пищи (30 мин окно)
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Добавляем столбец emoji с дефолтным значением
                db.execSQL("ALTER TABLE day_entries ADD COLUMN emoji TEXT NOT NULL DEFAULT '🍽️'")
                // Добавляем столбец mealSessionId (для существующих записей = createdAt)
                db.execSQL("ALTER TABLE day_entries ADD COLUMN mealSessionId INTEGER NOT NULL DEFAULT 0")
                // Устанавливаем mealSessionId = createdAt для существующих записей
                db.execSQL("UPDATE day_entries SET mealSessionId = createdAt")
                // Создаём индекс для быстрой группировки
                db.execSQL("CREATE INDEX IF NOT EXISTS index_day_entries_mealSessionId ON day_entries(mealSessionId)")
            }
        }
    }
}
