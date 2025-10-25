# Decisión Arquitectónica: Sistema de Autenticación por Huella Dactilar

**Fecha**: 2025-10-24
**Estado**: ✅ IMPLEMENTADO
**Decisión**: Vinculación de huella específica por empleado usando Android KeyStore

---

## Contexto

La aplicación de control de asistencia requiere un sistema de autenticación biométrica que permita:
- Verificar la identidad del empleado de forma segura
- Funcionar sin conexión a internet (offline-first)
- No requerir hardware adicional (usar solo el sensor del dispositivo Android)

### Opciones Evaluadas

#### Opción 1: ID + Cualquier Huella del Sistema
**Descripción**: El empleado ingresa su ID y coloca cualquier huella válida registrada en Android.

**Pros:**
- ✅ Simple de implementar
- ✅ No requiere proceso de vinculación
- ✅ Sobrevive a formateos/cambios de dispositivo

**Contras:**
- ❌ **Baja seguridad**: Un empleado puede dar su ID a otro
- ❌ **Fraude fácil**: "Uso ID de Juan, pero mi propia huella"
- ❌ **No verifica identidad real**: Solo valida que hay una persona con huella

**Conclusión**: ❌ **DESCARTADA** - Insegura para un sistema de control de asistencia empresarial

---

#### Opción 2: Extracción de Templates con SDK Externo
**Descripción**: Usar librerías comerciales (Neurotechnology, ZKTeco, Aware) para extraer templates de huellas.

**Pros:**
- ✅ Alta seguridad
- ✅ Templates portables entre dispositivos
- ✅ Permite sincronización en la nube

**Contras:**
- ❌ **Requiere hardware externo** (lector USB/Bluetooth)
- ❌ **Android no expone templates** del sensor integrado por seguridad (TEE - Trusted Execution Environment)
- ❌ **Costo adicional**: $50-200 USD por lector
- ❌ **Complejidad**: Integración de SDKs comerciales

**Documentación de referencia**:
- [Android AOSP - Fingerprint HIDL](https://source.android.com/docs/security/features/authentication/fingerprint-hal)
- [Stack Overflow - Android Fingerprint Raw Data](https://stackoverflow.com/questions/34472663/android-fingerprint-raw-data)

**Conclusión**: ❌ **DESCARTADA** - Imposible con sensor integrado de Android

---

#### Opción 3: Vinculación con Android KeyStore (SELECCIONADA)
**Descripción**: Crear una clave criptográfica única por empleado en el Android KeyStore, protegida por la huella específica que el empleado registró.

**Cómo funciona:**

1. **Registro del Empleado**:
   - Empleado completa formulario y activa "Habilitar Huella Digital"
   - Captura fotos faciales (opcional)
   - App muestra `BiometricPrompt`: "Vincular Huella Digital"
   - Empleado coloca **UNA de sus huellas** registradas en Android
   - App genera clave criptográfica en KeyStore con `setUserAuthenticationRequired(true)`
   - Clave **SOLO se puede desbloquear con ESA huella específica**
   - Se guarda `fingerprintKeystoreAlias = "fingerprint_key_EMPLEADO123"` en BD

2. **Registro de Asistencia**:
   - Empleado ingresa su **ID** (ej: "12345")
   - App busca empleado en BD y obtiene su `fingerprintKeystoreAlias`
   - App muestra `BiometricPrompt`: "Verifica tu identidad - Juan Pérez"
   - Empleado coloca **su huella**
   - Android intenta desbloquear la clave criptográfica del empleado
   - ✅ **Si es la misma huella del registro** → Clave se desbloquea → Asistencia registrada
   - ❌ **Si es otra huella** → Error "Huella no coincide"

**Pros:**
- ✅ **Alta seguridad**: Cada empleado tiene su propia clave vinculada a SU huella
- ✅ **Anti-fraude**: No se puede usar la huella de otro empleado
- ✅ **No requiere hardware externo**: Usa sensor integrado de Android
- ✅ **Aprovecha TEE**: Las huellas nunca salen del Secure Enclave
- ✅ **API estándar**: `BiometricPrompt` + `KeyStore` (Android nativo)

**Contras:**
- ⚠️ **Acoplado al dispositivo**: Las claves están en el KeyStore local
- ⚠️ **Formateo = pérdida de datos**: Al formatear se pierden las claves
- ⚠️ **Cambio de dispositivo**: Requiere re-vincular todos los empleados
- ⚠️ **Múltiples dispositivos**: Cada tablet necesita su propia vinculación

**Conclusión**: ✅ **SELECCIONADA** - Mejor balance seguridad/viabilidad

---

## Decisión Final

**Sistema implementado**: **Vinculación de huella específica por empleado usando Android KeyStore**

### Justificación

1. **Seguridad adecuada para el caso de uso**:
   - Empresas pequeñas/medianas (10-100 empleados)
   - 1-3 dispositivos fijos en la empresa
   - Cambio de hardware infrecuente

2. **No requiere inversión adicional**:
   - Funciona con cualquier Android con sensor de huella
   - No necesita comprar lectores externos
   - Implementación con APIs nativas de Android

3. **Balance riesgo/beneficio aceptable**:
   - Riesgo de formateo: Mitigado con backups del dispositivo
   - Riesgo de pérdida: Los empleados se pueden re-vincular en ~30 segundos
   - Beneficio: Autenticación biométrica real sin costo

### Limitaciones Conocidas

#### 1. Formateo del Dispositivo
**Problema**: Al formatear Android, se pierde el KeyStore completo.

**Impacto**: Todos los empleados deben re-vincular su huella.

**Mitigación**:
- Documentar proceso de respaldo antes de formatear
- Backup completo del dispositivo con `adb backup`
- En caso extremo: Re-vinculación toma 30 seg/empleado

#### 2. Cambio de Dispositivo
**Problema**: Las claves no se transfieren automáticamente.

**Impacto**: En un dispositivo nuevo, todos deben re-vincularse.

**Mitigación**:
- Proceso de migración documentado
- Script de "Reset masivo" para marcar todos los empleados como "pendientes de re-vinculación"

#### 3. Múltiples Dispositivos
**Problema**: Si hay 3 tablets, cada una tiene su propio KeyStore.

**Impacto**: Empleado debe vincular su huella en cada tablet que use.

**Mitigación**:
- Asignar tablets fijas a departamentos/turnos
- Documentar en qué tablets está vinculado cada empleado

---

## Sincronización con la Nube

### ⚠️ Campo NO Sincronizable

**`fingerprintKeystoreAlias`**: Este campo **NO se sincroniza** con el backend.

#### Razón:

El alias de la clave (`fingerprint_key_12345`) es **específico del KeyStore del dispositivo local**. En otro dispositivo, esta clave no existe y el alias no tiene significado.

#### Campos que SÍ se sincronizan:

```json
{
  "employeeId": "12345",
  "fullName": "Juan Pérez",
  "department": "Ventas",
  "position": "Vendedor",
  "faceEmbeddings": [...],           // ✅ SÍ - Portable entre dispositivos
  "hasFingerprintEnabled": true      // ✅ SÍ - Indica capacidad del empleado
  // fingerprintKeystoreAlias        // ❌ NO - Específico del dispositivo
}
```

#### Flujo Multi-Dispositivo:

```
TABLET-001:
1. Juan se registra con huella
2. Local: fingerprintKeystoreAlias = "key_juan_001"
3. Sincroniza a nube: {id, name, embeddings, hasFingerprintEnabled=true}

SERVIDOR:
4. Guarda: {id, name, embeddings, hasFingerprintEnabled=true}
5. NO guarda: fingerprintKeystoreAlias (no se envía)

TABLET-002 (nuevo dispositivo):
6. Descarga de nube: {id, name, embeddings, hasFingerprintEnabled=true}
7. Juan debe RE-VINCULAR su huella en este nuevo dispositivo
8. Local: fingerprintKeystoreAlias = "key_juan_002" (diferente alias, nueva clave)
```

#### Registros de Asistencia:

Los registros de asistencia SÍ incluyen el método de autenticación usado:

```json
{
  "employeeId": "12345",
  "type": "ENTRY",
  "timestamp": 1729800000000,
  "authMethod": "FINGERPRINT",     // ✅ Para auditoría
  "deviceId": "tablet-001",        // ✅ Identifica qué dispositivo
  "confidence": 1.0,
  "synced": true
}
```

---

## Implementación Técnica

### Componentes

1. **`BiometricKeyManager.kt`**:
   - Crea claves en Android KeyStore
   - Vincula claves con `setUserAuthenticationRequired(true)`
   - Verifica huellas intentando desbloquear claves

2. **`Employee.kt`**:
   - Campo `hasFingerprintEnabled: Boolean`
   - Campo `fingerprintKeystoreAlias: String?`

3. **`FingerprintEnrollmentScreen.kt`**:
   - UI para vincular huella durante registro
   - Detección automática si no hay huellas en Android
   - Instrucciones claras al usuario

4. **`BiometricAuthScreen.kt`**:
   - UI para autenticación con ID + Huella
   - Validación contra clave específica del empleado

### Flujo de Datos

```
REGISTRO:
Usuario → Formulario → Activa huella → Captura fotos → Registrar
                                          ↓
                              BiometricPrompt.authenticate()
                                          ↓
                              KeyGenerator.generateKey(alias)
                                          ↓
                        BD: fingerprintKeystoreAlias = "key_123"

ASISTENCIA:
Usuario → Ingresa ID "123" → BD busca alias "key_123"
                                          ↓
                              BiometricPrompt.authenticate(CryptoObject)
                                          ↓
                              ¿Huella desbloquea key_123?
                                    ✅ SÍ → Registra
                                    ❌ NO → Error
```

---

## Alternativas para el Futuro

Si los contras se vuelven críticos (ej: empresa grande, muchos dispositivos), considerar:

1. **Reconocimiento Facial Exclusivo**:
   - Ya está implementado y funciona offline
   - No tiene el problema de vinculación por dispositivo
   - Precisión: 95-99%

2. **Reconocimiento de Voz** (nueva opción):
   - SDK: VOSK (open source) o VeriSpeak (comercial)
   - Portables entre dispositivos
   - Complementa al facial

3. **Hardware Externo + SDK Comercial**:
   - Para empresas grandes (500+ empleados)
   - Lectores profesionales (Suprema, ZKTeco)
   - Templates sincronizables en la nube

---

## Referencias

- [Android Biometric API](https://developer.android.com/training/sign-in/biometric-auth)
- [Android KeyStore System](https://developer.android.com/training/articles/keystore)
- [BiometricPrompt Documentation](https://developer.android.com/reference/android/hardware/biometrics/BiometricPrompt)
- [AOSP Fingerprint HAL](https://source.android.com/docs/security/features/authentication/fingerprint-hal)

---

## Historial de Cambios

| Fecha | Cambio |
|-------|--------|
| 2025-10-24 | Decisión inicial: Vinculación con KeyStore |
| 2025-10-24 | Evaluación de alternativas (ID+Sistema, SDKs externos) |
| 2025-10-24 | Implementación completa del sistema |
