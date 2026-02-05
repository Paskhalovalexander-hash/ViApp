package com.example.vitanlyapp.domain.model

/**
 * Текущие и максимальные значения по КБЖУ (калории, белки, жиры, углеводы).
 */
data class KBJUData(
    val currentCalories: Int,
    val currentProtein: Int,
    val currentFat: Int,
    val currentCarbs: Int,
    val maxCalories: Int,
    val maxProtein: Int,
    val maxFat: Int,
    val maxCarbs: Int
) {
    companion object {
        fun default() = KBJUData(
            currentCalories = 0,
            currentProtein = 0,
            currentFat = 0,
            currentCarbs = 0,
            maxCalories = 2000,
            maxProtein = 100,
            maxFat = 70,
            maxCarbs = 250
        )
    }
}
