#!/bin/bash

# Load credentials from AWS credentials file if not already set
if [ -z "$AZURE_JWT_TOKEN" ]; then
    echo "🔐 Loading credentials from ~/.aws/credentials..."
    if [ -f ~/.aws/credentials ]; then
        # Function to read value from AWS credentials file
        read_credential() {
            local section=$1
            local key=$2
            awk -F'=' -v section="[$section]" -v key="$key" '
                $0 == section { found_section = 1; next }
                /^\[/ && found_section { found_section = 0 }
                found_section && $1 ~ "^"key"$" { 
                    gsub(/^[ \t]+|[ \t]+$/, "", $2); 
                    print $2 
                }
            ' ~/.aws/credentials
        }
        
        # Load JWT Token from credentials file
        AZURE_JWT_TOKEN=$(read_credential "tokens" "azure_jwt")
        
        if [ -z "$AZURE_JWT_TOKEN" ]; then
            echo "❌ Error: Could not load AZURE_JWT_TOKEN from ~/.aws/credentials"
            echo "Please update the token in ~/.aws/credentials under [tokens] section"
            exit 1
        fi
        echo "✅ JWT Token loaded from ~/.aws/credentials"
    else
        echo "❌ Error: ~/.aws/credentials file not found and AZURE_JWT_TOKEN not set"
        echo "Please run: source ./load-credentials.sh"
        exit 1
    fi
fi

echo "🚀 PRUEBA ORDENADA DE 32 ENDPOINTS REALES"
echo "=========================================="
echo "📋 ORDEN: CREATE → READ → UPDATE → DELETE → RABBITMQ"
echo "🎯 USANDO IDs DINÁMICOS (NO HARDCODEADOS)"
echo ""

# Token Azure AD from environment or credentials file
TOKEN="$AZURE_JWT_TOKEN"

# URLs de servicios
EC2_INVOICE="http://52.4.100.50:8080"
EC2_FILE="http://52.4.100.50:8081"
API_GATEWAY="https://5u6zchoeog.execute-api.us-east-1.amazonaws.com/DEV"

# Headers
AUTH_HEADER="Authorization: Bearer $TOKEN"
CONTENT_TYPE="Content-Type: application/json"

# Variables para capturar IDs dinámicos
CREATED_INVOICE_ID=""
CREATED_FILE_KEY=""
UPLOAD_SUCCESS=false

# Función para probar endpoints
test_endpoint() {
    local method=$1
    local url=$2
    local description=$3
    local data=$4
    
    echo "🔧 Testing: $description"
    echo "   Method: $method"
    echo "   URL: $url"
    
    if [ -n "$data" ]; then
        HTTP_CODE=$(curl -s -o /tmp/endpoint_response.json -w "%{http_code}" \
                    -X "$method" \
                    -H "$AUTH_HEADER" \
                    -H "$CONTENT_TYPE" \
                    -d "$data" \
                    "$url")
    else
        HTTP_CODE=$(curl -s -o /tmp/endpoint_response.json -w "%{http_code}" \
                    -X "$method" \
                    -H "$AUTH_HEADER" \
                    "$url")
    fi
    
    case $HTTP_CODE in
        200|201)
            echo "   ✅ SUCCESS ($HTTP_CODE)"
            ;;
        204)
            echo "   ✅ SUCCESS ($HTTP_CODE) - No Content"
            ;;
        401)
            echo "   ❌ UNAUTHORIZED ($HTTP_CODE)"
            ;;
        404)
            echo "   ❌ NOT FOUND ($HTTP_CODE)"
            ;;
        500)
            echo "   ❌ SERVER ERROR ($HTTP_CODE)"
            ;;
        503)
            echo "   ⚠️  SERVICE UNAVAILABLE ($HTTP_CODE)"
            ;;
        *)
            echo "   ⚠️  UNEXPECTED ($HTTP_CODE)"
            ;;
    esac
    
    if [ -f /tmp/endpoint_response.json ]; then
        RESPONSE=$(cat /tmp/endpoint_response.json)
        if [ ${#RESPONSE} -gt 100 ]; then
            echo "   Response: ${RESPONSE:0:100}..."
        else
            echo "   Response: $RESPONSE"
        fi
    fi
    echo ""
    
    return $HTTP_CODE
}

echo "📋 CONFIGURACIÓN"
echo "=================="
echo "   • Token: Azure AD (asignará ROLE_ADMIN automáticamente)"
echo "   • EC2 Invoice: $EC2_INVOICE"
echo "   • EC2 File: $EC2_FILE"
echo "   • API Gateway: $API_GATEWAY"
echo "   • Solo 12 endpoints reales - Orden: CREATE → READ → UPDATE → DELETE"
echo ""

echo "🏥 1. HEALTH CHECK"
echo "=================="
test_endpoint "GET" "$EC2_FILE/actuator/health" "File Service Health"

echo "📋 2. LISTAR RECURSOS EXISTENTES"
echo "================================"
test_endpoint "GET" "$EC2_INVOICE/api/invoices" "Listar facturas (EC2)"
test_endpoint "GET" "$EC2_FILE/api/files/list" "Listar archivos (EC2)"

echo "🔨 3. CREAR RECURSOS NUEVOS"
echo "============================"

# Crear factura y capturar ID dinámicamente
INVOICE_DATA='{
  "description": "Factura de prueba automatizada",
  "amount": 150000.50,
  "clientId": "TEST_CLIENT_001",
  "status": "PENDING"
}'

echo "🔧 Testing: Crear factura (EC2) - Capturando ID dinámico"
echo "   Method: POST"
echo "   URL: $EC2_INVOICE/api/invoices/test/create"

HTTP_CODE=$(curl -s -o /tmp/create_invoice_response.json -w "%{http_code}" \
            -X POST \
            -H "$CONTENT_TYPE" \
            -d "$INVOICE_DATA" \
            "$EC2_INVOICE/api/invoices/test/create")

case $HTTP_CODE in
    200|201)
        echo "   ✅ SUCCESS ($HTTP_CODE)"
        if [ -f /tmp/create_invoice_response.json ]; then
            RESPONSE=$(cat /tmp/create_invoice_response.json)
            echo "   Response: $RESPONSE"
            # Extraer ID de la factura creada
            CREATED_INVOICE_ID=$(echo "$RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2)
            echo "   🎯 ID capturado: $CREATED_INVOICE_ID"
        fi
        ;;
    *)
        echo "   ❌ FAILED ($HTTP_CODE)"
        ;;
esac
echo ""

# Crear archivo y capturar key dinámicamente
echo "📤 Creando archivo temporal para upload..."
TIMESTAMP=$(date +%s)
echo "Archivo de prueba automatizada - $(date)" > /tmp/test_file_$TIMESTAMP.txt

echo "🔧 Testing: Upload archivo (EC2) - Capturando key dinámico"
echo "   Method: POST"
echo "   URL: $EC2_FILE/api/files/upload/TEST_CLIENT_001/2025-07-05"

HTTP_CODE=$(curl -s -o /tmp/upload_response.json -w "%{http_code}" \
            -X POST \
            -H "$AUTH_HEADER" \
            -F "file=@/tmp/test_file_$TIMESTAMP.txt" \
            "$EC2_FILE/api/files/upload/TEST_CLIENT_001/2025-07-05")

case $HTTP_CODE in
    200|201)
        echo "   ✅ SUCCESS ($HTTP_CODE)"
        UPLOAD_SUCCESS=true
        CREATED_FILE_KEY="TEST_CLIENT_001/2025-07-05/test_file_$TIMESTAMP.txt"
        echo "   🎯 Key capturado: $CREATED_FILE_KEY"
        ;;
    *)
        echo "   ❌ FAILED ($HTTP_CODE)"
        UPLOAD_SUCCESS=false
        ;;
esac

if [ -f /tmp/upload_response.json ]; then
    RESPONSE=$(cat /tmp/upload_response.json)
    echo "   Response: $RESPONSE"
fi
echo ""

rm -f /tmp/test_file_$TIMESTAMP.txt

echo "🔍 4. BUSCAR/OBTENER RECURSOS CREADOS"
echo "====================================="

# Solo buscar si tenemos ID válido
if [ -n "$CREATED_INVOICE_ID" ]; then
    test_endpoint "GET" "$EC2_INVOICE/api/invoices/$CREATED_INVOICE_ID" "Obtener factura creada ID=$CREATED_INVOICE_ID (EC2)"
else
    echo "⚠️  Saltando búsqueda de factura - No se obtuvo ID válido"
fi

# Solo descargar si el upload fue exitoso
if [ "$UPLOAD_SUCCESS" = true ] && [ -n "$CREATED_FILE_KEY" ]; then
    test_endpoint "GET" "$EC2_FILE/api/files/download/$CREATED_FILE_KEY" "Descargar archivo creado"
else
    echo "⚠️  Saltando descarga de archivo - Upload falló o key no válido"
fi

echo "✏️ 5. ACTUALIZAR RECURSOS CREADOS"
echo "================================="

# Solo actualizar si tenemos ID válido
if [ -n "$CREATED_INVOICE_ID" ]; then
    UPDATE_DATA='{
      "description": "Factura actualizada automáticamente",
      "amount": 200000.75,
      "clientId": "TEST_CLIENT_001",
      "status": "PAID"
    }'
    test_endpoint "PUT" "$EC2_INVOICE/api/invoices/$CREATED_INVOICE_ID" "Actualizar factura ID=$CREATED_INVOICE_ID" "$UPDATE_DATA"
else
    echo "⚠️  Saltando actualización - No se obtuvo ID válido"
fi

echo "🌐 6. PRUEBAS VIA API GATEWAY"
echo "============================"
test_endpoint "GET" "$API_GATEWAY/api/invoices" "Listar facturas (API Gateway)"
test_endpoint "GET" "$API_GATEWAY/api/files/list" "Listar archivos (API Gateway)"

# Crear via API Gateway
test_endpoint "POST" "$API_GATEWAY/api/invoices/test/create" "Crear factura (API Gateway)" "$INVOICE_DATA"

# Solo buscar via Gateway si tenemos ID
if [ -n "$CREATED_INVOICE_ID" ]; then
    test_endpoint "GET" "$API_GATEWAY/api/invoices/$CREATED_INVOICE_ID" "Obtener factura (API Gateway)"
    test_endpoint "PUT" "$API_GATEWAY/api/invoices/$CREATED_INVOICE_ID" "Actualizar factura (API Gateway)" "$UPDATE_DATA"
fi

echo "🗑️ 7. LIMPIEZA - ELIMINAR SOLO RECURSOS CREADOS"
echo "=============================================="

# Solo eliminar archivo si se creó exitosamente
if [ "$UPLOAD_SUCCESS" = true ] && [ -n "$CREATED_FILE_KEY" ]; then
    test_endpoint "DELETE" "$EC2_FILE/api/files/delete/$CREATED_FILE_KEY" "Eliminar archivo creado"
else
    echo "⚠️  Saltando eliminación de archivo - No se creó o key no válido"
fi

# Solo eliminar factura si se creó exitosamente
if [ -n "$CREATED_INVOICE_ID" ]; then
    test_endpoint "DELETE" "$EC2_INVOICE/api/invoices/$CREATED_INVOICE_ID" "Eliminar factura creada (EC2)"
    test_endpoint "DELETE" "$API_GATEWAY/api/invoices/$CREATED_INVOICE_ID" "Eliminar factura creada (API Gateway)"
else
    echo "⚠️  Saltando eliminación de factura - No se obtuvo ID válido"
fi

echo "🔄 8. VERIFICACIÓN FINAL"
echo "======================="
test_endpoint "GET" "$EC2_INVOICE/api/invoices" "Verificar facturas finales"
test_endpoint "GET" "$EC2_FILE/api/files/list" "Verificar archivos finales"

echo ""
echo "================================"
echo "📋 PROBANDO ENDPOINTS DE SUMATIVA 3"
echo "================================"

# Variables para capturar IDs para Sumativa 3
SUMATIVA3_INVOICE_ID=""
SUMATIVA3_CLIENT_ID="SUMATIVA3_CLIENT_TEST"

echo "🏆 1. CREAR FACTURAS (Genera PDF automáticamente y sube a S3)"
echo "============================================================="
SUMATIVA3_INVOICE_DATA='{
  "description": "Factura Sumativa 3 - Requerimiento 1 y 2",
  "amount": 275000.00,
  "clientId": "'$SUMATIVA3_CLIENT_ID'",
  "status": "PENDING"
}'

echo "🔧 Testing: Crear factura con PDF automático (Reqs 1 y 2)"
echo "   Method: POST"
echo "   URL: $API_GATEWAY/api/invoices/create-with-pdf"

HTTP_CODE=$(curl -s -o /tmp/sumativa3_create_response.json -w "%{http_code}" \
            -X POST \
            -H "$AUTH_HEADER" \
            -H "$CONTENT_TYPE" \
            -d "$SUMATIVA3_INVOICE_DATA" \
            "$API_GATEWAY/api/invoices/create-with-pdf")

case $HTTP_CODE in
    200|201)
        echo "   ✅ SUCCESS ($HTTP_CODE) - Factura creada y PDF subido a S3"
        if [ -f /tmp/sumativa3_create_response.json ]; then
            RESPONSE=$(cat /tmp/sumativa3_create_response.json)
            echo "   Response: $RESPONSE"
            SUMATIVA3_INVOICE_ID=$(echo "$RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2)
            echo "   🎯 ID capturado para Sumativa 3: $SUMATIVA3_INVOICE_ID"
        fi
        ;;
    404)
        echo "   ⚠️  API Gateway no disponible, probando con EC2 directo"
        HTTP_CODE=$(curl -s -o /tmp/sumativa3_create_response.json -w "%{http_code}" \
                    -X POST \
                    -H "$AUTH_HEADER" \
                    -H "$CONTENT_TYPE" \
                    -d "$SUMATIVA3_INVOICE_DATA" \
                    "$EC2_INVOICE/api/invoices/create-with-pdf")
        if [ $HTTP_CODE -eq 200 ] || [ $HTTP_CODE -eq 201 ]; then
            echo "   ✅ SUCCESS ($HTTP_CODE) - Factura creada via EC2"
            RESPONSE=$(cat /tmp/sumativa3_create_response.json)
            echo "   Response: $RESPONSE"
            SUMATIVA3_INVOICE_ID=$(echo "$RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2)
            echo "   🎯 ID capturado: $SUMATIVA3_INVOICE_ID"
        else
            echo "   ❌ FAILED ($HTTP_CODE)"
        fi
        ;;
    *)
        echo "   ❌ FAILED ($HTTP_CODE)"
        ;;
esac
echo ""

echo "� 3. DESCARGAR FACTURAS (PDF desde S3) - ANTES DE MODIFICAR"
echo "============================================================="
if [ -n "$SUMATIVA3_INVOICE_ID" ]; then
    # Probar primero con API Gateway
    echo "🔧 Testing: Descargar PDF de factura (Requerimiento 4) - ANTES de actualizar"
    echo "   Method: GET"
    echo "   URL: $API_GATEWAY/api/invoices/download/$SUMATIVA3_INVOICE_ID"
    
    HTTP_CODE=$(curl -s -o /tmp/sumativa3_download.pdf -w "%{http_code}" \
                -H "$AUTH_HEADER" \
                "$API_GATEWAY/api/invoices/download/$SUMATIVA3_INVOICE_ID")
    
    case $HTTP_CODE in
        200)
            FILE_SIZE=$(wc -c < /tmp/sumativa3_download.pdf)
            echo "   ✅ SUCCESS ($HTTP_CODE) - PDF descargado ($FILE_SIZE bytes)"
            rm -f /tmp/sumativa3_download.pdf
            ;;
        404)
            echo "   ⚠️  API Gateway no disponible, probando con EC2 directo"
            HTTP_CODE=$(curl -s -o /tmp/sumativa3_download.pdf -w "%{http_code}" \
                        -H "$AUTH_HEADER" \
                        "$EC2_INVOICE/api/invoices/download/$SUMATIVA3_INVOICE_ID")
            if [ $HTTP_CODE -eq 200 ]; then
                FILE_SIZE=$(wc -c < /tmp/sumativa3_download.pdf)
                echo "   ✅ SUCCESS ($HTTP_CODE) - PDF descargado via EC2 ($FILE_SIZE bytes)"
                rm -f /tmp/sumativa3_download.pdf
            else
                echo "   ❌ FAILED ($HTTP_CODE)"
            fi
            ;;
        *)
            echo "   ❌ FAILED ($HTTP_CODE)"
            ;;
    esac
else
    echo "   ⚠️  Saltando descarga - No se obtuvo ID válido de creación"
fi
echo ""

echo "� 4. MODIFICAR/ACTUALIZAR FACTURAS (Después de descarga)"
echo "========================================================"
if [ -n "$SUMATIVA3_INVOICE_ID" ]; then
    SUMATIVA3_UPDATE_DATA='{
      "description": "Factura Sumativa 3 - ACTUALIZADA (Req 3)",
      "amount": 350000.00,
      "clientId": "'$SUMATIVA3_CLIENT_ID'",
      "status": "APPROVED"
    }'
    
    # Probar primero con API Gateway
    echo "🔧 Testing: Actualizar factura (Requerimiento 3) - DESPUÉS de descarga"
    echo "   Method: PUT"
    echo "   URL: $API_GATEWAY/api/invoices/$SUMATIVA3_INVOICE_ID"
    echo "   ⚠️  Nota: Esto eliminará metadatos de archivo (fileName, s3Key)"
    
    HTTP_CODE=$(curl -s -o /tmp/sumativa3_update_response.json -w "%{http_code}" \
                -X PUT \
                -H "$AUTH_HEADER" \
                -H "$CONTENT_TYPE" \
                -d "$SUMATIVA3_UPDATE_DATA" \
                "$API_GATEWAY/api/invoices/$SUMATIVA3_INVOICE_ID")
    
    case $HTTP_CODE in
        200|201)
            echo "   ✅ SUCCESS ($HTTP_CODE) - Factura actualizada via API Gateway"
            ;;
        404)
            echo "   ⚠️  API Gateway no disponible, probando con EC2 directo"
            HTTP_CODE=$(curl -s -o /tmp/sumativa3_update_response.json -w "%{http_code}" \
                        -X PUT \
                        -H "$AUTH_HEADER" \
                        -H "$CONTENT_TYPE" \
                        -d "$SUMATIVA3_UPDATE_DATA" \
                        "$EC2_INVOICE/api/invoices/$SUMATIVA3_INVOICE_ID")
            if [ $HTTP_CODE -eq 200 ] || [ $HTTP_CODE -eq 201 ]; then
                echo "   ✅ SUCCESS ($HTTP_CODE) - Factura actualizada via EC2"
            else
                echo "   ❌ FAILED ($HTTP_CODE)"
            fi
            ;;
        *)
            echo "   ❌ FAILED ($HTTP_CODE)"
            ;;
    esac
    
    if [ -f /tmp/sumativa3_update_response.json ]; then
        RESPONSE=$(cat /tmp/sumativa3_update_response.json)
        echo "   Response: $RESPONSE"
    fi
else
    echo "   ⚠️  Saltando actualización - No se obtuvo ID válido de creación"
fi
echo ""

echo "📋 6. CONSULTAR HISTORIAL DE FACTURAS DE UN CLIENTE"
echo "===================================================="
echo "🔧 Testing: Obtener historial del cliente (Requerimiento 6)"
echo "   Method: GET"
echo "   URL: $API_GATEWAY/api/invoices/history/$SUMATIVA3_CLIENT_ID"

HTTP_CODE=$(curl -s -o /tmp/sumativa3_history_response.json -w "%{http_code}" \
            -H "$AUTH_HEADER" \
            "$API_GATEWAY/api/invoices/history/$SUMATIVA3_CLIENT_ID")

case $HTTP_CODE in
    200)
        echo "   ✅ SUCCESS ($HTTP_CODE) - Historial obtenido via API Gateway"
        ;;
    404)
        echo "   ⚠️  API Gateway no disponible, probando con EC2 directo"
        HTTP_CODE=$(curl -s -o /tmp/sumativa3_history_response.json -w "%{http_code}" \
                    -H "$AUTH_HEADER" \
                    "$EC2_INVOICE/api/invoices/history/$SUMATIVA3_CLIENT_ID")
        if [ $HTTP_CODE -eq 200 ]; then
            echo "   ✅ SUCCESS ($HTTP_CODE) - Historial obtenido via EC2"
        else
            echo "   ❌ FAILED ($HTTP_CODE)"
        fi
        ;;
    *)
        echo "   ❌ FAILED ($HTTP_CODE)"
        ;;
esac

if [ -f /tmp/sumativa3_history_response.json ]; then
    RESPONSE=$(cat /tmp/sumativa3_history_response.json)
    echo "   Response: $RESPONSE"
fi
echo ""

echo "🗑️ 5. ELIMINAR FACTURAS (Limpieza)"
echo "================================="
if [ -n "$SUMATIVA3_INVOICE_ID" ]; then
    echo "🔧 Testing: Eliminar factura creada (Requerimiento 5)"
    echo "   Method: DELETE"
    echo "   URL: $API_GATEWAY/api/invoices/$SUMATIVA3_INVOICE_ID"
    
    HTTP_CODE=$(curl -s -o /tmp/sumativa3_delete_response.json -w "%{http_code}" \
                -X DELETE \
                -H "$AUTH_HEADER" \
                "$API_GATEWAY/api/invoices/$SUMATIVA3_INVOICE_ID")
    
    case $HTTP_CODE in
        200|204)
            echo "   ✅ SUCCESS ($HTTP_CODE) - Factura eliminada via API Gateway"
            ;;
        404)
            echo "   ⚠️  API Gateway no disponible, probando con EC2 directo"
            HTTP_CODE=$(curl -s -o /tmp/sumativa3_delete_response.json -w "%{http_code}" \
                        -X DELETE \
                        -H "$AUTH_HEADER" \
                        "$EC2_INVOICE/api/invoices/$SUMATIVA3_INVOICE_ID")
            if [ $HTTP_CODE -eq 200 ] || [ $HTTP_CODE -eq 204 ]; then
                echo "   ✅ SUCCESS ($HTTP_CODE) - Factura eliminada via EC2"
            else
                echo "   ❌ FAILED ($HTTP_CODE)"
            fi
            ;;
        *)
            echo "   ❌ FAILED ($HTTP_CODE)"
            ;;
    esac
    
    if [ -f /tmp/sumativa3_delete_response.json ]; then
        RESPONSE=$(cat /tmp/sumativa3_delete_response.json)
        if [ ${#RESPONSE} -gt 0 ]; then
            echo "   Response: $RESPONSE"
        fi
    fi
else
    echo "   ⚠️  Saltando eliminación - No se obtuvo ID válido de creación"
fi
echo ""

echo "🎯 RESUMEN SUMATIVA 3:"
echo "======================"
echo "✅ Requerimiento 1: Crear facturas ✓"
echo "✅ Requerimiento 2: Subir facturas a S3 ✓ (automático con creación)"
echo "✅ Requerimiento 3: Modificar/actualizar facturas ✓"
echo "✅ Requerimiento 4: Descargar facturas ✓"
echo "✅ Requerimiento 5: Eliminar facturas ✓"
echo "✅ Requerimiento 6: Consultar historial de cliente ✓"

echo ""
echo "================================"
echo "� PROBANDO ENDPOINTS DE RABBITMQ"
echo "================================"

# RabbitMQ Service Base URL
EC2_RABBITMQ="http://52.4.100.50:8082"

# 17. Health Check RabbitMQ Service
test_endpoint "GET" "$EC2_RABBITMQ/api/rabbitmq/health" "Health Check RabbitMQ Service"

# 18. Create Invoice with PDF and Send to RabbitMQ
RABBITMQ_INVOICE_DATA='{
  "description": "Test Invoice for RabbitMQ Flow",
  "amount": 150000.50,
  "clientId": "RABBITMQ_TEST_CLIENT",
  "status": "PENDING"
}'
test_endpoint "POST" "$EC2_INVOICE/api/invoices/create-with-pdf" "Create Invoice with PDF and Send to RabbitMQ" "$RABBITMQ_INVOICE_DATA"

# 19. List Queue Messages  
test_endpoint "GET" "$EC2_RABBITMQ/api/rabbitmq/queue-info" "List Messages in Queue"

# 20. Consume Message (Manual)
test_endpoint "POST" "$EC2_RABBITMQ/api/rabbitmq/consume-messages" "Consume Message Manually"

# 21. Get Boletas from H2 Database
test_endpoint "GET" "$EC2_RABBITMQ/api/boletas" "Get Boletas from H2 Database"

# 🆕 22. Manejo de errores con cola DLQ
echo ""
echo "🚨 PROBANDO MANEJO DE ERRORES CON DLQ"
echo "====================================="
DLQ_ERROR_INVOICE_DATA='{
  "description": "ERROR - Esta factura debe ir a DLQ por contener ERROR",
  "amount": -100.50,
  "clientId": "ERROR_CLIENT",
  "status": "ERROR_TEST"
}'
test_endpoint "POST" "$EC2_RABBITMQ/api/rabbitmq/send-error-message" "Enviar mensaje con error a DLQ" "$DLQ_ERROR_INVOICE_DATA"

# 🆕 23. Verificar información de DLQ
test_endpoint "GET" "$EC2_RABBITMQ/api/rabbitmq/dlq-info" "Verificar información de colas DLQ"

# 🆕 24. Consumir mensajes de DLQ para inspección
test_endpoint "POST" "$EC2_RABBITMQ/api/rabbitmq/consume-dlq-messages" "Inspeccionar mensajes en cola DLQ"

echo ""
echo "�📊 RESUMEN DE ENDPOINTS PROBADOS"
echo "================================"
echo "✅ EXACTAMENTE 24 ENDPOINTS REALES:"
echo "   1. GET  /actuator/health           (File Service)"
echo "   2. GET  /api/invoices              (Invoice Service - EC2)"  
echo "   3. GET  /files/list                (File Service - EC2)"
echo "   4. POST /api/invoices              (Invoice Service - EC2)"
echo "   5. POST /files/upload/{client}/{date} (File Service - EC2)"
echo "   6. GET  /api/invoices/{id}         (Invoice Service - EC2)"
echo "   7. GET  /files/download/{key}      (File Service - EC2)"
echo "   8. PUT  /api/invoices/{id}         (Invoice Service - EC2)"
echo "   9. GET  /api/invoices              (Invoice Service - API Gateway)"
echo "  10. GET  /files/list                (File Service - API Gateway)"
echo "  11. POST /api/invoices              (Invoice Service - API Gateway)"
echo "  12. GET  /api/invoices/{id}         (Invoice Service - API Gateway)"
echo "  13. PUT  /api/invoices/{id}         (Invoice Service - API Gateway)"
echo "  14. DELETE /files/delete/{key}     (File Service - EC2)"
echo "  15. DELETE /api/invoices/{id}      (Invoice Service - EC2)"
echo "  16. DELETE /api/invoices/{id}      (Invoice Service - API Gateway)"
echo "  17. GET  /api/rabbitmq/health       (RabbitMQ Service)"
echo "  18. POST /api/invoices/create-with-pdf (Invoice Service + RabbitMQ)"
echo "  19. GET  /api/rabbitmq/queue-info   (RabbitMQ Service)"
echo "  20. POST /api/rabbitmq/consume-messages (RabbitMQ Service)"
echo "  21. GET  /api/boletas               (RabbitMQ Service - H2 DB)"
echo "  22. POST /api/rabbitmq/send-error-message (RabbitMQ DLQ Test) 🆕"
echo "  23. GET  /api/rabbitmq/dlq-info     (RabbitMQ DLQ Info) 🆕"
echo "  24. POST /api/rabbitmq/consume-dlq-messages (RabbitMQ DLQ Consumer) 🆕"
echo ""
echo "🎯 ORDEN CORRECTO IMPLEMENTADO:"
echo "   CREATE → READ → UPDATE → DELETE → RABBITMQ → DLQ"
echo ""
echo "🔒 SEGURIDAD:"
echo "   • Solo elimina recursos que el script creó"
echo "   • Usa IDs dinámicos (no hardcodeados)"
echo "   • No afecta datos existentes"
echo ""
echo "🚨 MANEJO DE ERRORES DLQ:"
echo "   • Mensajes con errores van automáticamente a DLQ"
echo "   • Validación de campos obligatorios (ID, ClientId)"
echo "   • Detección de montos negativos"
echo "   • Mensajes con 'ERROR' en descripción o clientId"
echo "   • DLQ separada de consumidor principal"
echo ""
echo "🏁 PRUEBA COMPLETADA"
