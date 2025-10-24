# Decisión de Diseño: Congruencia entre Registro y Reconocimiento

**Fecha**: 2025-01-24
**Estado**: ✅ Implementado
**Tipo**: Decisión Arquitectónica

---

## Contexto

El sistema de reconocimiento facial captura múltiples fotos del empleado durante el registro desde diferentes ángulos. Posteriormente, durante el proceso de asistencia, se utiliza un sistema de liveness detection (detección de vida) que pide al usuario realizar acciones aleatorias para verificar que es una persona real y no una foto/video.

## Problema Identificado

Había una **inconsistencia crítica** entre:

1. **Qué poses capturamos durante el REGISTRO**
2. **Qué poses pedimos durante el RECONOCIMIENTO (liveness)**

### Registro Original (10 fotos)
```
Fotos 1-3:  De FRENTE          (ángulo Y: -15° a +15°)
Fotos 4-6:  Giradas IZQUIERDA  (ángulo Y: -45° a -20°)
Fotos 7-9:  Giradas DERECHA    (ángulo Y: +20° a +45°)
Foto 10:    De FRENTE          (ángulo Y: -15° a +15°)
```

### Liveness Detection Original (5 desafíos)
```
✅ TURN_LEFT  - Girar izquierda  → SÍ tenemos fotos de entrenamiento
✅ TURN_RIGHT - Girar derecha    → SÍ tenemos fotos de entrenamiento
⚠️ BLINK      - Parpadear        → NO afecta embedding (válido para anti-spoofing)
❌ SMILE      - Sonreír          → NO tenemos fotos sonriendo
❌ LOOK_UP    - Mirar arriba     → NO tenemos fotos mirando arriba
```

## Consecuencias del Problema

Si pedimos al usuario que realice una acción (ej: mirar arriba) durante el reconocimiento, pero **no tenemos fotos de esa pose** en el registro, pueden ocurrir dos escenarios malos:

1. **Falso Negativo**: El sistema no reconoce al empleado porque su pose actual (mirando arriba) no coincide con ningún embedding almacenado (todos de frente/izquierda/derecha).

2. **Inconsistencia Conceptual**: Estamos pidiendo algo que nunca entrenamos, lo cual es conceptualmente incorrecto en ML.

## Decisión Tomada

**Reducir los desafíos de liveness detection a solo aquellos que son congruentes con las poses capturadas durante el registro.**

### Desafíos Permitidos (3)

1. **BLINK (Parpadear)**
   - ✅ Válido para anti-spoofing (detecta que no es una foto estática)
   - ✅ No afecta significativamente el embedding facial
   - ✅ Los ojos cerrados momentáneamente no impiden la identificación

2. **TURN_LEFT (Girar izquierda)**
   - ✅ Tenemos 3 fotos con esta pose (fotos 4-6)
   - ✅ El modelo tiene embeddings entrenados con esta rotación
   - ✅ Coincidencia directa entre registro y reconocimiento

3. **TURN_RIGHT (Girar derecha)**
   - ✅ Tenemos 3 fotos con esta pose (fotos 7-9)
   - ✅ El modelo tiene embeddings entrenados con esta rotación
   - ✅ Coincidencia directa entre registro y reconocimiento

### Desafíos Eliminados (2)

1. **SMILE (Sonreír)**
   - ❌ No capturamos fotos sonriendo durante el registro
   - ❌ Los embeddings almacenados son de rostros neutros
   - ❌ Sonreír cambia significativamente las características faciales
   - **Riesgo**: Falsos negativos al no reconocer al empleado sonriendo

2. **LOOK_UP (Mirar arriba)**
   - ❌ No capturamos fotos mirando arriba durante el registro
   - ❌ Todos los embeddings son con cabeza horizontal (pitch ≈ 0°)
   - ❌ Mirar arriba cambia ángulos faciales significativamente
   - **Riesgo**: Falsos negativos al no reconocer la pose

## Implementación

### Archivo Modificado
`app/src/main/java/com/attendance/facerecognition/ml/LivenessDetector.kt`

### Cambios Realizados

```kotlin
// ANTES (5 desafíos)
enum class ChallengeType {
    BLINK,
    SMILE,        // ❌ ELIMINADO
    TURN_LEFT,
    TURN_RIGHT,
    LOOK_UP       // ❌ ELIMINADO
}

// DESPUÉS (3 desafíos)
enum class ChallengeType {
    BLINK,          // ✅ Anti-spoofing, no afecta embedding
    TURN_LEFT,      // ✅ Tenemos fotos de entrenamiento
    TURN_RIGHT      // ✅ Tenemos fotos de entrenamiento
}
```

Se eliminaron las funciones auxiliares:
- `verifySmile()`
- `verifyLookUp()`

Se actualizó `getChallengeText()` y `verifyChallenge()` para manejar solo los 3 tipos permitidos.

## Alternativas Consideradas

### Alternativa 1: Agregar más poses al registro
**Descripción**: Capturar fotos adicionales (sonriendo, mirando arriba) durante el registro.

**Pros**:
- Mayor variedad de desafíos de liveness
- Más robusto ante diferentes expresiones/poses

**Cons**:
- ❌ Proceso de registro más largo (más fotos = más tiempo)
- ❌ Más datos a almacenar
- ❌ Peor experiencia de usuario (más cansado)

**Veredicto**: Rechazada. Preferimos un registro rápido.

---

### Alternativa 2: Usar solo poses frontales + parpadeo
**Descripción**: Eliminar también TURN_LEFT y TURN_RIGHT, usar solo frente + parpadeo.

**Pros**:
- Registro aún más rápido (solo 3-4 fotos de frente)
- Más simple

**Cons**:
- ❌ Menos seguridad anti-spoofing (menos variedad)
- ❌ Menos robusto ante diferentes iluminaciones/ángulos

**Veredicto**: Rechazada. Las poses laterales mejoran significativamente la precisión.

---

### Alternativa 3: Liveness pasivo (sin desafíos)
**Descripción**: No pedir ninguna acción, usar solo análisis pasivo (textura, profundidad).

**Pros**:
- Experiencia de usuario más fluida

**Cons**:
- ❌ Más vulnerable a ataques de replay (foto/video)
- ❌ Requiere modelos adicionales más complejos

**Veredicto**: Rechazada para MVP. Considerar para futuro.

## Impacto

### Seguridad
- ✅ **Mantiene** protección anti-spoofing (parpadeo)
- ✅ **Mantiene** variedad de poses (izquierda, derecha)
- ✅ **Elimina** riesgo de falsos negativos por poses no entrenadas

### Experiencia de Usuario
- ✅ Desafíos más predecibles
- ✅ Mayor tasa de éxito en reconocimiento
- ✅ Menos frustración

### Mantenibilidad
- ✅ Código más simple (menos casos a manejar)
- ✅ Menos funciones de verificación
- ✅ Documentación clara del razonamiento

## Reglas para Futuras Modificaciones

### ⚠️ Si quieres AGREGAR un nuevo desafío de liveness:

1. **Primero** modifica el registro para capturar fotos con esa pose
2. **Luego** agrega el desafío al enum `ChallengeType`
3. **Actualiza** el BACKLOG.md explicando el cambio
4. **Actualiza** este documento

### ⚠️ Si quieres MODIFICAR el registro:

1. **Primero** revisa qué desafíos de liveness usan esas poses
2. **Ajusta** los desafíos si es necesario
3. **Documenta** el cambio

## Principio de Diseño Establecido

> **"Solo pediremos al usuario realizar acciones durante el reconocimiento que hayamos capturado explícitamente durante el registro."**

Este principio garantiza congruencia entre entrenamiento e inferencia, reduciendo falsos negativos y mejorando la experiencia del usuario.

---

## Referencias

- `app/src/main/java/com/attendance/facerecognition/ml/LivenessDetector.kt` - Implementación
- `app/src/main/java/com/attendance/facerecognition/ui/viewmodels/EmployeeRegistrationViewModel.kt` - Lógica de registro
- `docs/01-BACKLOG.md` - US-011 (Registro de huella como alternativa)

---

**Actualizado**: 2025-01-24
**Revisión**: v1.0
