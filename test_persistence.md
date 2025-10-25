# Prueba de Persistencia de Base de Datos

## Objetivo
Verificar que los datos se guardan en DISCO y no en RAM

## Procedimiento

### Paso 1: Instalar la app
```bash
./gradlew installDebug
```

### Paso 2: Abrir la app y crear datos
1. Abrir app
2. Crear usuario admin (ej: username: "admin", PIN: "1234")
3. Crear un empleado de prueba
4. Registrar una asistencia

### Paso 3: Cerrar COMPLETAMENTE la app
```bash
# Opción A: Desde UI
Settings → Apps → Attendance App → Force Stop

# Opción B: Desde ADB
adb shell am force-stop com.attendance.facerecognition
```

### Paso 4: Reabrir la app
```bash
adb shell am start -n com.attendance.facerecognition/.MainActivity
```

## Resultados Esperados

### ✅ SI ESTÁ EN DISCO (correcto):
- ✅ No pide crear usuario admin de nuevo
- ✅ El empleado creado sigue ahí
- ✅ El registro de asistencia sigue ahí
- ✅ Puedes hacer login con el mismo PIN

### ❌ SI ESTUVIERA EN RAM (incorrecto):
- ❌ Pide crear usuario admin de nuevo
- ❌ No hay empleados
- ❌ No hay registros de asistencia
- ❌ Todo se perdió

## Verificación Adicional - Inspeccionar BD con ADB

### Ver archivos de la BD:
```bash
adb shell run-as com.attendance.facerecognition ls -lh databases/
```

**Output esperado:**
```
-rw-rw---- 1 u0_a123 u0_a123 45.0K face_recognition_attendance_db
-rw-rw---- 1 u0_a123 u0_a123 32.0K face_recognition_attendance_db-shm
-rw-rw---- 1 u0_a123 u0_a123  8.0K face_recognition_attendance_db-wal
```

### Ver tamaño del archivo:
```bash
adb shell run-as com.attendance.facerecognition du -h databases/face_recognition_attendance_db
```

Si el tamaño es > 0, los datos están guardados.

## ⚠️ Problema Conocido

La app usa `.fallbackToDestructiveMigration()` que BORRA TODOS LOS DATOS cuando:
- Se actualiza la versión de la BD (ej: de v9 a v10)
- No hay migración definida

**Esto NO significa que esté en RAM**, sino que Room borra y recrea la BD en cada update.

## Solución

Implementar migraciones adecuadas o remover `.fallbackToDestructiveMigration()` en producción.
