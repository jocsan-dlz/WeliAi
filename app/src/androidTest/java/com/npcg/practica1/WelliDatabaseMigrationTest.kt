package com.npcg.practica1

import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.npcg.practica1.data.local.WelliDatabase
import com.npcg.practica1.data.local.entity.IngredienteInventarioEntity
import com.npcg.practica1.data.local.entity.PreferenciaAlimentariaEntity
import com.npcg.practica1.data.local.entity.PreferenciaItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class WelliDatabaseMigrationTest {
    @Test
    fun migrationFromVersionOneKeepsFourTablesAndCreatesUserDataTables() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "welli-migration-${UUID.randomUUID()}.db"
        val oldDatabase = Room.databaseBuilder(context, LegacyWelliDatabase::class.java, databaseName).build()

        oldDatabase.openHelper.writableDatabase.execSQL(
            "INSERT INTO usuario (id_usuario,nombre,correo,contrasena_hash,fecha_registro,estado) VALUES (4,'Test','test@example.com','hash','2026-11-26',1)"
        )
        oldDatabase.openHelper.writableDatabase.execSQL(
            "INSERT INTO receta (id_receta,id_usuario,nombre,descripcion,instrucciones,tiempo_preparacion,presupuesto,fecha_creacion) VALUES (7,4,'Sopa',NULL,NULL,20,10.0,'2026-11-26')"
        )
        oldDatabase.openHelper.writableDatabase.execSQL(
            "INSERT INTO ingrediente (id_ingrediente,nombre,categoria,unidad_medida) VALUES (8,'Papa','Verduras','pieza')"
        )
        oldDatabase.openHelper.writableDatabase.execSQL(
            "INSERT INTO receta_ingrediente (id_receta_ingrediente,id_receta,id_ingrediente,cantidad,unidad) VALUES (9,7,8,2.0,'pieza')"
        )
        oldDatabase.close()

        val database = Room.databaseBuilder(context, WelliDatabase::class.java, databaseName)
            .addMigrations(WelliDatabase.MIGRATION_1_2)
            .build()
        val sqlite = database.openHelper.writableDatabase
        assertEquals(1, sqlite.countRows("usuario"))
        assertEquals(1, sqlite.countRows("receta"))
        assertEquals(1, sqlite.countRows("ingrediente"))
        assertEquals(1, sqlite.countRows("receta_ingrediente"))
        assertEquals(0, sqlite.countRows("inventario_ingrediente"))
        assertEquals(0, sqlite.countRows("preferencia_alimentaria"))
        assertEquals(0, sqlite.countRows("preferencia_item"))

        val insertedId = database.inventarioDao().insert(
            IngredienteInventarioEntity(
                id_usuario = 4,
                nombre = "Aguacate",
                nombre_normalizado = "aguacate",
                cantidad = 2.0,
                unidad = "pieza",
                categoria = "Frutas",
                fecha_registro = 1L
            )
        )
        assertEquals(1, database.inventarioDao().observeForUser(4, "", null).first().size)
        assertEquals("Test", database.usuarioDao().getUsuarioById(4)?.nombre)
        assertEquals(1L, insertedId)

        database.preferenciasAlimentariasDao().saveForUser(
            PreferenciaAlimentariaEntity(4, "Vegana", "Ahorro", 3, 1L),
            listOf(PreferenciaItemEntity(id_usuario = 4, tipo = "alergia", nombre = "Nuez", nombre_normalizado = "nuez"))
        )

        database.close()
        val reopenedDatabase = Room.databaseBuilder(context, WelliDatabase::class.java, databaseName)
            .addMigrations(WelliDatabase.MIGRATION_1_2)
            .build()
        assertEquals(1, reopenedDatabase.inventarioDao().observeForUser(4, "", null).first().size)
        assertEquals("Vegana", reopenedDatabase.preferenciasAlimentariasDao().observeForUser(4).first()?.tipo_alimentacion)
        assertEquals("Nuez", reopenedDatabase.preferenciasAlimentariasDao().observeItemsForUser(4).first().single().nombre)
        reopenedDatabase.close()
        context.deleteDatabase(databaseName)
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.countRows(table: String): Int =
        query("SELECT COUNT(*) FROM `$table`").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
}

@Database(
    entities = [
        LegacyUsuarioEntity::class,
        LegacyRecetaEntity::class,
        LegacyIngredienteEntity::class,
        LegacyRecetaIngredienteEntity::class
    ],
    version = 1,
    exportSchema = false
)
private abstract class LegacyWelliDatabase : RoomDatabase()

@Entity(tableName = "usuario", indices = [Index(value = ["correo"], unique = true)])
private data class LegacyUsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id_usuario: Int = 0,
    val nombre: String,
    val correo: String,
    val contrasena_hash: String,
    val fecha_registro: String,
    val estado: Boolean = true
)

@Entity(
    tableName = "receta",
    foreignKeys = [ForeignKey(entity = LegacyUsuarioEntity::class, parentColumns = ["id_usuario"], childColumns = ["id_usuario"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.NO_ACTION)],
    indices = [Index(value = ["id_usuario"])]
)
private data class LegacyRecetaEntity(
    @PrimaryKey(autoGenerate = true) val id_receta: Int = 0,
    val id_usuario: Int,
    val nombre: String,
    val descripcion: String?,
    val instrucciones: String?,
    val tiempo_preparacion: Int,
    val presupuesto: Double,
    val fecha_creacion: String
)

@Entity(tableName = "ingrediente", indices = [Index(value = ["nombre"], unique = true)])
private data class LegacyIngredienteEntity(
    @PrimaryKey(autoGenerate = true) val id_ingrediente: Int = 0,
    val nombre: String,
    val categoria: String,
    val unidad_medida: String
)

@Entity(
    tableName = "receta_ingrediente",
    foreignKeys = [
        ForeignKey(entity = LegacyRecetaEntity::class, parentColumns = ["id_receta"], childColumns = ["id_receta"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.NO_ACTION),
        ForeignKey(entity = LegacyIngredienteEntity::class, parentColumns = ["id_ingrediente"], childColumns = ["id_ingrediente"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.NO_ACTION)
    ],
    indices = [Index(value = ["id_receta", "id_ingrediente"], unique = true), Index(value = ["id_ingrediente"])]
)
private data class LegacyRecetaIngredienteEntity(
    @PrimaryKey(autoGenerate = true) val id_receta_ingrediente: Int = 0,
    val id_receta: Int,
    val id_ingrediente: Int,
    val cantidad: Double,
    val unidad: String
)
