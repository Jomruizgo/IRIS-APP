#!/bin/bash

# Script para verificar que la base de datos se está guardando en disco
# y no en memoria RAM

echo "=========================================="
echo "VERIFICACIÓN DE PERSISTENCIA DE BASE DE DATOS"
echo "=========================================="
echo ""

PACKAGE_NAME="com.attendance.facerecognition"
DB_NAME="face_recognition_attendance_db"

echo "1. Verificando si la app está instalada..."
adb shell pm list packages | grep "$PACKAGE_NAME"
if [ $? -ne 0 ]; then
    echo "❌ La app no está instalada"
    exit 1
fi
echo "✅ App instalada"
echo ""

echo "2. Ubicación de la base de datos:"
DB_PATH="/data/data/$PACKAGE_NAME/databases/$DB_NAME"
echo "   $DB_PATH"
echo ""

echo "3. Verificando existencia del archivo de BD..."
adb shell "su -c 'ls -lh $DB_PATH'" 2>/dev/null || adb shell "run-as $PACKAGE_NAME ls -lh databases/$DB_NAME"
if [ $? -eq 0 ]; then
    echo "✅ Archivo de base de datos existe en disco"
else
    echo "⚠️  No se pudo acceder al archivo (puede requerir root o app en modo debug)"
fi
echo ""

echo "4. Verificando archivos relacionados..."
adb shell "run-as $PACKAGE_NAME ls -lh databases/" 2>/dev/null
echo ""

echo "5. Verificando SharedPreferences (passphrase de encriptación)..."
adb shell "run-as $PACKAGE_NAME ls -lh shared_prefs/" 2>/dev/null
echo ""

echo "=========================================="
echo "PRUEBA DE PERSISTENCIA"
echo "=========================================="
echo ""
echo "INSTRUCCIONES:"
echo "1. Abre la app y crea un usuario admin"
echo "2. Cierra COMPLETAMENTE la app (Force Stop)"
echo "3. Reabre la app"
echo ""
echo "RESULTADO ESPERADO:"
echo "✅ La app NO pide crear usuario de nuevo"
echo "✅ Los datos del usuario siguen ahí"
echo "✅ Esto confirma que la BD está en DISCO"
echo ""
echo "RESULTADO SI ESTUVIERA EN RAM:"
echo "❌ Al reabrir, pediría crear usuario de nuevo"
echo "❌ Todos los datos se habrían perdido"
echo ""

echo "=========================================="
echo "VERIFICACIÓN ADICIONAL - Tamaño de BD"
echo "=========================================="
echo ""
echo "Consultando tamaño del archivo de BD..."
adb shell "run-as $PACKAGE_NAME du -h databases/$DB_NAME" 2>/dev/null

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Si el archivo tiene tamaño > 0, los datos están guardados"
    echo "✅ Si es 0 bytes, la BD está vacía pero sigue siendo persistente"
else
    echo ""
    echo "⚠️  No se pudo verificar el tamaño (requiere app en modo debug)"
fi

echo ""
echo "=========================================="
echo "FIN DE LA VERIFICACIÓN"
echo "=========================================="
