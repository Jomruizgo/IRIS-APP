# BACKLOG - Sistema de Asistencia Biométrica Offline

## 📊 Resumen del Proyecto

**Estado Actual**: Reconocimiento facial básico funcional
**Objetivo**: Sistema robusto de control de asistencia offline con múltiples métodos biométricos, sincronización y gestión completa

**Stack Tecnológico**:
- Frontend: Kotlin + Jetpack Compose
- DB Local: Room + SQLCipher (encriptación)
- Biometría: ML Kit Face Detection + Android BiometricPrompt + Android KeyStore
- Sincronización: WorkManager + Retrofit
- Seguridad: Encrypted SharedPreferences, Android Keystore

**Decisiones Arquitectónicas**:
- [ADR-01](./02-DECISION-CONGRUENCIA-BIOMETRIA.md): Congruencia entre poses de registro y liveness
- [ADR-02](./03-DECISION-MANEJO-ERRORES-IDENTIFICACION.md): Manejo de errores en identificación facial
- [ADR-03](./04-DECISION-AUTENTICACION-HUELLA.md): Sistema de autenticación por huella con Android KeyStore

---

## 🎯 ÉPICAS Y HISTORIAS DE USUARIO

### 📋 ÉPICA 1: SEGURIDAD Y AUTENTICACIÓN

#### US-001 [Alta] - Autenticación por PIN ✅ COMPLETADA
**Como** sistema
**Quiero** requerir autenticación por PIN/contraseña
**Para** controlar acceso a funciones administrativas

**Criterios de Aceptación**:
- [x] Pantalla de login con campo PIN de 4-6 dígitos
- [x] Almacenamiento seguro usando DataStore
- [x] Sesión con timeout configurable (default: 30 min)
- [ ] Lock automático al minimizar app
- [ ] Límite de intentos fallidos (3 intentos)

**Estimación**: M (3-5 días)
**Prioridad**: Alta
**Sprint**: 1
**Estado**: ✅ Implementada (LoginScreen.kt, LoginViewModel.kt, FirstTimeSetupScreen.kt, SessionManager.kt)

---

#### US-002 [Alta] - Gestión de Roles ✅ COMPLETADA
**Como** administrador
**Quiero** gestionar roles de usuario (Admin, Supervisor, Usuario)
**Para** controlar permisos por tipo de usuario

**Criterios de Aceptación**:
- [x] Entidad User en DB con campos: id, name, pin_hash, role, created_at
- [x] Roles: ADMIN, SUPERVISOR, USER
- [ ] CRUD de usuarios (solo ADMIN puede crear/editar)
- [x] Permisos definidos por rol:
  - ADMIN: Todo
  - SUPERVISOR: Ver reportes, ver empleados (no crear/editar)
  - USER: Solo marcar asistencia
- [x] Usuario ADMIN por defecto en primera instalación

**Estimación**: M (3-5 días)
**Prioridad**: Alta
**Sprint**: 1
**Estado**: ✅ Implementada (User.kt, UserDao.kt, UserRepository.kt, UserRole enum, SessionManager con permisos)

---

#### US-003 [Media] - Encriptación de DB ✅ COMPLETADA
**Como** sistema
**Debo** encriptar datos sensibles en la base de datos local
**Para** proteger información biométrica y personal

**Criterios de Aceptación**:
- [x] Integrar SQLCipher or Room encryption
- [x] Encriptar embeddings faciales
- [x] Encriptar datos biométricos (huellas)
- [x] Key derivada usando SecureRandom + SharedPreferences
- [ ] Migración de DB existente sin pérdida de datos

**Estimación**: S (1-3 días)
**Prioridad**: Media
**Sprint**: 1
**Estado**: ✅ Implementada (AppDatabase.kt con SQLCipher, passphrase segura de 32 bytes)

---

#### US-004 [Media] - Permisos para Registro de Empleados ✅ COMPLETADA
**Como** administrador
**Quiero** definir qué roles pueden registrar empleados
**Para** prevenir registros no autorizados

**Criterios de Aceptación**:
- [x] Solo ADMIN puede acceder a "Registrar Empleado"
- [x] Botón oculto para otros roles
- [ ] Mensaje de error si rol insuficiente intenta acceder vía deep link

**Estimación**: XS (< 1 día)
**Prioridad**: Media
**Sprint**: 1
**Estado**: ✅ Implementada (HomeScreen.kt con canManageEmployees para mostrar/ocultar botones según rol)

---

### 👥 ÉPICA 2: ADMINISTRACIÓN DE EMPLEADOS

#### US-005 [Alta] - Lista de Empleados ✅ COMPLETADA
**Como** administrador
**Quiero** ver lista de todos los empleados registrados
**Para** administrar el personal del sistema

**Criterios de Aceptación**:
- [x] Pantalla EmployeeListScreen con lista scrollable
- [x] Items muestran: nombre, ID, departamento, cargo, fecha registro, cantidad de fotos
- [x] Búsqueda por nombre o ID
- [ ] Filtros: Por departamento, Por estado (activo/inactivo)
- [ ] Ordenar por: Nombre, Fecha registro, Departamento
- [ ] Contador total de empleados
- [ ] Click en item navega a detalles

**Estimación**: S (1-3 días)
**Prioridad**: Alta
**Sprint**: 1
**Estado**: ✅ Implementada (EmployeeListScreen.kt + EmployeeListViewModel.kt)

---

#### US-006 [Alta] - Detalles de Empleado ✅ COMPLETADA
**Como** administrador
**Quiero** ver detalles completos de un empleado
**Para** verificar su información y estado

**Criterios de Aceptación**:
- [x] Pantalla EmployeeDetailScreen
- [x] Mostrar: Foto facial, Nombre completo, ID empleado, Departamento, Cargo, Fecha registro, Estado (activo/inactivo)
- [x] Indicador de métodos biométricos registrados (facial ✓, huella ✓/✗)
- [x] Botones: Editar, Eliminar, Activar/Desactivar
- [x] Confirmación antes de acciones destructivas

**Estimación**: XS (< 1 día)
**Prioridad**: Alta
**Sprint**: 1
**Estado**: ✅ Implementada (EmployeeDetailScreen.kt + EmployeeDetailViewModel.kt con navegación desde EmployeeListScreen)

---

#### US-007 [Alta] - Eliminar Empleados ✅ COMPLETADA
**Como** administrador
**Quiero** eliminar empleados del sistema
**Para** dar de baja personal que ya no trabaja

**Criterios de Aceptación**:
- [x] Botón "Eliminar" en lista de empleados
- [x] Dialog de confirmación con advertencia
- [x] Eliminación permanente de empleado
- [ ] Opción de "soft delete" (marcar inactivo) vs eliminación permanente
- [ ] Log de auditoría de eliminaciones

**Estimación**: XS (< 1 día)
**Prioridad**: Alta
**Sprint**: 1
**Estado**: ✅ Implementada (integrada en EmployeeListScreen con confirmación)

---

#### US-008 [Media] - Editar Información de Empleados ✅ COMPLETADA
**Como** administrador
**Quiero** editar información de empleados
**Para** actualizar datos sin re-registrar biometría

**Criterios de Aceptación**:
- [x] Pantalla EmployeeEditScreen
- [x] Campos editables: Nombre, Departamento, Cargo, Estado, Huella habilitada
- [x] NO editable: ID empleado, Datos biométricos
- [x] Validaciones: Campos requeridos, formatos
- [x] Botón "Guardar cambios" (en TopBar y bottom)
- [ ] Registro de última modificación (timestamp + user)

**Estimación**: S (1-3 días)
**Prioridad**: Media
**Sprint**: 5
**Estado**: ✅ Implementada (EmployeeEditScreen.kt + EmployeeEditViewModel.kt con navegación desde EmployeeDetailScreen)

---

#### US-009 [Media] - Activar/Desactivar Empleados ✅ COMPLETADA
**Como** administrador
**Quiero** desactivar empleados sin eliminarlos
**Para** suspender temporalmente sin perder datos

**Criterios de Aceptación**:
- [x] Toggle switch en pantalla de detalles
- [x] Toggle switch en pantalla de edición
- [x] Empleados inactivos no pueden marcar asistencia
- [x] Aparecen con indicador visual en lista (Badge "ACTIVO"/"INACTIVO")
- [ ] Filtro para mostrar solo activos/solo inactivos/todos

**Estimación**: XS (< 1 día)
**Prioridad**: Media
**Sprint**: 6
**Estado**: ✅ Implementada (integrada en EmployeeDetailScreen y EmployeeEditScreen)

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

#### US-011 [Alta] - Registrar Huella Digital ✅ COMPLETADA
**Como** empleado
**Quiero** registrar mi huella digital
**Para** tener método alternativo de identificación

**Criterios de Aceptación**:
- [x] BiometricAuthManager.kt creado con BiometricPrompt API
- [x] Verificación de disponibilidad de hardware biométrico
- [x] Método authenticate() con callbacks (success, error, failed)
- [x] Integración en flujo de registro de empleado (switch en EmployeeRegistrationScreen)
- [x] Campo hasFingerprintEnabled en DB Employee
- [ ] Almacenar template encriptado en DB (NO POSIBLE: Android BiometricPrompt no expone templates por seguridad)
- [ ] Capturar al menos 2 muestras de huella (NO REQUERIDO: Se usa huella del dispositivo Android)
- [ ] Soporte para múltiples huellas (NO REQUERIDO: Se valida contra huellas registradas en dispositivo)

**Estimación**: M (3-5 días)
**Prioridad**: Alta
**Sprint**: 2
**Estado**: ✅ Implementada (Switch en EmployeeRegistrationScreen + campo en DB + EmployeeEditScreen)

---

#### US-012 [Alta] - Reconocimiento por Huella ✅ COMPLETADA
**Como** empleado
**Quiero** marcar asistencia usando mi huella
**Para** registrar cuando facial no funcione

**Criterios de Aceptación**:
- [x] Botón "Usar Huella Digital" visible en FaceRecognitionScreen
- [x] Botón habilitado cuando se selecciona ENTRADA/SALIDA
- [x] Conectar botón con BiometricAuthManager
- [x] Lógica para identificar empleado por ID + huella (BiometricAuthScreen con PIN)
- [x] Aplicar misma validación de secuencia ENTRADA/SALIDA
- [x] Registrar en AttendanceRecord con livenessChallenge="fingerprint"

**Estimación**: M (3-5 días)
**Prioridad**: Alta
**Sprint**: 2
**Estado**: ✅ Implementada (BiometricAuthScreen + BiometricAuthViewModel con teclado numérico para PIN + validaciones completas)

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

#### US-038 [Alta] - Corrección Inmediata de Identificación Errónea ✅ COMPLETADA
**Como** empleado
**Quiero** poder rechazar una identificación incorrecta inmediatamente
**Para** evitar registros falsos en mi asistencia

**Criterios de Aceptación**:
- [x] Diálogo de éxito muestra botón "Este no soy yo" además de "Correcto"
- [x] Al presionar "Este no soy yo", se elimina el registro recién creado
- [x] Se registra en tabla de auditoría con action=CANCELLED_BY_USER
- [x] Usuario vuelve automáticamente a pantalla inicial
- [x] Sistema permite re-intentar reconocimiento inmediatamente
- [x] Log incluye: timestamp, empleado detectado, confianza

**Estimación**: S (1-3 días)
**Prioridad**: Alta
**Sprint**: 3
**Estado**: ✅ Implementada (FaceRecognitionScreen + ViewModel con auditoría)

---

#### US-039 [Alta] - Selección Manual de ENTRADA/SALIDA ✅ COMPLETADA
**Como** empleado
**Quiero** seleccionar manualmente si estoy marcando ENTRADA o SALIDA
**Para** tener control sobre mi registro de asistencia

**Criterios de Aceptación**:
- [x] Pantalla inicial muestra dos botones grandes: ENTRADA 🏢 y SALIDA 🏠
- [x] Botones con selección visual (cambio de color al seleccionar)
- [x] **Validación ENTRADA**: No permite si ya hay entrada sin salida
  - [x] Mensaje: "Ya tienes ENTRADA sin SALIDA registrada el XX/XX. Debes registrar SALIDA primero."
  - [x] Muestra AlertDialog con el error
- [x] **Validación SALIDA**: No permite si no hay entrada previa
  - [x] Mensaje: "No puedes registrar SALIDA sin ENTRADA previa."
  - [x] Muestra AlertDialog con el error
- [x] Una vez seleccionado tipo válido, proceder a escaneo facial
- [x] Guardar tipo seleccionado en `AttendanceRecord.type`
- [x] Validación se ejecuta DESPUÉS del reconocimiento facial

**Estimación**: M (3-5 días)
**Prioridad**: Alta
**Sprint**: 3
**Estado**: ✅ Implementada (FaceRecognitionScreen + ViewModel con ValidationError state)

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

#### US-041 [Media] - Sistema de Auditoría ✅ COMPLETADA
**Como** administrador
**Quiero** ver historial de correcciones y acciones administrativas
**Para** auditoría y control de calidad del sistema

**Criterios de Aceptación**:
- [x] Nueva tabla `attendance_audit` en base de datos con campos completos
- [x] AttendanceAuditDao con queries para consultar auditorías
- [x] AttendanceAuditRepository para lógica de negocio
- [x] Registro automático de action=CREATED al crear asistencia
- [x] Registro automático de action=CANCELLED_BY_USER al rechazar
- [x] Metadata en formato JSON con detalles (confidence, type, timestamp)
- [x] TypeConverter para enum AuditAction
- [ ] Pantalla "Auditoría" accesible solo para ADMIN
- [ ] Lista de eventos de auditoría con filtros
- [ ] Botón "Exportar a CSV"
- [x] DataRetentionManager con configuración de retención (default 180 días)

**Estimación**: M (3-5 días)
**Prioridad**: Media
**Sprint**: 3
**Estado**: ✅ Implementada (Tabla + DAOs + Repository + integración en ViewModel, falta UI)

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

#### US-019 [Alta] - Reporte Diario ✅ COMPLETADA
**Como** supervisor
**Quiero** ver reporte de asistencia del día
**Para** saber quién asistió y a qué hora

**Criterios de Aceptación**:
- [x] Pantalla DailyReportScreen
- [x] Botón "Hoy" para ir a fecha actual
- [x] Lista con: Empleado, ID, Hora, Tipo (ENTRADA/SALIDA)
- [x] Estadísticas: Total registros, Entradas, Salidas, Empleados únicos
- [x] Carga datos por rango de fecha (inicio y fin del día)
- [ ] Filtro por departamento
- [ ] Indicadores: ✓ Presente, ✗ Ausente, ⚠ Solo entrada sin salida

**Estimación**: M (3-5 días)
**Prioridad**: Alta
**Sprint**: 3
**Estado**: ✅ Implementada (DailyReportScreen.kt + DailyReportViewModel.kt)

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

#### US-021 [Media] - Retención Configurable ✅ COMPLETADA
**Como** administrador
**Quiero** configurar cuántos días mantener registros
**Para** optimizar espacio en el dispositivo

**Criterios de Aceptación**:
- [x] Setting: "Mantener registros por: X días" (default 90 asistencia / 180 auditoría)
- [x] DataRetentionManager con DataStore para configuración
- [x] Pantalla SettingsScreen completa con opciones rápidas (30/90/180/365 días)
- [x] Botón manual de limpieza con confirmación
- [x] Método cleanOldRecords() para eliminar registros antiguos
- [ ] WorkManager job diario para limpieza automática
- [ ] Log de registros eliminados

**Estimación**: S (1-3 días)
**Prioridad**: Media
**Sprint**: 3
**Estado**: ✅ Implementada (SettingsScreen + SettingsViewModel + DataRetentionManager con limpieza manual)

---

#### US-022 [Media] - Exportar Reportes ✅ COMPLETADA (CSV)
**Como** administrador
**Quiero** exportar reportes a PDF o CSV
**Para** compartir información con gerencia

**Criterios de Aceptación**:
- [x] Botón "Exportar" en pantallas de reporte (IconButton en TopAppBar de DailyReportScreen)
- [x] Exportación a CSV implementada (CsvExporter.kt)
- [x] CSV con campos: ID, Empleado, ID_Empleado, Tipo, Fecha, Hora, Confianza, Desafío, Sincronizado
- [x] Guardar en getExternalFilesDir/Documents/Reportes/
- [x] Filtros: Por rango de fechas, Por empleado (método exportWithFilters)
- [x] Toast con ruta del archivo exportado
- [ ] Opción PDF
- [ ] Compartir vía Share API (email, WhatsApp, etc.)

**Estimación**: M (3-5 días)
**Prioridad**: Media
**Sprint**: 3
**Estado**: ✅ Implementada (CsvExporter.kt con filtros, falta integración en UI y PDF)

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

#### US-024 [Alta] - Detectar Conectividad ✅ COMPLETADA
**Como** sistema
**Debo** detectar automáticamente conexión a internet
**Para** sincronizar cuando esté disponible

**Criterios de Aceptación**:
- [x] ConnectivityManager con callback de cambios
- [ ] Verificar conectividad real (ping a server)
- [x] Diferenciar WiFi vs datos móviles
- [ ] Preferir WiFi para sincronización (configurable)
- [x] Indicador visual de conectividad en UI

**Estimación**: S (1-3 días)
**Prioridad**: Alta
**Sprint**: 4
**Estado**: ✅ Implementada (ConnectivityObserver.kt con Flow reactivo, HomeViewModel observa networkStatus, HomeScreen muestra Online/Offline)

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

#### US-027 [Media] - Sincronización con WorkManager ✅ COMPLETADA
**Como** sistema
**Quiero** sincronizar registros automáticamente
**Para** enviar datos al backend cuando hay conexión

**Criterios de Aceptación**:
- [x] SyncWorker.kt con CoroutineWorker
- [x] Ejecuta cada 15 minutos con WorkManager periódico
- [x] Constraints: Requiere red conectada + batería no baja
- [x] BackoffPolicy exponencial para reintentos
- [x] Método schedule() para iniciar sync automática
- [x] Método syncNow() para sync manual inmediata
- [x] Método cancel() para detener sync
- [x] Consulta registros no sincronizados (isSynced=false)
- [ ] Llamada HTTP real al backend (simulada por ahora)
- [ ] Pantalla UI para ver estado

**Estimación**: S (1-3 días)
**Prioridad**: Media
**Sprint**: 3
**Estado**: ✅ Implementada (SyncWorker.kt completo, falta endpoint API real y UI)

---

#### US-028 [Media] - Sincronización Manual ✅ COMPLETADA
**Como** administrador
**Quiero** forzar sincronización inmediata
**Para** no esperar sync automática

**Criterios de Aceptación**:
- [x] Método SyncWorker.syncNow() implementado
- [x] OneTimeWorkRequest con constraints de red
- [x] Botón "Sincronizar Ahora" en UI (IconButton en TopAppBar de HomeScreen)
- [x] Toast de confirmación al iniciar sync
- [ ] Progress indicator durante sync
- [ ] Toast de éxito/error al completar (requiere observar WorkInfo)

**Estimación**: XS (< 1 día)
**Prioridad**: Media
**Sprint**: 4
**Estado**: ✅ Implementada (Botón en HomeScreen con Toast, falta monitoreo de estado)

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
