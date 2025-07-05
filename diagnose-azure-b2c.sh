#!/bin/bash

echo "🔧 Diagnóstico y Corrección de Azure B2C"
echo "========================================"

# Configuración actual
TENANT="duoccloudnatives6"
POLICY="B2C_1_AppS3"
CLIENT_ID="054924b5-14ae-4ede-9d8b-a1a71a1e723f"

echo "📋 Configuración actual detectada:"
echo "   Tenant: $TENANT"
echo "   Policy: $POLICY" 
echo "   Client ID: $CLIENT_ID"

echo ""
echo "🔍 Probando diferentes URLs de Azure B2C..."

# URLs to test
URLS=(
    "https://$TENANT.b2clogin.com/$TENANT.onmicrosoft.com/v2.0/.well-known/openid_configuration?p=$POLICY"
    "https://$TENANT.b2clogin.com/$TENANT.onmicrosoft.com/$POLICY/v2.0/.well-known/openid_configuration"
    "https://login.microsoftonline.com/$TENANT.onmicrosoft.com/v2.0/.well-known/openid_configuration"
    "https://$TENANT.b2clogin.com/$TENANT.onmicrosoft.com/.well-known/openid_configuration?p=$POLICY"
)

for url in "${URLS[@]}"; do
    echo ""
    echo "   Testing: $url"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$url")
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "   ✅ WORKING - HTTP $HTTP_CODE"
        echo "   📋 Checking JSON structure..."
        
        RESPONSE=$(curl -s "$url")
        if echo "$RESPONSE" | jq . > /dev/null 2>&1; then
            echo "   ✅ Valid JSON response"
            
            JWK_URI=$(echo "$RESPONSE" | jq -r '.jwks_uri // empty')
            ISSUER=$(echo "$RESPONSE" | jq -r '.issuer // empty')
            
            if [ "$JWK_URI" != "" ]; then
                echo "   🔑 JWK URI: $JWK_URI"
                
                # Test JWK endpoint
                JWK_HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$JWK_URI")
                if [ "$JWK_HTTP_CODE" = "200" ]; then
                    echo "   ✅ JWK endpoint accessible"
                    KEY_COUNT=$(curl -s "$JWK_URI" | jq '.keys | length')
                    echo "   📊 Available keys: $KEY_COUNT"
                    
                    echo ""
                    echo "🎯 CONFIGURACIÓN CORRECTA ENCONTRADA:"
                    echo "   Issuer URI: $url"
                    echo "   JWK Set URI: $JWK_URI"
                    echo "   Issuer: $ISSUER"
                    
                    # Generate corrected configuration
                    echo ""
                    echo "📝 CONFIGURACIÓN CORREGIDA PARA application.properties:"
                    echo "spring.security.oauth2.resourceserver.jwt.issuer-uri=$ISSUER"
                    echo ""
                    echo "📝 CONFIGURACIÓN CORREGIDA PARA SecurityConfig.java:"
                    echo "NimbusJwtDecoder.withJwkSetUri(\"$JWK_URI\")"
                    
                    exit 0
                else
                    echo "   ❌ JWK endpoint not accessible (HTTP: $JWK_HTTP_CODE)"
                fi
            fi
        else
            echo "   ❌ Invalid JSON response"
        fi
    else
        echo "   ❌ HTTP $HTTP_CODE"
    fi
done

echo ""
echo "❌ No se encontró una configuración válida de Azure B2C"
echo ""
echo "🛠️  SOLUCIONES POSIBLES:"
echo "1. Verificar que el tenant '$TENANT' existe y está activo"
echo "2. Verificar que la policy '$POLICY' está configurada correctamente"
echo "3. Verificar que el Client ID '$CLIENT_ID' es correcto"
echo ""
echo "🔧 PASOS PARA VERIFICAR EN AZURE PORTAL:"
echo "1. Ir a Azure Portal > Azure AD B2C"
echo "2. Verificar User flows (policies)"
echo "3. Verificar App registrations"
echo "4. Obtener la configuración correcta de endpoints"
echo ""
echo "📚 URLs de referencia para testing manual:"
echo "- https://$TENANT.b2clogin.com/$TENANT.onmicrosoft.com/$POLICY/oauth2/v2.0/authorize"
echo "- https://login.microsoftonline.com/$TENANT.onmicrosoft.com/oauth2/v2.0/authorize"

echo ""
echo "🔄 Mientras tanto, puedes usar estos endpoints para pruebas locales SIN autenticación:"
echo "- H2 Console: http://localhost:8080/h2-console"
echo "- Health Check: http://localhost:8081/actuator/health"
