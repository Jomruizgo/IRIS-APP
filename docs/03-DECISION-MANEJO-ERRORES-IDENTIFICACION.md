# Decisión de Diseño: Manejo de Errores de Identificación y Control Manual

**Fecha**: 2025-01-24
**Estado**: 📋 Planificado
**Tipo**: Decisión Arquitectónica + Mejora UX

---

## Contexto

En un sistema de reconocimiento facial, existen dos escenarios problemáticos que debemos manejar:

1. **Falso Positivo**: El sistema identifica incorrectamente a una persona como otra
2. **Falta de control**: El usuario no puede elegir si está marcando ENTRADA o SALIDA

## Problema 1: Plan para Falsos Positivos

### Escenario Real
```
Usuario: Pedro García
Sistema: "✅ ¡Asistencia Registrada! Juan López - ENTRADA"
Pedro: "¡Ese no soy yo!"
```

**Consecuencias actuales:**
- ❌ Pedro no puede corregir el error inmediatamente
- ❌ Juan López tiene un registro falso en su historial
- ❌ Pedro no tiene registro de su asistencia real
- ❌ No hay forma de auditar estos errores

### Solución Propuesta: Sistema de Corrección Inmediata

#### Opción A: Botón "Este no soy yo" (ELEGIDA)

**Flujo de corrección:**
```
1. Sistema reconoce → Muestra diálogo de éxito
2. Usuario ve el nombre incorrecto
3. Usuario presiona "Este no soy yo"
4. Sistema:
   - Cancela el registro inmediatamente
   - Marca el registro como "CANCELADO" en auditoría
   - Vuelve a la pantalla de reconocimiento
   - Log: "Usuario rechazó identificación como Juan López"
5. Usuario vuelve a intentar reconocimiento
```

**Implementación:**
```kotlin
AlertDialog(
    title = "¡Asistencia Registrada!",
    text = "Juan López\nENTRADA - 8:05 AM\nConfianza: 92%",
    confirmButton = { Button("✅ Correcto") },
    dismissButton = { TextButton("❌ Este no soy yo") {
        viewModel.cancelLastRegistration()
        viewModel.reset()
    }}
)
```

#### Opción B: Supervisión posterior (Complementaria)

**Para errores no detectados de inmediato:**

1. **Panel de Admin: Registros Recientes**
   - Ver últimos 50 registros
   - Filtrar por "Última hora"
   - Botón "Eliminar registro" (requiere confirmación)
   - Requiere rol ADMIN

2. **Auditoría completa**
   - Tabla `attendance_audit`:
     - `id`, `attendance_id`, `action` (CREATED, CANCELLED, DELETED_BY_ADMIN)
     - `performed_by_user_id`, `reason`, `timestamp`

---

## Problema 2: Selección Manual de ENTRADA/SALIDA

### Situación Actual (Automática)

```kotlin
// Sistema decide automáticamente
val lastRecord = repository.getLastRecordForEmployee(employeeId)
val recordType = if (lastRecord?.type == AttendanceType.ENTRY) {
    AttendanceType.EXIT
} else {
    AttendanceType.ENTRY
}
```

**Problemas:**
- ❌ Usuario no puede corregir si el sistema se equivocó
- ❌ Si olvidó marcar salida ayer, hoy no puede marcar entrada
- ❌ Sin flexibilidad para casos especiales

### Solución Propuesta: Selección Manual con Validación

#### Flujo de Usuario

**Pantalla Inicial:**
```
┌─────────────────────────────┐
│  Registrar Asistencia       │
├─────────────────────────────┤
│                             │
│  ¿Qué deseas registrar?     │
│                             │
│  ┌───────────┐ ┌──────────┐│
│  │  ENTRADA  │ │  SALIDA  ││
│  │    🏢     │ │    🏠    ││
│  └───────────┘ └──────────┘│
│                             │
│  Última: SALIDA - Ayer 6pm  │
└─────────────────────────────┘
```

**Validación en Tiempo Real:**
```kotlin
// Usuario selecciona ENTRADA
if (lastRecord?.type == AttendanceType.ENTRY && !lastRecord.hasExit) {
    showError("Ya tienes una ENTRADA sin SALIDA (${lastRecord.timestamp})")
    // Opción 1: Desactivar botón ENTRADA
    // Opción 2: Mostrar advertencia + permitir forzar (solo ADMIN)
}

// Usuario selecciona SALIDA
if (lastRecord == null || lastRecord.type == AttendanceType.EXIT) {
    showError("No puedes marcar SALIDA sin ENTRADA previa")
    // Solo permitir si es ADMIN con confirmación
}
```

#### Implementación

**Estado en ViewModel:**
```kotlin
data class RecognitionUiState {
    val selectedType: AttendanceType? = null
    val lastRecord: AttendanceRecord? = null
    val canSelectEntry: Boolean = true
    val canSelectExit: Boolean = false
    val warningMessage: String? = null
}
```

**Validación:**
```kotlin
fun selectAttendanceType(type: AttendanceType) {
    val last = _lastRecord.value

    when (type) {
        AttendanceType.ENTRY -> {
            if (last?.type == AttendanceType.ENTRY) {
                _uiState.value = RecognitionUiState.Error(
                    "Ya registraste ENTRADA a las ${last.timestamp.format()}" +
                    "\nDebes registrar SALIDA primero."
                )
                return
            }
        }
        AttendanceType.EXIT -> {
            if (last?.type == AttendanceType.EXIT || last == null) {
                _uiState.value = RecognitionUiState.Error(
                    "No puedes registrar SALIDA sin ENTRADA previa"
                )
                return
            }
        }
    }

    _selectedType.value = type
    _uiState.value = RecognitionUiState.ReadyToScan
}
```

**Casos Especiales (Solo ADMIN):**

Si es usuario ADMIN, mostrar opción adicional:
```
⚠️ Advertencia: Ya tienes ENTRADA sin SALIDA
¿Qué deseas hacer?
[ ] Marcar ENTRADA de todos modos (corregir error anterior)
[ ] Cancelar y marcar SALIDA primero
```

---

## Arquitectura de Auditoría

### Nueva Tabla: `attendance_audit`

```sql
CREATE TABLE attendance_audit (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    attendance_id INTEGER,  -- Puede ser NULL si fue cancelado antes de guardar
    action TEXT NOT NULL,   -- CREATED, CANCELLED_BY_USER, DELETED_BY_ADMIN, FORCED_BY_ADMIN
    employee_id_detected TEXT,  -- Quién fue detectado (puede ser incorrecto)
    employee_id_actual TEXT,    -- Quién realmente era (si se corrigió)
    performed_by_user_id INTEGER,  -- Qué usuario admin hizo la acción
    reason TEXT,
    metadata TEXT,  -- JSON con detalles adicionales
    timestamp INTEGER NOT NULL,

    FOREIGN KEY (attendance_id) REFERENCES attendance_records(id)
)
```

### Ejemplo de Logs de Auditoría

**Caso 1: Usuario rechaza identificación**
```json
{
  "action": "CANCELLED_BY_USER",
  "employee_id_detected": "EMP002",
  "employee_id_actual": null,
  "reason": "Usuario rechazó identificación",
  "metadata": {
    "confidence": 0.87,
    "timestamp_shown": "2025-01-24T08:05:23Z",
    "rejection_timestamp": "2025-01-24T08:05:30Z"
  }
}
```

**Caso 2: Admin elimina registro incorrecto**
```json
{
  "action": "DELETED_BY_ADMIN",
  "attendance_id": 12345,
  "employee_id_detected": "EMP002",
  "performed_by_user_id": 1,
  "reason": "Registro incorrecto - era otra persona",
  "metadata": {
    "original_type": "ENTRY",
    "original_timestamp": "2025-01-24T08:05:23Z"
  }
}
```

**Caso 3: Admin fuerza entrada duplicada**
```json
{
  "action": "FORCED_BY_ADMIN",
  "attendance_id": 12346,
  "employee_id_detected": "EMP001",
  "performed_by_user_id": 1,
  "reason": "Corrigiendo registro - olvidó marcar salida ayer",
  "metadata": {
    "type": "ENTRY",
    "previous_entry_without_exit": 12340
  }
}
```

---

## Historias de Usuario Nuevas

### US-038 [Alta] - Corrección Inmediata de Identificación Errónea
**Como** empleado
**Quiero** poder rechazar una identificación incorrecta inmediatamente
**Para** evitar registros falsos en mi asistencia

**Criterios de Aceptación**:
- [ ] Diálogo de éxito muestra botón "Este no soy yo"
- [ ] Al presionar, se cancela el registro
- [ ] Se registra en auditoría la cancelación
- [ ] Usuario vuelve a pantalla de reconocimiento
- [ ] Sistema permite re-intentar inmediatamente

**Estimación**: S (1-3 días)
**Sprint**: 3

---

### US-039 [Alta] - Selección Manual de ENTRADA/SALIDA
**Como** empleado
**Quiero** seleccionar manualmente si estoy marcando ENTRADA o SALIDA
**Para** tener control sobre mi registro de asistencia

**Criterios de Aceptación**:
- [ ] Pantalla inicial muestra dos botones: ENTRADA y SALIDA
- [ ] Sistema muestra último registro para contexto
- [ ] Validación: No permitir SALIDA sin ENTRADA previa
- [ ] Validación: No permitir ENTRADA si ya hay entrada sin salida
- [ ] Mensaje de error claro explicando el problema
- [ ] Botones deshabilitados visualmente si no son válidos

**Estimación**: M (3-5 días)
**Sprint**: 3

---

### US-040 [Media] - Panel de Admin para Corregir Registros
**Como** administrador
**Quiero** ver y corregir registros recientes de asistencia
**Para** solucionar errores de identificación no detectados inmediatamente

**Criterios de Aceptación**:
- [ ] Pantalla "Registros Recientes" (últimos 50)
- [ ] Filtros: Por fecha, Por empleado, Última hora
- [ ] Botón "Eliminar" en cada registro (requiere confirmación)
- [ ] Dialog de confirmación pide razón de eliminación
- [ ] Se registra en auditoría quién eliminó y por qué
- [ ] Solo accesible para rol ADMIN

**Estimación**: M (3-5 días)
**Sprint**: 5

---

### US-041 [Media] - Sistema de Auditoría
**Como** administrador
**Quiero** ver historial de correcciones y acciones administrativas
**Para** auditoría y control de calidad

**Criterios de Aceptación**:
- [ ] Tabla `attendance_audit` en base de datos
- [ ] Registrar: Creación, Cancelación por usuario, Eliminación por admin
- [ ] Pantalla de auditoría accesible solo para ADMIN
- [ ] Filtros: Por empleado, Por acción, Por fecha
- [ ] Exportar log de auditoría a CSV
- [ ] Retención configurable (default 180 días)

**Estimación**: M (3-5 días)
**Sprint**: 5

---

### US-042 [Baja] - Forzar Registro (Solo ADMIN)
**Como** administrador
**Quiero** forzar un registro aunque viole las reglas
**Para** corregir situaciones especiales

**Criterios de Aceptación**:
- [ ] Si usuario es ADMIN, mostrar opción "Forzar de todos modos"
- [ ] Dialog de advertencia explicando la violación
- [ ] Requiere ingresar razón obligatoria
- [ ] Se registra en auditoría como FORCED_BY_ADMIN
- [ ] Solo disponible para ENTRADA duplicada (no SALIDA sin entrada)

**Estimación**: S (1-3 días)
**Sprint**: 6

---

## Flujos Completos

### Flujo 1: Reconocimiento Exitoso Normal
```
1. Usuario selecciona ENTRADA o SALIDA
2. Sistema valida selección → ✅ Válido
3. Usuario escanea rostro
4. Sistema reconoce correctamente → ✅ Juan López (92%)
5. Dialog: "✅ ¡Correcto!" | "❌ Este no soy yo"
6. Usuario presiona "✅ Correcto"
7. Registro guardado exitosamente
```

### Flujo 2: Usuario Rechaza Identificación
```
1. Usuario selecciona ENTRADA
2. Usuario escanea rostro
3. Sistema reconoce INCORRECTAMENTE → ❌ Pedro García (88%)
4. Dialog: "✅ ¡Correcto!" | "❌ Este no soy yo"
5. Usuario presiona "❌ Este no soy yo"
6. Sistema:
   - Cancela el registro (no lo guarda)
   - Log auditoría: CANCELLED_BY_USER
   - Vuelve a pantalla inicial
7. Usuario vuelve a intentar
```

### Flujo 3: Validación Previene Error
```
1. Usuario tiene ENTRADA registrada hoy a las 8:00 AM (sin salida)
2. Usuario selecciona ENTRADA nuevamente
3. Sistema valida → ❌ Invalido
4. Sistema muestra:
   "Ya tienes ENTRADA sin SALIDA (Hoy 8:00 AM)
    Debes registrar SALIDA primero."
5. Botón ENTRADA deshabilitado
6. Usuario puede:
   - Seleccionar SALIDA (habilitado)
   - O contactar admin para corregir
```

### Flujo 4: Admin Fuerza Registro
```
1. Usuario ADMIN selecciona ENTRADA
2. Tiene ENTRADA sin salida del día anterior
3. Sistema muestra advertencia:
   "⚠️ Ya tienes ENTRADA sin SALIDA (Ayer 8:00 AM)
    ¿Forzar ENTRADA de todos modos?"
4. Admin presiona "Sí, forzar"
5. Dialog: "Razón: _____________" (obligatorio)
6. Admin escribe: "Olvidó marcar salida ayer"
7. Sistema guarda con flag `forced=true`
8. Log auditoría: FORCED_BY_ADMIN
```

---

## Métricas de Calidad

### Indicadores a Monitorear

1. **Tasa de Rechazo de Identificación**
   - Meta: <2% de registros cancelados por usuarios
   - Si >5%: Revisar umbral de confianza

2. **Tiempo Promedio de Corrección**
   - Meta: Usuario rechaza en <10 segundos
   - Indica que el error fue evidente

3. **Registros Forzados por Admin**
   - Meta: <5 por semana
   - Si aumenta: Capacitar usuarios o ajustar UX

4. **Falsos Positivos Detectados Tardíamente**
   - Meta: <1 por mes (eliminados por admin después)
   - Estos son los MÁS peligrosos

---

## Principios de Diseño Establecidos

1. **"El usuario siempre tiene razón"**
   - Si dice que no es él, confiar inmediatamente
   - No poner barreras para corregir

2. **"Prevenir mejor que corregir"**
   - Validación estricta de ENTRADA/SALIDA
   - Feedback claro antes de escanear

3. **"Auditar todo"**
   - Cada cambio queda registrado
   - Trazabilidad completa para análisis

4. **"Flexibilidad con responsabilidad"**
   - Admin puede forzar, pero debe justificar
   - Registros forzados claramente marcados

---

## Próximos Pasos

1. ✅ Documento creado
2. ⏳ Actualizar `docs/01-BACKLOG.md` con US-038 a US-042
3. ⏳ Implementar US-039 primero (selección manual)
4. ⏳ Luego US-038 (botón "Este no soy yo")
5. ⏳ Crear tabla de auditoría
6. ⏳ Implementar panel de admin (US-040, US-041)

---

**Actualizado**: 2025-01-24
**Revisión**: v1.0
**Implementado**: 🚧 Pendiente
