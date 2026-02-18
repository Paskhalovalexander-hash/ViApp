package com.example.vitanlyapp.data.local

import androidx.room.TypeConverter
import com.example.vitanlyapp.data.local.entity.MealType
import com.example.vitanlyapp.data.local.entity.MessageRole
import com.example.vitanlyapp.domain.model.ActivityLevel
import com.example.vitanlyapp.domain.model.Gender
import com.example.vitanlyapp.domain.model.UserGoal

/**
 * TypeConverters для Room.
 * Преобразуют enum'ы в строки и обратно для хранения в SQLite.
 */
class Converters {

    // ══════════════════════════════════════════════════════════════════════════
    // Gender
    // ══════════════════════════════════════════════════════════════════════════

    @TypeConverter
    fun fromGender(value: Gender): String = value.name

    @TypeConverter
    fun toGender(value: String): Gender =
        Gender.entries.find { it.name == value } ?: Gender.MALE

    // ══════════════════════════════════════════════════════════════════════════
    // ActivityLevel
    // ══════════════════════════════════════════════════════════════════════════

    @TypeConverter
    fun fromActivityLevel(value: ActivityLevel): String = value.name

    @TypeConverter
    fun toActivityLevel(value: String): ActivityLevel =
        ActivityLevel.entries.find { it.name == value } ?: ActivityLevel.MODERATE

    // ══════════════════════════════════════════════════════════════════════════
    // UserGoal
    // ══════════════════════════════════════════════════════════════════════════

    @TypeConverter
    fun fromUserGoal(value: UserGoal): String = value.name

    @TypeConverter
    fun toUserGoal(value: String): UserGoal =
        UserGoal.entries.find { it.name == value } ?: UserGoal.MAINTAIN

    // ══════════════════════════════════════════════════════════════════════════
    // MealType
    // ══════════════════════════════════════════════════════════════════════════

    @TypeConverter
    fun fromMealType(value: MealType): String = value.name

    @TypeConverter
    fun toMealType(value: String): MealType =
        MealType.entries.find { it.name == value } ?: MealType.SNACK

    // ══════════════════════════════════════════════════════════════════════════
    // MessageRole
    // ══════════════════════════════════════════════════════════════════════════

    @TypeConverter
    fun fromMessageRole(value: MessageRole): String = value.name

    @TypeConverter
    fun toMessageRole(value: String): MessageRole =
        MessageRole.entries.find { it.name == value } ?: MessageRole.USER
}
