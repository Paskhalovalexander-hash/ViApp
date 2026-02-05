package com.example.vitanlyapp.domain.repository

import com.example.vitanlyapp.domain.model.KBJUData
import kotlinx.coroutines.flow.StateFlow

/**
 * Репозиторий данных КБЖУ. Domain-интерфейс без привязки к Android/UI/сети.
 */
interface KbjuRepository {

    fun getKbju(): StateFlow<KBJUData>

    suspend fun updateKbju(data: KBJUData)
}
