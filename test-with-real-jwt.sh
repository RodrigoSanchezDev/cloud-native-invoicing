#!/bin/bash

echo "🎯 TEST FINAL CON JWT VÁLIDO"
echo "============================"
echo ""

# URL CORRECTA - HTTPS
API_GW_URL="https://5u6zchoeog.execute-api.us-east-1.amazonaws.com/DEV"

echo "📋 INSTRUCCIONES:"
echo "=================="
echo "1. Ve a tu aplicación frontend"
echo "2. Abre Developer Tools (F12)"
echo "3. Ve a Network tab"
echo "4. Haz login con Azure AD B2C"
echo "5. Busca una request que contenga 'Authorization: Bearer'"
echo "6. Copia el JWT completo (sin 'Bearer ')"
echo "7. Pégalo aquí cuando se te pida"
echo ""

read -p "🔑 Pega tu JWT válido aquí: " JWT_TOKEN

if [ -z "$JWT_TOKEN" ]; then
    echo "❌ No se proporcionó JWT. Saliendo..."
    exit 1
fi

echo ""
echo "🔍 Probando API Gateway con JWT válido..."
echo "========================================"

echo ""
echo "1️⃣ GET /api/invoices con JWT válido:"
echo "==================================="
curl -v -X GET \
     -H "Authorization: Bearer $JWT_TOKEN" \
     -H "Content-Type: application/json" \
     $API_GW_URL/api/invoices

echo ""
echo ""
echo "2️⃣ POST /api/invoices con JWT válido:"
echo "===================================="
curl -v -X POST \
     -H "Authorization: Bearer $JWT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"description":"Test via API Gateway","amount":100.50,"clientId":"test-client"}' \
     $API_GW_URL/api/invoices

echo ""
echo ""
echo "✅ Si ves HTTP 200 y datos JSON, ¡TODO FUNCIONA PERFECTAMENTE!"
