#!/bin/bash

echo "🔄 ACTUALIZANDO FILE-SERVICE CON CAMBIOS DEL REPOSITORIO"
echo "======================================================="

echo "📥 1. Actualizando código desde GitHub..."
git pull origin main

if [ $? -ne 0 ]; then
    echo "❌ Error al actualizar código desde GitHub"
    exit 1
fi

echo "✅ Código actualizado desde GitHub"

echo "🛑 2. Deteniendo y eliminando contenedor actual..."
docker stop file-service 2>/dev/null || true
docker rm file-service 2>/dev/null || true

echo "🗑️ 3. Eliminando imagen anterior..."
docker rmi file-service:latest 2>/dev/null || true

echo "📦 4. Compilando nuevo código..."
cd file-service
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Error en compilación"
    exit 1
fi

echo "✅ Compilación exitosa"

echo "🐳 5. Construyendo nueva imagen Docker..."
docker build -f ../Dockerfile.file -t file-service:latest .

if [ $? -ne 0 ]; then
    echo "❌ Error construyendo imagen Docker"
    exit 1
fi

echo "✅ Nueva imagen Docker construida"

echo "🚀 6. Ejecutando nuevo contenedor..."
docker run -d \
  --name file-service \
  -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=cloud \
  file-service:latest

if [ $? -ne 0 ]; then
    echo "❌ Error ejecutando contenedor"
    exit 1
fi

echo "⏳ 7. Esperando que el servicio esté listo..."
sleep 20

echo "🏥 8. Verificando que el servicio esté funcionando..."
HEALTH_CHECK=$(curl -s http://localhost:8081/actuator/health | grep '"status":"UP"')

if [ -n "$HEALTH_CHECK" ]; then
    echo "✅ File Service está funcionando correctamente"
else
    echo "⚠️  File Service puede estar iniciando. Verificando logs..."
    docker logs file-service --tail 10
fi

echo ""
echo "🎯 VERIFICACIÓN DE ENDPOINTS CORREGIDOS"
echo "======================================"

echo "📋 Verificando que los endpoints están mapeados correctamente..."

# Test básico de endpoints
echo "🔧 Probando endpoint de listado..."
curl -s -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8081/files/list > /dev/null
if [ $? -eq 0 ]; then
    echo "✅ Endpoint /files/list responde"
else
    echo "❌ Endpoint /files/list no responde"
fi

echo ""
echo "🎉 ACTUALIZACIÓN COMPLETADA"
echo "=========================="
echo "✅ Código actualizado desde GitHub"
echo "✅ Nueva imagen Docker construida"
echo "✅ Contenedor redesplegado"
echo ""
echo "📋 PRÓXIMOS PASOS:"
echo "1. Ejecuta tu script de pruebas: ./test-endpoints-clean.sh"
echo "2. Los endpoints /download y /delete ahora deberían funcionar"
echo ""
echo "🔍 Si sigues con problemas, verifica los logs:"
echo "   docker logs file-service"
