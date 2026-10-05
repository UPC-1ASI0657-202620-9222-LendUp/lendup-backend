# LendUp API

Backend de LendUp, una plataforma de préstamo de objetos entre estudiantes universitarios. Desarrollado con **Java 21**, **Spring Boot 4**, **Spring JDBC**, **MySQL** y **Firebase Authentication**. La API expone **50 endpoints** organizados en ocho módulos.

## Arquitectura

Los módulos son `identidad`, `catalogo`, `reservas`, `prestamos`, `evidencias`, `pagos`, `reputacion` y `notificaciones`. Cada uno separa interfaces REST, aplicación, dominio e infraestructura. El paquete `shared` contiene componentes técnicos comunes.

## Configuración

Los archivos de `src/main/resources` se utilizan de esta manera:

| Archivo | Función |
| --- | --- |
| `application.properties` | Configuración compartida. Utiliza `dev` cuando no se indica un perfil. |
| `application-dev.properties` | Conexión local a MySQL en Docker y proyecto Firebase de desarrollo. |
| `application-prod.properties` | Conexión a MySQL de Railway y proyecto Firebase mediante variables de entorno de Render. |
| `schema.sql` y `data.sql` | Inicialización del esquema y los datos de referencia. |

En Render se configura `SPRING_PROFILES_ACTIVE=prod` junto con `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD` y `FIREBASE_PROJECT_ID`. La variable `GOOGLE_APPLICATION_CREDENTIALS` apunta al Secret File de Firebase, por ejemplo `/etc/secrets/firebase-admin.json`.

Las contraseñas y el JSON de la cuenta de servicio no se guardan en el repositorio.

## Ejecución local

Se requiere **JDK 21**, **Maven** y **Docker**. Desde la raíz del proyecto:

```bash
docker compose up -d
mvn spring-boot:run
```

Sin un perfil explícito se utiliza `dev`, cuya conexión predeterminada apunta a MySQL en `localhost:3306/lendup`.

Para usar Firebase real localmente, configura `FIREBASE_PROJECT_ID` y `GOOGLE_APPLICATION_CREDENTIALS` en el entorno de ejecución. Para utilizar Firebase Authentication Emulator, inicia:

```bash
firebase emulators:start --only auth --project demo-lendup
```

Luego ejecuta `.\start-local.ps1` en otra terminal PowerShell. El frontend debe conectarse al mismo proyecto y emulador.

## Autenticación

El cliente inicia sesión con Firebase Authentication y envía el ID token en las solicitudes protegidas:

```http
Authorization: Bearer <ID_TOKEN>
```

El backend verifica el token y vincula su `uid` con el usuario registrado en MySQL.

## Documentación de la API

| Entorno | Swagger UI | OpenAPI |
| --- | --- | --- |
| Local | [Swagger local](http://localhost:8080/swagger-ui.html) | [Especificación local](http://localhost:8080/v3/api-docs) |
| Render | [Swagger desplegado](https://lendup-backend.onrender.com/swagger-ui.html) | [Especificación desplegada](https://lendup-backend.onrender.com/v3/api-docs) |

## Endpoints

### Identidad

| Método | Ruta |
| --- | --- |
| POST | `/api/v1/estudiantes` |
| GET | `/api/v1/estudiantes/me` |
| PUT | `/api/v1/estudiantes/me` |
| POST | `/api/v1/estudiantes/me/verificacion` |
| POST | `/api/v1/estudiantes/me/aceptacion-terminos` |
| GET | `/api/v1/estudiantes/{id}` |

### Catálogo

| Método | Ruta |
| --- | --- |
| POST | `/api/v1/objetos` |
| PUT | `/api/v1/objetos/{id}` |
| PATCH | `/api/v1/objetos/{id}/estado` |
| PUT | `/api/v1/objetos/{id}/disponibilidad` |
| GET | `/api/v1/objetos` |
| GET | `/api/v1/objetos/{id}` |
| GET | `/api/v1/terminos` |

### Reservas

| Método | Ruta |
| --- | --- |
| POST | `/api/v1/solicitudes` |
| GET | `/api/v1/solicitudes` |
| GET | `/api/v1/solicitudes/{id}` |
| POST | `/api/v1/solicitudes/{id}/aceptacion` |
| POST | `/api/v1/solicitudes/{id}/rechazo` |
| POST | `/api/v1/solicitudes/{id}/cancelacion` |
| GET | `/api/v1/reservas` |
| POST | `/api/v1/reservas/{id}/cancelacion` |
| GET | `/api/v1/reservas/{id}/contacto` |

### Préstamos

| Método | Ruta |
| --- | --- |
| GET | `/api/v1/prestamos` |
| GET | `/api/v1/prestamos/{id}` |
| POST | `/api/v1/prestamos/{id}/entrega` |
| POST | `/api/v1/prestamos/{id}/recepcion` |
| POST | `/api/v1/prestamos/{id}/extensiones` |
| POST | `/api/v1/prestamos/{id}/extensiones/{subid}/respuesta` |
| POST | `/api/v1/prestamos/{id}/extensiones/cotizacion` |
| POST | `/api/v1/prestamos/{id}/reprogramaciones` |
| POST | `/api/v1/prestamos/{id}/reprogramaciones/{subid}/respuesta` |
| POST | `/api/v1/prestamos/{id}/devolucion` |
| POST | `/api/v1/prestamos/{id}/confirmacion-devolucion` |
| GET | `/api/v1/prestamos/{id}/cotizacion-pago` |
| GET | `/api/v1/calendario` |

### Evidencias e Incidencias

| Método | Ruta |
| --- | --- |
| POST | `/api/v1/prestamos/{id}/evidencias` |
| POST | `/api/v1/prestamos/{id}/analisis-evidencias` |
| POST | `/api/v1/incidencias` |
| GET | `/api/v1/incidencias/{id}` |
| GET | `/api/v1/admin/incidencias` |
| POST | `/api/v1/admin/incidencias/{id}/resolucion` |

### Pagos y Garantías

| Método | Ruta |
| --- | --- |
| GET | `/api/v1/medios-pago` |
| POST | `/api/v1/pagos` |
| POST | `/api/v1/garantias` |
| GET | `/api/v1/prestamos/{id}/transacciones` |
| POST | `/api/v1/webhooks/mercado-pago` |

### Reputación

| Método | Ruta |
| --- | --- |
| POST | `/api/v1/prestamos/{id}/calificaciones` |
| GET | `/api/v1/estudiantes/{id}/reputacion` |

### Notificaciones

| Método | Ruta |
| --- | --- |
| GET | `/api/v1/notificaciones` |
| PATCH | `/api/v1/notificaciones/{id}` |

Las operaciones que dependen de proveedores externos de pagos, almacenamiento de archivos e IA todavía registran estados pendientes.
## Verificación de correo obligatoria

Antes de usar la API de la aplicación, el token de Firebase debe incluir `email_verified=true`.
Las únicas rutas de onboarding que admiten una cuenta autenticada sin correo verificado son
`POST /api/v1/estudiantes` y `GET /api/v1/estudiantes/me`. El webhook público conserva su comportamiento.
El rechazo usa HTTP 403 y `error=EMAIL_NOT_VERIFIED`.

La consulta del perfil sincroniza `estado_verificacion=VERIFICADO` y `verificado_en`
solo desde el token validado por Firebase. El correo del registro debe coincidir con el del token.
El frontend envía el enlace mediante Firebase, recarga el usuario y renueva el token tras abrirlo.
Las cuentas existentes también deben verificar el correo antes de usar la aplicación.
