package com.npcg.practica1.data.repository

import com.npcg.practica1.data.local.dao.PreferenciasAlimentariasDao
import com.npcg.practica1.data.local.dao.UsuarioDao
import com.npcg.practica1.data.mapper.toEntity
import com.npcg.practica1.data.mapper.toItems
import com.npcg.practica1.data.mapper.toPreferenciasDomain
import com.npcg.practica1.data.util.displayLabel
import com.npcg.practica1.data.util.normalizedLabel
import com.npcg.practica1.domain.model.PresupuestosAlimentarios
import com.npcg.practica1.domain.model.PreferenciasAlimentarias
import com.npcg.practica1.domain.model.TiposAlimentacion
import com.npcg.practica1.domain.repository.PreferenciasAlimentariasRepository
import com.npcg.practica1.domain.repository.SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class PreferenciasAlimentariasRepositoryImpl @Inject constructor(
    private val preferenciasDao: PreferenciasAlimentariasDao,
    private val usuarioDao: UsuarioDao,
    private val sessionStore: SessionStore
) : PreferenciasAlimentariasRepository {
    override fun observarPreferencias(): Flow<PreferenciasAlimentarias> =
        observeActiveUserId(sessionStore, usuarioDao).flatMapLatest { userId ->
            if (userId == null) flowOf(PreferenciasAlimentarias())
            else combine(
                preferenciasDao.observeForUser(userId),
                preferenciasDao.observeItemsForUser(userId)
            ) { preference, items -> toPreferenciasDomain(preference, items) }
        }

    override suspend fun guardarPreferencias(preferencias: PreferenciasAlimentarias): Result<Unit> = try {
        require(preferencias.tipoAlimentacion in TiposAlimentacion.all) { "Selecciona un tipo de alimentación válido." }
        require(preferencias.presupuesto in PresupuestosAlimentarios.all) { "Selecciona un presupuesto válido." }
        require(preferencias.porcionesHabituales in 1..20) { "Las porciones deben estar entre 1 y 20." }

        val cleanRestrictions = cleanItems(preferencias.restricciones)
        val cleanAllergies = cleanItems(preferencias.alergias)
        require((cleanRestrictions + cleanAllergies).none { it.isBlank() }) { "Los nombres de alergias y restricciones son obligatorios." }
        require((cleanRestrictions + cleanAllergies).all { it.length <= 60 }) { "Cada elemento debe tener 60 caracteres o menos." }

        val ownerId = requireActiveUserId(sessionStore, usuarioDao)
        val clean = preferencias.copy(restricciones = cleanRestrictions, alergias = cleanAllergies)
        preferenciasDao.saveForUser(
            preferencias = clean.toEntity(ownerId, System.currentTimeMillis()),
            items = clean.toItems(ownerId, ::normalizedLabel)
        )
        Result.success(Unit)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }

    private fun cleanItems(values: List<String>): List<String> {
        require(values.all(String::isNotBlank)) { "Los nombres de alergias y restricciones son obligatorios." }
        val cleaned = values.map(::displayLabel)
        require(cleaned.all { it.length <= 60 }) { "Cada elemento debe tener 60 caracteres o menos." }
        return cleaned.distinctBy(::normalizedLabel)
    }
}
