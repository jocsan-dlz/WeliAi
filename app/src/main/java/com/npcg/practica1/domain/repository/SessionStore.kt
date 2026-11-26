package com.npcg.practica1.domain.repository

import kotlinx.coroutines.flow.Flow

interface SessionStore {
    suspend fun saveUserId(userId: Int)
    suspend fun getUserId(): Int?
    fun observeUserId(): Flow<Int?>
    suspend fun clear()
}
