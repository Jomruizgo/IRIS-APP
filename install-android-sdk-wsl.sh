#!/bin/bash
# Script para instalar Android SDK en WSL

set -e

echo "========================================="
echo "Instalando Android SDK Command Line Tools en WSL"
echo "========================================="
echo ""

# Crear directorio para Android SDK
ANDROID_HOME="$HOME/Android/Sdk"
mkdir -p "$ANDROID_HOME"

# Descargar Android Command Line Tools
echo "📥 Descargando Android Command Line Tools..."
cd /tmp
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip

# Extraer
echo "📦 Extrayendo..."
unzip -q commandlinetools-linux-11076708_latest.zip
mkdir -p "$ANDROID_HOME/cmdline-tools/latest"
mv cmdline-tools/* "$ANDROID_HOME/cmdline-tools/latest/"
rm commandlinetools-linux-11076708_latest.zip

# Configurar variables de entorno
echo ""
echo "🔧 Configurando variables de entorno..."
cat >> ~/.bashrc << 'EOF'

# Android SDK
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin"
export PATH="$PATH:$ANDROID_HOME/platform-tools"
export PATH="$PATH:$ANDROID_HOME/emulator"
EOF

source ~/.bashrc

# Instalar componentes necesarios
echo ""
echo "📦 Instalando Android SDK components..."
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo ""
echo "✅ Android SDK instalado en: $ANDROID_HOME"
echo ""
echo "Cierra y vuelve a abrir la terminal para aplicar cambios."
