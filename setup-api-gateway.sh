#!/bin/bash
set -euo pipefail

# AWS API Gateway Configuration Script
# This script configures API Gateway to proxy requests to the microservices

# Configuration variables
API_NAME="invoice-management-api"
REGION="${AWS_REGION:-us-east-1}"
EC2_INSTANCE_IP="${EC2_HOST:-52.4.100.50}"
INVOICE_SERVICE_PORT="8080"
FILE_SERVICE_PORT="8081"

# Azure AD B2C Configuration
AZURE_TENANT_ID="28dbf599-4a0c-47c3-be6a-0790f3c7f43b"
AZURE_CLIENT_ID="eafae8e9-4496-4f00-a278-4ff30c03272c"
AZURE_ISSUER="https://duoccloudnatives6.b2clogin.com/28dbf599-4a0c-47c3-be6a-0790f3c7f43b/v2.0/"
AZURE_JWKS_URI="https://duoccloudnatives6.b2clogin.com/duoccloudnatives6.onmicrosoft.com/discovery/v2.0/keys?p=B2C_1_AppS3"

echo "🚀 Configurando AWS API Gateway para Invoice Management..."

# Create the REST API
echo "📋 Creando REST API..."
API_ID=$(aws apigateway create-rest-api \
    --name "$API_NAME" \
    --description "API Gateway for Invoice Management Microservices" \
    --endpoint-configuration types=REGIONAL \
    --region "$REGION" \
    --query 'id' \
    --output text)

echo "✅ API creada con ID: $API_ID"

# Get the root resource ID
ROOT_RESOURCE_ID=$(aws apigateway get-resources \
    --rest-api-id "$API_ID" \
    --region "$REGION" \
    --query 'items[?path==`/`].id' \
    --output text)

echo "📁 Root Resource ID: $ROOT_RESOURCE_ID"

# Create /api resource
API_RESOURCE_ID=$(aws apigateway create-resource \
    --rest-api-id "$API_ID" \
    --parent-id "$ROOT_RESOURCE_ID" \
    --path-part "api" \
    --region "$REGION" \
    --query 'id' \
    --output text)

echo "📁 /api Resource ID: $API_RESOURCE_ID"

# Create /api/invoices resource
INVOICES_RESOURCE_ID=$(aws apigateway create-resource \
    --rest-api-id "$API_ID" \
    --parent-id "$API_RESOURCE_ID" \
    --path-part "invoices" \
    --region "$REGION" \
    --query 'id' \
    --output text)

echo "📁 /api/invoices Resource ID: $INVOICES_RESOURCE_ID"

# Create /api/invoices/{proxy+} resource for catch-all
INVOICES_PROXY_RESOURCE_ID=$(aws apigateway create-resource \
    --rest-api-id "$API_ID" \
    --parent-id "$INVOICES_RESOURCE_ID" \
    --path-part "{proxy+}" \
    --region "$REGION" \
    --query 'id' \
    --output text)

echo "📁 /api/invoices/{proxy+} Resource ID: $INVOICES_PROXY_RESOURCE_ID"

# Create /files resource
FILES_RESOURCE_ID=$(aws apigateway create-resource \
    --rest-api-id "$API_ID" \
    --parent-id "$ROOT_RESOURCE_ID" \
    --path-part "files" \
    --region "$REGION" \
    --query 'id' \
    --output text)

echo "📁 /files Resource ID: $FILES_RESOURCE_ID"

# Create /files/{proxy+} resource for catch-all
FILES_PROXY_RESOURCE_ID=$(aws apigateway create-resource \
    --rest-api-id "$API_ID" \
    --parent-id "$FILES_RESOURCE_ID" \
    --path-part "{proxy+}" \
    --region "$REGION" \
    --query 'id' \
    --output text)

echo "📁 /files/{proxy+} Resource ID: $FILES_PROXY_RESOURCE_ID"

# Function to create authorizer for Azure AD
create_jwt_authorizer() {
    local api_id="$1"
    
    echo "🔐 Creando JWT Authorizer para Azure AD..."
    
    aws apigateway create-authorizer \
        --rest-api-id "$api_id" \
        --name "AzureB2CAuthorizer" \
        --type "JWT" \
        --jwt-configuration audience="$AZURE_CLIENT_ID",issuer="$AZURE_ISSUER" \
        --identity-source '$request.header.Authorization' \
        --region "$REGION" \
        --query 'id' \
        --output text
}

# Create JWT Authorizer
echo "🔐 Creando JWT Authorizer para Azure AD..."
AUTHORIZER_ID=$(create_jwt_authorizer "$API_ID")
if [ -n "$AUTHORIZER_ID" ]; then
    echo "✅ Authorizer creado con ID: $AUTHORIZER_ID"
else
    echo "⚠️  Authorizer no creado, configurar manualmente"
    AUTHORIZER_ID=""
fi

# Function to create method with integration
create_method_with_integration() {
    local api_id="$1"
    local resource_id="$2"
    local http_method="$3"
    local backend_url="$4"
    local auth_type="${5:-NONE}"
    local authorizer_id="${6:-}"
    
    # Create method
    if [ "$auth_type" = "CUSTOM" ] && [ -n "$authorizer_id" ]; then
        aws apigateway put-method \
            --rest-api-id "$api_id" \
            --resource-id "$resource_id" \
            --http-method "$http_method" \
            --authorization-type "$auth_type" \
            --authorizer-id "$authorizer_id" \
            --region "$REGION"
    else
        aws apigateway put-method \
            --rest-api-id "$api_id" \
            --resource-id "$resource_id" \
            --http-method "$http_method" \
            --authorization-type "NONE" \
            --region "$REGION"
    fi
    
    # Create integration
    aws apigateway put-integration \
        --rest-api-id "$api_id" \
        --resource-id "$resource_id" \
        --http-method "$http_method" \
        --type "HTTP_PROXY" \
        --integration-http-method "$http_method" \
        --uri "$backend_url" \
        --region "$REGION"
}

# Create methods for invoice service
echo "🔗 Configurando métodos para invoice-service..."

METHODS=("GET" "POST" "PUT" "DELETE" "OPTIONS")
INVOICE_BACKEND_URL="http://${EC2_INSTANCE_IP}:${INVOICE_SERVICE_PORT}/api/invoices/{proxy}"

for method in "${METHODS[@]}"; do
    echo "Creating $method method for /api/invoices/{proxy+}"
    create_method_with_integration "$API_ID" "$INVOICES_PROXY_RESOURCE_ID" "$method" "$INVOICE_BACKEND_URL" "CUSTOM" "$AUTHORIZER_ID"
done

# Create methods for file service
echo "🔗 Configurando métodos para file-service..."

FILE_BACKEND_URL="http://${EC2_INSTANCE_IP}:${FILE_SERVICE_PORT}/files/{proxy}"

for method in "${METHODS[@]}"; do
    echo "Creating $method method for /files/{proxy+}"
    create_method_with_integration "$API_ID" "$FILES_PROXY_RESOURCE_ID" "$method" "$FILE_BACKEND_URL" "CUSTOM" "$AUTHORIZER_ID"
done

# Deploy the API
echo "🚀 Desplegando API..."
DEPLOYMENT_ID=$(aws apigateway create-deployment \
    --rest-api-id "$API_ID" \
    --stage-name "prod" \
    --stage-description "Production stage" \
    --description "Initial deployment" \
    --region "$REGION" \
    --query 'id' \
    --output text)

echo "✅ Deployment ID: $DEPLOYMENT_ID"

# Get the invoke URL
INVOKE_URL="https://${API_ID}.execute-api.${REGION}.amazonaws.com/prod"

echo ""
echo "🎉 ¡Configuración de API Gateway completada!"
echo "📡 API Gateway URL: $INVOKE_URL"
echo "🔗 Invoice Service: $INVOKE_URL/api/invoices/"
echo "🔗 File Service: $INVOKE_URL/files/"
echo ""
echo "📋 Configuración guardada:"
echo "  - API ID: $API_ID"
echo "  - Region: $REGION"
echo "  - Stage: prod"
echo "  - Authorizer ID: ${AUTHORIZER_ID:-Not configured}"
echo ""
echo "🔧 Variables de entorno para aplicaciones:"
echo "export AWS_API_GATEWAY_URL='$INVOKE_URL'"
echo "export AWS_API_GATEWAY_API_ID='$API_ID'"
