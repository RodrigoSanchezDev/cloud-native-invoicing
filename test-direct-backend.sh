#!/bin/bash

# Replace with your EC2 instance's public IP
EC2_IP="52.4.100.50"

# Your Azure B2C JWT token with extension_Roles claim - THE CORRECT ONE!
JWT_TOKEN="eyJhbGciOiJSUzI1NiIsImtpZCI6Ilg1ZVhrNHh5b2pORnVtMWtsMll0djhkbE5QNC1jNTdkTzZRR1RWQndhTmsiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NTE2MjI4MDgsIm5iZiI6MTc1MTYxOTIwOCwidmVyIjoiMS4wIiwiaXNzIjoiaHR0cHM6Ly9kdW9jY2xvdWRuYXRpdmVzNi5iMmNsb2dpbi5jb20vMjhkYmY1OTktNGEwYy00N2MzLWJlNmEtMDc5MGYzYzdmNDNiL3YyLjAvIiwic3ViIjoiMzg4ZTYxMzItNzcwNC00ZWU5LTkzMzAtNjcxNThlNWI5NzYxIiwiYXVkIjoiMDU0OTI0YjUtMTRhZS00ZWRlLTlkOGItYTFhNzFhMWU3MjNmIiwibm9uY2UiOiJkZWZhdWx0Tm9uY2UiLCJpYXQiOjE3NTE2MTkyMDgsImF1dGhfdGltZSI6MTc1MTYxOTIwOCwiY2l0eSI6IlNhbHQgTGFrZSBDaXR5IiwiY291bnRyeSI6IlVuaXRlZCBTdGF0ZXMiLCJnaXZlbl9uYW1lIjoiUm9kcmlnbyIsImZhbWlseV9uYW1lIjoiU2FuY2hleiIsImV4dGVuc2lvbl9Sb2xlcyI6IkFkbWluIiwidGZwIjoiQjJDXzFfQXBwUzMifQ.o9Ft3VMBaicGMa4sYpa0NVR-rtYYkVh4WNrDcCUa7wKEPY0vjG-j9wWqhtw5dntv9FTxXEs5ngIc24g_jGhNytzim11BeuES3QDWOcqC9TQPgIYTTQkDfJ4brPDAzvhwmwGt5KrWPA8pLG6cCfUO3sjX2MjV2m0dBty10LRZEKIDAkGnSI29xZPjh018pfn5J8fAcWduH8zgzYo8XuNftFNhuAzx0HTUJD-4EqykDv-5hvYacqx4VSgZxkUUZByIif_6fpQZnSfGpSQmgLQsRgQhxrnGOKrMrWk6mdEp6sZ0NDL63Mpb3zPKNXaWmbJ9_wdmSYU6Wt0wxxuD3AmrWw"

echo "=== Testing DIRECT to Backend (bypassing API Gateway) ==="
echo ""

echo "1. Testing GET /api/invoices directly to backend:"
curl -v -H "Authorization: Bearer $JWT_TOKEN" \
     -H "Content-Type: application/json" \
     http://$EC2_IP:8080/api/invoices

echo ""
echo "2. Testing POST /api/invoices directly to backend:"
curl -v -X POST \
     -H "Authorization: Bearer $JWT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "customerName": "Test Customer",
       "customerEmail": "test@example.com", 
       "items": [
         {
           "description": "Test Service",
           "quantity": 1,
           "unitPrice": 100.00
         }
       ],
       "issueDate": "2025-07-04",
       "dueDate": "2025-08-04"
     }' \
     http://$EC2_IP:8080/api/invoices

echo ""
echo "=== Done - Check backend logs with: docker logs invoice-service --tail 50 ==="
