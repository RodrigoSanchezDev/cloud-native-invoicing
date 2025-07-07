# 🐰 RabbitMQ Integration - Cloud Native Invoicing

## 📋 Descripción del Sistema

Este microservicio implementa **RabbitMQ** para el manejo de colas de mensajes en el sistema de facturación, cumpliendo con los siguientes requisitos:

### ✅ Requisitos Cumplidos

1. **✅ Configura un servicio de colas RabbitMQ** (30 Puntos)
   - Configuración completa de RabbitMQ con Exchange, Queue y Binding
   - Configuración de seguridad con Azure AD B2C
   - Integración con Docker para despliegue en EC2

2. **✅ Endpoint para enviar mensajes a la cola** (30 Puntos)
   - Endpoint: `POST /api/rabbitmq/send-message`
   - Integración automática desde `invoice-service` al crear boletas
   - Autenticación requerida con rol ADMIN

3. **✅ Endpoint para consumir mensajes y guardar en Oracle Cloud** (35 Puntos)
   - Consumer automático que procesa mensajes de la cola
   - Almacenamiento en base de datos Oracle Cloud
   - Endpoints para consultar boletas procesadas

---

## 🏗️ Arquitectura

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Invoice-Service│───▶│   RabbitMQ      │───▶│ RabbitMQ-Service│
│  (Puerto 8080)  │    │   (Puerto 5672) │    │  (Puerto 8082)  │
│                 │    │                 │    │                 │
│ Crea boletas y  │    │ Cola: invoice.  │    │ Consume mensajes│
│ envía a cola MQ │    │ queue           │    │ y guarda en     │
│                 │    │                 │    │ Oracle Cloud    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

---

## 🔧 Componentes del Sistema

### 1. **RabbitMQ Server**
- **Puerto**: 5672 (AMQP) / 15672 (Management UI)
- **Credenciales**: admin/admin123
- **Exchange**: invoice.exchange
- **Queue**: invoice.queue
- **Routing Key**: invoice.created

### 2. **Invoice-Service** (Modificado)
- Envía mensajes automáticamente al crear boletas
- Configuración opcional de RabbitMQ (`RABBITMQ_ENABLED=true`)
- No afecta el flujo principal si RabbitMQ está deshabilitado

### 3. **RabbitMQ-Service** (Nuevo)
- **Puerto**: 8082
- Consumer automático de mensajes
- Almacenamiento en Oracle Cloud
- APIs REST para consultas

---

## 📡 Endpoints Disponibles

### 🔹 RabbitMQ-Service Endpoints

#### Enviar Mensaje Manual a la Cola
```http
POST /api/rabbitmq/send-message
Authorization: Bearer {token}
Content-Type: application/json

{
  "invoiceId": 1,
  "clientId": "CLIENT_001", 
  "invoiceDate": "2025-07-06",
  "fileName": "factura.pdf",
  "s3Key": "CLIENT_001/2025-07-06/factura.pdf",
  "description": "Factura de servicios",
  "status": "CREATED"
}
```

#### Listar Todas las Boletas Procesadas
```http
GET /api/rabbitmq/boletas
Authorization: Bearer {token}
```

#### Obtener Boletas por Cliente
```http
GET /api/rabbitmq/boletas/client/{clientId}
Authorization: Bearer {token}
```

#### Obtener Boletas por ID de Factura
```http
GET /api/rabbitmq/boletas/invoice/{invoiceId}
Authorization: Bearer {token}
```

#### Health Check
```http
GET /api/rabbitmq/health
```

---

## 🗄️ Base de Datos Oracle Cloud

### Tabla: `boletas_oracle`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | BIGINT | ID único (auto-increment) |
| `client_id` | VARCHAR | ID del cliente |
| `invoice_date` | DATE | Fecha de la factura |
| `file_name` | VARCHAR | Nombre del archivo |
| `s3_key` | VARCHAR | Clave S3 del archivo |
| `original_invoice_id` | BIGINT | ID de la factura original |
| `processed_date` | TIMESTAMP | Fecha de procesamiento |
| `status` | VARCHAR | Estado (PROCESSED) |
| `description` | VARCHAR(1000) | Descripción |

---

## 🚀 Despliegue

### Desarrollo Local con Docker Compose
```bash
# Construir el proyecto
./mvnw clean package -DskipTests

# Levantar toda la infraestructura
docker-compose up -d

# Verificar servicios
docker-compose ps
```

### Producción en EC2
Los servicios se despliegan automáticamente via GitHub Actions:

1. **RabbitMQ Server**: `rabbitmq:3.11-management`
2. **Invoice-Service**: Con RabbitMQ habilitado
3. **RabbitMQ-Service**: Puerto 8082
4. **File-Service**: Sin cambios (puerto 8081)

---

## 🔐 Configuración de Seguridad

- **Autenticación**: Azure AD B2C con Bearer Token
- **Autorización**: Rol `ROLE_ADMIN` requerido
- **RabbitMQ**: Credenciales admin/admin123
- **Oracle Cloud**: Configuración mediante variables de entorno

---

## 🌐 URLs de Acceso

### EC2 Production
- **Invoice Service**: `http://52.4.100.50:8080`
- **File Service**: `http://52.4.100.50:8081`
- **RabbitMQ Service**: `http://52.4.100.50:8082`
- **RabbitMQ Management**: `http://52.4.100.50:15672`

### API Gateway
- **Base URL**: `https://5u6zchoeog.execute-api.us-east-1.amazonaws.com/DEV`

---

## 🧪 Flujo de Pruebas Completo

1. **Crear una boleta** en Invoice-Service
   ```bash
   POST http://52.4.100.50:8080/api/invoices/CLIENT_001
   ```

2. **Verificar mensaje en RabbitMQ** (automático)
   - El mensaje se envía automáticamente a la cola

3. **Verificar procesamiento** en Oracle Cloud
   ```bash
   GET http://52.4.100.50:8082/api/rabbitmq/boletas
   ```

4. **Consultar boletas por cliente**
   ```bash
   GET http://52.4.100.50:8082/api/rabbitmq/boletas/client/CLIENT_001
   ```

---

## ⚠️ Variables de Entorno Requeridas

```bash
# RabbitMQ
RABBITMQ_HOST=rabbitmq
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=admin123

# Oracle Cloud Database
ORACLE_DB_URL=jdbc:oracle:thin:@//host:1521/service
ORACLE_DB_USERNAME=your_user
ORACLE_DB_PASSWORD=your_password

# Azure AD B2C
AZURE_TENANT_ID=28dbf599-4a0c-47c3-be6a-0790f3c7f43b
AZURE_CLIENT_ID=eafae8e9-4496-4f00-a278-4ff30c03272c
AZURE_JWK_SET_URI=https://DuoccloudnativeS6.b2clogin.com/...
```

---

## 📊 Monitoreo y Logs

- **RabbitMQ Management UI**: http://52.4.100.50:15672
- **Health Checks**: Disponibles en `/actuator/health`
- **Logs**: Configurados con nivel DEBUG para RabbitMQ

---

## ✨ Características Técnicas

- ✅ **Spring Boot 3.5.0** con Java 17
- ✅ **Spring AMQP** para RabbitMQ
- ✅ **Spring Security** con OAuth2
- ✅ **JPA/Hibernate** para Oracle Cloud
- ✅ **Docker** containerización completa
- ✅ **CI/CD** con GitHub Actions
- ✅ **Health Checks** y monitoring
- ✅ **Logging** estructurado

---

**🎯 Sistema completamente funcional cumpliendo todos los requisitos de la pauta**

> Desarrollado por: **Rodrigo Sanchez** - [sanchezdev.com](https://sanchezdev.com)
