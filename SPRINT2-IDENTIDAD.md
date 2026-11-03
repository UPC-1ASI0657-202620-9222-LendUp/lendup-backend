# Guía para Identidad: endpoints internos que usan los microservicios

Reservas, Pagos y Notificaciones validan el Firebase ID token por su cuenta, pero necesitan que Identidad les traduzca ese token a datos internos. Estos dos endpoints son **internos**: el API Gateway no los expone; solo se llaman entre servicios.

## 1. UID de Firebase → `usuario_id`

Lo usan Reservas, Pagos y Notificaciones (el mismo contrato que ya documentó Reservas).

```http
GET {IDENTITY_SERVICE_BASE_URL}{IDENTITY_FIREBASE_USER_URI_TEMPLATE}
Authorization: Bearer {IDENTITY_SERVICE_TOKEN}
```

- La plantilla lleva el UID como única variable. Ejemplo: `IDENTITY_FIREBASE_USER_URI_TEMPLATE=/internal/v1/usuarios/firebase/{uid}`.
- **200:** `{ "usuario_id": "UUID" }`.
- **404:** el UID no tiene perfil en LendUp. Los servicios lo traducen en "Registra tu perfil en LendUp".
- **401/403:** el token de servicio no es válido.

**Mientras Identidad no exista**, en local los servicios funcionan sin token de servicio apuntando al monolito. Así reenvían el `Authorization` del usuario:

```
IDENTITY_SERVICE_BASE_URL=http://host.docker.internal:8080
IDENTITY_FIREBASE_USER_URI_TEMPLATE=/api/v1/estudiantes/me
IDENTITY_SERVICE_TOKEN=
```

`GET /api/v1/estudiantes/me` responde `{"usuario":{"id":"..."}}` y los servicios aceptan ese formato también.

## 2. `usuario_id` → correo

Lo usa Notificaciones para enviar correos (RF48, RF22).

```http
GET {IDENTITY_SERVICE_BASE_URL}{IDENTITY_USER_EMAIL_URI_TEMPLATE}
Authorization: Bearer {IDENTITY_SERVICE_TOKEN}
```

- La plantilla lleva `{usuario_id}`. Ejemplo: `IDENTITY_USER_EMAIL_URI_TEMPLATE=/internal/v1/usuarios/{usuario_id}/correo`.
- **200:** `{ "correo": "alumno@upc.edu.pe" }`. También se acepta `correo_institucional`, que es el nombre de la columna en `usuarios`.
- **404:** usuario inexistente. El correo queda `FALLIDO` tras los reintentos y el aviso en la aplicación sigue disponible.

Sin este endpoint, Notificaciones genera los avisos en la aplicación pero no envía correos. Como **no** hay un usuario que reenvíe su token en este caso, se necesita el token de servicio.

## Token de servicio

Puede ser un secreto compartido simple (un valor largo y aleatorio en Secrets Manager) que Identidad compara con su variable de entorno. Cada servicio lo recibe por `IDENTITY_SERVICE_TOKEN`; nunca va en el código ni en Git.
