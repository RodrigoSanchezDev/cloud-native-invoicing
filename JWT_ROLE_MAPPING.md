# JWT Role Mapping Configuration

## Descripción

Este proyecto ahora incluye un sistema personalizado de mapeo de roles JWT que permite convertir los roles de Azure AD B2C a roles específicos de Spring Security.

## Configuración Implementada

### JwtRoleConverter

Se ha implementado un `JwtRoleConverter` personalizado en ambos servicios (`invoice-service` y `file-service`) que:

1. **Extrae roles del claim `extension_Roles`**: Azure AD B2C envía los roles en el claim personalizado `extension_Roles`
2. **Mapea roles de Azure a roles de Spring Security**: Convierte roles como "Admin" a los roles específicos requeridos por la aplicación
3. **Mantiene compatibilidad**: También soporta el claim estándar `roles` para máxima compatibilidad

### Mapeo de Roles

El sistema mapea los roles de Azure AD B2C de la siguiente manera:

| Role en Azure AD B2C | Roles de Spring Security |
|----------------------|---------------------------|
| `Admin` | `ROLE_InvoiceManager`, `ROLE_InvoiceReader` |
| `Manager` | `ROLE_InvoiceManager`, `ROLE_InvoiceReader` |
| `Reader` | `ROLE_InvoiceReader` |
| `User` | `ROLE_USER` |
| Cualquier otro | `ROLE_InvoiceReader` (por defecto) |

### Endpoints Protegidos

Los endpoints están protegidos con los siguientes roles:

#### Invoice Service (`/api/invoices/**`)
- **Crear factura** (`POST /{clientId}`, `POST /`): Requiere `ROLE_InvoiceManager`
- **Leer facturas** (`GET /history/{clientId}`, `GET /`, `GET /{id}`): Requiere `ROLE_InvoiceManager` o `ROLE_InvoiceReader`
- **Descargar factura** (`GET /download/{id}`): Requiere `ROLE_InvoiceManager` o `ROLE_InvoiceReader`
- **Actualizar factura** (`PUT /{id}`): Requiere `ROLE_InvoiceManager`
- **Eliminar factura** (`DELETE /{id}`): Requiere `ROLE_InvoiceManager`

#### File Service (`/files/**`)
- **Subir archivo** (`POST /upload/{client}/{date}`): Requiere `ROLE_InvoiceManager`
- **Descargar archivo** (`GET /download/{key}`): Requiere `ROLE_InvoiceManager` o `ROLE_InvoiceReader`
- **Eliminar archivo** (`DELETE /delete/{key}`): Requiere `ROLE_InvoiceManager`
- **Listar archivos** (`GET /list`): Requiere `ROLE_InvoiceManager` o `ROLE_InvoiceReader`

## Ejemplo de JWT

Con la configuración actual de Azure AD B2C, el JWT contiene:

```json
{
  "extension_Roles": "Admin",
  "aud": "xxxxxxxxx",
  "iss": "https://sanchezdev.b2clogin.com/...",
  "sub": "xxxxxxxxx",
  // ... otros claims estándar
}
```

El `JwtRoleConverter` procesa este token y asigna automáticamente las autoridades `ROLE_InvoiceManager` y `ROLE_InvoiceReader` al usuario, permitiendo el acceso a todos los endpoints.

## Configuración de Seguridad

En ambos servicios, el `SecurityConfig.java` está configurado para:

1. **Usar autenticación JWT**: `oauth2ResourceServer(oauth2 -> oauth2.jwt(...))`
2. **Aplicar el converter personalizado**: `jwt.jwtAuthenticationConverter(jwtRoleConverter)`
3. **Habilitar seguridad a nivel de método**: `@EnableMethodSecurity(prePostEnabled = true)`

## Ventajas de Esta Implementación

1. **Flexibilidad**: Fácil modificación del mapeo de roles sin cambiar los controladores
2. **Seguridad**: Los roles se mapean de forma centralizada y consistente
3. **Escalabilidad**: Nuevos roles se pueden agregar fácilmente al converter
4. **Compatibilidad**: Funciona con Azure AD B2C y otros proveedores JWT
5. **Mantenibilidad**: Lógica de autorización clara y bien definida

## Próximos Pasos

Si se requieren roles adicionales en el futuro:

1. Agregar nuevos casos al método `mapAzureRolesToSpringRoles()` en `JwtRoleConverter`
2. Definir nuevos `@PreAuthorize` en los controladores si es necesario
3. Actualizar la documentación de roles

## Testing

Para probar la funcionalidad:

1. Obtener un JWT token de Azure AD B2C con el rol "Admin"
2. Realizar peticiones a los endpoints protegidos con el token en el header `Authorization: Bearer <token>`
3. Verificar que se permite el acceso según los permisos configurados
