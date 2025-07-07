#!/bin/bash

echo "🐰 Iniciando sistema completo con RabbitMQ..."

# Construir todos los microservicios
echo "📦 Construyendo microservicios..."
./mvnw clean package -DskipTests

echo "🐳 Construyendo imágenes Docker..."

# Construir imágenes
docker build -f Dockerfile.invoice -t sanchezdev01/invoice-service:latest .
docker build -f Dockerfile.file -t sanchezdev01/file-service:latest .
docker build -f Dockerfile.rabbitmq -t sanchezdev01/rabbitmq-service:latest .

echo "🚀 Levantando servicios con docker-compose..."

# Levantar con docker-compose
docker-compose up -d

echo "⏳ Esperando que los servicios estén listos..."
sleep 30

echo "✅ Verificando estado de los servicios..."
docker-compose ps

echo ""
echo "🌐 URLs disponibles:"
echo "   📊 RabbitMQ Management UI: http://localhost:15672 (admin/admin123)"
echo "   🗄️  H2 Console (Oracle sim): http://localhost:8083"
echo "   📋 Invoice Service: http://localhost:8080"
echo "   📁 File Service: http://localhost:8081"
echo "   🐰 RabbitMQ Service: http://localhost:8082"
echo ""
echo "🧪 Endpoints de prueba:"
echo "   Health Check RabbitMQ: GET http://localhost:8082/api/rabbitmq/health"
echo "   Listar boletas Oracle: GET http://localhost:8082/api/rabbitmq/boletas"
echo "   Enviar mensaje: POST http://localhost:8082/api/rabbitmq/send-message"
echo ""
echo "🔑 Para autenticación, usa tu token de Azure AD B2C"
echo ""
echo "✨ Sistema listo para pruebas!"
