# Integración de WelliAI con OpenAI

Sprint 7 añade generación de recetas y chat de cocina. Android nunca llama directamente a OpenAI: la clave vive únicamente en el backend local.

## Arquitectura

```text
Jetpack Compose → ViewModel → UseCase → WelliAiRepository → Retrofit/OkHttp
                                                        ↓
                                           welliai-backend (Ktor)
                                                        ↓
                                           OpenAI Responses API
```

La aplicación lee inventario y preferencias de Room mediante los repositorios existentes. Solo envía al backend los ingredientes seleccionados, cantidades, unidades, categorías, presupuesto, tipo de alimentación, restricciones, alergias y porciones. La sesión y el identificador local del usuario no se envían al backend.

El backend está limitado a loopback (`127.0.0.1`/`localhost`/`::1`) porque la autenticación actual de WelliAI es local y el backend no puede verificarla. La configuración HTTPS de Android para release deja preparada la URL del servidor futuro, pero no convierte este backend local en un servicio apto para Internet.

## Configuración local del backend

Requisitos: JDK 21 y el wrapper Gradle incluido en la raíz del proyecto. No hay una segunda copia del wrapper dentro del backend.

1. Pide al titular de la cuenta o a una persona adulta responsable que revoque cualquier clave compartida en un chat, repositorio o captura y configure una nueva antes de continuar.
2. Copia `welliai-backend/.env.example` como `welliai-backend/.env`.
3. En `.env`, define `OPENAI_API_KEY` con la clave nueva y `OPENAI_MODEL` con un identificador habilitado en tu cuenta que sea compatible con Responses API y salidas estructuradas. El backend no elige un modelo por ti.
4. Ejecuta desde la carpeta raíz:

```powershell
Copy-Item welliai-backend/.env.example welliai-backend/.env
notepad welliai-backend/.env
.\gradlew.bat -p welliai-backend run
```

El archivo `.env` está ignorado por Git. No pegues la clave en Android Studio, Kotlin, `BuildConfig`, `local.properties`, pruebas o logs. No la incluyas en capturas. El uso de la API puede generar cargos; la persona titular de la cuenta debe revisar su uso y límites.

Si prefieres variables del proceso en vez del archivo local, configúralas solo en una terminal privada y después ejecuta el mismo comando:

```powershell
$env:OPENAI_API_KEY = '<CLAVE_NUEVA_CONFIGURADA_LOCALMENTE>'
$env:OPENAI_MODEL = '<MODELO_HABILITADO_EN_TU_CUENTA>'
.\gradlew.bat -p welliai-backend run
```

El servidor escucha en `127.0.0.1:8080`. El emulador Android accede al equipo anfitrión mediante `10.0.2.2`; el build debug ya utiliza esa dirección. Un teléfono físico no puede usar esa dirección del emulador.

## Modelo y API de OpenAI

El backend usa `POST https://api.openai.com/v1/responses`, con autenticación Bearer desde `OPENAI_API_KEY`. El nombre del modelo procede de `OPENAI_MODEL`. No se fija un modelo en el código porque disponibilidad, permisos y compatibilidad dependen de la cuenta. Consulta los modelos habilitados para tu cuenta y escoge uno que admita Responses API y Structured Outputs.

Las recetas solicitan una salida JSON estructurada y la respuesta se valida antes de enviarla a Android. El chat mantiene como máximo 12 mensajes recientes en memoria de la aplicación y no guarda conversaciones en Room. Las solicitudes no se reintentan automáticamente.

## Endpoints del backend

### `GET /health/live`

Comprueba que el proceso responde; no contacta OpenAI.

### `POST /api/v1/ai/recipes/generate`

Ejemplo abreviado:

```json
{
  "ingredients": [
    { "name": "Frijol", "quantity": 500, "unit": "g", "category": "Legumbres" },
    { "name": "Tomate", "quantity": 3, "unit": "pieza", "category": "Verduras" }
  ],
  "budget": "Cero gasto",
  "foodType": "Tradicional",
  "restrictions": [],
  "allergies": [],
  "servings": 2
}
```

La respuesta indica si se pudo generar una receta. Si no hay suficiente información o ingredientes, contiene un mensaje y `recipe: null`. Una receta incluye ingredientes marcados como disponibles o adicionales, pasos, tiempo, porciones, sustituciones y un costo solamente si el modelo dispone de información suficiente.

### `POST /api/v1/ai/chat`

Ejemplo:

```json
{
  "messages": [
    { "role": "user", "content": "¿Cómo puedo sazonar frijoles cocidos?" }
  ]
}
```

El historial debe alternar entre `user` y `assistant`, terminar con un mensaje `user` y respetar los límites de tamaño.

Los errores se devuelven como JSON con un código y un mensaje seguro para mostrar. No incluyen la clave ni el cuerpo crudo de errores de OpenAI.

## Android

- Debug: `http://10.0.2.2:8080/`, únicamente para el backend local del emulador. El permiso HTTP sin cifrar se configura solo en el manifiesto debug.
- Release: requiere la propiedad Gradle `welliaiBackendUrl` o la variable `WELLIAI_BACKEND_URL` con una URL HTTPS. No agregues claves OpenAI a ninguna variante Android.
- Generador: abre “Crear receta con WelliAI”, selecciona ingredientes, confirma presupuesto y genera. Las preferencias guardadas se envían junto con la selección.
- Chat: desde el resultado de receta toca “Preguntar a WelliAI”. El historial vive en memoria y “Iniciar nueva conversación” lo limpia.

El endpoint backend está protegido para desarrollo local. No publiques una URL accesible por Internet hasta contar con autenticación verificable, control de uso por usuario y despliegue HTTPS.

## Pruebas

Desde la raíz del repositorio:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat -p welliai-backend test
```

Las pruebas automatizadas usan dobles o servidor de prueba y no hacen solicitudes reales a OpenAI ni necesitan credenciales.

## Seguridad y producción

- `OPENAI_API_KEY` y `OPENAI_MODEL` se leen del entorno del backend o de su `.env` local. Nunca se registran.
- El backend valida el JSON, limita cuerpo, mensajes, ingredientes y solicitudes; usa timeouts y no reintenta llamadas automáticamente.
- El costo estimado se omite cuando no existe información verificable de precios. Una respuesta de IA no garantiza que la receta sea segura para una alergia: revisa siempre etiquetas y contaminación cruzada.
- Las respuestas de IA son sugerencias culinarias y no sustituyen indicaciones médicas.
- Para producción faltan un backend desplegado con autenticación verificable y HTTPS, límites de uso asociados a usuarios, monitoreo sin datos sensibles y un modelo habilitado comprobado para la cuenta. La API puede tener costos y límites variables; revisa tu cuenta antes de usarla.
