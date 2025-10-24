# CLAUDE.md

Este archivo proporciona orientación a Claude Code (claude.ai/code) cuando trabaja con código en este repositorio.

## Descripción del Proyecto

Sistema de control de asistencia con reconocimiento facial para Android que funciona 100% offline. Utiliza TensorFlow Lite con MobileFaceNet para generar embeddings faciales y ML Kit para detección de rostros y liveness detection.

## Stack Tecnológico

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose con Material3
- **Base de datos**: Room (SQLite)
- **ML**: TensorFlow Lite + MobileFaceNet, ML Kit Face Detection
- **Cámara**: CameraX
- **Sincronización**: WorkManager + Retrofit
- **Mínimo SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34

## Comandos Esenciales

### Build y Compilación

```bash
# Limpiar proyecto
./gradlew clean

# Build debug
./gradlew assembleDebug

# Build release
./gradlew assembleRelease

# Instalar en dispositivo conectado
./gradlew installDebug

# Build completo (limpieza + build + instalación)
./gradlew clean assembleDebug installDebug
```

### Testing

```bash
# Tests unitarios
./gradlew test

# Tests instrumentados (requiere dispositivo/emulador)
./gradlew connectedAndroidTest

# Test específico
./gradlew test --tests NombreDelTest
```

### Verificación de Código

```bash
# Lint
./gradlew lint

# Ver reporte de lint
./gradlew lintDebug
# El reporte estará en: app/build/reports/lint-results-debug.html
```

### Desarrollo

```bash
# Sincronizar dependencias
./gradlew --refresh-dependencies

# Ver dependencias del proyecto
./gradlew dependencies

# Verificar que el modelo ML está presente
ls -lh app/src/main/assets/mobilefacenet.tflite
```

## Arquitectura del Proyecto

### Capa de Datos (`app/src/main/java/com/attendance/facerecognition/data/`)

- **`local/entities/`**: Entidades Room
  - `Employee.kt`: Almacena empleados con sus embeddings faciales (vectores de 128 dimensiones)
  - `AttendanceRecord.kt`: Registros de entrada/salida con timestamp
  - `FloatListConverter`: TypeConverter para serializar embeddings como JSON en SQLite

- **`local/dao/`**: Data Access Objects para operaciones CRUD
  - `EmployeeDao.kt`: Operaciones sobre empleados
  - `AttendanceDao.kt`: Operaciones sobre registros de asistencia

- **`local/database/`**: Configuración de Room
  - `AppDatabase.kt`: Singleton de la base de datos con patrón synchronized

### Capa ML (`app/src/main/java/com/attendance/facerecognition/ml/`)

- **`FaceDetector.kt`**: Wrapper de ML Kit Face Detection
  - Detecta rostros en bitmap
  - Extrae características: ángulos Euler (X/Y/Z), probabilidad de ojos abiertos/cerrados, sonrisa
  - Recorta rostros con padding configurable
  - Usa modo PERFORMANCE_MODE_ACCURATE con landmarks y clasificación activados

- **`FaceRecognizer.kt`**: Reconocimiento facial con TensorFlow Lite
  - Carga modelo MobileFaceNet desde `assets/mobilefacenet.tflite` (5MB)
  - Input: 112x112 RGB, normalizado a rango [-1, 1]
  - Output: Vector de 128 dimensiones (L2 normalizado)
  - Métodos de similitud: coseno y distancia euclidiana
  - Configurado con 4 threads para mejor rendimiento

- **`LivenessDetector.kt`**: Sistema challenge-response para anti-spoofing
  - 5 tipos de desafíos: parpadeo, sonrisa, giro izquierda/derecha, mirar arriba
  - Previene ataques con fotos o videos estáticos

### Capa UI (`app/src/main/java/com/attendance/facerecognition/ui/`)

- **`screens/HomeScreen.kt`**: Pantalla principal con navegación
- **`screens/EmployeeRegistrationScreen.kt`**: Registro de nuevos empleados
- **`screens/FaceRecognitionScreen.kt`**: Reconocimiento y registro de asistencia
- **`theme/`**: Configuración de Material3 Theme

### MainActivity

`MainActivity.kt`: Activity principal que usa Jetpack Compose con navegación entre pantallas

## Flujo de Datos

### Registro de Empleado

1. Capturar 7-10 fotos del rostro desde diferentes ángulos (no implementado aún)
2. Para cada foto:
   - `FaceDetector.detectFaces()` → Detecta y recorta rostro
   - `FaceRecognizer.generateEmbedding()` → Genera vector de 128 dimensiones
3. Guardar embeddings en Room DB vía `EmployeeDao.insert()`

### Reconocimiento y Asistencia

1. Sistema genera desafío aleatorio con `LivenessDetector`
2. Usuario completa desafío (liveness detection)
3. Si pasa:
   - `FaceDetector.detectFaces()` → Detecta rostro
   - `FaceRecognizer.generateEmbedding()` → Genera embedding
   - `FaceRecognizer.findBestMatch()` → Compara con embeddings almacenados usando similitud coseno
   - Si confianza > 85% → Guardar registro en `AttendanceDao`
4. `WorkManager` sincroniza con servidor cuando hay WiFi (no implementado)

## Modelo MobileFaceNet

**Ubicación obligatoria**: `app/src/main/assets/mobilefacenet.tflite`

El modelo NO está en control de versiones. Si falta, descargarlo:

```bash
./download_model.sh
```

O manualmente:

```bash
mkdir -p app/src/main/assets
wget https://github.com/shubham0204/OnDevice-Face-Recognition-Android/raw/main/app/src/main/assets/mobile_face_net.tflite \
     -O app/src/main/assets/mobilefacenet.tflite
```

## Consideraciones Importantes

### Rendimiento

- **Gama baja** (2GB RAM): 2-4s de reconocimiento, ~95% precisión
- **Gama media** (4GB RAM): 1-2s, ~97% precisión
- **Gama alta** (6GB+ RAM): <1s, ~99% precisión

Para optimizar:
- Descomentar GPU Delegate en `FaceRecognizer.kt` línea 38 (requiere configuración adicional)
- Ajustar `setNumThreads()` según el hardware
- Pre-cargar modelo al inicio de la app

### Embeddings

- Cada empleado almacena múltiples embeddings (7-10) para mejor precisión
- Los embeddings se normalizan con L2 normalization
- Se serializan como JSON en SQLite usando `FloatListConverter`
- Almacenamiento aproximado: ~10KB por empleado

### Umbrales

- Similitud coseno mínima: 0.7 (70%) por defecto en `findBestMatch()`
- Recomendado para producción: 0.85 (85%) para reducir falsos positivos
- Ajustar según resultados en pruebas reales

### Liveness Detection

- Los umbrales están en `DetectedFace.kt`:
  - Ojos cerrados: < 0.4
  - Ojos abiertos: > 0.6
  - Sonrisa: > 0.7
  - Giro de cabeza: > 20 grados

### Limitaciones

- Requiere buena iluminación (sin implementación de modo nocturno)
- Cambios significativos de apariencia (barba, gafas) pueden requerir re-registro
- CameraX aún no está integrado en las pantallas
- No hay ViewModels ni Repository pattern implementados

## Estructura de Paquetes

```
com.attendance.facerecognition/
├── data/
│   └── local/
│       ├── entities/      # Room entities
│       ├── dao/           # DAOs
│       └── database/      # DB config
├── ml/                    # Componentes ML
├── ui/
│   ├── screens/           # Composables de pantallas
│   └── theme/             # Tema Material3
└── MainActivity.kt
```

## Próximos Pasos de Desarrollo

1. Integrar CameraX en pantallas de registro y reconocimiento
2. Implementar ViewModels para cada pantalla
3. Crear capa Repository para abstraer acceso a datos
4. Implementar sincronización con WorkManager
5. Manejar permisos de cámara dinámicamente con Accompanist
6. Testing unitario de componentes ML
7. Testing instrumentado de flujo completo

## Dependencias Clave

- Room: 2.6.1 (con KSP)
- ML Kit Face Detection: 16.1.6
- TensorFlow Lite: 2.14.0 (con soporte GPU)
- CameraX: 1.3.1
- Compose BOM: 2024.01.00
- WorkManager: 2.9.0
- Retrofit: 2.9.0 (para sincronización futura)
- Accompanist Permissions: 0.34.0

## Estructura de Documentación

Toda la documentación del proyecto se encuentra organizada en la carpeta `docs/` con numeración secuencial:

- `docs/01-BACKLOG.md` - Historias de usuario, épicas y planificación de sprints
- `docs/02-*.md` - Futuros documentos (arquitectura, API, etc.)

Los archivos que permanecen en la raíz:
- `CLAUDE.md` - Este archivo (orientación para Claude Code)
- `README.md` - Presentación del proyecto
- `.gitignore` - Archivos ignorados por Git

**Convención de nombres**: `docs/NN-NOMBRE.md` donde NN es el número secuencial de creación (01, 02, 03...).

## Notas de Desarrollo

- El proyecto usa Gradle Kotlin DSL (.kts)
- KSP en lugar de KAPT para Room
- Java 17 configurado en `compileOptions`
- `fallbackToDestructiveMigration()` activado (solo para desarrollo)
- ProGuard/R8 desactivado en debug
