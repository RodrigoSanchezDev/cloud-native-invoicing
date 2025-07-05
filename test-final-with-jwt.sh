#!/bin/bash

echo "🎯 TEST FINAL CON JWT VÁLIDO"
echo "============================"
echo ""

# URL CORRECTA - HTTPS
API_GW_URL="https://5u6zchoeog.execute-api.us-east-1.amazonaws.com/DEV"

# JWT válido recién generado
JWT_TOKEN="eyJhbGciOiJSUzI1NiIsImtpZCI6Ilg1ZVhrNHh5b2pORnVtMWtsMll0djhkbE5QNC1jNTdkTzZRR1RWQndhTmsiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NTE2NzgzNTgsIm5iZiI6MTc1MTY3NDc1OCwidmVyIjoiMS4wIiwiaXNzIjoiaHR0cHM6Ly9kdW9jY2xvdWRuYXRpdmVzNi5iMmNsb2dpbi5jb20vMjhkYmY1OTktNGEwYy00N2MzLWJlNmEtMDc5MGYzYzdmNDNiL3YyLjAvIiwic3ViIjoiMzg4ZTYxMzItNzcwNC00ZWU5LTkzMzAtNjcxNThlNWI5NzYxIiwiYXVkIjoiMDU0OTI0YjUtMTRhZS00ZWRlLTlkOGItYTFhNzFhMWU3MjNmIiwibm9uY2UiOiJkZWZhdWx0Tm9uY2UiLCJpYXQiOjE3NTE2NzQ3NTgsImF1dGhfdGltZSI6MTc1MTY3NDc1OCwiY2l0eSI6IlNhbHQgTGFrZSBDaXR5IiwiY291bnRyeSI6IlVuaXRlZCBTdGF0ZXMiLCJnaXZlbl9uYW1lIjoiUm9kcmlnbyIsImZhbWlseV9uYW1lIjoiU2FuY2hleiIsImV4dGVuc2lvbl9Sb2xlcyI6IkFkbWluIiwidGZwIjoiQjJDXzFfQXBwUzMifQ.nerL5lL4MC8U7kPIqhxB1wgTfPEeFaR3oBUzSQwNLeXKuYIdSKj-GLpzh_mgsCl5kLDiLUeCnWpsi_S_0XCPAAzmDlm2huRt328dl2ij316t97kdp0Q37Hi53p2NpMmd2hsrchOWXASYvC8nrKG2cp87PELng-pIM56Q1RHqs1QzgszbV2EqOWejyzrclPl_hgJjgj0sGXflEI5U0Mr1ayOZTFSKDegFgrEMVOXcJ9sJ17UnCt6krPTWyjEiqxXJVIfLZs6hPjC_umfPCmtTj0lsB1-O1vWA7HjT3Loamg8G72flvpn532JZtPFp2cjdaFkqmsYacdBfnBW71pui3g"

echo "🔑 JWT Válido detectado con claims:"
echo "==================================="
echo "- sub: 388e6132-7704-4ee9-9330-67158e5b9761"
echo "- extension_Roles: Admin"
echo "- exp: $(date -d @1751629480 2>/dev/null || date -r 1751629480 2>/dev/null || echo 'Valid until expiry')"
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
echo "3️⃣ GET /api/invoices nuevamente (para ver el nuevo invoice):"
echo "==========================================================="
curl -v -X GET \
     -H "Authorization: Bearer $JWT_TOKEN" \
     -H "Content-Type: application/json" \
     $API_GW_URL/api/invoices

echo ""
echo ""
echo "✅ ESPERAMOS VER:"
echo "================"
echo "- HTTP 200 OK en todas las requests"
echo "- JSON response con invoices"
echo "- Logs en el backend mostrando 'roles: Admin'"
echo "- Nuevo invoice creado en el POST"
echo ""
echo "🎉 Si todo funciona, ¡EL PROBLEMA ESTÁ 100% RESUELTO!"
