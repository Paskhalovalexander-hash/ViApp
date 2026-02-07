package com.example.vitanlyapp.domain.repository

import com.example.vitanlyapp.domain.model.KBJUData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Репозиторий данных КБЖУ. Domain-интерфейс без привязки к Android/UI/сети.
 */
interface KbjuRepository {

    fun getKbju(): StateFlow<KBJUData>

    /**
     * Получить КБЖУ за указанную дату как Flow.
     * @param date дата в формате "yyyy-MM-dd"
     */
    fun getKbjuForDateFlow(date: String): Flow<KBJUData>

    suspend fun updateKbju(data: KBJUData)
}
