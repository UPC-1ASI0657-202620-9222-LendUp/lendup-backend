# Revisión de consistencia — LendUp Backend

## Verificado en esta entrega

- Los 8 BC tienen controlador REST, servicio de aplicación, contrato de repositorio y adaptador JDBC; se despliegan en un solo proceso.
- Las 50 rutas invocan acciones existentes en su servicio; no hay rutas duplicadas.
- `schema.sql` declara 21 tablas de negocio y `webhook_events`. Las columnas obligatorias de `SchemaCatalog` coinciden con `NOT NULL` sin valor predeterminado del esquema; las referencias de FK apuntan a tablas declaradas.
- Los imports internos apuntan a archivos Java presentes. Los controladores y servicios usan el `JsonMapper` de Jackson 3, compatible con la configuración predeterminada de Spring Boot 4; el webhook captura `tools.jackson.core.JacksonException`.
- La actualización de publicación permite modificar `categoria_id` desde el contrato tipado. Las solicitudes y disponibilidades validan que `desde < hasta`.
- Las incidencias solo pueden registrarse y consultarse por participantes del préstamo; se refleja su cantidad pendiente para impedir finalización prematura. Su resolución exige rol administrador y rechaza doble resolución.
- Las notificaciones leídas quedan en estado `LEIDA`. La activación y finalización del préstamo registran sus marcas de tiempo. Se valida el rango de calificaciones, los tipos de cambio de fecha y las transiciones básicas de cancelación.

## Límites que siguen abiertos

- La ejecución de `mvn test`, el arranque con Java 21, la inicialización efectiva de MySQL y pruebas de llamadas HTTP deben hacerse en un entorno con JDK 21, Maven, MySQL y Firebase Authentication Emulator o credenciales válidas. Esta revisión es estática y no garantiza ausencia de otros errores de compilación o ejecución.
- Los servicios externos (Mercado Pago, Cloudinary, Gemini) siguen sin integración. Las solicitudes de esos flujos guardan intención `PENDIENTE`; un webhook solo queda `PENDIENTE_VALIDACION`. No se confirma ningún pago automáticamente.
- Los BC comparten un repositorio técnico y algunas transacciones escriben en tablas de varios BC. Su independencia es organizativa dentro del monolito; aún no hay contratos internos ni mensajería para extraerlos como microservicios.
- No se implementan todas las validaciones, políticas de precios, conciliación financiera, entrega de correos, proyecciones de lectura (`bloqueos_reserva_lectura`), carga multimedia, comparación de evidencias ni manejo de alta concurrencia. No usar esta base para operaciones financieras reales.
- `schema.sql` usa `CREATE TABLE IF NOT EXISTS`; sobre una base previa no actualiza estructuras. Para comparar con esta entrega, arrancar con un esquema `lendup` nuevo o preparar migraciones versionadas.
