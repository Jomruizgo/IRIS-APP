#!/bin/bash
# Script wrapper para configurar variables de entorno antes de compilar

export ANDROID_HOME="/mnt/c/Users/Jorge Ruiz/AppData/Local/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$PATH"

echo "🔧 Variables de entorno configuradas:"
echo "   ANDROID_HOME: $ANDROID_HOME"
echo ""

# Ejecutar gradlew con todos los argumentos
./gradlew "$@"
