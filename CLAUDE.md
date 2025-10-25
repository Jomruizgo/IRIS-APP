# CLAUDE.md

Este archivo proporciona orientación a Claude Code (claude.ai/code) cuando trabaja con código en este repositorio.

## Descripción del Proyecto

Sistema de control de asistencia con reconocimiento facial para Android que funciona 100% offline. Utiliza TensorFlow Lite con MobileFaceNet para generar embeddings faciales y ML Kit para detección de rostros y liveness detection.

## Stack Tecnológico

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose con Material3
- **Base de datos**: Room (SQLite) con SQLCipher (encriptación)
- **ML**: TensorFlow Lite + MobileFaceNet, ML Kit Face Detection
- **Biometría**: Android KeyStore + BiometricPrompt (autenticación con huella dactilar)
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
  - `Employee.kt`: Almacena empleados con sus embeddings faciales (vectores de 192 dimensiones) y alias de KeyStore para huella dactilar
  - `AttendanceRecord.kt`: Registros de entrada/salida con timestamp
  - `User.kt`: Usuarios administrativos con roles (ADMIN, SUPERVISOR, USER)
  - `DeviceRegistration.kt`: Dispositivos registrados con tenant ID para multi-tenancy
  - `AttendanceAudit.kt`: Auditoría de cambios en registros
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
  - 3 tipos de desafíos: parpadeo, giro izquierda/derecha (congruente con registro)
  - Previene ataques con fotos o videos estáticos

### Capa Biométrica (`app/src/main/java/com/attendance/facerecognition/biometric/`)

- **`BiometricKeyManager.kt`**: Gestor de claves criptográficas en Android KeyStore
  - Cada empleado tiene su propia SecretKey protegida por biometría
  - `enrollFingerprint()`: Genera clave y vincula huella durante registro
  - `verifyFingerprint()`: Verifica huella usando clave específica del empleado
  - Usa `BiometricPrompt.CryptoObject` para autenticación por operación
  - Claves se invalidan si se agregan nuevas huellas al dispositivo

- **`BiometricAuthManager.kt`**: Wrapper simple de BiometricPrompt (legacy, reemplazado por BiometricKeyManager)

### Capa UI (`app/src/main/java/com/attendance/facerecognition/ui/`)

- **`screens/HomeScreen.kt`**: Pantalla principal con navegación y estado de sincronización
- **`screens/LoginScreen.kt`**: Autenticación con usuario + PIN
- **`screens/FirstTimeSetupScreen.kt`**: Configuración inicial del administrador
- **`screens/EmployeeRegistrationScreen.kt`**: Registro de empleados con captura facial y opcional huella
- **`screens/FingerprintEnrollmentScreen.kt`**: Vinculación de huella dactilar después de registro facial
- **`screens/BiometricAuthScreen.kt`**: Autenticación con ID + huella dactilar
- **`screens/FaceRecognitionScreen.kt`**: Reconocimiento facial y registro de asistencia
- **`screens/EmployeeListScreen.kt`**: Lista de empleados con búsqueda
- **`screens/DailyReportScreen.kt`**: Reporte de asistencia diaria
- **`screens/SettingsScreen.kt`**: Configuración de retención de datos, sincronización manual
- **`theme/`**: Configuración de Material3 Theme

### MainActivity

`MainActivity.kt`: Activity principal que usa Jetpack Compose con navegación entre pantallas

## Flujos de Datos

### Registro de Empleado con Reconocimiento Facial

1. Admin ingresa datos: nombre, ID, departamento, cargo
2. Admin marca checkbox "Habilitar Huella Digital" (opcional)
3. Capturar 7-10 fotos del rostro desde diferentes ángulos:
   - Fotos 1-3: De frente
   - Fotos 4-6: Girar ligeramente a la izquierda
   - Fotos 7-9: Girar ligeramente a la derecha
   - Foto 10: De frente nuevamente
4. Para cada foto:
   - `FaceDetector.detectFaces()` → Detecta y recorta rostro
   - `FaceRecognizer.generateEmbedding()` → Genera vector de 192 dimensiones
5. Guardar empleado con embeddings en Room DB
6. **Si activó huella dactilar:**
   - Mostrar `FingerprintEnrollmentScreen`
   - `BiometricKeyManager.enrollFingerprint()` → Genera SecretKey en KeyStore
   - Usuario coloca huella → vincula huella con la clave
   - Guardar alias de KeyStore en `Employee.fingerprintKeystoreAlias`

### Autenticación con Reconocimiento Facial

1. Usuario selecciona tipo de registro (ENTRADA/SALIDA)
2. Sistema genera desafío aleatorio con `LivenessDetector` (BLINK, TURN_LEFT, TURN_RIGHT)
3. Usuario completa desafío (liveness detection)
4. Si pasa:
   - `FaceDetector.detectFaces()` → Detecta rostro
   - `FaceRecognizer.generateEmbedding()` → Genera embedding
   - `FaceRecognizer.findBestMatch()` → Compara con embeddings almacenados usando similitud coseno
   - Si confianza > 85% → Validar entrada/salida
   - Guardar registro en `AttendanceDao`
   - Registrar en `AttendanceAudit`
5. Mostrar confirmación con opción "Este no soy yo" para cancelar

### Autenticación con ID + Huella Dactilar

1. Usuario selecciona "Registrar con ID + Huella"
2. Selecciona tipo de registro (ENTRADA/SALIDA)
3. Ingresa su ID de empleado en teclado numérico
4. Sistema busca empleado por ID
5. **Validaciones:**
   - Si empleado no existe → Error: "Empleado no encontrado"
   - Si `hasFingerprintEnabled = false` → Error: "No tiene huella registrada. Intenta con otro método o contacta al administrador"
   - Si `fingerprintKeystoreAlias` es null → Error: "No tiene huella vinculada. Contacta al administrador"
6. `BiometricKeyManager.verifyFingerprint()` con alias específico del empleado
7. Usuario coloca huella → intenta desbloquear SecretKey
8. Si huella coincide (solo la registrada puede desbloquear):
   - Validar entrada/salida
   - Guardar registro con `confidence = 1.0` (100%)
   - Registrar en auditoría
9. Si huella no coincide → Error con posibilidad de reintentar

### Sincronización con Backend (Multi-Tenancy)

1. Dispositivo debe activarse con código: `TENANT-CODIGO` (ej: ACME-ABC123)
2. Backend devuelve JWT con `tenant_id` embebido
3. `WorkManager` ejecuta sincronización periódica cuando hay WiFi
4. Envía registros pendientes (`isSynced = 0`) con headers:
   - `Authorization: Bearer <jwt_token>`
   - `X-Tenant-ID: <tenant_id>`
5. Backend guarda en DynamoDB con composite keys: `tenant_id#employee_id`
6. Marca registros como `isSynced = 1`
7. `DataRetentionManager` elimina solo registros sincronizados antiguos

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

### Autenticación Biométrica con Huella Dactilar

**Implementación con Android KeyStore:**
- Cada empleado tiene una SecretKey única en KeyStore
- La clave se configura con `setUserAuthenticationRequired(true)`
- Solo la huella registrada originalmente puede desbloquear la clave
- Usa `BiometricPrompt.CryptoObject` para vincular huella con operación criptográfica
- Si se agregan nuevas huellas al dispositivo, las claves se invalidan (`setInvalidatedByBiometricEnrollment`)

**Seguridad:**
- NO se almacenan plantillas biométricas (Android lo prohíbe por seguridad)
- Las huellas permanecen en TEE (Trusted Execution Environment)
- Cada empleado solo puede autenticarse con SU huella específica
- Cualquier otra huella del dispositivo NO funcionará

**Flujo:**
1. Registro: `BiometricKeyManager.enrollFingerprint()` → Genera clave + pide huella
2. Autenticación: `BiometricKeyManager.verifyFingerprint(keystoreAlias)` → Solo esa huella desbloquea
3. Almacenamiento: Solo se guarda el alias de KeyStore en `Employee.fingerprintKeystoreAlias`

### Base de Datos

- **Motor**: SQLite con SQLCipher (encriptación AES-256)
- **Versión actual**: 7
- **Passphrase**: Generada aleatoriamente (32 bytes) y guardada en SharedPreferences
- **Migración**: `fallbackToDestructiveMigration()` (solo para desarrollo)
- **Entidades**: Employee, AttendanceRecord, AttendanceAudit, User, DeviceRegistration

### Limitaciones

- Requiere buena iluminación para reconocimiento facial
- Cambios significativos de apariencia pueden requerir re-registro
- Reconocimiento facial funciona mejor en dispositivos gama media/alta
- Huella dactilar requiere sensor biométrico en el dispositivo
- La app funciona 100% offline, sincronización requiere WiFi

## Estructura de Paquetes

```
com.attendance.facerecognition/
├── biometric/             # Autenticación biométrica
│   ├── BiometricKeyManager.kt
│   └── BiometricAuthManager.kt (legacy)
├── data/
│   ├── local/
│   │   ├── entities/      # Room entities
│   │   ├── dao/           # DAOs
│   │   └── database/      # AppDatabase + encriptación
│   └── repository/        # Repositories
├── export/                # Exportación de reportes
├── ml/                    # Componentes ML (TensorFlow Lite + ML Kit)
├── settings/              # Configuración y retención de datos
├── sync/                  # Sincronización con backend
├── ui/
│   ├── components/        # Componentes reutilizables (CameraPreview)
│   ├── screens/           # Composables de pantallas
│   ├── viewmodels/        # ViewModels por pantalla
│   └── theme/             # Tema Material3
└── MainActivity.kt
```

## Estado Actual de Implementación

### ✅ Completado
- Registro de empleados con captura facial (7-10 fotos)
- Reconocimiento facial con liveness detection
- Autenticación con ID + huella dactilar (por empleado específico)
- Sistema de roles (ADMIN, SUPERVISOR, USER)
- Autenticación con usuario + PIN
- Lista de empleados con búsqueda
- Reporte diario de asistencia
- Auditoría de registros
- Encriptación de base de datos (SQLCipher)
- Multi-tenancy con device registration
- Retención de datos con eliminación segura
- Sincronización manual y automática (estructura)
- Logo IRIS integrado en todas las pantallas

### 🚧 Pendiente
- Backend con DynamoDB (documentado en docs/04-API-REQUIREMENTS.md)
- Testing unitario e instrumentado
- Exportación de reportes a PDF/Excel
- Modo nocturno para mejor captura
- Optimización GPU para reconocimiento facial

## Dependencias Clave

- Room: 2.6.1 (con KSP)
- SQLCipher: 4.5.4 (encriptación de base de datos)
- Biometric: 1.1.0 (autenticación con huella)
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
- `docs/02-DECISION-CONGRUENCIA-BIOMETRIA.md` - Decisión arquitectónica sobre liveness detection

Los archivos que permanecen en la raíz:
- `CLAUDE.md` - Este archivo (orientación para Claude Code)
- `README.md` - Presentación del proyecto
- `.gitignore` - Archivos ignorados por Git

**Convención de nombres**: `docs/NN-NOMBRE.md` donde NN es el número secuencial de creación (01, 02, 03...).

### Decisiones Arquitectónicas Importantes

1. **Congruencia Biométrica** (`docs/02-*`): Solo pedimos poses en liveness que capturamos en registro
   - Desafíos permitidos: BLINK, TURN_LEFT, TURN_RIGHT
   - Desafíos eliminados: SMILE, LOOK_UP (no tenemos fotos de entrenamiento)

## Notas de Desarrollo

- El proyecto usa Gradle Kotlin DSL (.kts)
- KSP en lugar de KAPT para Room
- Java 17 configurado en `compileOptions`
- `fallbackToDestructiveMigration()` activado (solo para desarrollo)
- ProGuard/R8 desactivado en debug
