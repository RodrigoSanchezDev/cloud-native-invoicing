#!/bin/bash

echo "🧹 Limpiando contenedores existentes en EC2..."

# Limpiar contenedores existentes
echo "⏹️  Deteniendo contenedores..."
docker stop $(docker ps -q) 2>/dev/null || echo "No hay contenedores ejecutándose"

echo "🗑️  Eliminando contenedores..."
docker rm $(docker ps -aq) 2>/dev/null || echo "No hay contenedores para eliminar"

echo "🧹 Limpiando redes no utilizadas..."
docker network prune -f

echo "📥 Descargando imágenes más recientes..."
docker pull sanchezdev01/invoice-service:latest
docker pull sanchezdev01/file-service:latest  
docker pull sanchezdev01/rabbitmq-service:latest
docker pull rabbitmq:3.11-management

echo "🚀 Iniciando servicios con docker-compose..."
docker-compose up -d

echo "⏳ Esperando que los servicios estén listos..."
sleep 60

echo "✅ Verificando estado de los servicios..."
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "🔍 Verificando logs de servicios..."
echo "--- RabbitMQ Service Logs ---"
docker logs rabbitmq-service --tail 5 2>/dev/null || echo "RabbitMQ Service aún no iniciado"

echo ""
echo "--- Invoice Service Logs ---"
docker logs invoice-service --tail 5 2>/dev/null || echo "Invoice Service aún no iniciado"

echo ""
echo "🌐 Servicios disponibles en:"
echo "   📊 RabbitMQ Management: http://52.4.100.50:15672 (admin/admin123)"
echo "   📋 Invoice Service: http://52.4.100.50:8080"
echo "   📁 File Service: http://52.4.100.50:8081"  
echo "   🐰 RabbitMQ Service: http://52.4.100.50:8082"

echo ""
echo "🧪 Endpoints de prueba:"
echo "   GET  http://52.4.100.50:8082/api/rabbitmq/boletas"
echo "   GET  http://52.4.100.50:8080/actuator/health"
echo "   GET  http://52.4.100.50:8081/actuator/health"
echo "   GET  http://52.4.100.50:8082/actuator/health"
