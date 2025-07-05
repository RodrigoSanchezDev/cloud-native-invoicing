# 📋 ENDPOINTS PARA POSTMAN - CLOUD NATIVE INVOICING

## 🔑 Configuración de Autenticación

**Authorization:** Bearer Token  
**Token:** Tu token JWT de Azure AD (el que probamos anteriormente)

> ✅ **Nota**: El sistema asigna automáticamente el rol `ROLE_ADMIN` a tokens que no contengan roles

## 🌐 URLs Base

- **EC2 Invoice Service**: `http://52.4.100.50:8080`
- **EC2 File Service**: `http://52.4.100.50:8081`
- **API Gateway**: `https://5u6zchoeog.execute-api.us-east-1.amazonaws.com/DEV`

## 📄 INVOICE SERVICE - Gestión de Facturas

### 1. Listar todas las facturas
- **Method**: `GET`
- **URL**: `{{base_url}}/api/invoices`
- **Headers**: Authorization: Bearer {{token}}

### 2. Crear nueva factura
- **Method**: `POST`
- **URL**: `{{base_url}}/api/invoices`
- **Headers**: 
  - Authorization: Bearer {{token}}
  - Content-Type: application/json
- **Body** (JSON):
```json
{
  "description": "Factura de prueba - Servicios de desarrollo",
  "amount": 150000.50,
  "clientId": "CLIENT_001",
  "status": "PENDING"
}
```

### 3. Obtener factura por ID
- **Method**: `GET`
- **URL**: `{{base_url}}/api/invoices/1`
- **Headers**: Authorization: Bearer {{token}}

### 4. Actualizar factura
- **Method**: `PUT`
- **URL**: `{{base_url}}/api/invoices/1`
- **Headers**: 
  - Authorization: Bearer {{token}}
  - Content-Type: application/json
- **Body** (JSON):
```json
{
  "description": "Factura actualizada - Servicios premium",
  "amount": 200000.75,
  "clientId": "CLIENT_001",
  "status": "PAID"
}
```

### 5. Eliminar factura
- **Method**: `DELETE`
- **URL**: `{{base_url}}/api/invoices/1`
- **Headers**: Authorization: Bearer {{token}}

## 📁 FILE SERVICE - Gestión de Archivos

### 1. Listar archivos
- **Method**: `GET`
- **URL**: `http://52.4.100.50:8081/files/list`
- **Headers**: Authorization: Bearer {{token}}

### 2. Subir archivo de factura
- **Method**: `POST`
- **URL**: `http://52.4.100.50:8081/files/upload/CLIENT_001/2025-07-04`
- **Headers**: Authorization: Bearer {{token}}
- **Body**: form-data
  - Key: `file`
  - Value: [Seleccionar archivo PDF]

### 3. Descargar archivo
- **Method**: `GET`
- **URL**: `http://52.4.100.50:8081/files/download/{filename}`
- **Headers**: Authorization: Bearer {{token}}

### 4. Eliminar archivo
- **Method**: `DELETE`
- **URL**: `http://52.4.100.50:8081/files/delete/{filename}`
- **Headers**: Authorization: Bearer {{token}}

## 🏥 HEALTH CHECKS (Sin autenticación)

### Invoice Service Health
- **Method**: `GET`
- **URL**: `http://52.4.100.50:8080/actuator/health`

### File Service Health
- **Method**: `GET`
- **URL**: `http://52.4.100.50:8081/actuator/health`

## 🔧 Variables de Entorno para Postman

Crea las siguientes variables en Postman:

```
base_url_invoice_ec2 = http://52.4.100.50:8080
base_url_file_ec2 = http://52.4.100.50:8081
base_url_api_gateway = https://5u6zchoeog.execute-api.us-east-1.amazonaws.com/DEV
token = [Tu token JWT de Azure AD]
```

## ✅ Estados de Respuesta Esperados

- **200**: Success
- **201**: Created
- **204**: No Content (para DELETE exitoso)
- **401**: Unauthorized (token inválido)
- **404**: Not Found
- **500**: Server Error

## 🎯 Flujo Completo de Pruebas

1. **Health Check** - Verificar que los servicios estén activos
2. **Crear Factura** - POST /api/invoices
3. **Listar Facturas** - GET /api/invoices
4. **Obtener Factura** - GET /api/invoices/{id}
5. **Subir Archivo** - POST /files/upload/{client}/{date}
6. **Listar Archivos** - GET /files/list
7. **Actualizar Factura** - PUT /api/invoices/{id}
8. **Descargar Archivo** - GET /files/download/{filename}
9. **Eliminar Archivo** - DELETE /files/delete/{filename}
10. **Eliminar Factura** - DELETE /api/invoices/{id}

---

> 🚀 **Todos los endpoints están funcionando correctamente con Azure AD y asignación automática de rol ADMIN**
