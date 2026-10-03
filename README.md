# LendUp API

Backend de LendUp, una plataforma de préstamo de objetos entre estudiantes universitarios. Desarrollado con Java 21, Spring Boot 4 y MySQL. La API expone 50 endpoints.

## Arquitectura

El proyecto está organizado en ocho módulos:

- `identidad`: usuarios, perfiles y roles.
- `catalogo`: publicaciones y disponibilidad de objetos.
- `reservas`: solicitudes y reservas.
- `prestamos`: entrega, extensión y devolución de objetos.
- `evidencias`: evidencias e incidencias.
- `pagos`: pagos y garantías.
- `reputacion`: calificaciones.
- `notificaciones`: avisos a los usuarios.

Cada contexto separa interfaces REST, aplicación, dominio e infraestructura. El paquete `shared` contiene componentes técnicos comunes.

## Base de datos

Para iniciar MySQL local con Docker:

    docker compose up -d

La conexión se configura mediante `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.

## Autenticación

Firebase Authentication emite el ID token. El backend lo verifica y vincula su `uid` con el usuario registrado en MySQL. Las solicitudes protegidas incluyen la cabecera `Authorization: Bearer <ID_TOKEN>`.

## Ejecución local

Con JDK 21 y Maven:

    mvn spring-boot:run

Swagger UI: http://localhost:8080/swagger-ui.html

Consulta [ENDPOINTS.md](ENDPOINTS.md) para las rutas y [JSON_CONTRACTS.md](JSON_CONTRACTS.md) para ejemplos de solicitudes. 