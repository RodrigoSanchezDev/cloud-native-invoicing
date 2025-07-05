#!/bin/bash

echo "🚀 Pruebas locales Docker - Invoice Management System"
echo "=================================================="

# Función para mostrar estado con colores
show_status() {
    if [ $1 -eq 0 ]; then
        echo "✅ $2"
    else
        echo "❌ $2"
    fi
}

echo ""
echo "1. Verificando que los contenedores estén ejecutándose..."
CONTAINERS_RUNNING=$(docker ps --filter "name=local-" --format "table {{.Names}}\t{{.Status}}" | wc -l)
if [ $CONTAINERS_RUNNING -gt 1 ]; then
    show_status 0 "Contenedores Docker ejecutándose"
    docker ps --filter "name=local-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
else
    show_status 1 "Contenedores Docker no están ejecutándose"
    exit 1
fi

echo ""
echo "2. Probando conectividad básica..."

# Test File Service Health
echo "   - Probando File Service Health..."
curl -s -f http://localhost:8081/actuator/health > /dev/null
show_status $? "File Service Health Check"

# Test Invoice Service (should return auth error, but means it's responding)
echo "   - Probando Invoice Service (esperamos error de auth)..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/invoices)
if [ "$RESPONSE" = "401" ] || [ "$RESPONSE" = "403" ]; then
    show_status 0 "Invoice Service responde (error de auth esperado: $RESPONSE)"
else
    show_status 1 "Invoice Service no responde correctamente (código: $RESPONSE)"
fi

echo ""
echo "3. Verificando logs de arranque..."
echo "   - Últimas líneas del File Service:"
docker logs local-file-service --tail 3 | grep -E "(Started|ERROR|Exception)" || echo "     No hay errores críticos"

echo "   - Últimas líneas del Invoice Service:"
docker logs local-invoice-service --tail 3 | grep -E "(Started|ERROR|Exception)" || echo "     No hay errores críticos"

echo ""
echo "4. Verificando puertos..."
netstat -an | grep "8080\|8081" | grep LISTEN > /dev/null
show_status $? "Puertos 8080 y 8081 están escuchando"

echo ""
echo "5. Verificando directorio EFS local..."
if [ -d "/tmp/efs-local/invoices" ]; then
    show_status 0 "Directorio EFS local existe: /tmp/efs-local/invoices"
    ls -la /tmp/efs-local/invoices/ 2>/dev/null || echo "     (directorio vacío - normal para primera ejecución)"
else
    show_status 1 "Directorio EFS local no existe"
fi

echo ""
echo "🎯 RESUMEN:"
echo "- File Service disponible en: http://localhost:8081"
echo "- Invoice Service disponible en: http://localhost:8080"
echo "- Health Check: http://localhost:8081/actuator/health"
echo "- H2 Console: http://localhost:8080/h2-console"
echo ""
echo "📝 NOTAS:"
echo "- Los servicios requieren autenticación JWT para endpoints principales"
echo "- Para pruebas completas, necesitarás tokens válidos de Azure AD B2C"
echo "- EFS simulado en: /tmp/efs-local/invoices"
echo ""
echo "🛑 Para detener los contenedores:"
echo "   docker stop local-file-service local-invoice-service"
echo "   docker rm local-file-service local-invoice-service"
