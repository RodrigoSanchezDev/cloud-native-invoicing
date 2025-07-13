#!/bin/bash

echo "⚡ DESPLIEGUE RÁPIDO A EC2 - SOLO REINICIO DE SERVICIOS"
echo "======================================================"

# Configuración
PEM_PATH="/Users/rodrigosanchezcornejo/Library/Mobile Documents/com~apple~CloudDocs/Cursos Profesionales/Ing. Desarrollo de Software/DESARROLLO CLOUD NATIVE/Semana 7 /semana7.pem"
EC2_HOST="ec2-user@ec2-52-4-100-50.compute-1.amazonaws.com"

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

info() {
    echo -e "${BLUE}[INFO] $1${NC}"
}

# Función para ejecutar comandos SSH
ssh_exec() {
    ssh -i "$PEM_PATH" "$EC2_HOST" "$1"
}

# Función para transferir archivos
scp_transfer() {
    scp -i "$PEM_PATH" "$1" "$EC2_HOST:$2"
}

# 1. TRANSFERIR ARCHIVOS ACTUALIZADOS
log "1. Transfiriendo archivos actualizados..."
scp_transfer "docker-compose.yml" "~/"
scp_transfer "test_endpoints_updated.sh" "~/"
ssh_exec "chmod +x test_endpoints_updated.sh"
log "✅ Archivos transferidos"

# 2. REINICIAR SERVICIOS
log "2. Reiniciando servicios..."
ssh_exec "docker-compose down"
ssh_exec "docker-compose up -d"
log "✅ Servicios reiniciados"

# 3. ESPERAR Y VERIFICAR
info "Esperando a que los servicios se inicien..."
sleep 20

log "3. Verificando estado..."
ssh_exec "docker ps"

echo ""
echo "⚡ DESPLIEGUE RÁPIDO COMPLETADO"
echo "=============================="
echo "🧪 Para ejecutar pruebas:"
echo "   ssh -i \"$PEM_PATH\" $EC2_HOST"
echo "   ./test_endpoints_updated.sh"
log "✅ Reinicio completado"
