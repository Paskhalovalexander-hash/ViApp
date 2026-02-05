package com.example.vitanlyapp.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Команда от AI-агента для выполнения действия в приложении.
 *
 * AI возвращает команды в формате:
 * ```json
 * {"type": "set_weight", "value": 85.5}
 * ```
 *
 * Типы команд:
 * - Профиль: set_weight, set_height, set_age, set_gender, set_activity
 * - Цели: set_goal, set_target_weight, set_tempo
 * - Еда: add_food, delete_food, delete_meal, clear_day
 */
@Serializable
sealed class AgentCommand {

    // ══════════════════════════════════════════════════════════════════════════
    // Команды профиля
    // ══════════════════════════════════════════════════════════════════════════

    /** Установить текущий вес (кг) */
    @Serializable
    @SerialName("set_weight")
    data class SetWeight(val value: Float) : AgentCommand()

    /** Установить рост (см) */
    @Serializable
    @SerialName("set_height")
    data class SetHeight(val value: Int) : AgentCommand()

    /** Установить возраст (лет) */
    @Serializable
    @SerialName("set_age")
    data class SetAge(val value: Int) : AgentCommand()

    /** Установить пол (male/female) */
    @Serializable
    @SerialName("set_gender")
    data class SetGender(val value: Gender) : AgentCommand()

    /** Установить уровень активности (sedentary/light/moderate/active/very_active) */
    @Serializable
    @SerialName("set_activity")
    data class SetActivity(val value: ActivityLevel) : AgentCommand()

    // ══════════════════════════════════════════════════════════════════════════
    // Команды целей
    // ══════════════════════════════════════════════════════════════════════════

    /** Установить цель (lose/gain/maintain) */
    @Serializable
    @SerialName("set_goal")
    data class SetGoal(val value: UserGoal) : AgentCommand()

    /** Установить целевой вес (кг) */
    @Serializable
    @SerialName("set_target_weight")
    data class SetTargetWeight(val value: Float) : AgentCommand()

    /** Установить темп изменения веса (кг/неделя) */
    @Serializable
    @SerialName("set_tempo")
    data class SetTempo(val value: Float) : AgentCommand()

    // ══════════════════════════════════════════════════════════════════════════
    // Команды еды
    // ══════════════════════════════════════════════════════════════════════════

    /** Добавить запись о еде (данные в food_entries) */
    @Serializable
    @SerialName("add_food")
    data object AddFood : AgentCommand()

    /** Удалить запись о еде по названию */
    @Serializable
    @SerialName("delete_food")
    data class DeleteFood(val name: String) : AgentCommand()

    /** Удалить весь приём пищи по session_id */
    @Serializable
    @SerialName("delete_meal")
    data class DeleteMeal(val session_id: Long) : AgentCommand()

    /** Очистить все записи за текущий день */
    @Serializable
    @SerialName("clear_day")
    data object ClearDay : AgentCommand()

    // ══════════════════════════════════════════════════════════════════════════
    // Команды прямого управления (UI-действия)
    // ══════════════════════════════════════════════════════════════════════════

    /** Удалить запись о еде по ID (для UI-действий) */
    @Serializable
    @SerialName("delete_food_by_id")
    data class DeleteFoodById(val id: Long) : AgentCommand()

    /** Повторить запись о еде (создать копию в текущей сессии) */
    @Serializable
    @SerialName("repeat_food")
    data class RepeatFood(val id: Long) : AgentCommand()

    /** Изменить вес записи (пропорционально пересчитать КБЖУ) */
    @Serializable
    @SerialName("update_food_weight")
    data class UpdateFoodWeight(
        val id: Long,
        val newWeightGrams: Int
    ) : AgentCommand()
}
