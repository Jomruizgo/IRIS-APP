# IRIS - Identificación Rápida e Inteligente Sin conexión

![Logo IRIS](docs/iris-logo.png)

**IRIS** es un sistema robusto de control de asistencia biométrica para Android que utiliza reconocimiento facial con ML Kit y TensorFlow Lite, diseñado para funcionar **completamente offline** con sincronización opcional cuando hay conexión a internet.

## 📖 Significado del Nombre

**IRIS** es un acrónimo que representa:
- **I**dentificación
- **R**ápida e
- **I**nteligente
- **S**in conexión

El nombre hace referencia al iris del ojo humano, elemento clave en la identificación biométrica, y simboliza la capacidad del sistema para reconocer personas de forma instantánea y precisa sin necesidad de conectividad.

*Eslogan:* **"IRIS - Te vemos, te reconocemos"**

## 🎯 Características Principales

### ✅ Reconocimiento Facial
- **ML Kit Face Detection** para detección de rostros en tiempo real
- **TensorFlow Lite + MobileFaceNet** para generación de embeddings (192 dimensiones)
- **Liveness Detection** activo con desafíos aleatorios (parpadeo, giros de cabeza)
- **Selección manual** de ENTRADA/SALIDA con validación de secuencia
- **Umbral de confianza**: 85% para reconocimiento exitoso
- **Múltiples fotos por empleado** (10 fotos desde diferentes ángulos)

### 🔐 Seguridad y Control de Errores
- **Botón "Este no soy yo"** para corrección inmediata de falsos positivos
- **Validación de secuencia**: No permite 2 entradas o 2 salidas seguidas
- **Auditoría completa** con tabla `attendance_audit` que registra todas las acciones
- **Autenticación biométrica** (huella digital) como método alternativo

### 📊 Reportes y Gestión
- **Reporte diario** con estadísticas
- **Lista de empleados** con búsqueda y eliminación
- **Exportación a CSV** con filtros
- **Retención de datos configurable**

### 🔄 Sincronización
- **Offline-First**: Funciona sin internet
- **WorkManager** para sincronización automática
- **Reintentos automáticos** con backoff exponencial

## 🏗️ Arquitectura

### Patrón MVVM
```
View (Compose) ← ViewModel ← Repository ← Room Database
```

### Tecnologías Principales
- **Kotlin** + **Jetpack Compose**
- **Room** + **SQLite**
- **ML Kit** + **TensorFlow Lite**
- **CameraX**
- **WorkManager**
- **DataStore**

## 🚀 User Stories Implementadas

### ✅ Sprint 3 - Completado
- **US-039**: Selección manual ENTRADA/SALIDA con validación
- **US-038**: Botón "Este no soy yo" para corrección inmediata
- **US-041**: Sistema de auditoría completo
- **US-005**: Lista de empleados con búsqueda
- **US-007**: Eliminación de empleados
- **US-019**: Reporte diario con estadísticas

### ✅ Componentes Adicionales
- **BiometricAuthManager**: Autenticación por huella
- **CsvExporter**: Exportación de reportes
- **DataRetentionManager**: Gestión de retención de datos
- **SyncWorker**: Sincronización con backend

## 📦 Instalación

### Requisitos
- Android Studio Hedgehog o superior
- JDK 21
- Android SDK 34
- Dispositivo físico con cámara

### Pasos
1. Clonar repositorio
2. Descargar `mobilefacenet.tflite` y colocar en `app/src/main/assets/`
3. Sincronizar Gradle
4. Ejecutar en dispositivo físico

## 📚 Documentación

Ver carpeta `docs/` para:
- `01-BACKLOG.md`: Historias de usuario completas
- `02-DECISION-CONGRUENCIA-BIOMETRIA.md`: Decisión de diseño sobre liveness
- `03-DECISION-MANEJO-ERRORES-IDENTIFICACION.md`: Gestión de falsos positivos

## 📄 Licencia

Proyecto educativo - TalentoTech IA 2025
