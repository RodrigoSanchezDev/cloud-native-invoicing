# Configuración de Postman para Azure AD B2C

## Problema Identificado

El token JWT generado desde Postman con OAuth 2.0 estándar NO contiene los claims de roles requeridos por la aplicación. 

El token actual viene de Azure AD regular (`login.microsoftonline.com`) y no de Azure AD B2C (`duocccloudnatives6.b2clogin.com`).

## Configuración Correcta para Postman

Para generar un token JWT válido con roles desde Azure AD B2C, configura Postman de la siguiente manera:

### 1. Configuración OAuth 2.0 en Postman

- **Auth Type**: OAuth 2.0
- **Grant Type**: Authorization Code (with PKCE) o Authorization Code
- **Authorization URL**: `https://duocccloudnatives6.b2clogin.com/duocccloudnatives6.onmicrosoft.com/oauth2/v2.0/authorize?p=B2C_1_AppS6`
- **Access Token URL**: `https://duocccloudnatives6.b2clogin.com/duocccloudnatives6.onmicrosoft.com/oauth2/v2.0/token?p=B2C_1_AppS6`
- **Client ID**: `eafae8e9-4496-4f00-a278-4ff30c03272c`
- **Client Secret**: (tu client secret de Azure AD B2C)
- **Scope**: `https://duocccloudnatives6.onmicrosoft.com/eafae8e9-4496-4f00-a278-4ff30c03272c/.default`

### 2. URLs Correctas para Azure AD B2C

**Authorization URL**:
```
https://duocccloudnatives6.b2clogin.com/duocccloudnatives6.onmicrosoft.com/oauth2/v2.0/authorize?p=B2C_1_AppS6
```

**Token URL**:
```
https://duocccloudnatives6.b2clogin.com/duocccloudnatives6.onmicrosoft.com/oauth2/v2.0/token?p=B2C_1_AppS6
```

### 3. Verificación del Token

Un token válido de Azure AD B2C debe contener:

```json
{
  "iss": "https://duocccloudnatives6.b2clogin.com/28dbf599-4a0c-47c3-be6a-0790f3c7f43b/v2.0/",
  "extension_Roles": "Admin",
  "tfp": "B2C_1_AppS6",
  // ... otros claims
}
```

## Alternativa: Token Manual para Testing

Si tienes problemas configurando Postman, puedes:

1. Ir directamente al portal de Azure AD B2C
2. Usar la función "Test now" en tu user flow B2C_1_AppS6
3. Copiar el token resultante y usarlo manualmente en Postman

## URLs de Referencia

- **Portal Azure AD B2C**: https://portal.azure.com/#blade/Microsoft_AAD_B2CAdmin
- **Test User Flow**: Navegar a Azure AD B2C > User flows > B2C_1_AppS6 > Run user flow
- **JWT Decoder**: https://jwt.io/ (para verificar el contenido del token)

## Troubleshooting

Si aún no aparece `extension_Roles` en el token:

1. Verificar que el rol "Admin" esté asignado al usuario en Azure AD B2C
2. Confirmar que `extension_Roles` esté configurado como claim en el user flow
3. Verificar que la aplicación tenga los permisos correctos en Azure AD B2C
