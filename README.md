# LendUp Backend — base de integración

Monolito modular Java 21 / Spring Boot 4, MySQL y Firebase Authentication. Las 21 tablas de negocio proceden de los diagramas GraphQL aportados. Se exponen las 41 rutas principales del informe y nueve rutas complementarias previamente identificadas.

## Arranque

1. Iniciar MySQL: `docker compose up -d`.
2. Para pruebas locales en Windows, usar Firebase Authentication Emulator en dos terminales PowerShell, desde la carpeta del proyecto (requiere Firebase CLI):

   ```powershell
   # Terminal 1: el proyecto "demo-lendup" es local y no necesita credenciales reales
   firebase emulators:start --only auth --project demo-lendup
   ```

   ```powershell
   # Terminal 2: configura el mismo proyecto y arranca Spring Boot
   .\start-local.ps1
   ```

   El emulador se ejecuta en `127.0.0.1:9099`. El frontend también debe usar Firebase Authentication Emulator con `demo-lendup` y enviar el ID token obtenido a la API. El backend sigue verificando el token; definir la variable por sí sola no genera un token.

3. Para usar un proyecto Firebase real, **no** definir `FIREBASE_AUTH_EMULATOR_HOST`; definir `FIREBASE_PROJECT_ID` y `GOOGLE_APPLICATION_CREDENTIALS` con una cuenta de servicio válida en el entorno de ejecución. Después ejecutar `mvn spring-boot:run` con JDK 21. Swagger: `http://localhost:8080/swagger-ui.html`.

El cliente obtiene un token Firebase y lo envía como `Authorization: Bearer <ID_TOKEN>`. El `uid` se vincula a `usuarios.firebase_uid` al registrar el perfil.

## Estado de esta base

Esta entrega crea las 21 tablas de negocio, una tabla técnica para webhooks, y expone las 50 rutas. Las rutas de creación responden 201 con el registro persistido. Las operaciones de proveedores guardan un estado pendiente; **no acreditan pagos ni suben archivos o ejecutan IA**. La integración futura deberá actualizar los registros a partir de respuestas verificadas. No conectar pagos reales con esta base.

La verificación estudiantil queda `PENDIENTE`; las imágenes y videos pueden quedar con `estado_integracion=PENDIENTE`; el análisis queda `PENDIENTE`; la garantía y los pagos quedan `PENDIENTE`; los webhooks entrantes quedan `PENDIENTE_VALIDACION`. Las rutas `GET` y de modificación devuelven 200. Las cotizaciones de pago son preliminares: la comisión del proveedor queda sin valor hasta que exista integración.

Consulte `JSON_CONTRACTS.md` para cuerpos de ejemplo. Se usa JSON `snake_case` conforme a las columnas de los esquemas; timestamps se guardan en UTC. Las validaciones completas de negocio, reprogramación concurrente, importes definitivos, firma del webhook, eventos y transición financiera deben terminarse antes del uso real.

La creación de tablas usa `schema.sql` en el arranque para facilitar desarrollo. Las FK dentro de cada contexto están en el esquema; faltan los CHECK y las validaciones entre módulos. Antes de producción se deben sustituir por migraciones versionadas y añadir pruebas de integración.

## Ocho bounded contexts en un monolito

Un único `LendUpApplication` arranca todos los BC bajo `com.lendup`: `identidad`, `catalogo`, `reservas`, `prestamos`, `evidencias`, `pagos`, `reputacion` y `notificaciones`. Cada BC se organiza en `interfaces/rest/resources` (controladores y solicitudes JSON tipadas), `application` (casos de uso), `domain/repositories` (puerto) e `infrastructure/persistence/jdbc` (adaptador). `shared` aloja solamente seguridad, errores, documentación y acceso JDBC común.

Esto mantiene un único proceso y una base MySQL; **no despliega ocho microservicios**. Los BC comparten hoy un puerto técnico de acceso a tablas; algunas transacciones atraviesan varios BC (por ejemplo, aceptar una reserva crea un préstamo). Para aislamiento estricto o futura extracción se deben reemplazar esas escrituras directas por contratos entre BC y repositorios específicos por agregado. Esta estructura replica la separación de capas de `monitoring`, pero la persistencia es JDBC, no JPA.

## Revisión

Consulte `REVISION.md` para los controles estáticos realizados, los ajustes de coherencia y las limitaciones de la base.
