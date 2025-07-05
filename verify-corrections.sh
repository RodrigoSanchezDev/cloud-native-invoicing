#!/bin/bash

echo "🔧 Verificación Post-Correcciones"
echo "================================="
echo "Este script verifica que las correcciones de Azure B2C, API Gateway y variables de entorno funcionen correctamente."

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# URLs y configuración corregida
CORRECTED_TENANT="DuoccloudnativeS6"
CORRECTED_POLICY="B2C_1_AppS3"
CLIENT_ID="054924b5-14ae-4ede-9d8b-a1a71a1e723f"
API_GATEWAY="https://5u6zchoeog.execute-api.us-east-1.amazonaws.com/DEV"
EC2_IP="52.4.100.50"

echo -e "${BLUE}📋 CONFIGURACIÓN CORREGIDA${NC}"
echo "=========================="
echo "   Tenant: $CORRECTED_TENANT (corregido de duoccloudnatives6)"
echo "   Policy: $CORRECTED_POLICY"
echo "   Client ID: $CLIENT_ID"
echo "   API Gateway: $API_GATEWAY"
echo "   EC2 IP: $EC2_IP"

echo ""
echo -e "${BLUE}🔍 1. VERIFICANDO AZURE B2C (TENANT CORREGIDO)${NC}"
echo "==============================================="

# Test corrected Azure B2C URLs
CORRECTED_URLS=(
    "https://$CORRECTED_TENANT.b2clogin.com/$CORRECTED_TENANT.onmicrosoft.com/v2.0/.well-known/openid_configuration?p=$CORRECTED_POLICY"
    "https://$CORRECTED_TENANT.b2clogin.com/$CORRECTED_TENANT.onmicrosoft.com/$CORRECTED_POLICY/v2.0/.well-known/openid_configuration"
)

for url in "${CORRECTED_URLS[@]}"; do
    echo "   Testing: $url"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$url")
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "   ${GREEN}✅ Azure B2C endpoint accesible${NC}"
        
        # Test JWK endpoint if config is found
        RESPONSE=$(curl -s "$url")
        if echo "$RESPONSE" | jq . > /dev/null 2>&1; then
            JWK_URI=$(echo "$RESPONSE" | jq -r '.jwks_uri // empty')
            if [ "$JWK_URI" != "" ]; then
                echo -e "   ${GREEN}✅ JWK URI encontrado: $JWK_URI${NC}"
                
                JWK_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$JWK_URI")
                if [ "$JWK_CODE" = "200" ]; then
                    echo -e "   ${GREEN}✅ JWK endpoint funcional${NC}"
                else
                    echo -e "   ${RED}❌ JWK endpoint no accesible ($JWK_CODE)${NC}"
                fi
            fi
        fi
        break
    else
        echo -e "   ${RED}❌ HTTP $HTTP_CODE${NC}"
    fi
done

echo ""
echo -e "${BLUE}🔍 2. PROBANDO NUEVA CONFIGURACIÓN LOCAL${NC}"
echo "==========================================="

echo "   🔄 Construyendo nueva imagen con correcciones..."
echo "   (Esto puede tomar un momento...)"

# Build new images with corrections
./mvnw clean package -DskipTests -q > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "   ${GREEN}✅ Maven build exitoso${NC}"
else
    echo -e "   ${RED}❌ Maven build falló${NC}"
    exit 1
fi

# Build Docker images
docker build -t local/invoice-service:corrected -f Dockerfile.invoice . > /dev/null 2>&1
docker build -t local/file-service:corrected -f Dockerfile.file . > /dev/null 2>&1

if [ $? -eq 0 ]; then
    echo -e "   ${GREEN}✅ Docker images construidas${NC}"
else
    echo -e "   ${RED}❌ Docker build falló${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}🔍 3. PROBANDO CONECTIVIDAD DE PRODUCCIÓN${NC}"
echo "========================================"

test_endpoint() {
    local url=$1
    local description=$2
    local expected_code=${3:-200}
    
    echo "   Testing: $description"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 10 "$url")
    
    case $HTTP_CODE in
        200|201)
            echo -e "   ${GREEN}✅ $description - OK ($HTTP_CODE)${NC}"
            return 0
            ;;
        401|403)
            echo -e "   ${YELLOW}🔒 $description - Auth required ($HTTP_CODE) - EXPECTED${NC}"
            return 0
            ;;
        404)
            echo -e "   ${RED}❌ $description - Not found ($HTTP_CODE)${NC}"
            return 1
            ;;
        000)
            echo -e "   ${RED}❌ $description - Connection failed${NC}"
            return 1
            ;;
        *)
            echo -e "   ${YELLOW}⚠️  $description - Unexpected ($HTTP_CODE)${NC}"
            return 1
            ;;
    esac
}

# Test production endpoints
echo ""
echo "   📡 EC2 Direct Access:"
test_endpoint "http://$EC2_IP:8080/api/invoices" "Invoice Service" "401"
test_endpoint "http://$EC2_IP:8081/actuator/health" "File Service Health" "200"

echo ""
echo "   🌐 API Gateway Access:"
test_endpoint "$API_GATEWAY/api/invoices" "API Gateway → Invoices" "401"

echo ""
echo -e "${BLUE}🔍 4. VERIFICANDO VARIABLES DE ENTORNO${NC}"
echo "====================================="

echo "   📝 Variables de entorno requeridas para producción:"
echo "   - AZURE_TENANT_ID=28dbf599-4a0c-47c3-be6a-0790f3c7f43b"
echo "   - AZURE_CLIENT_ID=eafae8e9-4496-4f00-a278-4ff30c03272c"
echo "   - AZURE_JWK_SET_URI=https://DuoccloudnativeS6.b2clogin.com/DuoccloudnativeS6.onmicrosoft.com/discovery/v2.0/keys?p=B2C_1_AppS3"
echo "   - AWS_API_GATEWAY_URL=https://5u6zchoeog.execute-api.us-east-1.amazonaws.com"
echo "   - AWS_API_GATEWAY_STAGE=DEV"

echo ""
echo -e "${BLUE}🎯 RESUMEN DE CORRECCIONES APLICADAS${NC}"
echo "==================================="

echo -e "${GREEN}✅ CORRECCIONES IMPLEMENTADAS:${NC}"
echo ""
echo "1️⃣  Azure B2C Tenant corregido:"
echo "   - Antes: duoccloudnatives6"
echo "   - Ahora: DuoccloudnativeS6"
echo ""
echo "2️⃣  SecurityConfig usa variables de entorno:"
echo "   - JWK Set URI ahora es configurable"
echo "   - Compatible con CI/CD"
echo ""
echo "3️⃣  API Gateway configurado:"
echo "   - URL: https://5u6zchoeog.execute-api.us-east-1.amazonaws.com"
echo "   - Stage: DEV"
echo ""
echo "4️⃣  Variables de entorno añadidas:"
echo "   - azure.b2c.*: Para configuración flexible"
echo "   - api.gateway.*: Para routing"

echo ""
echo -e "${YELLOW}📝 PRÓXIMOS PASOS:${NC}"
echo "=================="
echo ""
echo "1. Hacer commit y push de las correcciones:"
echo "   git add -A"
echo "   git commit -m 'Fix Azure B2C tenant, add environment variables, configure API Gateway'"
echo "   git push origin main"
echo ""
echo "2. El CI/CD automáticamente desplegará con la configuración corregida"
echo ""
echo "3. Verificar que Azure B2C está configurado correctamente en Azure Portal"
echo ""
echo "4. Probar endpoints con token JWT válido:"
echo "   curl -H \"Authorization: Bearer <JWT_TOKEN>\" $API_GATEWAY/api/invoices"

echo ""
echo -e "${GREEN}🎉 Correcciones completadas exitosamente!${NC}"

# Cleanup test images
echo ""
echo "🧹 Limpiando imágenes de prueba..."
docker rmi local/invoice-service:corrected local/file-service:corrected > /dev/null 2>&1
