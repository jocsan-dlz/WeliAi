package com.npcg.practica1.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.npcg.practica1.data.local.entity.IngredienteEntity
import com.npcg.practica1.data.local.entity.RecetaEntity
import com.npcg.practica1.data.local.entity.UsuarioEntity

class WelliDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        const val DATABASE_NAME = "welli_database"
        const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Tabla USUARIO
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS usuario (
                id_usuario INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                correo TEXT NOT NULL UNIQUE,
                contrasena_hash TEXT NOT NULL,
                fecha_registro TEXT NOT NULL,
                estado INTEGER NOT NULL DEFAULT 1
            );
        """.trimIndent())

        // 2. Tabla RECETA
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS receta (
                id_receta INTEGER PRIMARY KEY AUTOINCREMENT,
                id_usuario INTEGER NOT NULL,
                nombre TEXT NOT NULL,
                descripcion TEXT,
                instrucciones TEXT,
                tiempo_preparacion INTEGER NOT NULL,
                presupuesto REAL NOT NULL,
                fecha_creacion TEXT NOT NULL,
                FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE
            );
        """.trimIndent())

        // 3. Tabla INGREDIENTE
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS ingrediente (
                id_ingrediente INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                categoria TEXT NOT NULL,
                unidad_medida TEXT NOT NULL
            );
        """.trimIndent())

        // 4. Tabla RECETA_INGREDIENTE
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS receta_ingrediente (
                id_receta_ingrediente INTEGER PRIMARY KEY AUTOINCREMENT,
                id_receta INTEGER NOT NULL,
                id_ingrediente INTEGER NOT NULL,
                cantidad REAL NOT NULL,
                unidad TEXT NOT NULL,
                FOREIGN KEY (id_receta) REFERENCES receta(id_receta) ON DELETE CASCADE,
                FOREIGN KEY (id_ingrediente) REFERENCES ingrediente(id_ingrediente) ON DELETE CASCADE
            );
        """.trimIndent())

        // Insertar usuario inicial (Jodiluz)
        val userId = insertInitialUser(db, "Jodiluz", "jodiluz777@gmail.com", "0203004yo//")

        // Insertar ingredientes iniciales
        insertInitialIngredients(db)

        // Insertar receta inicial
        insertInitialRecipe(db, userId.toInt())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS receta_ingrediente")
        db.execSQL("DROP TABLE IF EXISTS ingrediente")
        db.execSQL("DROP TABLE IF EXISTS receta")
        db.execSQL("DROP TABLE IF EXISTS usuario")
        onCreate(db)
    }

    private fun insertInitialUser(db: SQLiteDatabase, nombre: String, correo: String, contrasena: String): Long {
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("correo", correo)
            put("contrasena_hash", contrasena)
            put("fecha_registro", "2026-11-19 12:00:00")
            put("estado", 1)
        }
        return db.insertWithOnConflict("usuario", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun insertInitialIngredients(db: SQLiteDatabase) {
        val ingredients = listOf(
            Triple("Tomate", "Verduras", "uds"),
            Triple("Huevo", "Proteínas", "uds"),
            Triple("Queso", "Lácteos", "g"),
            Triple("Cebolla", "Verduras", "uds")
        )
        for ((nombre, cat, unidad) in ingredients) {
            val values = ContentValues().apply {
                put("nombre", nombre)
                put("categoria", cat)
                put("unidad_medida", unidad)
            }
            db.insert("ingrediente", null, values)
        }
    }

    private fun insertInitialRecipe(db: SQLiteDatabase, userId: Int) {
        val values = ContentValues().apply {
            put("id_usuario", if (userId > 0) userId else 1)
            put("nombre", "Tortilla Española Inteligente")
            put("descripcion", "Deliciosa tortilla rápida con huevo, queso y verduras.")
            put("instrucciones", "1. Batir los huevos. 2. Picar los tomates y cebollas. 3. Cocinar a fuego lento con queso.")
            put("tiempo_preparacion", 20)
            put("presupuesto", 45.0)
            put("fecha_creacion", "2026-11-19 12:05:00")
        }
        val recetaId = db.insert("receta", null, values)

        if (recetaId > 0) {
            val pivoteValues = ContentValues().apply {
                put("id_receta", recetaId)
                put("id_ingrediente", 1)
                put("cantidad", 2.0)
                put("unidad", "uds")
            }
            db.insert("receta_ingrediente", null, pivoteValues)
        }
    }

    fun validateUser(correo: String, contrasena: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM usuario WHERE correo = ? AND contrasena_hash = ?",
            arrayOf(correo.trim(), contrasena)
        )
        val isValid = cursor.count > 0
        cursor.close()
        return isValid
    }

    fun getUsuarioByCorreo(correo: String): UsuarioEntity? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM usuario WHERE correo = ?", arrayOf(correo.trim()))
        var user: UsuarioEntity? = null
        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndex("id_usuario")
            val nombreIndex = cursor.getColumnIndex("nombre")
            val correoIndex = cursor.getColumnIndex("correo")
            val passIndex = cursor.getColumnIndex("contrasena_hash")
            val fechaIndex = cursor.getColumnIndex("fecha_registro")
            val estadoIndex = cursor.getColumnIndex("estado")

            user = UsuarioEntity(
                id_usuario = cursor.getInt(idIndex),
                nombre = cursor.getString(nombreIndex),
                correo = cursor.getString(correoIndex),
                contrasena_hash = cursor.getString(passIndex),
                fecha_registro = cursor.getString(fechaIndex),
                estado = cursor.getInt(estadoIndex) == 1
            )
        }
        cursor.close()
        return user
    }

    fun getIngredientesList(): List<IngredienteEntity> {
        val db = readableDatabase
        val list = mutableListOf<IngredienteEntity>()
        val cursor = db.rawQuery("SELECT * FROM ingrediente", null)
        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndex("id_ingrediente")
            val nombreIndex = cursor.getColumnIndex("nombre")
            val catIndex = cursor.getColumnIndex("categoria")
            val unidadIndex = cursor.getColumnIndex("unidad_medida")

            do {
                list.add(
                    IngredienteEntity(
                        id_ingrediente = cursor.getInt(idIndex),
                        nombre = cursor.getString(nombreIndex),
                        categoria = cursor.getString(catIndex),
                        unidad_medida = cursor.getString(unidadIndex)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getRecetasList(): List<RecetaEntity> {
        val db = readableDatabase
        val list = mutableListOf<RecetaEntity>()
        val cursor = db.rawQuery("SELECT * FROM receta", null)
        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndex("id_receta")
            val userIdIndex = cursor.getColumnIndex("id_usuario")
            val nombreIndex = cursor.getColumnIndex("nombre")
            val descIndex = cursor.getColumnIndex("descripcion")
            val instIndex = cursor.getColumnIndex("instrucciones")
            val tiempoIndex = cursor.getColumnIndex("tiempo_preparacion")
            val presupIndex = cursor.getColumnIndex("presupuesto")
            val fechaIndex = cursor.getColumnIndex("fecha_creacion")

            do {
                list.add(
                    RecetaEntity(
                        id_receta = cursor.getInt(idIndex),
                        id_usuario = cursor.getInt(userIdIndex),
                        nombre = cursor.getString(nombreIndex),
                        descripcion = cursor.getString(descIndex),
                        instrucciones = cursor.getString(instIndex),
                        tiempo_preparacion = cursor.getInt(tiempoIndex),
                        presupuesto = cursor.getDouble(presupIndex),
                        fecha_creacion = cursor.getString(fechaIndex)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}
