# ADR-06: Sistema de Aprobación de Registros Manuales

**Estado:** Aceptado
**Fecha:** 2025-10-25
**Decisores:** Equipo de desarrollo
**Relacionado con:** [05-BACKLOG-FASE-2.md](05-BACKLOG-FASE-2.md)

## Contexto

El reconocimiento facial puede fallar por múltiples razones:
- Cambios en la apariencia del empleado (barba, gafas, peinado)
- Condiciones de iluminación deficientes
- Problemas técnicos con la cámara
- Empleados no registrados que necesitan asistencia urgente

En estos casos, necesitamos un mecanismo alternativo que:
1. Permita registrar asistencia manualmente
2. Mantenga la integridad del sistema evitando fraudes
3. Proporcione auditoría completa del proceso
4. Minimice carga administrativa

## Decisión

Implementamos un **sistema de registros pendientes de aprobación** con las siguientes características:

### 1. Flujo de Registro Manual

**Trigger:**
- Usuario intenta reconocimiento facial y falla (confianza < 85%)
- Sistema ofrece opción "Registro Manual"

**Proceso:**
1. Usuario ingresa:
   - ID de empleado (ej: "12345")
   - Nombre completo (ej: "Juan Pérez")
2. Sistema captura la foto actual del rostro
3. Sistema crea `PendingAttendanceRecord` con:
   - Datos ingresados manualmente
   - Foto facial como evidencia
   - Timestamp del intento
   - Razón: `FACIAL_FAILED`
   - Estado: `PENDING`

**Almacenamiento:**
- Entidad: `PendingAttendanceRecord` en Room DB
- Fotos: `/data/data/.../files/pending_photos/` (JPEG 90%)
- Estado inicial: `PENDING`

### 2. Proceso de Aprobación

**Actores autorizados:**
- Rol `ADMIN`: puede aprobar cualquier registro
- Rol `SUPERVISOR`: puede aprobar cualquier registro

**Autenticación requerida:**
- Huella digital (`AdminBiometricPrompt`) antes de cada acción
- Valida que quien aprueba tenga huella registrada

**Pantalla de aprobación:**
- `PendingApprovalScreen`: Lista de registros pendientes
- Vista individual muestra:
  - Foto del intento
  - Nombre y ID ingresado
  - Timestamp del intento
  - Razón del registro manual

**Acciones:**
1. **Aprobar:**
   - Requiere autenticación biométrica
   - Convierte el registro pendiente a `AttendanceRecord` oficial
   - Marca estado como `APPROVED`
   - Registra ID del supervisor que aprobó
   - Opcional: notas de aprobación

2. **Rechazar:**
   - Requiere autenticación biométrica
   - Marca estado como `REJECTED`
   - Registra ID del supervisor que rechazó
   - Obligatorio: notas explicando el rechazo

### 3. Razones de Registro Manual

```kotlin
enum class PendingReason {
    FACIAL_FAILED,      // Reconocimiento facial falló
    NOT_ENROLLED,       // Empleado no tiene registro facial
    MANUAL_REQUEST,     // Usuario prefirió registro manual
    TECHNICAL_ISSUE     // Problemas técnicos con cámara/sistema
}
```

### 4. Estados del Registro

```kotlin
enum class PendingStatus {
    PENDING,    // Esperando revisión
    APPROVED,   // Aprobado por supervisor
    REJECTED,   // Rechazado por supervisor
    EXPIRED     // Auto-expirado después de 7 días
}
```

### 5. Políticas de Expiración

**Auto-expiración:**
- Registros con estado `PENDING` > 7 días → `EXPIRED`
- Ejecutado por: `PendingAttendanceDao.markExpiredRecords()`
- Frecuencia: Diaria (via WorkManager)

**Limpieza:**
- Registros con estado `APPROVED`/`REJECTED` > 30 días → Eliminados
- Ejecutado por: `PendingAttendanceDao.cleanupOldReviewedRecords()`
- Frecuencia: Semanal (via WorkManager)

### 6. Auditoría

Toda acción se registra en `AttendanceAudit`:
- Creación del registro pendiente
- Aprobación/Rechazo con ID del supervisor
- Metadata incluye:
  - Timestamp de revisión
  - Notas del supervisor
  - Razón original del registro manual

## Alternativas Consideradas

### Opción A: Aprobación automática después de N horas
**Pros:**
- Reduce carga administrativa
- No requiere intervención manual

**Contras:**
- Permite fraudes sin supervisión
- No valida identidad real del empleado
- Viola principio de auditoría

**Decisión:** Rechazada

### Opción B: Código PIN para registro manual
**Pros:**
- No requiere foto
- Más rápido

**Contras:**
- No hay evidencia visual
- PINs pueden compartirse
- No previene fraude

**Decisión:** Rechazada

### Opción C: Registro manual solo offline, aprobación obligatoria
**Pros:**
- Máxima seguridad
- Control total sobre aprobaciones

**Contras:**
- No permite excepciones urgentes
- Bloquea empleados en casos legítimos

**Decisión:** Seleccionada (implementada)

## Consecuencias

### Positivas
✅ Empleados no bloqueados por fallos técnicos
✅ Evidencia fotográfica de cada intento
✅ Supervisión humana previene fraudes
✅ Auditoría completa del proceso
✅ No requiere conectividad (funciona offline)
✅ Notificación visual con badge en HomeScreen

### Negativas
⚠️ Carga adicional para supervisores/admins
⚠️ Registros no inmediatos (requieren aprobación)
⚠️ Almacenamiento adicional para fotos
⚠️ Posible acumulación de registros pendientes

### Mitigaciones

**Para carga administrativa:**
- Auto-expiración de registros antiguos
- Interfaz de aprobación rápida con swipe gestures (TODO)
- Aprobación batch futura (TODO)

**Para almacenamiento:**
- Fotos JPEG comprimidas (90% calidad)
- Limpieza automática de registros antiguos
- Límite de resolución 800x600 (TODO)

**Para acumulación:**
- Badge visible en HomeScreen con contador
- Notificaciones push cuando hay > 10 pendientes (TODO)
- Dashboard de métricas para admins (TODO)

## Implementación

### Componentes Creados

**Entidades:**
- `PendingAttendanceRecord` (Room entity)
- Enums: `PendingReason`, `PendingStatus`

**DAOs:**
- `PendingAttendanceDao`:
  - `getPendingRecords()`: Flow<List<PendingAttendanceRecord>>
  - `getPendingCount()`: Flow<Int>
  - `markExpiredRecords()`
  - `cleanupOldReviewedRecords()`

**Repositories:**
- `PendingAttendanceRepository`:
  - `approveRecord(recordId, supervisorId, notes)`
  - `rejectRecord(recordId, supervisorId, notes)`

**ViewModels:**
- `PendingApprovalViewModel`:
  - Carga lista de pendientes
  - Maneja aprobación/rechazo
  - Valida autenticación de supervisor

**Pantallas:**
- `PendingApprovalScreen`:
  - Lista scrollable de registros pendientes
  - Vista detalle con foto y datos
  - Botones Aprobar/Rechazar con auth biométrica

**Componentes UI:**
- Badge en HomeScreen TopAppBar (ícono de campana)
- Diálogo de registro manual en `FaceRecognitionScreen`
- `AdminBiometricPrompt` reutilizable

### Base de Datos

**Migration 10 → 11:**
```sql
CREATE TABLE IF NOT EXISTS pending_attendance_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    employeeId TEXT NOT NULL,
    employeeName TEXT,
    timestamp INTEGER NOT NULL,
    type TEXT NOT NULL,
    photoPath TEXT NOT NULL,
    deviceId TEXT NOT NULL,
    reason TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    reviewedBy INTEGER,
    reviewedAt INTEGER,
    reviewNotes TEXT,
    createdAt INTEGER NOT NULL
)
```

### Rutas de Navegación

- `/pending_approval`: Pantalla de aprobaciones
- Callback: `onNavigateToPendingApproval` agregado a `HomeScreen`

## Notas de Seguridad

1. **Autenticación biométrica obligatoria** para aprobar/rechazar
2. **No se confía en input manual** - solo como referencia para supervisión
3. **Foto requerida** - no se permite registro sin evidencia visual
4. **Auditoría completa** - toda acción registrada con timestamp e ID de supervisor
5. **Expiración automática** - previene acumulación infinita de pendientes

## Referencias

- [US-201: Registros manuales pendientes](05-BACKLOG-FASE-2.md#us-201-registros-manuales-pendientes-de-aprobación-must-have)
- [US-202: Pantalla de aprobación](05-BACKLOG-FASE-2.md#us-202-pantalla-de-aprobación-de-registros-pendientes-must-have)
- [US-203: Autenticación de supervisor](05-BACKLOG-FASE-2.md#us-203-autenticación-de-supervisor-para-aprobaciones-must-have)
- [02-DECISION-CONGRUENCIA-BIOMETRIA.md](02-DECISION-CONGRUENCIA-BIOMETRIA.md) - Relacionado con autenticación biométrica
- [03-DECISION-MANEJO-ERRORES-IDENTIFICACION.md](03-DECISION-MANEJO-ERRORES-IDENTIFICACION.md) - Contexto de errores de identificación

## Próximas Mejoras

1. **Aprobación batch** - Aprobar múltiples registros a la vez
2. **Filtros** - Por empleado, fecha, razón, estado
3. **Exportación** - CSV de registros pendientes/históricos
4. **Notificaciones push** - Alertar a supervisores de nuevos pendientes
5. **Métricas** - Dashboard de tasa de aprobación/rechazo
6. **Límite de resolución de fotos** - 800x600 para reducir almacenamiento
