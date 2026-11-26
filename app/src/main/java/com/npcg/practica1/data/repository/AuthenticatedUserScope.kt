package com.npcg.practica1.data.repository

import com.npcg.practica1.data.local.dao.UsuarioDao
import com.npcg.practica1.domain.model.SesionNoValidaException
import com.npcg.practica1.domain.repository.SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest

internal suspend fun requireActiveUserId(sessionStore: SessionStore, usuarioDao: UsuarioDao): Int {
    val userId = sessionStore.getUserId() ?: throw SesionNoValidaException()
    if (usuarioDao.getUsuarioById(userId)?.estado != true) throw SesionNoValidaException()
    return userId
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun observeActiveUserId(sessionStore: SessionStore, usuarioDao: UsuarioDao): Flow<Int?> =
    sessionStore.observeUserId().distinctUntilChanged().mapLatest { userId ->
        if (userId == null || usuarioDao.getUsuarioById(userId)?.estado != true) null else userId
    }
