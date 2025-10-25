# BACKLOG FASE 2 - Autenticación Administrativa y Aprobaciones

## 📊 Resumen de la Fase 2

**Fecha de Inicio**: 2025-10-25
**Estado Actual**: En Planificación
**Objetivo**: Implementar autenticación administrativa con huella, sistema de aprobación de registros pendientes, y multi-tenancy

**Decisiones Arquitectónicas Relacionadas**:
- [ADR-04](./04-DECISION-AUTENTICACION-HUELLA.md): Sistema de autenticación por huella con Android KeyStore
- Pendiente: ADR-05 - Sistema de aprobación de registros manuales

---

## 🎯 ÉPICAS Y HISTORIAS DE USUARIO

### 📋 ÉPICA 1: AUTENTICACIÓN ADMINISTRATIVA CON HUELLA

#### US-101 [Alta] - Autenticación Biométrica para Administradores
**Como** administrador
**Quiero** usar mi huella digital para autorizar operaciones críticas
**Para** tener mayor seguridad que solo usar PIN

**Contexto**:
Después de investigar las limitaciones de Android, se descubrió que BiometricPrompt NO puede identificar huellas específicas (cualquier huella registrada en el dispositivo desbloquea cualquier clave del KeyStore). Por lo tanto, la autenticación por huella se usará SOLO para administradores (2-3 personas), no para empleados (50+ personas).

**Criterios de Aceptación**:
- [ ] Entidad `User` tiene campos: `hasFingerprintEnabled`, `fingerprintKeystoreAlias`
- [ ] `UserRegistrationScreen` permite registrar huella (solo para rol ADMIN)
- [ ] Huella se vincula usando BiometricKeyManager con Android KeyStore
- [ ] Empleados NO tienen opción de registrar huella
- [ ] Operaciones críticas requieren huella del admin:
  - [ ] Eliminar empleados
  - [ ] Eliminar registros de asistencia
  - [ ] Exportar reportes a Excel/PDF
  - [ ] Cambiar URL del servidor
  - [ ] Eliminar usuarios
- [ ] Componente reutilizable `AdminBiometricPrompt` para todas las operaciones

**Estimación**: M (4-5 horas)
**Prioridad**: Alta
**Sprint**: 2.1
**Estado**: 🔄 Pendiente

---

#### US-102 [Media] - Migración de Código de Huella desde Empleados
**Como** desarrollador
**Quiero** mover el código de autenticación por huella de empleados a usuarios
**Para** aprovechar la implementación existente sin reescribirla

**Criterios de Aceptación**:
- [ ] Remover `hasFingerprintEnabled` y `fingerprintKeystoreAlias` de `Employee.kt`
- [ ] Agregar los mismos campos a `User.kt`
- [ ] Eliminar `FingerprintEnrollmentScreen` de flujo de registro de empleados
- [ ] Crear nueva pantalla de registro de huella en gestión de usuarios
- [ ] Actualizar `BiometricKeyManager` para usar `User` en lugar de `Employee`
- [ ] Migrar datos existentes si hay empleados con huella registrada
- [ ] Eliminar `BiometricAuthScreen` y `BiometricAuthViewModel` (ya no se usan para asistencia)

**Estimación**: S (2-3 horas)
**Prioridad**: Media
**Sprint**: 2.1
**Estado**: 🔄 Pendiente

---

### 📋 ÉPICA 2: REGISTROS PENDIENTES Y SISTEMA DE APROBACIÓN

#### US-201 [Crítica] - Registro Manual cuando Facial Falla
**Como** empleado
**Quiero** poder registrar mi asistencia manualmente si el sistema no reconoce mi rostro
**Para** no perder mi registro de asistencia por problemas técnicos

**Contexto**:
Existen casos donde el reconocimiento facial puede fallar:
- Mala iluminación
- Cambio drástico de apariencia (cirugía, barba nueva, etc.)
- Empleado nuevo sin embeddings registrados
- Problemas técnicos con la cámara

En estos casos, el empleado debe poder crear un registro manual que quedará pendiente de aprobación por un supervisor.

**Criterios de Aceptación**:
- [ ] Entidad `PendingAttendanceRecord` creada con campos:
  - employeeId, employeeName, timestamp, type (ENTRY/EXIT)
  - photoPath (foto capturada), deviceId, reason
  - status (PENDING/APPROVED/REJECTED)
  - reviewedBy, reviewedAt, reviewNotes
- [ ] `FaceRecognitionScreen` muestra botón "No me reconoció - Registrar manualmente" cuando:
  - Confianza facial < 70%
  - No se detecta ningún rostro después de 10 segundos
  - Empleado presiona botón manual
- [ ] Diálogo de registro manual solicita:
  - ID del empleado (input numérico)
  - Confirmación del tipo (ENTRADA/SALIDA)
- [ ] Sistema captura foto actual del frame de la cámara
- [ ] Foto se guarda en `/storage/emulated/0/Pictures/AttendanceApp/pending/{timestamp}.jpg`
- [ ] Registro se guarda en BD con status = PENDING
- [ ] Mensaje de confirmación: "Registro enviado para aprobación del supervisor"

**Estimación**: L (6-8 horas)
**Prioridad**: Crítica
**Sprint**: 2.2
**Estado**: 🔄 Pendiente

---

#### US-202 [Crítica] - Pantalla de Revisión para Supervisores
**Como** supervisor o administrador
**Quiero** ver todos los registros pendientes de aprobación
**Para** aprobar o rechazar registros manuales

**Criterios de Aceptación**:
- [ ] Nueva pantalla `PendingApprovalScreen` con:
  - Lista de registros pendientes ordenados por fecha (más recientes primero)
  - Filtros: Todos, Hoy, Última semana, Por empleado
  - Badge con contador de pendientes
- [ ] Cada tarjeta de registro muestra:
  - Foto capturada del empleado
  - ID y nombre del empleado
  - Fecha y hora del registro
  - Tipo (ENTRADA/SALIDA)
  - Razón (facial_failed, not_enrolled, manual_request)
  - Dispositivo que lo registró
- [ ] Botones de acción por registro:
  - ✓ Aprobar (verde)
  - ✗ Rechazar (rojo)
  - 👁 Ver detalles (ampliar foto)
- [ ] Modal de rechazo permite ingresar notas: "Por qué se rechaza"
- [ ] Acceso desde HomeScreen con icono de notificaciones + badge
- [ ] Solo visible para roles: ADMIN, SUPERVISOR

**Estimación**: M (5-6 horas)
**Prioridad**: Crítica
**Sprint**: 2.2
**Estado**: 🔄 Pendiente

---

#### US-203 [Alta] - Autenticación del Supervisor para Aprobar/Rechazar
**Como** sistema
**Quiero** verificar la identidad del supervisor antes de aprobar/rechazar
**Para** asegurar que solo supervisores autorizados toman estas decisiones

**Criterios de Aceptación**:
- [ ] Al presionar "Aprobar" o "Rechazar", se muestra diálogo de autenticación
- [ ] Supervisor puede elegir método:
  - Opción A: Huella digital (si tiene registrada)
  - Opción B: Reconocimiento facial
  - Opción C: PIN (fallback)
- [ ] Si autenticación falla, no se procesa la aprobación/rechazo
- [ ] Si autenticación exitosa:
  - Registro pendiente cambia status a APPROVED o REJECTED
  - Se guarda `reviewedBy` con ID del supervisor
  - Se guarda `reviewedAt` con timestamp actual
  - Si APPROVED: Se crea registro real en `AttendanceRecord`
  - Si REJECTED: Solo se actualiza status (no se crea registro)
- [ ] Auditoría completa en `AttendanceAudit` con:
  - Acción: PENDING_APPROVED o PENDING_REJECTED
  - Metadata: supervisorId, reason, authMethod

**Estimación**: M (4-5 horas)
**Prioridad**: Alta
**Sprint**: 2.2
**Estado**: 🔄 Pendiente

---

#### US-204 [Media] - Notificaciones de Registros Pendientes
**Como** supervisor
**Quiero** recibir notificaciones cuando hay registros pendientes
**Para** revisarlos oportunamente

**Criterios de Aceptación**:
- [ ] Badge en icono de notificaciones de HomeScreen muestra contador
- [ ] Contador se actualiza en tiempo real (Flow de Room)
- [ ] Al entrar a PendingApprovalScreen, contador se resetea
- [ ] Opcional: Notificación push local si hay pendientes > 2 horas sin revisar
- [ ] Opcional: Sonido/vibración cuando se crea nuevo pendiente (solo si app está abierta)

**Estimación**: S (2-3 horas)
**Prioridad**: Media
**Sprint**: 2.2
**Estado**: 🔄 Pendiente

---

#### US-205 [Baja] - Expiración de Registros Pendientes
**Como** sistema
**Quiero** marcar como expirados los registros pendientes después de 7 días
**Para** mantener la lista limpia y forzar decisiones oportunas

**Criterios de Aceptación**:
- [ ] Job en WorkManager que corre diariamente
- [ ] Busca registros con status = PENDING y createdAt > 7 días
- [ ] Cambia status a EXPIRED
- [ ] Registros expirados se muestran en pestaña separada (solo lectura)
- [ ] Supervisor puede configurar días de expiración en Settings (1-30 días)

**Estimación**: S (2-3 horas)
**Prioridad**: Baja
**Sprint**: 2.3 (Opcional)
**Estado**: 🔄 Pendiente

---

### 📋 ÉPICA 3: MULTI-TENANCY Y SINCRONIZACIÓN EN LA NUBE

#### US-301 [Alta] - Código de Tenant por Empresa
**Como** empresa cliente
**Quiero** que mi tablet use un código único de empresa (tenant)
**Para** que mis datos estén separados de otras empresas en la nube

**Contexto**:
El backend está diseñado para multi-tenancy. Cada empresa tiene un código único (ej: "EMPRESA-ABC-2024"). Todos los endpoints de la API requieren este código para segmentar datos.

**Criterios de Aceptación**:
- [ ] Crear `TenantManager` con DataStore para almacenar:
  - tenantCode: String
  - tenantName: String
  - serverUrl: String
- [ ] `DeviceActivationScreen` tiene paso adicional:
  - Paso 1: Ingresar código de empresa
  - Paso 2: Validar código con servidor (GET /api/tenants/{code})
  - Paso 3: Si válido, guardar tenantCode localmente
  - Paso 4: Activar dispositivo
- [ ] Agregar campo `tenantCode` a entidad `User`
- [ ] Crear `TenantInterceptor` que agrega header `X-Tenant-Code` a todas las requests
- [ ] Actualizar todos los DTOs de autenticación/sincronización con tenantCode:
  - LoginRequestDto
  - DeviceActivationRequestDto
  - SyncEmployeesRequestDto
  - SyncAttendanceRequestDto

**Estimación**: M (4-5 horas)
**Prioridad**: Alta
**Sprint**: 2.3
**Estado**: 🔄 Pendiente

---

#### US-302 [Media] - Validación de Tenant en Activación
**Como** sistema
**Quiero** validar que el código de tenant existe en el servidor
**Para** evitar activaciones con códigos incorrectos

**Criterios de Aceptación**:
- [ ] Endpoint `GET /api/tenants/{code}` retorna:
  ```json
  {
    "code": "EMPRESA-ABC-2024",
    "name": "Empresa ABC S.A.",
    "active": true,
    "maxDevices": 5
  }
  ```
- [ ] Si código no existe: Mostrar error "Código de empresa inválido"
- [ ] Si código existe pero `active = false`: "Empresa inactiva, contacte soporte"
- [ ] Si código válido: Guardar `tenantCode` y `tenantName` en TenantManager
- [ ] Mostrar nombre de empresa en pantalla de activación para confirmación

**Estimación**: S (2 horas)
**Prioridad**: Media
**Sprint**: 2.3
**Estado**: 🔄 Pendiente

---

#### US-303 [Media] - Mostrar Tenant en Configuración
**Como** administrador
**Quiero** ver qué empresa/tenant está configurada en el dispositivo
**Para** confirmar que está correctamente configurado

**Criterios de Aceptación**:
- [ ] SettingsScreen muestra sección "Información de Empresa":
  - Código de Empresa: EMPRESA-ABC-2024
  - Nombre: Empresa ABC S.A.
  - Servidor: https://api.attendance.com
- [ ] Botón "Cambiar Empresa" (solo si no hay datos sincronizados):
  - Muestra advertencia: "Esto borrará todos los datos locales"
  - Requiere autenticación de ADMIN
  - Limpia tenantCode y vuelve a DeviceActivationScreen

**Estimación**: XS (1-2 horas)
**Prioridad**: Media
**Sprint**: 2.3
**Estado**: 🔄 Pendiente

---

## 📅 PLANIFICACIÓN DE SPRINTS

### Sprint 2.1: Autenticación Administrativa (1-2 días)
**Objetivo**: Mover autenticación por huella de empleados a administradores

**Tareas**:
1. US-102: Migrar código de huella a usuarios
2. US-101: Implementar autenticación administrativa
3. Testing de operaciones protegidas

**Estimación Total**: 6-8 horas

---

### Sprint 2.2: Sistema de Aprobaciones (2-3 días)
**Objetivo**: Implementar flujo completo de registros pendientes

**Tareas**:
1. US-201: Registro manual cuando facial falla
2. US-202: Pantalla de revisión para supervisores
3. US-203: Autenticación del supervisor
4. US-204: Notificaciones de pendientes
5. Testing del flujo completo

**Estimación Total**: 17-22 horas

---

### Sprint 2.3: Multi-Tenancy (1-2 días)
**Objetivo**: Implementar soporte de multi-tenancy para sincronización en la nube

**Tareas**:
1. US-301: Código de tenant por empresa
2. US-302: Validación de tenant
3. US-303: Mostrar tenant en configuración
4. Testing de sincronización multi-tenant

**Estimación Total**: 7-9 horas

---

### Sprint 2.4: Documentación (medio día)
**Objetivo**: Actualizar toda la documentación

**Tareas**:
1. Crear ADR-05: Sistema de aprobación de registros manuales
2. Actualizar CLAUDE.md con nuevas pantallas
3. Actualizar 04-API-REQUIREMENTS.md con tenant
4. Actualizar README.md con nuevas funcionalidades

**Estimación Total**: 2-3 horas

---

## 📊 ESTIMACIÓN TOTAL DE LA FASE 2

- **Sprint 2.1**: 6-8 horas (1-2 días)
- **Sprint 2.2**: 17-22 horas (2-3 días)
- **Sprint 2.3**: 7-9 horas (1-2 días)
- **Sprint 2.4**: 2-3 horas (0.5 días)

**Total estimado**: 32-42 horas (4-6 días de trabajo)

---

## 🎯 DEFINICIÓN DE "DONE"

Una historia se considera completa cuando:
- ✅ Código implementado y compila sin errores
- ✅ UI funciona según diseño
- ✅ Casos de error manejados correctamente
- ✅ Testing manual exitoso
- ✅ Código comentado en partes complejas
- ✅ Documentación actualizada (si aplica)
- ✅ Cambios de BD documentados con versión incrementada

---

## 📝 NOTAS TÉCNICAS

### Almacenamiento de Fotos Pendientes

**Decisión**: Guardar fotos en sistema de archivos, no en BD

**Ruta**: `/storage/emulated/0/Pictures/AttendanceApp/pending/{timestamp}_{employeeId}.jpg`

**Razones**:
- Fotos pueden ser grandes (500KB - 2MB)
- SQLite no es eficiente para BLOBs grandes
- Más fácil de depurar (puedes ver las fotos)
- Sincronización puede comprimir antes de enviar

**Limpieza**:
- Fotos de registros APPROVED/REJECTED se eliminan después de 30 días
- Job de WorkManager corre semanalmente para limpiar

### Roles con Acceso a Aprobaciones

**Decisión**: ADMIN y SUPERVISOR pueden aprobar/rechazar

**Razones**:
- ADMIN: Control total
- SUPERVISOR: Puede estar en el piso de producción sin ser admin
- USER: Solo puede registrar asistencia, no aprobar

### Sincronización de Registros Pendientes

**Decisión**: Solo se sincronizan cuando se aprueban

**Flujo**:
1. Registro manual → Se guarda local como PENDING (NO se sincroniza)
2. Supervisor aprueba → Se crea AttendanceRecord → Se sincroniza con nube
3. Supervisor rechaza → Permanece local, NO se sincroniza nunca

**Razón**: El backend solo debe tener registros válidos, no pendientes
