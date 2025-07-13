#!/bin/bash

echo "🚀 DESPLIEGUE COMPLETO A EC2 - CLOUD NATIVE INVOICING"
echo "======================================================"

# Configuración
PEM_PATH="/Users/rodrigosanchezcornejo/Library/Mobile Documents/com~apple~CloudDocs/Cursos Profesionales/Ing. Desarrollo de Software/DESARROLLO CLOUD NATIVE/Semana 7 /semana7.pem"
EC2_HOST="ec2-user@ec2-52-4-100-50.compute-1.amazonaws.com"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para logging
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

error() {
    echo -e "${RED}[ERROR] $1${NC}"
}

warning() {
    echo -e "${YELLOW}[WARNING] $1${NC}"
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

# Verificar conectividad
log "Verificando conectividad con EC2..."
if ! ssh_exec "echo 'Conexión exitosa'" > /dev/null 2>&1; then
    error "No se puede conectar a EC2. Verificar PEM y IP."
    exit 1
fi
log "✅ Conectividad verificada"

# 1. DETENER SERVICIOS EXISTENTES
log "1. Deteniendo servicios existentes..."
ssh_exec "docker-compose down 2>/dev/null || true"
ssh_exec "docker rm -f \$(docker ps -aq) 2>/dev/null || true"
log "✅ Servicios detenidos"

# 2. TRANSFERIR DOCKER-COMPOSE
log "2. Transfiriendo docker-compose.yml..."
scp_transfer "docker-compose.yml" "~/"
log "✅ docker-compose.yml transferido"

# 3. TRANSFERIR SCRIPT DE PRUEBAS
log "3. Transfiriendo script de pruebas..."
scp_transfer "test_endpoints_updated.sh" "~/"
ssh_exec "chmod +x test_endpoints_updated.sh"
log "✅ Script de pruebas transferido"

# 4. CONSTRUIR Y TRANSFERIR IMAGENES DOCKER
log "4. Construyendo imágenes Docker..."

# Construir invoice-service
info "Construyendo invoice-service..."
if ! docker build -f Dockerfile.invoice -t invoice-service:latest .; then
    error "Fallo al construir invoice-service"
    exit 1
fi
log "✅ invoice-service construido"

# Construir file-service
info "Construyendo file-service..."
if ! docker build -f Dockerfile.file -t file-service:latest .; then
    error "Fallo al construir file-service"
    exit 1
fi
log "✅ file-service construido"

# Construir rabbitmq-service
info "Construyendo rabbitmq-service..."
if ! docker build -f Dockerfile.rabbitmq -t rabbitmq-service:latest .; then
    error "Fallo al construir rabbitmq-service"
    exit 1
fi
log "✅ rabbitmq-service construido"

# 5. EXPORTAR Y TRANSFERIR IMAGENES
log "5. Exportando y transfiriendo imágenes Docker..."

# Crear directorio temporal
mkdir -p /tmp/docker-images

# Exportar imágenes
info "Exportando imágenes..."
docker save invoice-service:latest | gzip > /tmp/docker-images/invoice-service.tar.gz
docker save file-service:latest | gzip > /tmp/docker-images/file-service.tar.gz
docker save rabbitmq-service:latest | gzip > /tmp/docker-images/rabbitmq-service.tar.gz

# Transferir imágenes
info "Transfiriendo imágenes a EC2..."
scp_transfer "/tmp/docker-images/invoice-service.tar.gz" "~/"
scp_transfer "/tmp/docker-images/file-service.tar.gz" "~/"
scp_transfer "/tmp/docker-images/rabbitmq-service.tar.gz" "~/"

# Limpiar archivos temporales locales
rm -rf /tmp/docker-images
log "✅ Imágenes transferidas"

# 6. CARGAR IMAGENES EN EC2
log "6. Cargando imágenes en EC2..."
ssh_exec "docker load < invoice-service.tar.gz"
ssh_exec "docker load < file-service.tar.gz"
ssh_exec "docker load < rabbitmq-service.tar.gz"
log "✅ Imágenes cargadas en EC2"

# 7. LIMPIAR ARCHIVOS TAR EN EC2
log "7. Limpiando archivos temporales en EC2..."
ssh_exec "rm -f *.tar.gz"
log "✅ Archivos temporales eliminados"

# 8. INICIAR SERVICIOS
log "8. Iniciando servicios con docker-compose..."
ssh_exec "docker-compose up -d"

# Esperar a que los servicios se inicien
info "Esperando a que los servicios se inicien..."
sleep 30

# 9. VERIFICAR ESTADO DE SERVICIOS
log "9. Verificando estado de servicios..."
ssh_exec "docker ps"

# 10. VERIFICAR HEALTH CHECKS
log "10. Verificando health checks..."
info "Verificando invoice-service..."
if ssh_exec "curl -f http://localhost:8080/actuator/health > /dev/null 2>&1"; then
    log "✅ invoice-service está saludable"
else
    warning "⚠️  invoice-service podría no estar listo aún"
fi

info "Verificando file-service..."
if ssh_exec "curl -f http://localhost:8081/actuator/health > /dev/null 2>&1"; then
    log "✅ file-service está saludable"
else
    warning "⚠️  file-service podría no estar listo aún"
fi

info "Verificando rabbitmq-service..."
if ssh_exec "curl -f http://localhost:8082/api/rabbitmq/health > /dev/null 2>&1"; then
    log "✅ rabbitmq-service está saludable"
else
    warning "⚠️  rabbitmq-service podría no estar listo aún"
fi

# 11. MOSTRAR INFORMACIÓN FINAL
echo ""
echo "🎉 DESPLIEGUE COMPLETADO"
echo "======================="
echo "📍 Servicios disponibles en:"
echo "   • Invoice Service: http://52.4.100.50:8080"
echo "   • File Service: http://52.4.100.50:8081"
echo "   • RabbitMQ Service: http://52.4.100.50:8082"
echo "   • RabbitMQ Management: http://52.4.100.50:15672 (admin/admin123)"
echo ""
echo "🧪 Para ejecutar pruebas:"
echo "   ssh -i \"$PEM_PATH\" $EC2_HOST"
echo "   ./test_endpoints_updated.sh"
echo ""
echo "📊 Para ver logs:"
echo "   ssh -i \"$PEM_PATH\" $EC2_HOST"
echo "   docker logs [container-name]"
echo ""
log "✅ Despliegue completo finalizado exitosamente"
