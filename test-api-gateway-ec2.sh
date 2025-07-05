#!/bin/bash

echo "🌐 Pruebas de API Gateway y EC2"
echo "==============================="

# Configuración detectada del CI/CD
API_GATEWAY_URL="https://5u6zchoeog.execute-api.us-east-1.amazonaws.com"
API_GATEWAY_STAGE="DEV"
EC2_IP="52.4.100.50"

echo "📋 Configuración detectada:"
echo "   API Gateway: $API_GATEWAY_URL"
echo "   Stage: $API_GATEWAY_STAGE"
echo "   EC2 IP: $EC2_IP"

echo ""
echo "🔍 1. PROBANDO CONECTIVIDAD DIRECTA A EC2"
echo "========================================="

test_endpoint() {
    local url=$1
    local description=$2
    local expected_code=${3:-200}
    
    echo "   Testing: $url"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 10 "$url")
    
    if [ "$HTTP_CODE" = "$expected_code" ]; then
        echo "   ✅ $description - OK ($HTTP_CODE)"
        return 0
    elif [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
        echo "   🔒 $description - Auth required ($HTTP_CODE) - EXPECTED"
        return 0
    elif [ "$HTTP_CODE" = "000" ]; then
        echo "   ❌ $description - Connection failed (timeout/unreachable)"
        return 1
    else
        echo "   ⚠️  $description - Unexpected ($HTTP_CODE)"
        return 1
    fi
}

# Test direct EC2 connections
test_endpoint "http://$EC2_IP:8080/api/invoices" "Direct EC2 Invoice Service" "401"
test_endpoint "http://$EC2_IP:8081/actuator/health" "Direct EC2 File Service Health" "200"

echo ""
echo "🔍 2. PROBANDO API GATEWAY"
echo "=========================="

# Test API Gateway endpoints
API_BASE="$API_GATEWAY_URL/$API_GATEWAY_STAGE"

echo "   Base URL: $API_BASE"

# Common API Gateway paths
GATEWAY_ENDPOINTS=(
    "$API_BASE/invoices"
    "$API_BASE/api/invoices"
    "$API_BASE/health"
    "$API_BASE/actuator/health"
    "$API_BASE/"
)

for endpoint in "${GATEWAY_ENDPOINTS[@]}"; do
    test_endpoint "$endpoint" "API Gateway $(basename $endpoint)" "401"
done

echo ""
echo "🔍 3. VERIFICANDO CONFIGURACIÓN AZURE B2C"
echo "========================================"

# Test the CI/CD configured Azure tenant
CICD_TENANT="DuoccloudnativeS6"
CICD_POLICY="B2C_1_AppS3"

echo "   Tenant del CI/CD: $CICD_TENANT"
echo "   Policy: $CICD_POLICY"

CICD_URLS=(
    "https://$CICD_TENANT.b2clogin.com/$CICD_TENANT.onmicrosoft.com/v2.0/.well-known/openid_configuration?p=$CICD_POLICY"
    "https://$CICD_TENANT.b2clogin.com/$CICD_TENANT.onmicrosoft.com/$CICD_POLICY/v2.0/.well-known/openid_configuration"
)

for url in "${CICD_URLS[@]}"; do
    echo ""
    echo "   Testing: $url"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$url")
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "   ✅ WORKING - HTTP $HTTP_CODE"
        
        RESPONSE=$(curl -s "$url")
        if echo "$RESPONSE" | jq . > /dev/null 2>&1; then
            JWK_URI=$(echo "$RESPONSE" | jq -r '.jwks_uri // empty')
            ISSUER=$(echo "$RESPONSE" | jq -r '.issuer // empty')
            
            echo "   🔑 JWK URI: $JWK_URI"
            echo "   📋 Issuer: $ISSUER"
            
            echo ""
            echo "   🎯 CONFIGURACIÓN CORRECTA ENCONTRADA:"
            echo "   - El tenant en CI/CD ($CICD_TENANT) es diferente al del código (duoccloudnatives6)"
            echo "   - JWK URI funcional: $JWK_URI"
            
        fi
        break
    else
        echo "   ❌ HTTP $HTTP_CODE"
    fi
done

echo ""
echo "🔍 4. RESUMEN DE INCONSISTENCIAS"
echo "==============================="

echo "❌ PROBLEMAS DETECTADOS:"
echo "1. Tenant name mismatch:"
echo "   - En código: duoccloudnatives6"
echo "   - En CI/CD: DuoccloudnativeS6"
echo ""
echo "2. JWK URI hardcodeado en SecurityConfig vs variable de entorno en CI/CD"
echo ""
echo "3. Configuración de API Gateway no utilizada en el código"

echo ""
echo "🛠️  SOLUCIONES RECOMENDADAS:"
echo "============================"

echo ""
echo "1️⃣  CORREGIR TENANT NAME:"
echo "   Actualizar application.properties y SecurityConfig para usar: $CICD_TENANT"

echo ""
echo "2️⃣  USAR VARIABLES DE ENTORNO:"
echo "   Modificar SecurityConfig para usar \${azure.jwk.uri} en lugar de URL hardcodeada"

echo ""
echo "3️⃣  CONFIGURAR API GATEWAY EN EL CÓDIGO:"
echo "   Agregar configuración para usar el API Gateway: $API_GATEWAY_URL"

echo ""
echo "📝 COMANDOS PARA PRUEBAS MANUALES:"
echo "=================================="
echo ""
echo "# Probar EC2 directamente:"
echo "curl http://$EC2_IP:8080/api/invoices"
echo "curl http://$EC2_IP:8081/actuator/health"
echo ""
echo "# Probar API Gateway:"
echo "curl $API_BASE/api/invoices"
echo "curl $API_BASE/health"
echo ""
echo "# Obtener token de Azure B2C (si está configurado correctamente):"
echo "curl 'https://$CICD_TENANT.b2clogin.com/$CICD_TENANT.onmicrosoft.com/$CICD_POLICY/oauth2/v2.0/authorize?client_id=YOUR_CLIENT_ID&response_type=id_token&redirect_uri=YOUR_REDIRECT&scope=openid&nonce=123'"
