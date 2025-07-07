#!/bin/bash

echo "🐰 Sistema RabbitMQ Cloud Native Invoicing - Despliegue Completo"
echo "================================================================"

# Variables de configuración
EC2_USER="ec2-user"
EC2_HOST="ec2-52-4-100-50.compute-1.amazonaws.com"
SSH_KEY="semana7.pem"

# Función para verificar clave SSH
check_ssh_key() {
    if [ ! -f "$SSH_KEY" ]; then
        echo "❌ No se encuentra la clave SSH: $SSH_KEY"
        echo "💡 Asegúrate de que la clave esté en el directorio actual"
        exit 1
    fi
    
    # Verificar permisos de la clave
    chmod 400 "$SSH_KEY"
    echo "✅ Clave SSH verificada: $SSH_KEY"
}

# Función para subir código a GitHub
deploy_to_github() {
    echo ""
    echo "📤 Subiendo cambios a GitHub..."
    
    # Verificar si hay cambios
    if ! git diff --quiet || ! git diff --cached --quiet; then
        git add .
        git commit -m "Deploy: Updated services for EC2 deployment $(date +%Y-%m-%d_%H-%M-%S)"
        git push origin main
        echo "✅ Código subido a GitHub"
    else
        echo "ℹ️  No hay cambios nuevos para subir"
        git push origin main
    fi
    
    echo "⏳ Esperando que GitHub Actions complete el build (30 segundos)..."
    sleep 30
}

# Función para crear docker-compose para EC2
create_ec2_docker_compose() {
    echo "📄 Creando docker-compose para EC2..."
    
cat > docker-compose.ec2.yml << 'EOF'
version: '3.8'

services:
  # RabbitMQ Message Broker
  rabbitmq:
    image: rabbitmq:3.11-management
    container_name: rabbitmq-server
    ports:
      - "5672:5672"    # AMQP port
      - "15672:15672"  # Management UI port
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin123
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      timeout: 10s
      retries: 10
      interval: 10s

  # Invoice Service
  invoice-service:
    image: sanchezdev01/invoice-service:latest
    container_name: invoice-service
    ports:
      - "8080:8080"
    environment:
      - RABBITMQ_HOST=rabbitmq
      - RABBITMQ_PORT=5672
      - RABBITMQ_USERNAME=admin
      - RABBITMQ_PASSWORD=admin123
      - AZURE_TENANT_ID=28dbf599-4a0c-47c3-be6a-0790f3c7f43b
      - AZURE_CLIENT_ID=054924b5-14ae-4ede-9d8b-a1a71a1e723f
    depends_on:
      rabbitmq:
        condition: service_healthy
    restart: unless-stopped

  # File Service
  file-service:
    image: sanchezdev01/file-service:latest
    container_name: file-service
    ports:
      - "8081:8081"
    environment:
      - AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
      - AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
      - AWS_REGION=us-east-1
      - AZURE_TENANT_ID=28dbf599-4a0c-47c3-be6a-0790f3c7f43b
      - AZURE_CLIENT_ID=eafae8e9-4496-4f00-a278-4ff30c03272c
      - AZURE_JWK_SET_URI=https://DuoccloudnativeS6.b2clogin.com/DuoccloudnativeS6.onmicrosoft.com/discovery/v2.0/keys?p=B2C_1_AppS3
    restart: unless-stopped

  # RabbitMQ Service
  rabbitmq-service:
    image: sanchezdev01/rabbitmq-service:latest
    container_name: rabbitmq-service
    ports:
      - "8082:8082"
    environment:
      - RABBITMQ_HOST=rabbitmq
      - RABBITMQ_PORT=5672
      - RABBITMQ_USERNAME=admin
      - RABBITMQ_PASSWORD=admin123
      - RABBITMQ_VHOST=/
      # Use H2 for development in EC2
      - ORACLE_DB_URL=jdbc:h2:mem:oracledb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
      - ORACLE_DB_USERNAME=sa
      - ORACLE_DB_PASSWORD=
      - JPA_DDL_AUTO=create-drop
      - JPA_SHOW_SQL=false
      # Azure AD settings
      - AZURE_TENANT_ID=28dbf599-4a0c-47c3-be6a-0790f3c7f43b
      - AZURE_CLIENT_ID=eafae8e9-4496-4f00-a278-4ff30c03272c
      - AZURE_JWK_SET_URI=https://DuoccloudnativeS6.b2clogin.com/DuoccloudnativeS6.onmicrosoft.com/discovery/v2.0/keys?p=B2C_1_AppS3
    depends_on:
      rabbitmq:
        condition: service_healthy
    restart: unless-stopped

volumes:
  rabbitmq_data:
EOF

    echo "✅ docker-compose.ec2.yml creado"
}

# Función para desplegar en EC2
deploy_to_ec2() {
    echo ""
    echo "🚀 Iniciando despliegue en EC2..."
    
    # Crear archivo docker-compose para EC2
    create_ec2_docker_compose
    
    # Transferir docker-compose a EC2
    echo "📤 Transferir docker-compose.ec2.yml a EC2..."
    scp -i "$SSH_KEY" docker-compose.ec2.yml "$EC2_USER@$EC2_HOST:~/docker-compose.yml"
    
    # Conectar a EC2 y ejecutar despliegue
    echo "🔗 Conectando a EC2 y ejecutando despliegue..."
    ssh -i "$SSH_KEY" "$EC2_USER@$EC2_HOST" << 'ENDSSH'
        echo "🧹 Limpiando contenedores anteriores..."
        
        # Detener y eliminar contenedores existentes
        docker stop $(docker ps -q) 2>/dev/null || true
        docker rm $(docker ps -aq) 2>/dev/null || true
        
        echo "📥 Descargando imágenes más recientes..."
        
        # Descargar imágenes más recientes
        docker pull sanchezdev01/invoice-service:latest
        docker pull sanchezdev01/file-service:latest  
        docker pull sanchezdev01/rabbitmq-service:latest
        docker pull rabbitmq:3.11-management
        
        echo "🚀 Iniciando servicios con docker-compose..."
        
        # Iniciar servicios
        docker-compose up -d
        
        echo "⏳ Esperando que los servicios estén listos..."
        sleep 90
        
        echo "✅ Verificando estado de los servicios..."
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
        
        echo ""
        echo "🔍 Verificando logs de servicios..."
        echo "--- RabbitMQ Service Logs ---"
        docker logs rabbitmq-service --tail 5
        
        echo ""
        echo "--- Invoice Service Logs ---"
        docker logs invoice-service --tail 5
        
        echo ""
        echo "🌐 Servicios disponibles en:"
        echo "   📊 RabbitMQ Management: http://52.4.100.50:15672 (admin/admin123)"
        echo "   📋 Invoice Service: http://52.4.100.50:8080"
        echo "   📁 File Service: http://52.4.100.50:8081"  
        echo "   🐰 RabbitMQ Service: http://52.4.100.50:8082"
ENDSSH

    # Limpiar archivo temporal
    rm -f docker-compose.ec2.yml
}

# Función para verificar el estado de los servicios
check_services() {
    echo ""
    echo "🧪 Verificando health checks..."
    
    echo "⏳ Esperando que los servicios terminen de inicializar..."
    sleep 30
    
    # Verificar RabbitMQ Management
    echo "🐰 RabbitMQ Management UI: http://52.4.100.50:15672 (admin/admin123)"
    
    # Verificar health checks de los servicios
    echo "📋 Invoice Service Health: http://52.4.100.50:8080/actuator/health"
    echo "📁 File Service Health: http://52.4.100.50:8081/actuator/health"
    echo "🐰 RabbitMQ Service Health: http://52.4.100.50:8082/actuator/health"
    
    echo ""
    echo "🧪 Probando conectividad..."
    
    # Probar endpoints básicos
    echo "🔍 Probando endpoints..."
    curl -s -o /dev/null -w "📋 Invoice Service: %{http_code}\n" http://52.4.100.50:8080/actuator/health || echo "📋 Invoice Service: ❌ No disponible"
    curl -s -o /dev/null -w "📁 File Service: %{http_code}\n" http://52.4.100.50:8081/actuator/health || echo "📁 File Service: ❌ No disponible"  
    curl -s -o /dev/null -w "🐰 RabbitMQ Service: %{http_code}\n" http://52.4.100.50:8082/actuator/health || echo "🐰 RabbitMQ Service: ❌ No disponible"
}

# Función principal
main() {
    echo "🚀 Iniciando despliegue completo del sistema RabbitMQ..."
    
    # Verificar clave SSH
    check_ssh_key
    
    # Opción para solo hacer despliegue local o completo
    echo ""
    echo "Selecciona el tipo de despliegue:"
    echo "1) Solo desarrollo local (docker-compose)"
    echo "2) Despliegue completo en EC2 (recomendado)"
    read -p "Opción (1-2): " option
    
    case $option in
        1)
            echo "📦 Construyendo para desarrollo local..."
            ./mvnw clean package -DskipTests
            docker-compose up -d
            echo "✅ Sistema local disponible en:"
            echo "   📊 RabbitMQ Management: http://localhost:15672"
            echo "   📋 Invoice Service: http://localhost:8080"
            echo "   📁 File Service: http://localhost:8081"
            echo "   🐰 RabbitMQ Service: http://localhost:8082"
            ;;
        2)
            deploy_to_github
            deploy_to_ec2
            check_services
            echo ""
            echo "🎉 ¡Despliegue completo exitoso!"
            echo ""
            echo "🌐 URLs del sistema en EC2:"
            echo "   📊 RabbitMQ Management: http://52.4.100.50:15672 (admin/admin123)"
            echo "   📋 Invoice Service: http://52.4.100.50:8080"
            echo "   📁 File Service: http://52.4.100.50:8081"
            echo "   🐰 RabbitMQ Service: http://52.4.100.50:8082"
            echo ""
            echo "🧪 Endpoints de prueba para Postman:"
            echo "   GET  http://52.4.100.50:8082/api/rabbitmq/boletas"
            echo "   POST http://52.4.100.50:8082/api/rabbitmq/send-message"
            echo "   POST http://52.4.100.50:8080/api/invoices/CLIENT_001"
            echo ""
            echo "🔑 Usa tu token de Azure AD B2C para autenticación"
            ;;
        *)
            echo "❌ Opción inválida"
            exit 1
            ;;
    esac
}

# Ejecutar función principal
main
