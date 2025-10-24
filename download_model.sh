#!/bin/bash

# Script para descargar el modelo MobileFaceNet

set -e  # Salir si hay error

echo "==================================================="
echo "Descargando modelo MobileFaceNet para TensorFlow Lite"
echo "==================================================="
echo ""

# Crear directorio assets si no existe
ASSETS_DIR="app/src/main/assets"
mkdir -p "$ASSETS_DIR"

# URL del modelo (usando repositorio de GitHub que tiene el modelo)
MODEL_URL="https://github.com/MCarlomagno/FaceRecognitionAuth/raw/master/assets/mobilefacenet.tflite"
MODEL_PATH="$ASSETS_DIR/mobilefacenet.tflite"

echo "📥 Descargando modelo desde:"
echo "   $MODEL_URL"
echo ""

if command -v wget &> /dev/null; then
    wget -O "$MODEL_PATH" "$MODEL_URL"
elif command -v curl &> /dev/null; then
    curl -L -o "$MODEL_PATH" "$MODEL_URL"
else
    echo "❌ Error: wget o curl no están instalados"
    echo "   Instala uno de ellos para continuar:"
    echo "   - Ubuntu/Debian: sudo apt install wget"
    echo "   - Fedora: sudo dnf install wget"
    exit 1
fi

# Verificar que el archivo se descargó correctamente
if [ -f "$MODEL_PATH" ]; then
    FILE_SIZE=$(du -h "$MODEL_PATH" | cut -f1)
    echo ""
    echo "✅ Modelo descargado exitosamente!"
    echo "   Ubicación: $MODEL_PATH"
    echo "   Tamaño: $FILE_SIZE"
    echo ""
    echo "==================================================="
    echo "El modelo está listo para usar."
    echo "Ahora puedes compilar el proyecto en Android Studio."
    echo "==================================================="
else
    echo ""
    echo "❌ Error: El archivo no se descargó correctamente"
    exit 1
fi
