# Guía de Inicio Rápido

## ✅ Estado del Proyecto

El proyecto está completamente configurado y listo para compilar. Estos son los componentes implementados:

### Completado ✓

- [x] Estructura del proyecto Android
- [x] Configuración de dependencias (ML Kit, TensorFlow Lite, Room, Compose)
- [x] Modelo MobileFaceNet descargado (5.0 MB)
- [x] Entidades de base de datos (Employee, AttendanceRecord)
- [x] FaceDetector con ML Kit
- [x] FaceRecognizer con TensorFlow Lite
- [x] LivenessDetector con challenge-response
- [x] UI básica con Jetpack Compose (Home, Registro, Reconocimiento)

### Pendiente (para futuras iteraciones)

- [ ] Integración de CameraX en las pantallas
- [ ] ViewModels y Repository pattern completo
- [ ] Sincronización con WorkManager
- [ ] Testing

## 🚀 Cómo Ejecutar el Proyecto

### Opción 1: Desde Windows (Recomendado)

1. **Abrir Android Studio en Windows**

2. **Abrir el proyecto** desde la ruta WSL:
   ```
   \\wsl$\Ubuntu\home\jomruizgo\Projects\facerecognition
   ```

3. **Esperar a que Gradle sincronice**
   - Android Studio detectará automáticamente las dependencias
   - Esto puede tardar unos minutos la primera vez

4. **Conectar un dispositivo Android** o iniciar un emulador
   - Ve a `Tools > Device Manager`
   - Crea un nuevo dispositivo virtual (recomendado: Pixel 6 con API 34)

5. **Ejecutar la aplicación**
   - Click en el botón Run (▶️) o presiona `Shift + F10`

### Opción 2: Desde WSL (Requiere configuración X11)

Si quieres ejecutar Android Studio desde WSL, necesitarás:

```bash
# Instalar Android Studio
sudo apt update
sudo apt install -y wget unzip

# Descargar Android Studio
wget https://redirector.gvt1.com/edgedl/android/studio/ide-zips/2023.1.1.28/android-studio-2023.1.1.28-linux.tar.gz

# Extraer
sudo tar -xzf android-studio-*-linux.tar.gz -C /opt/

# Configurar servidor X (en Windows, instala VcXsrv o usa WSLg en Windows 11)

# Ejecutar Android Studio
/opt/android-studio/bin/studio.sh
```

## 📁 Estructura del Proyecto

```
app/
├── src/main/
│   ├── java/com/attendance/facerecognition/
│   │   ├── MainActivity.kt                    # Activity principal
│   │   ├── data/
│   │   │   └── local/
│   │   │       ├── entities/                  # Employee, AttendanceRecord
│   │   │       ├── dao/                       # EmployeeDao, AttendanceDao
│   │   │       └── database/                  # AppDatabase
│   │   ├── ml/
│   │   │   ├── FaceDetector.kt               # Detección con ML Kit
│   │   │   ├── FaceRecognizer.kt             # Reconocimiento con TFLite
│   │   │   └── LivenessDetector.kt           # Anti-spoofing
│   │   └── ui/
│   │       ├── screens/                       # Pantallas Compose
│   │       └── theme/                         # Tema de la app
│   ├── res/                                   # Recursos (layouts, strings, etc.)
│   └── assets/
│       └── mobilefacenet.tflite              # Modelo ML (5.0 MB) ✓
├── build.gradle.kts                           # Configuración del módulo
└── proguard-rules.pro                         # Reglas de ofuscación
```

## 🔧 Próximos Pasos de Desarrollo

Para completar la funcionalidad, estos son los pasos sugeridos:

### 1. Integrar CameraX

Crear `CameraManager.kt` para manejar la cámara:

```kotlin
// app/src/main/java/com/attendance/facerecognition/utils/CameraManager.kt
```

Esto permitirá capturar frames en tiempo real para:
- Registro de empleados (capturar múltiples fotos)
- Reconocimiento facial (capturar frame para análisis)
- Liveness detection (procesar frames consecutivos)

### 2. Implementar ViewModels

Crear ViewModels para cada pantalla:
- `EmployeeRegistrationViewModel.kt`
- `FaceRecognitionViewModel.kt`
- `HomeViewModel.kt`

### 3. Implementar Repositories

Crear capa de repositorio para abstraer el acceso a datos:
- `EmployeeRepository.kt`
- `AttendanceRepository.kt`

### 4. Conectar UI con ML

Integrar los componentes ML con las pantallas:
- En `EmployeeRegistrationScreen`: Usar FaceDetector y FaceRecognizer
- En `FaceRecognitionScreen`: Usar LivenessDetector y FaceRecognizer

### 5. Implementar Sincronización

Crear WorkManager para sincronizar datos cuando haya internet:
- `SyncWorker.kt`
- Configurar PeriodicWorkRequest

## 🐛 Troubleshooting

### Error: "Modelo no encontrado"

```
Error: mobilefacenet.tflite not found
```

**Solución:**
```bash
cd /home/jomruizgo/Projects/facerecognition
./download_model.sh
```

### Error de compilación en Gradle

**Solución:**
1. Click en `File > Invalidate Caches / Restart`
2. Ejecutar en terminal:
   ```bash
   cd /home/jomruizgo/Projects/facerecognition
   ./gradlew clean build
   ```

### ML Kit no descarga el modelo

**Solución:**
El modelo de ML Kit se descarga automáticamente la primera vez que se usa.
Asegúrate de tener internet en la primera ejecución.

## 📊 Rendimiento Esperado

### En Emulador
- ⚠️ El reconocimiento facial puede ser lento (5-10 segundos)
- ⚠️ ML Kit puede no funcionar correctamente
- **Recomendación:** Usar dispositivo físico para pruebas

### En Dispositivo Real (Gama Media)
- Detección de rostro: ~200ms
- Generación de embedding: ~1-2 segundos
- Liveness detection: ~2-4 segundos
- Total por reconocimiento: ~3-6 segundos

### Optimizaciones Futuras
- Usar GPU Delegate para TensorFlow Lite
- Implementar cache de embeddings
- Pre-cargar modelo al inicio de la app

## 📝 Notas Importantes

### Permisos

La app requiere:
- ✅ Permiso de CAMERA (ya declarado en AndroidManifest)
- ⏳ Solicitud dinámica de permisos (pendiente de implementar en UI)

### Base de Datos

- Room DB se crea automáticamente en el primer uso
- Ubicación: `/data/data/com.attendance.facerecognition/databases/`
- Para resetear: Desinstala y reinstala la app

### Modelo MobileFaceNet

- Tamaño: 5.0 MB
- Ubicación: `app/src/main/assets/mobilefacenet.tflite`
- Input: 112x112 RGB
- Output: Vector de 128 dimensiones

## 🆘 Soporte

Si encuentras problemas:

1. **Revisar logs de Android Studio**: Ver en `Logcat`
2. **Limpiar proyecto**: `Build > Clean Project`
3. **Rebuild**: `Build > Rebuild Project`
4. **Verificar JDK**: Debe ser JDK 17 o superior (tienes JDK 21 ✓)

## 🎯 Objetivos de la Próxima Iteración

1. Implementar captura de cámara en `EmployeeRegistrationScreen`
2. Implementar reconocimiento en tiempo real en `FaceRecognitionScreen`
3. Conectar con Room Database
4. Probar en dispositivo físico
5. Ajustar umbrales de confianza según resultados

---

**¡El proyecto está listo para compilar y ejecutar!** 🎉

La estructura base está completa. Ahora se puede iterar agregando funcionalidad incrementalmente.
