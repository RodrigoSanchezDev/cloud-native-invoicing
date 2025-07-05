#!/bin/bash

echo "🔄 REINICIANDO BACKEND PARA LIMPIAR CACHE JWKS"
echo "=============================================="
echo ""

echo "1️⃣ Parando contenedor invoice-service..."
docker stop invoice-service

echo ""
echo "2️⃣ Iniciando contenedor invoice-service..."
docker start invoice-service

echo ""
echo "3️⃣ Esperando que el servicio esté listo..."
sleep 15

echo ""
echo "4️⃣ Verificando que el servicio está corriendo..."
docker logs invoice-service --tail 10

echo ""
echo "✅ Servicio reiniciado. Ahora ejecuta:"
echo "   ./test-final-with-jwt.sh"
