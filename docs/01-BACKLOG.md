# BACKLOG - Sistema de Asistencia Biométrica Offline

## 📊 Resumen del Proyecto

**Estado Actual**: Reconocimiento facial básico funcional
**Objetivo**: Sistema robusto de control de asistencia offline con múltiples métodos biométricos, sincronización y gestión completa

**Stack Tecnológico**:
- Frontend: Kotlin + Jetpack Compose
- DB Local: Room + SQLCipher (encriptación)
- Biometría: ML Kit Face Detection + Android BiometricPrompt
- Sincronización: WorkManager + Retrofit
- Seguridad: Encrypted SharedPreferences, Android Keystore

---

## 🎯 ÉPICAS Y HISTORIAS DE USUARIO

### 📋 ÉPICA 1: SEGURIDAD Y AUTENTICACIÓN

#### US-001 [Alta] - Autenticación por PIN
**Como** sistema
**Quiero** requerir autenticación por PIN/contraseña
**Para** controlar acceso a funciones administrativas

**Criterios de Aceptación**:
- [ ] Pantalla de login con campo PIN de 4-6 dígitos
- [ ] Almacenamiento seguro usando Encrypted SharedPreferences
- [ ] Sesión con timeout configurable (default: 30 min)
- [ ] Lock automático al minimizar app
- [ ] Límite de intentos fallidos (3 intentos)

**Estimación**: M (3-5 días)
**Prioridad**: Alta
**Sprint**: 1

---

#### US-002 [Alta] - Gestión de Roles
**Como** administrador
**Quiero** gestionar roles de usuario (Admin, Supervisor, Usuario)
**Para** controlar permisos por tipo de usuario

**Criterios de Aceptación**:
- [ ] Entidad User en DB con campos: id, name, pin_hash, role, created_at
- [ ] Roles: ADMIN, SUPERVISOR, USER
- [ ] CRUD de usuarios (solo ADMIN puede crear/editar)
- [ ] Permisos definidos por rol:
  - ADMIN: Todo
  - SUPERVISOR: Ver reportes, ver empleados (no crear/editar)
  - USER: Solo marcar asistencia
- [ ] Usuario ADMIN por defecto en primera instalación

**Estimación**: M (3-5 días)
**Prioridad**: Alta
**Sprint**: 1

---

#### US-003 [Media] - Encriptación de DB
**Como** sistema
**Debo** encriptar datos sensibles en la base de datos local
**Para** proteger información biométrica y personal

**Criterios de Aceptación**:
- [ ] Integrar SQLCipher o Room encryption
- [ ] Encriptar embeddings faciales
- [ ] Encriptar datos biométricos (huellas)
- [ ] Key derivada de PIN del dispositivo + Android Keystore
- [ ] Migración de DB existente sin pérdida de datos

**Estimación**: S (1-3 días)
**Prioridad**: Media
**Sprint**: 1

---

#### US-004 [Media] - Permisos para Registro de Empleados
**Como** administrador
**Quiero** definir qué roles pueden registrar empleados
**Para** prevenir registros no autorizados

**Criterios de Aceptación**:
- [ ] Solo ADMIN puede acceder a "Registrar Empleado"
- [ ] Botón oculto para otros roles
- [ ] Mensaje de error si rol insuficiente intenta acceder vía deep link

**Estimación**: XS (< 1 día)
**Prioridad**: Media
**Sprint**: 1

---

### 👥 ÉPICA 2: ADMINISTRACIÓN DE EMPLEADOS

#### US-005 [Alta] - Lista de Empleados
**Como** administrador
**Quiero** ver lista de todos los empleados registrados
**Para** administrar el personal del sistema

**Criterios de Aceptación**:
- [ ] Pantalla EmployeeListScreen con lista scrollable
- [ ] Items muestran: foto, nombre, ID, departamento, estado
- [ ] Búsqueda por nombre o ID
- [ ] Filtros: Por departamento, Por estado (activo/inactivo)
- [ ] Ordenar por: Nombre, Fecha registro, Departamento
- [ ] Contador total de empleados
- [ ] Click en item navega a detalles

**Estimación**: S (1-3 días)
**Prioridad**: Alta
**Sprint**: 1
**Estado**: ⏳ Pendiente

---

#### US-006 [Alta] - Detalles de Empleado
**Como** administrador
**Quiero** ver detalles completos de un empleado
**Para** verificar su información y estado

**Criterios de Aceptación**:
- [ ] Pantalla EmployeeDetailScreen
- [ ] Mostrar: Foto facial, Nombre completo, ID empleado, Departamento, Cargo, Fecha registro, Estado (activo/inactivo)
- [ ] Indicador de métodos biométricos registrados (facial ✓, huella ✓/✗)
- [ ] Botones: Editar, Eliminar, Activar/Desactivar
- [ ] Confirmación antes de acciones destructivas

**Estimación**: XS (< 1 día)
**Prioridad**: Alta
**Sprint**: 1
**Estado**: ⏳ Pendiente

---

#### US-007 [Alta] - Eliminar Empleados
**Como** administrador
**Quiero** eliminar empleados del sistema
**Para** dar de baja personal que ya no trabaja

**Criterios de Aceptación**:
- [ ] Botón "Eliminar" en pantalla de detalles
- [ ] Dialog de confirmación con advertencia
- [ ] Eliminación en cascada: empleado + embeddings + registros asistencia
- [ ] Opción de "soft delete" (marcar inactivo) vs eliminación permanente
- [ ] Log de auditoría de eliminaciones

**Estimación**: XS (< 1 día)
**Prioridad**: Alta
**Sprint**: 1
**Estado**: ⏳ Pendiente

---

#### US-008 [Media] - Editar Información de Empleados
**Como** administrador
**Quiero** editar información de empleados
**Para** actualizar datos sin re-registrar biometría

**Criterios de Aceptación**:
- [ ] Pantalla EmployeeEditScreen
- [ ] Campos editables: Nombre, Departamento, Cargo, Estado
- [ ] NO editable: ID empleado, Datos biométricos
- [ ] Validaciones: Campos requeridos, formatos
- [ ] Botón "Guardar cambios"
- [ ] Registro de última modificación (timestamp + user)

**Estimación**: S (1-3 días)
**Prioridad**: Media
**Sprint**: 5
**Estado**: ⏳ Pendiente

---

#### US-009 [Media] - Activar/Desactivar Empleados
**Como** administrador
**Quiero** desactivar empleados sin eliminarlos
**Para** suspender temporalmente sin perder datos

**Criterios de Aceptación**:
- [ ] Toggle switch en pantalla de detalles
- [ ] Empleados inactivos no pueden marcar asistencia
- [ ] Aparecen con indicador visual en lista (gris, "INACTIVO")
- [ ] Filtro para mostrar solo activos/solo inactivos/todos

**Estimación**: XS (< 1 día)
**Prioridad**: Media
**Sprint**: 6
**Estado**: ⏳ Pendiente

---

#### US-010 [Baja] - Exportar Lista de Empleados
**Como** administrador
**Quiero** exportar lista de empleados a CSV
**Para** usar datos en otras herramientas

**Criterios de Aceptación**:
- [ ] Botón "Exportar" en EmployeeListScreen
- [ ] Generar CSV con: ID, Nombre, Departamento, Cargo, Estado, Fecha Registro
- [ ] Guardar en Downloads/FaceRecognition/
- [ ] Notificación con ruta del archivo
- [ ] Respetar filtros activos al exportar

**Estimación**: S (1-3 días)
**Prioridad**: Baja
**Sprint**: Backlog
**Estado**: ⏳ Pendiente

---

### 👆 ÉPICA 3: RECONOCIMIENTO BIOMÉTRICO - HUELLA DIGITAL

#### US-011 [Alta] - Registrar Huella Digital
**Como** empleado
**Quiero** registrar mi huella digital
**Para** tener método alternativo de identificación

**Criterios de Aceptación**:
- [ ] Integrar Android BiometricPrompt API
- [ ] Capturar al menos 2 muestras de huella
- [ ] Almacenar template encriptado en DB
- [ ] Soporte para múltiples huellas (índice + pulgar)
- [ ] Feedback visual del proceso de captura
- [ ] Validación de calidad de huella

**Estimación**: M (3-5 días)
**Prioridad**: Alta
**Sprint**: 2
**Estado**: ⏳ Pendiente

---

#### US-012 [Alta] - Reconocimiento por Huella
**Como** empleado
**Quiero** marcar asistencia usando mi huella
**Para** registrar cuando facial no funcione

**Criterios de Aceptación**:
- [ ] Pantalla de reconocimiento con opción "Usar Huella"
- [ ] Verificación 1:N contra todas las huellas registradas
- [ ] Threshold de similitud configurable
- [ ] Tiempo máximo de verificación: 3 segundos
- [ ] Registrar tipo de biometría usado en AttendanceRecord

**Estimación**: M (3-5 días)
**Prioridad**: Alta
**Sprint**: 2
**Estado**: ⏳ Pendiente

---

#### US-013 [Media] - Configurar Método Prioritario
**Como** administrador
**Quiero** configurar qué método biométrico usar primero
**Para** optimizar flujo según preferencia

**Criterios de Aceptación**:
- [ ] Setting: "Método primario: Facial / Huella / Preguntar"
- [ ] Si primario falla, ofrecer alternativa automáticamente
- [ ] Mostrar método usado en pantalla de reconocimiento
- [ ] Guardar preferencia por dispositivo

**Estimación**: S (1-3 días)
**Prioridad**: Media
**Sprint**: 2
**Estado**: ⏳ Pendiente

---

#### US-014 [Media] - Validar Calidad de Huella
**Como** sistema
**Debo** validar calidad de huella capturada
**Para** evitar registros defectuosos

**Criterios de Aceptación**:
- [ ] Detectar huellas borrosas
- [ ] Detectar presión insuficiente
- [ ] Detectar área de contacto muy pequeña
- [ ] Mensaje de error específico según problema
- [ ] Máximo 5 reintentos antes de cancelar

**Estimación**: M (3-5 días)
**Prioridad**: Media
**Sprint**: 2
**Estado**: ⏳ Pendiente

---

### 📊 ÉPICA 4: REGISTRO DE ASISTENCIA MEJORADO

#### US-015 [Media] - Dashboard Tiempo Real
**Como** supervisor
**Quiero** ver quién ha marcado asistencia hoy
**Para** monitorear presencia en tiempo real

**Criterios de Aceptación**:
- [ ] Pantalla Dashboard con tabs: "Entradas Hoy" / "Salidas Hoy"
- [ ] Lista con: Foto, Nombre, Hora, Método usado
- [ ] Auto-refresh cada 30 segundos
- [ ] Contador: "15/50 empleados han entrado"
- [ ] Filtro por departamento

**Estimación**: S (1-3 días)
**Prioridad**: Media
**Sprint**: 3
**Estado**: ⏳ Pendiente

---

#### US-016 [DEPRECADA] - Prevenir Duplicados
⚠️ **NOTA**: Esta historia fue reemplazada por US-039 (Selección Manual de ENTRADA/SALIDA) que incluye validación de duplicados más robusta.

---

#### US-038 [Alta] - Corrección Inmediata de Identificación Errónea
**Como** empleado
**Quiero** poder rechazar una identificación incorrecta inmediatamente
**Para** evitar registros falsos en mi asistencia

**Criterios de Aceptación**:
- [ ] Diálogo de éxito muestra botón "Este no soy yo" además de "Correcto"
- [ ] Al presionar "Este no soy yo", se cancela el registro antes de guardarlo
- [ ] Se registra en tabla de auditoría la cancelación
- [ ] Usuario vuelve automáticamente a pantalla de reconocimiento
- [ ] Sistema permite re-intentar reconocimiento inmediatamente
- [ ] No se guarda nada en `attendance_records` si fue cancelado
- [ ] Log incluye: timestamp, empleado detectado, confianza

**Estimación**: S (1-3 días)
**Prioridad**: Alta
**Sprint**: 3
**Estado**: ⏳ Pendiente

---

#### US-039 [Alta] - Selección Manual de ENTRADA/SALIDA
**Como** empleado
**Quiero** seleccionar manualmente si estoy marcando ENTRADA o SALIDA
**Para** tener control sobre mi registro de asistencia

**Criterios de Aceptación**:
- [ ] Pantalla inicial de reconocimiento muestra dos botones grandes: ENTRADA 🏢 y SALIDA 🏠
- [ ] Sistema consulta y muestra último registro del usuario para contexto
- [ ] **Validación ENTRADA**: No permitir si ya hay entrada sin salida correspondiente
  - Mensaje: "Ya tienes ENTRADA sin SALIDA (Hoy 8:00 AM). Debes marcar SALIDA primero."
  - Botón ENTRADA deshabilitado visualmente
- [ ] **Validación SALIDA**: No permitir si no hay entrada previa o última fue salida
  - Mensaje: "No puedes marcar SALIDA sin ENTRADA previa"
  - Botón SALIDA deshabilitado visualmente
- [ ] Una vez seleccionado tipo válido, proceder a escaneo facial
- [ ] Guardar tipo seleccionado en `AttendanceRecord.type`

**Estimación**: M (3-5 días)
**Prioridad**: Alta
**Sprint**: 3
**Estado**: ⏳ Pendiente

---

#### US-040 [Media] - Panel de Admin para Corregir Registros
**Como** administrador
**Quiero** ver y corregir registros recientes de asistencia
**Para** solucionar errores de identificación no detectados inmediatamente

**Criterios de Aceptación**:
- [ ] Nueva pantalla "Registros Recientes" accesible desde HomeScreen
- [ ] Lista muestra últimos 50 registros ordenados por fecha desc
- [ ] Cada item muestra: Empleado, Tipo (ENTRADA/SALIDA), Hora, Confianza, Método biométrico
- [ ] Filtros:
  - Por fecha (DatePicker)
  - Por empleado (Selector)
  - "Última hora" (toggle rápido)
- [ ] Botón "Eliminar" en cada registro (icono 🗑️)
- [ ] Dialog de confirmación:
  - "¿Eliminar registro de Juan López - ENTRADA 8:05 AM?"
  - Campo obligatorio: "Razón de eliminación: ___________"
  - Botones: "Cancelar" / "Sí, eliminar"
- [ ] Al eliminar:
  - Marca registro como `deleted=true` (soft delete)
  - Crea entry en `attendance_audit`
  - Toast: "Registro eliminado"
- [ ] Solo accesible para rol ADMIN

**Estimación**: M (3-5 días)
**Prioridad**: Media
**Sprint**: 5
**Estado**: ⏳ Pendiente

---

#### US-041 [Media] - Sistema de Auditoría
**Como** administrador
**Quiero** ver historial de correcciones y acciones administrativas
**Para** auditoría y control de calidad del sistema

**Criterios de Aceptación**:
- [ ] Nueva tabla `attendance_audit` en base de datos:
  ```sql
  CREATE TABLE attendance_audit (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      attendance_id INTEGER,
      action TEXT NOT NULL,  -- CREATED, CANCELLED_BY_USER, DELETED_BY_ADMIN, FORCED_BY_ADMIN
      employee_id_detected TEXT,
      employee_id_actual TEXT,
      performed_by_user_id INTEGER,
      reason TEXT,
      metadata TEXT,  -- JSON
      timestamp INTEGER NOT NULL
  )
  ```
- [ ] Pantalla "Auditoría" accesible solo para ADMIN
- [ ] Lista de todos los eventos de auditoría
- [ ] Filtros: Por empleado, Por acción, Por rango de fechas
- [ ] Cada item muestra: Acción, Empleado, Usuario que realizó acción, Razón, Fecha
- [ ] Botón "Exportar a CSV"
- [ ] Configuración de retención: "Mantener logs de auditoría por X días" (default 180)
- [ ] Job automático para limpiar logs antiguos

**Estimación**: M (3-5 días)
**Prioridad**: Media
**Sprint**: 5
**Estado**: ⏳ Pendiente

---

#### US-042 [Baja] - Forzar Registro (Solo ADMIN)
**Como** administrador
**Quiero** forzar un registro aunque viole las reglas de validación
**Para** corregir situaciones especiales o errores del usuario

**Criterios de Aceptación**:
- [ ] Si usuario autenticado es ADMIN, mostrar opción especial en validación
- [ ] Cuando validación detecta entrada duplicada:
  - Usuario normal: Solo mensaje de error
  - ADMIN: Mensaje + botón "Forzar de todos modos"
- [ ] Al presionar "Forzar de todos modos":
  - Dialog: "⚠️ Advertencia: Forzar ENTRADA con entrada previa sin salida"
  - Campo obligatorio: "Razón: ___________" (min 10 caracteres)
  - Botones: "Cancelar" / "Sí, forzar"
- [ ] Registro guardado con flag `forced=true`
- [ ] Entry en auditoría con action=FORCED_BY_ADMIN
- [ ] Metadata incluye: previous_entry_id, violation_type
- [ ] Solo permitir forzar ENTRADA duplicada (NO salida sin entrada)

**Estimación**: S (1-3 días)
**Prioridad**: Baja
**Sprint**: 6
**Estado**: ⏳ Pendiente

---

#### US-017 [Baja] - Mostrar Último Registro
**Como** empleado
**Quiero** ver mi último registro después de marcar
**Para** confirmar que se guardó correctamente

**Criterios de Aceptación**:
- [ ] Pantalla de éxito muestra: "Última entrada: Hoy 8:05 AM"
- [ ] Si es salida, calcular tiempo trabajado
- [ ] Mostrar método biométrico usado

**Estimación**: XS (< 1 día)
**Prioridad**: Baja
**Sprint**: Backlog
**Estado**: ⏳ Pendiente

---

#### US-018 [Baja] - Registrar Método Biométrico
**Como** sistema
**Debo** almacenar qué método se usó en cada registro
**Para** auditoría y estadísticas

**Criterios de Aceptación**:
- [ ] Campo `biometric_method` en AttendanceRecord: FACIAL / FINGERPRINT
- [ ] Migración de DB para registros existentes (default FACIAL)
- [ ] Mostrar en reportes qué método se usó

**Estimación**: XS (< 1 día)
**Prioridad**: Baja
**Sprint**: 3
**Estado**: ⏳ Pendiente

---

### 📈 ÉPICA 5: REPORTES

#### US-019 [Alta] - Reporte Diario
**Como** supervisor
**Quiero** ver reporte de asistencia del día
**Para** saber quién asistió y a qué hora

**Criterios de Aceptación**:
- [ ] Pantalla ReporteDiarioScreen
- [ ] DatePicker para seleccionar fecha
- [ ] Lista con: Empleado, Hora entrada, Hora salida, Total horas
- [ ] Indicadores: ✓ Presente, ✗ Ausente, ⚠ Solo entrada sin salida
- [ ] Filtro por departamento
- [ ] Resumen: X de Y empleados asistieron

**Estimación**: M (3-5 días)
**Prioridad**: Alta
**Sprint**: 3
**Estado**: ⏳ Pendiente

---

#### US-020 [Alta] - Historial por Empleado
**Como** administrador
**Quiero** ver historial completo de un empleado
**Para** revisar su asistencia histórica

**Criterios de Aceptación**:
- [ ] Selector de empleado (búsqueda)
- [ ] RangeDatePicker para período
- [ ] Lista cronológica de entradas/salidas
- [ ] Totales: Días asistidos, Horas totales, Tardanzas
- [ ] Paginación si más de 100 registros

**Estimación**: M (3-5 días)
**Prioridad**: Alta
**Sprint**: 3
**Estado**: ⏳ Pendiente

---

#### US-021 [Media] - Retención Configurable
**Como** administrador
**Quiero** configurar cuántos días mantener registros
**Para** optimizar espacio en el dispositivo

**Criterios de Aceptación**:
- [ ] Setting: "Mantener registros por: X días" (default 90)
- [ ] WorkManager job diario para limpieza
- [ ] Confirmar antes de eliminar registros antiguos
- [ ] Log de registros eliminados
- [ ] Opción de mantener indefinidamente

**Estimación**: S (1-3 días)
**Prioridad**: Media
**Sprint**: 3
**Estado**: ⏳ Pendiente

---

#### US-022 [Media] - Exportar Reportes
**Como** administrador
**Quiero** exportar reportes a PDF o CSV
**Para** compartir información con gerencia

**Criterios de Aceptación**:
- [ ] Botón "Exportar" en pantallas de reporte
- [ ] Opciones: PDF / CSV
- [ ] PDF: Logo, título, fecha generación, tabla formateada
- [ ] CSV: Datos crudos para Excel
- [ ] Guardar en Downloads/FaceRecognition/Reports/
- [ ] Compartir vía Share API (email, WhatsApp, etc.)

**Estimación**: M (3-5 días)
**Prioridad**: Media
**Sprint**: 5
**Estado**: ⏳ Pendiente

---

#### US-023 [Baja] - Estadísticas Básicas
**Como** supervisor
**Quiero** ver estadísticas resumidas
**Para** tener visión general rápida

**Criterios de Aceptación**:
- [ ] Cards en dashboard:
  - Total asistencias hoy
  - % puntualidad (entradas antes de 8:00 AM)
  - Empleado con más asistencias del mes
- [ ] Gráfico simple de asistencia últimos 7 días

**Estimación**: S (1-3 días)
**Prioridad**: Baja
**Sprint**: 6
**Estado**: ⏳ Pendiente

---

### 🔄 ÉPICA 6: SINCRONIZACIÓN CON BACKEND

#### US-024 [Alta] - Detectar Conectividad
**Como** sistema
**Debo** detectar automáticamente conexión a internet
**Para** sincronizar cuando esté disponible

**Criterios de Aceptación**:
- [ ] ConnectivityManager con callback de cambios
- [ ] Verificar conectividad real (ping a server)
- [ ] Diferenciar WiFi vs datos móviles
- [ ] Preferir WiFi para sincronización (configurable)
- [ ] Indicador visual de conectividad en UI

**Estimación**: S (1-3 días)
**Prioridad**: Alta
**Sprint**: 4
**Estado**: ⏳ Pendiente

---

#### US-025 [Alta] - Subir Registros Pendientes
**Como** sistema
**Debo** subir registros de asistencia al backend
**Para** centralizar información

**Criterios de Aceptación**:
- [ ] Queue local de registros pendientes de sync
- [ ] WorkManager para sincronización periódica
- [ ] Envío en lotes de máximo 100 registros
- [ ] Retry con exponential backoff (3 intentos)
- [ ] Marcar registros como sincronizados
- [ ] No eliminar hasta confirmar recepción exitosa

**Estimación**: L (5-10 días)
**Prioridad**: Alta
**Sprint**: 4
**Estado**: ⏳ Pendiente

---

#### US-026 [Alta] - Descargar Actualizaciones
**Como** sistema
**Debo** descargar datos actualizados desde backend
**Para** mantener dispositivo sincronizado

**Criterios de Aceptación**:
- [ ] Sincronización incremental (solo cambios desde última sync)
- [ ] Descargar: Empleados nuevos, Empleados editados, Empleados eliminados
- [ ] Merge con datos locales
- [ ] Resolución de conflictos (estrategia: último cambio gana)
- [ ] Timestamp de última sincronización

**Estimación**: L (5-10 días)
**Prioridad**: Alta
**Sprint**: 5
**Estado**: ⏳ Pendiente

---

#### US-027 [Media] - Estado de Sincronización
**Como** administrador
**Quiero** ver estado de sincronización
**Para** saber si hay datos pendientes

**Criterios de Aceptación**:
- [ ] Pantalla SyncStatusScreen
- [ ] Mostrar: Última sync exitosa (fecha/hora), Registros pendientes de subir, Errores recientes
- [ ] Log de últimas 20 sincronizaciones
- [ ] Botón "Ver detalles" para cada sync

**Estimación**: S (1-3 días)
**Prioridad**: Media
**Sprint**: 4
**Estado**: ⏳ Pendiente

---

#### US-028 [Media] - Sincronización Manual
**Como** administrador
**Quiero** forzar sincronización inmediata
**Para** no esperar sync automática

**Criterios de Aceptación**:
- [ ] Botón "Sincronizar Ahora" en SyncStatusScreen
- [ ] Progress indicator durante sync
- [ ] Toast de éxito/error al completar
- [ ] Respetar preferencias (WiFi only, etc.)

**Estimación**: XS (< 1 día)
**Prioridad**: Media
**Sprint**: 4
**Estado**: ⏳ Pendiente

---

#### US-029 [Media] - Resolución de Conflictos
**Como** sistema
**Debo** manejar conflictos de sincronización
**Para** evitar pérdida de datos

**Criterios de Aceptación**:
- [ ] Detectar conflictos: mismo empleado editado local y remotamente
- [ ] Estrategia: Último modificado gana (last-write-wins)
- [ ] Log de conflictos para auditoría
- [ ] Notificación a ADMIN si hay conflicto importante

**Estimación**: M (3-5 días)
**Prioridad**: Media
**Sprint**: 5
**Estado**: ⏳ Pendiente

---

#### US-030 [Baja] - Definir Contrato API
**Como** desarrollador
**Necesito** especificar contrato de API REST
**Para** que backend implemente endpoints compatibles

**Criterios de Aceptación**:
- [ ] Documento OpenAPI/Swagger
- [ ] Endpoints especificados:
  - POST /api/v1/attendance/sync
  - GET /api/v1/employees/sync
  - POST /api/v1/auth/device
- [ ] Ejemplos de request/response
- [ ] Códigos de error definidos
- [ ] Autenticación: Bearer token

**Estimación**: S (1-3 días)
**Prioridad**: Baja
**Sprint**: 4
**Estado**: ⏳ Pendiente

---

### ⚙️ ÉPICA 7: CONFIGURACIÓN DEL SISTEMA

#### US-031 [Media] - Pantalla de Configuración
**Como** administrador
**Quiero** configurar parámetros del sistema
**Para** personalizar comportamiento

**Criterios de Aceptación**:
- [ ] Pantalla SettingsScreen con secciones:
  - **Reconocimiento**: Umbral confianza facial (0.0-1.0), Método biométrico prioritario
  - **Retención**: Días mantener registros
  - **Sincronización**: URL backend, Frecuencia sync (minutos), Solo WiFi (toggle)
  - **Seguridad**: Timeout sesión (minutos)
- [ ] Validaciones de valores
- [ ] Botón "Restaurar valores por defecto"

**Estimación**: M (3-5 días)
**Prioridad**: Media
**Sprint**: 5
**Estado**: ⏳ Pendiente

---

#### US-032 [Baja] - Logs de Errores
**Como** administrador
**Quiero** ver logs de errores del sistema
**Para** diagnosticar problemas

**Criterios de Aceptación**:
- [ ] Pantalla LogsScreen
- [ ] Niveles: ERROR, WARNING, INFO
- [ ] Filtros: Por nivel, Por fecha, Por componente
- [ ] Detalles: Timestamp, mensaje, stack trace
- [ ] Botón "Exportar logs" a archivo .txt
- [ ] Límite: últimos 1000 logs

**Estimación**: S (1-3 días)
**Prioridad**: Baja
**Sprint**: Backlog
**Estado**: ⏳ Pendiente

---

#### US-033 [Baja] - Backup Manual de DB
**Como** administrador
**Quiero** hacer backup de la base de datos
**Para** prevenir pérdida de datos

**Criterios de Aceptación**:
- [ ] Botón "Crear Backup" en Settings
- [ ] Copiar DB a Downloads/FaceRecognition/Backups/
- [ ] Nombre archivo: backup_YYYYMMDD_HHmmss.db
- [ ] Toast con ruta del backup
- [ ] Opción de restaurar desde backup

**Estimación**: S (1-3 días)
**Prioridad**: Baja
**Sprint**: Backlog
**Estado**: ⏳ Pendiente

---

### 🛡️ ÉPICA 8: ROBUSTEZ Y MEJORAS

#### US-034 [Alta] - Manejo de Errores de Cámara
**Como** sistema
**Debo** manejar errores de cámara gracefully
**Para** no bloquear al usuario

**Criterios de Aceptación**:
- [ ] Try-catch en todas las operaciones de cámara
- [ ] Mensajes de error claros: "Cámara no disponible", "Permiso denegado", etc.
- [ ] Retry automático (máx 3 veces)
- [ ] Fallback a huella si cámara falla
- [ ] Botón "Reportar problema" que envía log

**Estimación**: S (1-3 días)
**Prioridad**: Alta
**Sprint**: 2
**Estado**: ⏳ Pendiente

---

#### US-035 [Media] - Optimizar Consumo de Batería
**Como** sistema
**Debo** minimizar consumo de batería
**Para** prolongar autonomía del dispositivo

**Criterios de Aceptación**:
- [ ] Liberar cámara inmediatamente después de captura
- [ ] Cancelar preview cuando app pasa a background
- [ ] WorkManager con constraints de batería
- [ ] No sincronizar si batería < 20%
- [ ] Modo "Ahorro de energía": Reducir FPS de preview

**Estimación**: M (3-5 días)
**Prioridad**: Media
**Sprint**: 6
**Estado**: ⏳ Pendiente

---

#### US-036 [Media] - Tests Unitarios
**Como** desarrollador
**Necesito** tests para lógica crítica
**Para** prevenir regresiones

**Criterios de Aceptación**:
- [ ] Tests para:
  - Cálculo de similitud de embeddings
  - Lógica de sincronización
  - Encriptación/desencriptación
  - Validación de duplicados
  - Resolución de conflictos
- [ ] Coverage mínimo 60%
- [ ] CI/CD con tests automáticos

**Estimación**: L (5-10 días)
**Prioridad**: Media
**Sprint**: 6
**Estado**: ⏳ Pendiente

---

#### US-037 [Baja] - Indicadores de Calidad
**Como** sistema
**Debo** mostrar feedback de calidad durante captura
**Para** guiar al usuario

**Criterios de Aceptación**:
- [ ] Overlay en cámara con:
  - Círculo/marco de posición ideal
  - Indicador de iluminación (🔆 Buena / ⚠️ Baja / ❌ Muy oscura)
  - Indicador de distancia (Más cerca / Perfecto / Más lejos)
  - Indicador de ángulo (Mira al frente)
- [ ] Colores: Verde (OK), Amarillo (Ajustar), Rojo (Mal)

**Estimación**: M (3-5 días)
**Prioridad**: Baja
**Sprint**: 6
**Estado**: ⏳ Pendiente

---

## 📅 PLANIFICACIÓN DE SPRINTS

### **SPRINT 1: Fundaciones de Seguridad y Administración** (2 semanas)
**Objetivo**: Crear base para gestión segura de empleados

- [x] ✅ Sistema de reconocimiento facial básico (ya funciona)
- [ ] US-001: Autenticación por PIN
- [ ] US-002: Gestión de roles
- [ ] US-003: Encriptación de DB
- [ ] US-005: Lista de empleados
- [ ] US-006: Detalles de empleado
- [ ] US-007: Eliminar empleados

**Entregables**: Sistema de login, pantalla de administración de empleados funcional

---

### **SPRINT 2: Reconocimiento por Huella** (2 semanas)
**Objetivo**: Implementar método biométrico alternativo

- [ ] US-011: Registro de huella digital
- [ ] US-012: Reconocimiento por huella
- [ ] US-013: Configuración de método prioritario
- [ ] US-014: Validación de calidad de huella
- [ ] US-034: Manejo robusto de errores de cámara

**Entregables**: Sistema dual facial + huella funcionando

---

### **SPRINT 3: Control de Errores + Reportes Básicos** (2 semanas)
**Objetivo**: Permitir corrección de errores de identificación y visibilidad de asistencias

**🔥 Crítico** (Nuevas funcionalidades para manejo de errores):
- [ ] US-039: Selección manual de ENTRADA/SALIDA con validación
- [ ] US-038: Botón "Este no soy yo" para rechazar identificación errónea
- [ ] US-041: Crear tabla `attendance_audit` en DB

**📊 Reportes**:
- [ ] US-019: Reporte diario
- [ ] US-020: Historial por empleado
- [ ] US-015: Dashboard en tiempo real
- [ ] US-018: Registrar método biométrico usado

**Entregables**:
- Sistema de corrección de errores funcional
- Pantallas de reportes básicas operativas
- Auditoría implementada

---

### **SPRINT 4: Sincronización (Fase 1)** (2 semanas)
**Objetivo**: Conectividad y subida de datos

- [ ] US-030: Definir contrato API REST
- [ ] US-024: Detección de conectividad
- [ ] US-025: Subir registros al backend
- [ ] US-027: Pantalla de estado de sincronización
- [ ] US-028: Sincronización manual

**Entregables**: App puede subir registros a backend

---

### **SPRINT 5: Sincronización (Fase 2) + Pulido** (2 semanas)
**Objetivo**: Sincronización bidireccional completa

- [ ] US-026: Descargar actualizaciones desde backend
- [ ] US-029: Resolución de conflictos
- [ ] US-022: Exportar reportes (PDF/CSV)
- [ ] US-031: Pantalla de configuración
- [ ] US-008: Editar empleados

**Entregables**: Sincronización completa funcionando

---

### **SPRINT 6: Optimización y Testing** (2 semanas)
**Objetivo**: Robustez y calidad

- [ ] US-035: Optimización de batería
- [ ] US-036: Tests unitarios (>60% coverage)
- [ ] US-037: Indicadores de calidad en captura
- [ ] US-009: Activar/desactivar empleados
- [ ] US-023: Estadísticas básicas
- [ ] US-004: Permisos para registro de empleados

**Entregables**: App optimizada y testeada, lista para producción

---

## 📊 MÉTRICAS DE SEGUIMIENTO

### Velocidad del Equipo
- **Story Points por Sprint**: TBD (medir en Sprint 1)
- **Burndown**: Actualizar diariamente

### Calidad
- **Code Coverage**: Meta >60%
- **Bugs Críticos**: Meta 0
- **Tiempo promedio resolución bugs**: Meta <24h

### Sincronización
- **Registros pendientes**: Objetivo <100
- **Tasa de éxito sync**: Meta >95%
- **Conflictos de merge**: Objetivo <5 por día

---

## 📝 DEFINICIÓN DE LISTO (Definition of Done)

Una historia se considera **LISTA** cuando:

- [x] Código implementado y funcional
- [x] Compilación exitosa sin errores
- [x] Código revisado (code review)
- [x] Tests unitarios escritos (si aplica)
- [x] Probado manualmente en dispositivo real
- [x] Sin errores conocidos bloqueantes
- [x] Logs de debug implementados
- [x] Manejo de errores con mensajes claros
- [x] Documentado en CLAUDE.md si afecta arquitectura
- [x] Actualizado el estado en BACKLOG.md

---

## 🔗 ESPECIFICACIÓN API BACKEND

### Endpoint 1: Sincronizar Registros de Asistencia

```http
POST /api/v1/attendance/sync
Authorization: Bearer {device_token}
Content-Type: application/json

{
  "device_id": "device_uuid_12345",
  "records": [
    {
      "employee_id": "EMP001",
      "type": "ENTRY",
      "timestamp": "2025-01-15T08:05:23Z",
      "biometric_method": "FACIAL",
      "confidence": 0.92,
      "local_id": "local_record_123"
    }
  ]
}

Response 200:
{
  "synced_count": 1,
  "failed": [],
  "server_timestamp": "2025-01-15T08:05:30Z"
}
```

### Endpoint 2: Sincronizar Empleados

```http
GET /api/v1/employees/sync?since=2025-01-14T00:00:00Z
Authorization: Bearer {device_token}

Response 200:
{
  "employees": [
    {
      "id": "EMP001",
      "name": "Juan Pérez",
      "employee_id": "1234",
      "department": "IT",
      "position": "Developer",
      "status": "ACTIVE",
      "updated_at": "2025-01-15T10:00:00Z"
    }
  ],
  "deleted_ids": ["EMP999"],
  "server_timestamp": "2025-01-15T12:00:00Z"
}
```

### Endpoint 3: Autenticación de Dispositivo

```http
POST /api/v1/auth/device
Content-Type: application/json

{
  "device_id": "device_uuid_12345",
  "pin_hash": "sha256_hash_of_pin"
}

Response 200:
{
  "token": "jwt_token_here",
  "expires_at": "2025-01-16T12:00:00Z",
  "permissions": ["READ_EMPLOYEES", "WRITE_ATTENDANCE"]
}
```

---

## 🎯 PRÓXIMOS PASOS INMEDIATOS

1. **Ahora mismo**: Implementar US-005 (Lista de Empleados)
2. **Esta semana**: Completar Sprint 1 (US-001 a US-007)
3. **Próxima semana**: Iniciar Sprint 2 (Huella Digital)

---

**Última actualización**: 2025-01-24
**Versión**: 1.0
**Mantenido por**: Claude Code Assistant
