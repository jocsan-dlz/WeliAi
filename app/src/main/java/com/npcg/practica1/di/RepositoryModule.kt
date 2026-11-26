package com.npcg.practica1.di

import com.npcg.practica1.data.repository.IngredienteRepositoryImpl
import com.npcg.practica1.data.repository.InventarioRepositoryImpl
import com.npcg.practica1.data.repository.PreferenciasAlimentariasRepositoryImpl
import com.npcg.practica1.data.remote.WelliAiRepositoryImpl
import com.npcg.practica1.data.repository.RecetaIngredienteRepositoryImpl
import com.npcg.practica1.data.repository.RecetaRepositoryImpl
import com.npcg.practica1.data.repository.UsuarioRepositoryImpl
import com.npcg.practica1.domain.repository.IngredienteRepository
import com.npcg.practica1.domain.repository.InventarioRepository
import com.npcg.practica1.domain.repository.PreferenciasAlimentariasRepository
import com.npcg.practica1.domain.repository.WelliAiRepository
import com.npcg.practica1.domain.repository.RecetaIngredienteRepository
import com.npcg.practica1.domain.repository.RecetaRepository
import com.npcg.practica1.domain.repository.UsuarioRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindUsuarioRepository(implementation: UsuarioRepositoryImpl): UsuarioRepository

    @Binds
    @Singleton
    abstract fun bindRecetaRepository(implementation: RecetaRepositoryImpl): RecetaRepository

    @Binds
    @Singleton
    abstract fun bindIngredienteRepository(implementation: IngredienteRepositoryImpl): IngredienteRepository

    @Binds
    @Singleton
    abstract fun bindInventarioRepository(implementation: InventarioRepositoryImpl): InventarioRepository

    @Binds
    @Singleton
    abstract fun bindPreferenciasAlimentariasRepository(
        implementation: PreferenciasAlimentariasRepositoryImpl
    ): PreferenciasAlimentariasRepository

    @Binds
    @Singleton
    abstract fun bindWelliAiRepository(implementation: WelliAiRepositoryImpl): WelliAiRepository

    @Binds
    @Singleton
    abstract fun bindRecetaIngredienteRepository(
        implementation: RecetaIngredienteRepositoryImpl
    ): RecetaIngredienteRepository
}
