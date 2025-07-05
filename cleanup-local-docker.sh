#!/bin/bash

echo "🧹 Limpiando contenedores locales de prueba..."
echo "=============================================="

echo "1. Deteniendo contenedores..."
docker stop local-file-service local-invoice-service 2>/dev/null || echo "   - Contenedores ya detenidos"

echo "2. Eliminando contenedores..."
docker rm local-file-service local-invoice-service 2>/dev/null || echo "   - Contenedores ya eliminados"

echo "3. Verificando limpieza..."
REMAINING=$(docker ps -a --filter "name=local-" --format "{{.Names}}" | wc -l | tr -d ' ')
if [ "$REMAINING" -eq 0 ]; then
    echo "✅ Todos los contenedores locales eliminados"
else
    echo "⚠️  Aún hay contenedores locales:"
    docker ps -a --filter "name=local-" --format "table {{.Names}}\t{{.Status}}"
fi

echo ""
echo "4. ¿Deseas eliminar también las imágenes locales? (s/N)"
read -r RESPONSE
if [[ "$RESPONSE" =~ ^[Ss]$ ]]; then
    echo "   Eliminando imágenes locales..."
    docker rmi local/file-service:latest local/invoice-service:latest 2>/dev/null || echo "   - Imágenes ya eliminadas"
    echo "✅ Imágenes locales eliminadas"
fi

echo ""
echo "5. ¿Deseas eliminar el directorio EFS local? (s/N)"
read -r RESPONSE
if [[ "$RESPONSE" =~ ^[Ss]$ ]]; then
    echo "   Eliminando /tmp/efs-local/..."
    rm -rf /tmp/efs-local/
    echo "✅ Directorio EFS local eliminado"
fi

echo ""
echo "🎉 Limpieza completada!"
echo ""
echo "📝 Para volver a ejecutar las pruebas:"
echo "   ./mvnw clean package -DskipTests"
echo "   docker build -t local/invoice-service:latest -f Dockerfile.invoice ."
echo "   docker build -t local/file-service:latest -f Dockerfile.file ."
echo "   ./test-local-docker.sh"
