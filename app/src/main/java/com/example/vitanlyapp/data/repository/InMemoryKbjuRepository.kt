package com.example.vitanlyapp.data.repository

import com.example.vitanlyapp.domain.model.KBJUData
import com.example.vitanlyapp.domain.repository.KbjuRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * In-memory реализация KbjuRepository.
 */
class InMemoryKbjuRepository @Inject constructor() : KbjuRepository {

    private val _data = MutableStateFlow(KBJUData.default())

    override fun getKbju(): StateFlow<KBJUData> = _data.asStateFlow()

    override fun getKbjuForDateFlow(date: String): Flow<KBJUData> = flowOf(_data.value)

    override suspend fun updateKbju(data: KBJUData) {
        _data.value = data
    }
}
