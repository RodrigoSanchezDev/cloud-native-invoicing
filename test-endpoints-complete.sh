#!/bin/bash

echo "🔍 Pruebas Completas de Endpoints y Configuración"
echo "==============================================="

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# URLs base
INVOICE_SERVICE="http://localhost:8080"
FILE_SERVICE="http://localhost:8081"

# Tu configuración de Azure B2C desde el código
AZURE_B2C_TENANT="duoccloudnatives6.b2clogin.com"
AZURE_B2C_POLICY="B2C_1_AppS3"
JWT_AUDIENCE="054924b5-14ae-4ede-9d8b-a1a71a1e723f"

echo -e "${BLUE}1. VERIFICANDO CONFIGURACIÓN DE AZURE B2C${NC}"
echo "==========================================="
echo "🔍 Tenant: $AZURE_B2C_TENANT"
echo "🔍 Policy: $AZURE_B2C_POLICY"
echo "🔍 Client ID (Audience): $JWT_AUDIENCE"

echo ""
echo "🌐 Verificando JWK endpoint de Azure B2C..."
JWK_ENDPOINT="https://$AZURE_B2C_TENANT/$AZURE_B2C_TENANT.onmicrosoft.com/$AZURE_B2C_POLICY/discovery/v2.0/keys"
echo "   URL: $JWK_ENDPOINT"

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$JWK_ENDPOINT")
if [ "$HTTP_CODE" = "200" ]; then
    echo -e "   ${GREEN}✅ JWK endpoint accesible${NC}"
    curl -s "$JWK_ENDPOINT" | jq '.keys | length' > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        KEY_COUNT=$(curl -s "$JWK_ENDPOINT" | jq '.keys | length')
        echo -e "   ${GREEN}✅ JSON válido con $KEY_COUNT claves públicas${NC}"
    else
        echo -e "   ${YELLOW}⚠️  Respuesta no es JSON válido${NC}"
    fi
else
    echo -e "   ${RED}❌ JWK endpoint no accesible (HTTP: $HTTP_CODE)${NC}"
fi

echo ""
echo -e "${BLUE}2. PROBANDO ENDPOINTS SIN AUTENTICACIÓN${NC}"
echo "========================================"

test_endpoint() {
    local url=$1
    local method=${2:-GET}
    local description=$3
    
    echo "   Testing: $method $url"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url")
    
    case $HTTP_CODE in
        200|201)
            echo -e "   ${GREEN}✅ $description - OK ($HTTP_CODE)${NC}"
            ;;
        401|403)
            echo -e "   ${YELLOW}🔒 $description - Auth required ($HTTP_CODE) - EXPECTED${NC}"
            ;;
        404)
            echo -e "   ${RED}❌ $description - Not found ($HTTP_CODE)${NC}"
            ;;
        500)
            echo -e "   ${RED}❌ $description - Server error ($HTTP_CODE)${NC}"
            ;;
        *)
            echo -e "   ${YELLOW}⚠️  $description - Unexpected ($HTTP_CODE)${NC}"
            ;;
    esac
}

# Health checks
test_endpoint "$FILE_SERVICE/actuator/health" "GET" "File Service Health"
test_endpoint "$INVOICE_SERVICE/h2-console" "GET" "H2 Console"

# Invoice Service endpoints
echo ""
echo "📋 Invoice Service Endpoints:"
test_endpoint "$INVOICE_SERVICE/api/invoices" "GET" "Get all invoices"
test_endpoint "$INVOICE_SERVICE/api/invoices/1" "GET" "Get invoice by ID"
test_endpoint "$INVOICE_SERVICE/api/invoices/history/test-client" "GET" "Get client history"

# File Service endpoints
echo ""
echo "📁 File Service Endpoints:"
test_endpoint "$FILE_SERVICE/files/list" "GET" "List files"

echo ""
echo -e "${BLUE}3. VERIFICANDO CONFIGURACIÓN JWT EN LOGS${NC}"
echo "========================================"

echo "🔍 Buscando configuración JWT en logs del Invoice Service..."
docker logs local-invoice-service 2>&1 | grep -i "jwt\|jwk\|issuer" | head -5
if [ $? -ne 0 ]; then
    echo "   No se encontraron logs específicos de JWT"
fi

echo ""
echo "🔍 Buscando configuración JWT en logs del File Service..."
docker logs local-file-service 2>&1 | grep -i "jwt\|jwk\|issuer" | head -5
if [ $? -ne 0 ]; then
    echo "   No se encontraron logs específicos de JWT"
fi

echo ""
echo -e "${BLUE}4. VERIFICANDO CONFIGURACIÓN DE CORS${NC}"
echo "====================================="

echo "🌐 Probando CORS preflight..."
curl -s -X OPTIONS "$INVOICE_SERVICE/api/invoices" \
    -H "Origin: http://localhost:3000" \
    -H "Access-Control-Request-Method: GET" \
    -H "Access-Control-Request-Headers: Authorization" \
    -v 2>&1 | grep -E "(Access-Control|HTTP/)"

echo ""
echo -e "${BLUE}5. ESTRUCTURA DE CONFIGURACIÓN DETECTADA${NC}"
echo "========================================"

echo "📝 Archivo application.properties (Invoice Service):"
docker exec local-invoice-service cat /app/BOOT-INF/classes/application.properties 2>/dev/null | grep -E "(jwt|issuer|azure|b2c)" || echo "   No se puede acceder a la configuración interna"

echo ""
echo "📝 Archivo application.properties (File Service):"
docker exec local-file-service cat /app/BOOT-INF/classes/application.properties 2>/dev/null | grep -E "(jwt|issuer|azure|b2c)" || echo "   No se puede acceder a la configuración interna"

echo ""
echo -e "${BLUE}6. RECOMENDACIONES PARA PRUEBAS COMPLETAS${NC}"
echo "========================================"

echo -e "${YELLOW}Para probar la autenticación completa necesitas:${NC}"
echo ""
echo "🔑 1. Obtener un JWT token válido de Azure B2C:"
echo "   - Ir a: https://$AZURE_B2C_TENANT/$AZURE_B2C_TENANT.onmicrosoft.com/$AZURE_B2C_POLICY/oauth2/v2.0/authorize?client_id=$JWT_AUDIENCE&response_type=id_token&redirect_uri=YOUR_REDIRECT&scope=openid&nonce=123"
echo ""
echo "🧪 2. Usar el token para probar endpoints:"
echo "   curl -H \"Authorization: Bearer YOUR_JWT_TOKEN\" $INVOICE_SERVICE/api/invoices"
echo ""
echo "🌐 3. Probar a través de API Gateway (en EC2):"
echo "   - URL de tu API Gateway configurada"
echo "   - Verificar que el routing funcione correctamente"
echo ""
echo "📁 4. Probar upload de archivos:"
echo "   curl -X POST $INVOICE_SERVICE/api/invoices/test-client \\"
echo "        -H \"Authorization: Bearer YOUR_JWT_TOKEN\" \\"
echo "        -F \"file=@test-file.txt\" \\"
echo "        -F \"date=2025-07-04\""

echo ""
echo -e "${GREEN}✅ Verificación de configuración completada${NC}"
echo -e "${YELLOW}⚠️  Para pruebas completas de autenticación, necesitas tokens JWT válidos${NC}"
