# Seguridad de cuentas y datos locales

## Contraseñas

La base activa de Room guarda `pbkdf2:<iteraciones>:<salt-hex>:<hash-hex>` con PBKDF2-HMAC-SHA256, salt aleatorio de 16 bytes y 600 000 iteraciones. El inicio de sesión acepta únicamente ese formato con longitudes y rangos válidos; valores en texto plano y formatos antiguos se rechazan. El hash se queda en `UsuarioEntity` y nunca se convierte en el modelo de dominio. El registro y el cambio de contraseña requieren al menos ocho caracteres.

## Cuentas heredadas

La versión anterior usaba `SQLiteOpenHelper` y guardaba la contraseña inicial en texto plano. Esas credenciales no se importan ni se validan. La app usa ahora un archivo de Room distinto (`welli_room_database`), por lo que los datos antiguos permanecen aislados en el archivo `welli_database` existente en instalaciones previas.

La recuperación segura debe requerir una verificación de identidad independiente de la contraseña antigua y establecer una contraseña nueva, guardada con el formato PBKDF2 actual. Si no existe un canal de verificación confiable, se debe pedir crear una cuenta nueva y eliminar los datos locales heredados después de confirmar que no se necesitan. Nunca se debe comparar la contraseña presentada contra el valor heredado ni convertir ese valor automáticamente en un hash.

## Esquema y actualización

Room mantiene cuatro tablas: `usuario`, `receta`, `ingrediente` y `receta_ingrediente`. El archivo Room inicia en versión 1 porque la app anterior no tenía una base de Room ni metadatos Room que se puedan migrar de forma segura. Para cambios futuros de esquema, aumentar la versión y agregar una migración explícita; no habilitar `fallbackToDestructiveMigration` para evitar pérdidas accidentales.
