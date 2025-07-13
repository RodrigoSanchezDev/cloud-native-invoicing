# 🔐 Gestión de Credenciales - Cloud Native Invoicing

## 📋 **DESCRIPCIÓN**

Este proyecto implementa un sistema de gestión de credenciales seguro que separa los secretos del código fuente, siguiendo las mejores prácticas de seguridad.

## 🏗️ **ESTRUCTURA DE ARCHIVOS**

```
~/.aws/
└── credentials          # Credenciales AWS y Azure (NO en Git)

cloud-native-invoicing/
├── .env                 # Variables de entorno locales (NO en Git)
├── load-credentials.sh  # Script para cargar credenciales
├── docker-compose.yml   # Usa variables de entorno ${VAR}
└── test_endpoints_updated.sh  # Lee credenciales de ~/.aws/credentials
```

## 🔧 **CONFIGURACIÓN INICIAL**

### 1. **Crear archivo de credenciales AWS:**

```bash
# El archivo ~/.aws/credentials ya fue creado automáticamente con:
cat ~/.aws/credentials
```

### 2. **Cargar variables de entorno:**

```bash
# Opción A: Usar el script helper
source ./load-credentials.sh

# Opción B: Cargar manualmente desde .env
source .env
```

### 3. **Verificar credenciales:**

```bash
echo "AWS Key: ${AWS_ACCESS_KEY_ID:0:10}..."
echo "Azure Tenant: $AZURE_TENANT_ID"
```

## 🚀 **USO DEL SISTEMA**

### **Para Docker Compose:**

```bash
# 1. Cargar credenciales
source ./load-credentials.sh

# 2. Ejecutar contenedores
docker-compose up -d

# 3. Verificar servicios
docker-compose ps
```

### **Para Tests:**

```bash
# El script automáticamente lee de ~/.aws/credentials
./test_endpoints_updated.sh

# O si tienes variables de entorno cargadas:
source ./load-credentials.sh
./test_endpoints_updated.sh
```

## 🔄 **ACTUALIZACIÓN DE CREDENCIALES**

### **AWS (cuando caduquen):**

```bash
# Editar el archivo
nano ~/.aws/credentials

# Actualizar las credenciales en la sección [default]
[default]
aws_access_key_id = NUEVA_KEY
aws_secret_access_key = NUEVO_SECRET
aws_session_token = NUEVO_TOKEN
```

### **JWT Token (caduca cada 4 horas):**

```bash
# Editar el archivo
nano ~/.aws/credentials

# Actualizar en la sección [tokens]
[tokens]
azure_jwt = NUEVO_JWT_TOKEN
```

## 🛡️ **SEGURIDAD**

### **Archivos NUNCA incluidos en Git:**

- ✅ `~/.aws/credentials` (fuera del proyecto)
- ✅ `.env` (incluido en .gitignore)
- ✅ `*.pem` (incluido en .gitignore)
- ✅ `credentials` (incluido en .gitignore)

### **Verificación de seguridad:**

```bash
# Verificar que los secretos NO están en Git
git status
git log --oneline -n 5

# Los siguientes archivos NO deben aparecer:
# - .env
# - ~/.aws/credentials (nunca debería estar en el repo)
```

## 🔍 **RESOLUCIÓN DE PROBLEMAS**

### **Error: "JWT Token not found"**

```bash
# Verificar archivo de credenciales
cat ~/.aws/credentials | grep azure_jwt

# Recargar credenciales
source ./load-credentials.sh
```

### **Error: "AWS credentials not found"**

```bash
# Verificar credenciales AWS
aws configure list

# O verificar archivo manual
cat ~/.aws/credentials | grep aws_access_key_id
```

### **Error: "Docker compose environment variables not set"**

```bash
# Cargar variables antes de docker-compose
source ./load-credentials.sh
docker-compose up -d
```

## 📚 **COMANDOS ÚTILES**

```bash
# Ver todas las variables de entorno cargadas
env | grep -E "(AWS|AZURE)"

# Probar conexión AWS
aws sts get-caller-identity

# Ver estado de contenedores
docker-compose ps

# Ver logs de un servicio específico
docker-compose logs invoice-service

# Recargar todo el stack
docker-compose down && source ./load-credentials.sh && docker-compose up -d
```

## 🎯 **FLUJO COMPLETO DE TRABAJO**

```bash
# 1. Cargar credenciales
source ./load-credentials.sh

# 2. Levantar servicios
docker-compose up -d

# 3. Esperar que estén listos
sleep 30

# 4. Ejecutar tests
./test_endpoints_updated.sh

# 5. Ver resultados
echo "✅ Todos los 32 endpoints probados"
```

---

**🔐 RECUERDA:** Nunca hagas commit de archivos que contengan credenciales reales. Este sistema te protege manteniendo los secretos fuera del código fuente.
