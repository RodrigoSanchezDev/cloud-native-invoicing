#!/bin/bash

echo "🔧 CORRIGIENDO CONFIGURACIÓN ISSUER Y RECONSTRUYENDO"
echo "==================================================="
echo ""

echo "🎯 CAMBIO REALIZADO:"
echo "- ELIMINADO: Variable ISSUER conflictiva en file-service"
echo "- MANTENIDO: JWKS correcto del User Flow B2C_1_AppS3"
echo ""

echo "1️⃣ Reconstruyendo file-service..."
cd file-service
mvn clean package -DskipTests

echo ""
echo "2️⃣ Parando contenedor file-service..."
cd ..
docker stop file-service 2>/dev/null || true

echo ""
echo "3️⃣ Reconstruyendo imagen file-service..."
docker build -f Dockerfile.file -t file-service .

echo ""
echo "4️⃣ Iniciando contenedor file-service..."
docker run -d --name file-service -p 8081:8081 file-service

echo ""
echo "5️⃣ Reiniciando invoice-service para limpiar cache..."
docker restart invoice-service

echo ""
echo "6️⃣ Esperando que los servicios estén listos..."
sleep 20

echo ""
echo "7️⃣ Verificando que los servicios están corriendo..."
docker logs invoice-service --tail 5
echo ""
docker logs file-service --tail 5

echo ""
echo "✅ Servicios reconstruidos y reiniciados"
echo "========================================"
echo ""
echo "🎯 AHORA PRUEBA CON UN JWT NUEVO:"
echo "1. Ve a tu frontend"
echo "2. Haz logout/login para obtener un JWT fresco"
echo "3. Ejecuta: ./test-final-with-jwt.sh"
