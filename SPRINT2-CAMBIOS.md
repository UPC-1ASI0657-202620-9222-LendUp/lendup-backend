# Cambios del Sprint 2 en el monolito

En el Sprint 2 salieron del monolito **Pagos y Garantías** (`lendup-payments-service`) y **Notificaciones** (`lendup-notifications-service`). Este documento resume lo que cambió en el monolito para trabajar con ellos. Los cambios propios de Préstamos y Evidencias (publicar sus eventos) los implementa su equipo; el contrato está en [SPRINT2-EVENTOS-PRESTAMOS-EVIDENCIAS.md](SPRINT2-EVENTOS-PRESTAMOS-EVIDENCIAS.md). Los endpoints internos que se piden a Identidad están en [SPRINT2-IDENTIDAD.md](SPRINT2-IDENTIDAD.md).

## Qué cambió

| Cambio | Archivos |
| --- | --- |
| Se eliminaron los módulos `pagos` y `notificaciones` (controladores, servicios y repositorios). El API Gateway envía sus rutas a los microservicios. | `pagos/**`, `notificaciones/**` |
| El monolito ya no escribe en `notificaciones`. Las notificaciones las genera `lendup-notifications-service` a partir de los eventos. | `FlowSupport`, `IncidentsService`, `ReservasApplicationService` |
| Resolver una incidencia ya no actualiza `garantias`. La liquidación la hace `lendup-payments-service` cuando recibe `IncidenciaResuelta`. | `IncidentsService` |
| `garantias`, `transacciones_economicas`, `notificaciones` y `webhook_events` salieron de `schema.sql` y `SchemaCatalog`. Se agregó `processed_events` (inbox de RabbitMQ). | `schema.sql`, `SchemaCatalog` |
| El webhook de Mercado Pago ya no es una ruta pública del monolito. | `FirebaseSecurity`, `FirebaseTokenFilter` |
| **Nuevo:** Préstamos consume `pagos.pago-confirmado.v1` y `pagos.garantia-constituida.v1` por la cola `prestamos.events.v1` (con DLQ `prestamos.events.v1.dlq`). | `prestamos/application/PaymentConfirmationService`, `prestamos/infrastructure/messaging/*` |

### Qué hace el consumidor

| Evento | Efecto en el monolito |
| --- | --- |
| `pagos.pago-confirmado.v1` con `tipo = PAGO_TARIFA` | Llena `prestamos.pago_tarifa_confirmado_en`. |
| `pagos.pago-confirmado.v1` con `tipo = PAGO_EXTENSION` | Si el cambio está `ACEPTADA_PENDIENTE_PAGO`: lo pasa a `APLICADA` (`transaccion_pago_id`, `pago_confirmado_en`, `fecha_resultante_en`, `aplicado_en`) y mueve `prestamos.devolucion_vigente_en`. En otro estado solo registra una advertencia. |
| `pagos.garantia-constituida.v1` | Llena `prestamos.garantia_constituida_en`. |

`POST /prestamos/{id}/entrega` sigue usando esas dos columnas, así que la regla "no se entrega sin pago y garantía" no cambia.

Cada mensaje se registra en `processed_events` en la misma transacción que el cambio, así que un mensaje repetido no se aplica dos veces. Si el préstamo no existe, el mensaje se reintenta 3 veces y termina en la DLQ.

## Configuración

| Variable | Valor por defecto | Uso |
| --- | --- | --- |
| `MESSAGING_ENABLED` | `false` en `dev`, `true` en `prod` | Activa la conexión a RabbitMQ y el consumidor. |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | `localhost` / `5672` | El mismo broker que usan Reservas, Pagos y Notificaciones. |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | `guest` / `guest` | En producción, usuario propio del monolito. |

Exchange `lendup.domain.events` (topic) y DLX `lendup.domain.events.dlx`: el mismo estándar que los microservicios.

## Puntos de atención

- **Bases existentes.** `schema.sql` usa `CREATE TABLE IF NOT EXISTS`, así que en una base ya creada las cuatro tablas siguen existiendo. Primero hay que migrar sus datos a los microservicios (ver `docs/migration-from-monolith.md` de cada uno). Después se pueden borrar a mano.
- **Notificaciones de Préstamos y Evidencias.** Antes el monolito escribía directamente los avisos de incidencia reportada, en revisión, descargo y resuelta. Ahora llegan solo cuando Evidencias publique sus eventos; mientras tanto, esos avisos no se generan. Notificaciones hoy reconoce `evidencias.incidencia-reportada.v1` y `evidencias.incidencia-resuelta.v1`.
- **El módulo `reservas` del monolito** queda sin uso cuando el gateway envía `/solicitudes` y `/reservas` a `lendup-reservations-service`. Crear el préstamo a partir de `reservas.confirmada.v1` le corresponde a Préstamos; ver [SPRINT2-EVENTOS-PRESTAMOS-EVIDENCIAS.md](SPRINT2-EVENTOS-PRESTAMOS-EVIDENCIAS.md).
