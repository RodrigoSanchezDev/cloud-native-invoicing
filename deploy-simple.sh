#!/bin/bash

echo "🐰 Despliegue Simplificado - Solo RabbitMQ + Invoice Service"
echo "============================================================"

# Variables
SSH_KEY="/Users/rodrigosanchezcornejo/Library/Mobile Documents/com~apple~CloudDocs/Cursos Profesionales/Ing. Desarrollo de Software/DESARROLLO CLOUD NATIVE/Semana 7 /semana7.pem"
EC2_HOST="52.4.100.50"
EC2_USER="ec2-user"

echo "📤 Subiendo cambios a GitHub..."
git add .
git commit -m "Deploy: RabbitMQ + Invoice Service for EC2 $(date +%Y-%m-%d_%H-%M-%S)"
git push origin main

echo "⏳ Esperando GitHub Actions (30 segundos)..."
sleep 30

echo "🚀 Creando docker-compose simplificado para EC2..."

cat > docker-compose-simple.yml << 'EOF'
version: '3.8'

services:
  # RabbitMQ Message Broker
  rabbitmq:
    image: rabbitmq:3.11-management
    container_name: rabbitmq-server
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin123
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      timeout: 10s
      retries: 10
      interval: 10s

  # Invoice Service
  invoice-service:
    image: sanchezdev01/invoice-service:latest
    container_name: invoice-service
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=ec2
      - RABBITMQ_HOST=rabbitmq
      - RABBITMQ_PORT=5672
      - RABBITMQ_USERNAME=admin
      - RABBITMQ_PASSWORD=admin123
      - RABBITMQ_ENABLED=true
    depends_on:
      rabbitmq:
        condition: service_healthy
    restart: unless-stopped

  # RabbitMQ Service
  rabbitmq-service:
    image: sanchezdev01/rabbitmq-service:latest
    container_name: rabbitmq-service
    ports:
      - "8082:8082"
    environment:
      - SPRING_PROFILES_ACTIVE=ec2
      - RABBITMQ_HOST=rabbitmq
      - RABBITMQ_PORT=5672
      - RABBITMQ_USERNAME=admin
      - RABBITMQ_PASSWORD=admin123
    depends_on:
      rabbitmq:
        condition: service_healthy
    restart: unless-stopped

volumes:
  rabbitmq_data:
EOF

echo "📤 Transferir docker-compose a EC2..."
scp -i "$SSH_KEY" docker-compose-simple.yml "$EC2_USER@$EC2_HOST:~/docker-compose.yml"

echo "🔗 Desplegando en EC2..."
ssh -i "$SSH_KEY" "$EC2_USER@$EC2_HOST" << 'ENDSSH'
    echo "🧹 Limpiando contenedores anteriores..."
    docker stop $(docker ps -q) 2>/dev/null || true
    docker rm $(docker ps -aq) 2>/dev/null || true
    
    echo "📥 Descargando imágenes más recientes..."
    docker pull sanchezdev01/invoice-service:latest
    docker pull sanchezdev01/rabbitmq-service:latest
    docker pull rabbitmq:3.11-management
    
    echo "🚀 Iniciando servicios..."
    docker-compose up -d
    
    echo "⏳ Esperando servicios..."
    sleep 90
    
    echo "✅ Estado de contenedores:"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    
    echo ""
    echo "🔍 Logs de RabbitMQ Service:"
    docker logs rabbitmq-service --tail 10
    
    echo ""
    echo "🌐 Servicios disponibles:"
    echo "   📊 RabbitMQ Management: http://52.4.100.50:15672 (admin/admin123)"
    echo "   📋 Invoice Service: http://52.4.100.50:8080"
    echo "   🐰 RabbitMQ Service: http://52.4.100.50:8082"
ENDSSH

echo ""
echo "🧪 Probando conectividad..."
sleep 30

echo "📋 Invoice Service Health:"
curl -s http://52.4.100.50:8080/actuator/health | jq . || echo "❌ No disponible"

echo ""
echo "🐰 RabbitMQ Service Health:"
curl -s http://52.4.100.50:8082/actuator/health | jq . || echo "❌ No disponible"

echo ""
echo "🎉 ¡Despliegue completo!"

# Limpiar archivo temporal
rm -f docker-compose-simple.yml
