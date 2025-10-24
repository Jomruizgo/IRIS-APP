# Sistema de Control de Asistencia con Reconocimiento Facial Offline

Aplicación Android para control de asistencia de personal usando reconocimiento facial que funciona 100% offline.

## Características

- ✅ **Reconocimiento facial offline** usando TensorFlow Lite + MobileFaceNet
- ✅ **Detección de vida (liveness)** con challenge-response activo
- ✅ **Base de datos local** con Room para almacenar empleados y registros
- ✅ **Sincronización automática** cuando hay WiFi disponible
- ✅ **UI moderna** con Jetpack Compose
- ✅ **Optimizado para dispositivos de gama baja/media**

## Stack Tecnológico

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose
- **Detección facial**: ML Kit Face Detection
- **Reconocimiento**: TensorFlow Lite + MobileFaceNet
- **Base de datos**: Room (SQLite)
- **Sincronización**: WorkManager
- **Cámara**: CameraX

## Requisitos

- Android Studio Hedgehog (2023.1.1) o superior
- JDK 17 o superior
- Android SDK API 34
- Dispositivo Android con:
  - Android 7.0 (API 24) o superior
  - Cámara frontal/trasera
  - Mínimo 2GB RAM

## Configuración del Proyecto

### 1. Descargar el Modelo MobileFaceNet

El modelo MobileFaceNet **NO** está incluido en el repositorio por tamaño. Debes descargarlo manualmente:

#### Opción A: Descargar modelo pre-entrenado

```bash
# 1. Crear directorio assets si no existe
mkdir -p app/src/main/assets

# 2. Descargar el modelo (opciones):

# Opción 1: Desde repositorio de TensorFlow
wget https://github.com/sirius-ai/MobileFaceNet_TF/raw/master/output/MobileFaceNet.tflite \
     -O app/src/main/assets/mobilefacenet.tflite

# Opción 2: Desde repositorio alternativo
wget https://github.com/shubham0204/OnDevice-Face-Recognition-Android/raw/main/app/src/main/assets/mobile_face_net.tflite \
     -O app/src/main/assets/mobilefacenet.tflite
```

#### Opción B: Convertir desde modelo TensorFlow

Si tienes el modelo original en formato TensorFlow:

```python
import tensorflow as tf

# Cargar el modelo
converter = tf.lite.TFLiteConverter.from_saved_model('path/to/mobilefacenet')

# Optimizar para móviles
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_types = [tf.float16]

# Convertir
tflite_model = converter.convert()

# Guardar
with open('mobilefacenet.tflite', 'wb') as f:
    f.write(tflite_model)
```

### 2. Verificar que el modelo esté en la ubicación correcta

```bash
ls -lh app/src/main/assets/mobilefacenet.tflite
```

Deberías ver un archivo de aproximadamente 4-5 MB.

### 3. Abrir el proyecto en Android Studio

```bash
# En Linux/WSL con Android Studio instalado
/opt/android-studio/bin/studio.sh .

# O si instalaste en Windows, abre el proyecto desde Windows
```

### 4. Sincronizar Gradle

Android Studio debería sincronizar automáticamente. Si no:
- Click en `File > Sync Project with Gradle Files`

### 5. Ejecutar en dispositivo o emulador

1. Conecta un dispositivo Android o inicia un emulador
2. Click en el botón "Run" (▶️) en Android Studio

## Arquitectura del Proyecto

```
app/
├── data/
│   ├── local/
│   │   ├── dao/              # Data Access Objects
│   │   ├── entities/         # Entidades Room (Employee, AttendanceRecord)
│   │   └── database/         # Configuración de Room DB
│   ├── models/               # Modelos de datos
│   └── repository/           # Repositorios
├── domain/
│   ├── usecases/             # Casos de uso de negocio
│   └── models/               # Modelos de dominio
├── ml/
│   ├── FaceDetector.kt       # ML Kit Face Detection
│   ├── FaceRecognizer.kt     # TensorFlow Lite + MobileFaceNet
│   └── LivenessDetector.kt   # Detección de vida
├── ui/
│   ├── screens/              # Pantallas Compose
│   ├── components/           # Componentes reutilizables
│   └── theme/                # Tema de la app
└── utils/
    ├── CameraManager.kt      # Gestión de cámara
    └── SyncManager.kt        # Sincronización con servidor
```

## Flujo de Funcionamiento

### Registro de Empleado

1. Usuario ingresa datos del empleado (nombre, ID, departamento)
2. Captura 7-10 fotos del rostro desde diferentes ángulos
3. ML Kit detecta y recorta el rostro de cada foto
4. MobileFaceNet genera embeddings (vectores de 128 dimensiones)
5. Los embeddings se almacenan en Room DB

### Reconocimiento y Registro de Asistencia

1. Empleado se posiciona frente a la cámara
2. Sistema genera desafío aleatorio (ej: "Parpadea 2 veces")
3. Empleado completa el desafío
4. Si pasa liveness detection:
   - ML Kit detecta y recorta el rostro
   - MobileFaceNet genera embedding
   - Se compara con embeddings almacenados usando similitud coseno
   - Si confianza > 85%, se registra entrada/salida
5. Registro se guarda en Room DB
6. WorkManager sincroniza cuando hay WiFi

## Liveness Detection

El sistema usa **challenge-response activo** con 5 tipos de desafíos:

1. **Parpadear** - Detecta apertura/cierre de ojos
2. **Sonreír** - Detecta sonrisa
3. **Girar cabeza izquierda** - Detecta rotación Y
4. **Girar cabeza derecha** - Detecta rotación Y
5. **Mirar arriba** - Detecta rotación X

Esto previene spoofing con fotos o videos estáticos.

## Rendimiento Esperado

### Dispositivos de Gama Baja (2GB RAM, Snapdragon 450)
- Tiempo de reconocimiento: 2-4 segundos
- Precisión: ~95% con buena iluminación

### Dispositivos de Gama Media (4GB RAM, Snapdragon 660+)
- Tiempo de reconocimiento: 1-2 segundos
- Precisión: ~97% con buena iluminación

### Dispositivos de Gama Alta (6GB+ RAM, Snapdragon 845+)
- Tiempo de reconocimiento: < 1 segundo
- Precisión: ~99% con buena iluminación

## Limitaciones Conocidas

⚠️ **Iluminación**: Requiere buena iluminación. En ambientes oscuros la precisión disminuye.

⚠️ **Registro**: Se necesitan 7-10 fotos de buena calidad por empleado.

⚠️ **Batería**: El uso continuo de la cámara consume batería. Se recomienda mantener el dispositivo conectado.

⚠️ **Almacenamiento**: Cada empleado ocupa ~10KB (embeddings). 50 empleados = ~500KB.

⚠️ **Cambios de apariencia**: Barba, gafas, cambios de peso pueden afectar el reconocimiento. Solución: re-registrar al empleado.

## Próximas Mejoras

- [ ] Modo nocturno con detección IR
- [ ] Soporte para múltiples dispositivos sincronizados
- [ ] Dashboard web para administradores
- [ ] Exportación de reportes en PDF/Excel
- [ ] Geolocalización opcional
- [ ] Notificaciones push

## Licencia

MIT License - Ver archivo LICENSE

## Soporte

Para preguntas o issues, abrir un issue en GitHub.
