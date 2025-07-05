#!/bin/bash

echo "🔧 TEST RÁPIDO API GATEWAY (URL CORREGIDA)"
echo "=========================================="
echo ""

# URL CORREGIDA - HTTPS en lugar de HTTP
API_GW_URL="https://5u6zchoeog.execute-api.us-east-1.amazonaws.com/DEV"

echo "🎯 URL corregida: $API_GW_URL"
echo ""

echo "1️⃣ Test básico de conectividad (HTTPS):"
echo "======================================="
curl -v -m 10 $API_GW_URL/

echo ""
echo ""
echo "2️⃣ Test GET /api/invoices sin auth (debería dar 401):"
echo "===================================================="
curl -v -m 10 $API_GW_URL/api/invoices

echo ""
echo ""
echo "3️⃣ Test OPTIONS para CORS:"
echo "=========================="
curl -v -X OPTIONS \
     -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: GET" \
     -H "Access-Control-Request-Headers: Authorization" \
     $API_GW_URL/api/invoices

echo ""
echo ""
echo "✅ Si estos tests funcionan, entonces el problema era la URL HTTP vs HTTPS"
