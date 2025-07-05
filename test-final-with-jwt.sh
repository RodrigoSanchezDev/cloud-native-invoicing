#!/bin/bash

echo "🎯 TEST FINAL CON JWT VÁLIDO"
echo "============================"
echo ""

# URL CORRECTA - HTTPS
API_GW_URL="https://5u6zchoeog.execute-api.us-east-1.amazonaws.com/DEV"

# JWT válido recién generado
JWT_TOKEN="eyJhbGciOiJSUzI1NiIsImtpZCI6Ilg1ZVhrNHh5b2pORnVtMWtsMll0djhkbE5QNC1jNTdkTzZRR1RWQndhTmsiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NTE2Mjk0ODAsIm5iZiI6MTc1MTYyNTg4MCwidmVyIjoiMS4wIiwiaXNzIjoiaHR0cHM6Ly9kdW9jY2xvdWRuYXRpdmVzNi5iMmNsb2dpbi5jb20vMjhkYmY1OTktNGEwYy00N2MzLWJlNmEtMDc5MGYzYzdmNDNiL3YyLjAvIiwic3ViIjoiMzg4ZTYxMzItNzcwNC00ZWU5LTkzMzAtNjcxNThlNWI5NzYxIiwiYXVkIjoiMDU0OTI0YjUtMTRhZS00ZWRlLTlkOGItYTFhNzFhMWU3MjNmIiwibm9uY2UiOiJkZWZhdWx0Tm9uY2UiLCJpYXQiOjE3NTE2MjU4ODAsImF1dGhfdGltZSI6MTc1MTYyNTg4MCwiY2l0eSI6IlNhbHQgTGFrZSBDaXR5IiwiY291bnRyeSI6IlVuaXRlZCBTdGF0ZXMiLCJnaXZlbl9uYW1lIjoiUm9kcmlnbyIsImZhbWlseV9uYW1lIjoiU2FuY2hleiIsImV4dGVuc2lvbl9Sb2xlcyI6IkFkbWluIiwidGZwIjoiQjJDXzFfQXBwUzMifQ.pXTVifQFR5qNi9icn0tS8yVBV5p5KeSSxVSuuDL5XdD9g7PXp8BrcRXJ8MOJZ2YsW5fM8ctr6KdQGwU660v_0YQvmL_MXzPa0BT34ABN27gc5b4IU_YoRJeQAlLuQGbkCRZ-Y4d4HB3adJP6X4vAiStZmTDoPVP41ZeuWK4ARKTQwcDxRc-Xz-uf02im6yO_i457baSNwJ9aK4qZPNMT0I-2G0puJrZ68WdpEYa9RTGCPr03YzA5GOwKp3_1MrT684W5XHR7fSGtpLLFjijyz7tutj-K9k9xhegKfThdhWgWAlOQSmULLkLMsi9-ddD454HNoeF9ZlHvxUG8Sx-TRw"

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
