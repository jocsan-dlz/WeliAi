package com.npcg.practica1.domain.repository

import com.npcg.practica1.domain.model.PreferenciasAlimentarias
import kotlinx.coroutines.flow.Flow

interface PreferenciasAlimentariasRepository {
    fun observarPreferencias(): Flow<PreferenciasAlimentarias>
    suspend fun guardarPreferencias(preferencias: PreferenciasAlimentarias): Result<Unit>
}
