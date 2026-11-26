package com.npcg.practica1.domain.repository

interface SessionStore {
    suspend fun saveUserId(userId: Int)
    suspend fun getUserId(): Int?
    suspend fun clear()
}
