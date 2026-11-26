package com.npcg.practica1.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.RoomDatabase
import com.npcg.practica1.data.local.dao.InventarioDao
import com.npcg.practica1.data.local.dao.PreferenciasAlimentariasDao
import com.npcg.practica1.data.local.dao.IngredienteDao
import com.npcg.practica1.data.local.dao.RecetaDao
import com.npcg.practica1.data.local.dao.RecetaIngredienteDao
import com.npcg.practica1.data.local.dao.UsuarioDao
import com.npcg.practica1.data.local.entity.IngredienteEntity
import com.npcg.practica1.data.local.entity.IngredienteInventarioEntity
import com.npcg.practica1.data.local.entity.PreferenciaAlimentariaEntity
import com.npcg.practica1.data.local.entity.PreferenciaItemEntity
import com.npcg.practica1.data.local.entity.RecetaEntity
import com.npcg.practica1.data.local.entity.RecetaIngredienteEntity
import com.npcg.practica1.data.local.entity.UsuarioEntity

@Database(
    entities = [
        UsuarioEntity::class,
        RecetaEntity::class,
        IngredienteEntity::class,
        RecetaIngredienteEntity::class,
        IngredienteInventarioEntity::class,
        PreferenciaAlimentariaEntity::class,
        PreferenciaItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class WelliDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun recetaDao(): RecetaDao
    abstract fun ingredienteDao(): IngredienteDao
    abstract fun recetaIngredienteDao(): RecetaIngredienteDao
    abstract fun inventarioDao(): InventarioDao
    abstract fun preferenciasAlimentariasDao(): PreferenciasAlimentariasDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `inventario_ingrediente` (
                        `id_ingrediente_inventario` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `id_usuario` INTEGER NOT NULL,
                        `nombre` TEXT NOT NULL,
                        `nombre_normalizado` TEXT NOT NULL,
                        `cantidad` REAL NOT NULL,
                        `unidad` TEXT NOT NULL,
                        `categoria` TEXT NOT NULL,
                        `fecha_registro` INTEGER NOT NULL,
                        FOREIGN KEY(`id_usuario`) REFERENCES `usuario`(`id_usuario`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventario_ingrediente_id_usuario` ON `inventario_ingrediente` (`id_usuario`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventario_ingrediente_id_usuario_categoria` ON `inventario_ingrediente` (`id_usuario`, `categoria`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_inventario_ingrediente_id_usuario_nombre_normalizado` ON `inventario_ingrediente` (`id_usuario`, `nombre_normalizado`)")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `preferencia_alimentaria` (
                        `id_usuario` INTEGER NOT NULL,
                        `tipo_alimentacion` TEXT NOT NULL,
                        `presupuesto` TEXT NOT NULL,
                        `porciones` INTEGER NOT NULL,
                        `fecha_actualizacion` INTEGER NOT NULL,
                        PRIMARY KEY(`id_usuario`),
                        FOREIGN KEY(`id_usuario`) REFERENCES `usuario`(`id_usuario`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `preferencia_item` (
                        `id_preferencia_item` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `id_usuario` INTEGER NOT NULL,
                        `tipo` TEXT NOT NULL,
                        `nombre` TEXT NOT NULL,
                        `nombre_normalizado` TEXT NOT NULL,
                        FOREIGN KEY(`id_usuario`) REFERENCES `usuario`(`id_usuario`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_preferencia_item_id_usuario` ON `preferencia_item` (`id_usuario`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_preferencia_item_id_usuario_tipo_nombre_normalizado` ON `preferencia_item` (`id_usuario`, `tipo`, `nombre_normalizado`)")
            }
        }
    }
}
