#!/bin/bash

# Test completo de todos los endpoints después de cambios CORS
echo "==============================================="
echo "🧪 TEST COMPLETO DESPUÉS DE CAMBIOS CORS"
echo "==============================================="
echo ""

# Token correcto con extension_Roles
JWT_TOKEN="eyJhbGciOiJSUzI1NiIsImtpZCI6Ilg1ZVhrNHh5b2pORnVtMWtsMll0djhkbE5QNC1jNTdkTzZRR1RWQndhTmsiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NTE2MjYyMjIsIm5iZiI6MTc1MTYyMjYyMiwidmVyIjoiMS4wIiwiaXNzIjoiaHR0cHM6Ly9kdW9jY2xvdWRuYXRpdmVzNi5iMmNsb2dpbi5jb20vMjhkYmY1OTktNGEwYy00N2MzLWJlNmEtMDc5MGYzYzdmNDNiL3YyLjAvIiwic3ViIjoiMzg4ZTYxMzItNzcwNC00ZWU5LTkzMzAtNjcxNThlNWI5NzYxIiwiYXVkIjoiMDU0OTI0YjUtMTRhZS00ZWRlLTlkOGItYTFhNzFhMWU3MjNmIiwibm9uY2UiOiJkZWZhdWx0Tm9uY2UiLCJpYXQiOjE3NTE2MjI2MjIsImF1dGhfdGltZSI6MTc1MTYyMjYyMiwiY2l0eSI6IlNhbHQgTGFrZSBDaXR5IiwiY291bnRyeSI6IlVuaXRlZCBTdGF0ZXMiLCJnaXZlbl9uYW1lIjoiUm9kcmlnbyIsImZhbWlseV9uYW1lIjoiU2FuY2hleiIsImV4dGVuc2lvbl9Sb2xlcyI6IkFkbWluIiwidGZwIjoiQjJDXzFfQXBwUzMifQ.Oded2zHGJjZ-yMaE6DQr3oumEBOOheGeBOA4cQLMG-o4sVosISedSLHC3o8qGCzhNwqH7SQ8kIRLOCJSqvACtPknwCGMki6KFu-rt6fln_ZsnyVWAkGZav6OFbFX6GAVjloqC1EUGTUjxyx74qxjcRVSYPk83_ZO8ptTssUp7ex5skJdPxQ9W_aIR690ZXhJpLweNcAw3RAJsJy0gVx57WnfFLJOdwk_1ZGbhTS6ozJKYrwoYWoKX497B0_DHc-yRlGk8XWHtTc7yYlYD71qTLHhNwAnj12Cu6cz-5HGczr-jaBBAbzAjOtVOxORUVldniwu0AMabr_OxH3EAFwIqA"

# IP de EC2
EC2_IP="52.4.100.50"

echo "🔧 Testing DIRECT to Backend (localhost):"
echo "=========================================="

echo ""
echo "1️⃣ GET /api/invoices (directo a localhost):"
RESPONSE=$(curl -s -w "HTTP_CODE:%{http_code}" \
     -H "Authorization: Bearer $JWT_TOKEN" \
     -H "Content-Type: application/json" \
     http://localhost:8080/api/invoices)
echo "Response: $RESPONSE"

echo ""
echo "2️⃣ POST /api/invoices (directo a localhost):"
RESPONSE=$(curl -s -w "HTTP_CODE:%{http_code}" \
     -X POST \
     -H "Authorization: Bearer $JWT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "customerName": "Test CORS Local",
       "customerEmail": "test@cors.com", 
       "items": [
         {
           "description": "Test after CORS changes",
           "quantity": 1,
           "unitPrice": 200.00
         }
       ],
       "issueDate": "2025-07-04",
       "dueDate": "2025-08-04"
     }' \
     http://localhost:8080/api/invoices)
echo "Response: $RESPONSE"

echo ""
echo ""
echo "🌐 Testing DIRECT to Backend (via public IP):"
echo "=============================================="

echo ""
echo "3️⃣ GET /api/invoices (directo a IP pública):"
RESPONSE=$(curl -s -w "HTTP_CODE:%{http_code}" \
     -H "Authorization: Bearer $JWT_TOKEN" \
     -H "Content-Type: application/json" \
     http://$EC2_IP:8080/api/invoices)
echo "Response: $RESPONSE"

echo ""
echo "4️⃣ POST /api/invoices (directo a IP pública):"
RESPONSE=$(curl -s -w "HTTP_CODE:%{http_code}" \
     -X POST \
     -H "Authorization: Bearer $JWT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "customerName": "Test CORS Public",
       "customerEmail": "test@public.com", 
       "items": [
         {
           "description": "Test public IP after CORS",
           "quantity": 1,
           "unitPrice": 300.00
         }
       ],
       "issueDate": "2025-07-04",
       "dueDate": "2025-08-04"
     }' \
     http://$EC2_IP:8080/api/invoices)
echo "Response: $RESPONSE"

echo ""
echo ""
echo "🚪 Testing via API Gateway:"
echo "============================"

echo ""
echo "5️⃣ GET via API Gateway:"
RESPONSE=$(curl -s -w "HTTP_CODE:%{http_code}" \
     -H "Authorization: Bearer $JWT_TOKEN" \
     -H "Content-Type: application/json" \
     http://5u6zchoeog.execute-api.us-east-1.amazonaws.com/DEV/api/invoices)
echo "Response: $RESPONSE"

echo ""
echo "6️⃣ POST via API Gateway:"
RESPONSE=$(curl -s -w "HTTP_CODE:%{http_code}" \
     -X POST \
     -H "Authorization: Bearer $JWT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "customerName": "Test API Gateway",
       "customerEmail": "test@gateway.com", 
       "items": [
         {
           "description": "Test via API Gateway after CORS",
           "quantity": 1,
           "unitPrice": 400.00
         }
       ],
       "issueDate": "2025-07-04",
       "dueDate": "2025-08-04"
     }' \
     http://5u6zchoeog.execute-api.us-east-1.amazonaws.com/DEV/api/invoices)
echo "Response: $RESPONSE"

echo ""
echo ""
echo "🏁 Test completado!"
echo "==================="
echo "Ahora ejecuta: docker logs invoice-service --tail 100"
echo "para ver todos los logs de las requests"
