package com.npcg.practica1.di

import android.content.Context
import androidx.room.Room
import com.npcg.practica1.data.local.WelliDatabase
import com.npcg.practica1.data.local.dao.IngredienteDao
import com.npcg.practica1.data.local.dao.InventarioDao
import com.npcg.practica1.data.local.dao.PreferenciasAlimentariasDao
import com.npcg.practica1.data.local.dao.RecetaDao
import com.npcg.practica1.data.local.dao.RecetaIngredienteDao
import com.npcg.practica1.data.local.dao.UsuarioDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val DATABASE_NAME = "welli_room_database"

    @Provides
    @Singleton
    fun provideWelliDatabase(@ApplicationContext context: Context): WelliDatabase =
        Room.databaseBuilder(context, WelliDatabase::class.java, DATABASE_NAME)
            .addMigrations(WelliDatabase.MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideUsuarioDao(database: WelliDatabase): UsuarioDao = database.usuarioDao()

    @Provides
    @Singleton
    fun provideRecetaDao(database: WelliDatabase): RecetaDao = database.recetaDao()

    @Provides
    @Singleton
    fun provideIngredienteDao(database: WelliDatabase): IngredienteDao = database.ingredienteDao()

    @Provides
    @Singleton
    fun provideRecetaIngredienteDao(database: WelliDatabase): RecetaIngredienteDao =
        database.recetaIngredienteDao()

    @Provides
    @Singleton
    fun provideInventarioDao(database: WelliDatabase): InventarioDao = database.inventarioDao()

    @Provides
    @Singleton
    fun providePreferenciasAlimentariasDao(database: WelliDatabase): PreferenciasAlimentariasDao =
        database.preferenciasAlimentariasDao()
}
