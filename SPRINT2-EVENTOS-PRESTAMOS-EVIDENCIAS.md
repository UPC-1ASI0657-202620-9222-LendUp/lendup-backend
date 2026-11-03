# Guía para Préstamos y Evidencias: eventos que necesitan Pagos y Notificaciones

`lendup-payments-service` y `lendup-notifications-service` ya están listos para consumir los eventos de Préstamos y Evidencias, pero esos eventos todavía no se publican. Esta guía define qué publicar, cuándo y con qué campos. La implementación le corresponde al equipo de Préstamos y Evidencias.

Este repositorio ya tiene la conexión a RabbitMQ y la tabla `processed_events` (ver [SPRINT2-CAMBIOS.md](SPRINT2-CAMBIOS.md)). Falta solo la parte de **publicar**.

## Estándar (igual que Reservas y Pagos)

- **Exchange:** `lendup.domain.events` (topic, durable).
- **Routing key:** `<contexto>.<evento-kebab>.v1`.
- **Envelope en snake_case:**

```json
{
  "event_id": "UUID nuevo por evento",
  "event_type": "PrestamoActivado",
  "event_version": 1,
  "aggregate_version": 1762009200000,
  "aggregate_id": "UUID del préstamo o de la incidencia",
  "occurred_at": "2026-11-05T15:00:00Z",
  "correlation_id": "X-Correlation-ID de la petición",
  "payload": { }
}
```

- **Publicación:** con *Transactional Outbox*. El evento se guarda en una tabla `outbox_events` en la **misma transacción** que el cambio del préstamo o de la incidencia, y una tarea programada lo publica con *publisher confirms*. Así nunca se publica un cambio que luego se revierte, ni se pierde un evento porque RabbitMQ estaba caído.

## Eventos a publicar

Los participantes (`prestatario_usuario_id` y `prestamista_usuario_id`) van en **todos** los eventos, porque Notificaciones los usa para saber a quién avisar.

| Routing key | `event_type` | Cuándo (código actual del monolito) | `payload` | Lo consumen |
| --- | --- | --- | --- | --- |
| `prestamos.creado.v1` | `PrestamoCreado` | Al crear el préstamo a partir de la reserva confirmada | `prestamo_id`, `reserva_id`, participantes | Pagos (relaciona `prestamo_id` con la reserva) |
| `prestamos.activado.v1` | `PrestamoActivado` | `PrestamosApplicationService` caso `receipt`, cuando el estado pasa a `ACTIVO` | `prestamo_id`, participantes | Pagos (libera la tarifa) y Notificaciones |
| `prestamos.extension-aceptada.v1` | `ExtensionAceptada` | Caso `extensionResponse` con estado `ACEPTADA_PENDIENTE_PAGO` | `prestamo_id`, `cambio_fecha_prestamo_id`, `costo_adicional` (> 0), `moneda`, participantes | Pagos (habilita `PAGO_EXTENSION`) y Notificaciones |
| `prestamos.finalizado.v1` | `PrestamoFinalizado` | Caso `confirmReturn`, cuando el estado pasa a `FINALIZADO` | `prestamo_id`, participantes | Pagos (devuelve la garantía) y Notificaciones |
| `evidencias.incidencia-reportada.v1` | `IncidenciaReportada` | `IncidentsService.report` | `incidencia_id`, `prestamo_id`, participantes | Notificaciones |
| `evidencias.incidencia-resuelta.v1` | `IncidenciaResuelta` | `IncidentsService.resolve` | `incidencia_id`, `prestamo_id`, `decision_garantia` (`SIN_AFECTACION`, `AFECTACION_PARCIAL` o `AFECTACION_TOTAL`), `monto_garantia_afectado`, `moneda`, `justificacion_resolucion`, participantes | Pagos (liquida la garantía) y Notificaciones |

Los JSON Schema exactos están en `contracts/events/v1/consumed-events.schema.json` de cada servicio y los ejemplos en `contracts/events/v1/examples.json`. Si el equipo necesita otro nombre de routing key, ambos servicios lo leen de variables de entorno (`LOAN_CREATED_ROUTING_KEY`, `LOAN_ACTIVATED_ROUTING_KEY`, `LOAN_FINISHED_ROUTING_KEY`, `EXTENSION_ACCEPTED_ROUTING_KEY`, `INCIDENT_REPORTED_ROUTING_KEY` e `INCIDENT_RESOLVED_ROUTING_KEY`), así que basta con avisar.

### Ejemplo: `prestamos.extension-aceptada.v1`

```json
{
  "event_id": "7d1c6d0e-5a4b-4f11-9a3e-2b8f7c1d0a11",
  "event_type": "ExtensionAceptada",
  "event_version": 1,
  "aggregate_version": 1762300000000,
  "aggregate_id": "c3333333-3333-4333-8333-333333333333",
  "occurred_at": "2026-11-05T15:00:00Z",
  "correlation_id": "req-321",
  "payload": {
    "prestamo_id": "c3333333-3333-4333-8333-333333333333",
    "cambio_fecha_prestamo_id": "e7777777-7777-4777-8777-777777777777",
    "costo_adicional": 40.00,
    "moneda": "PEN",
    "prestatario_usuario_id": "66666666-6666-4666-8666-666666666666",
    "prestamista_usuario_id": "55555555-5555-4555-8555-555555555555"
  }
}
```

## Código de referencia para el monolito

Esta es una forma simple de agregarlo con el estilo del monolito (JDBC). Es solo una referencia: el equipo puede adaptarla.

```sql
CREATE TABLE IF NOT EXISTS `outbox_events` (
  `event_id` CHAR(36) NOT NULL,
  `routing_key` VARCHAR(150) NOT NULL,
  `payload_json` JSON NOT NULL,
  `creado_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `publicado_en` DATETIME(6),
  PRIMARY KEY (`event_id`),
  KEY `idx_outbox_pendientes` (`publicado_en`, `creado_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

```java
// En el mismo @Transactional que actualiza el préstamo o la incidencia:
protected void publish(String routingKey,String eventType,String aggregateId,Map<String,Object> payload){
  var eventId=UUID.randomUUID().toString();
  var envelope=new LinkedHashMap<String,Object>();
  envelope.put("event_id",eventId);envelope.put("event_type",eventType);envelope.put("event_version",1);
  envelope.put("aggregate_version",System.currentTimeMillis());envelope.put("aggregate_id",aggregateId);
  envelope.put("occurred_at",java.time.Instant.now().toString());envelope.put("correlation_id",correlationId());
  envelope.put("payload",payload);
  db.update("INSERT INTO outbox_events (event_id,routing_key,payload_json) VALUES (:id,:key,:json)",
    Map.of("id",eventId,"key",routingKey,"json",json.writeValueAsString(envelope)));
}

// Relay: cada segundo publica lo pendiente y lo marca solo si RabbitMQ confirmó (publisher confirms).
@Scheduled(fixedDelayString="${OUTBOX_RELAY_DELAY_MS:1000}")
public void relay(){
  for(var row:db.queryForList("SELECT * FROM outbox_events WHERE publicado_en IS NULL ORDER BY creado_en LIMIT 50",Map.of())){
    var correlation=new CorrelationData(row.get("event_id").toString());
    rabbit.convertAndSend("lendup.domain.events",row.get("routing_key").toString(),
      MessageBuilder.withBody(row.get("payload_json").toString().getBytes(StandardCharsets.UTF_8))
        .setContentType("application/json").setMessageId(row.get("event_id").toString()).build(),correlation);
    if(correlation.getFuture().get(5,TimeUnit.SECONDS).ack())
      db.update("UPDATE outbox_events SET publicado_en=CURRENT_TIMESTAMP(6) WHERE event_id=:id",Map.of("id",row.get("event_id")));
  }
}
```

Requiere `spring.rabbitmq.publisher-confirm-type=correlated` y `@EnableScheduling`. `lendup-payments-service` tiene una versión completa y probada de este patrón en `infrastructure/messaging/OutboxRelay.java`.

## Otras tareas de Préstamos relacionadas

1. **Crear el préstamo desde `reservas.confirmada.v1`.** Cuando el gateway envíe `/solicitudes` y `/reservas` a `lendup-reservations-service`, el módulo `reservas` del monolito deja de usarse y ya no crea préstamos. Préstamos debe consumir `reservas.confirmada.v1` para crearlos y luego publicar `prestamos.creado.v1`. Como la tabla `reservas` del monolito deja de llenarse, el préstamo debería guardar su propia copia de la garantía acordada (`garantia_monetaria_acordada`, que trae el evento). Hoy `entrega` e `IncidentsService` la leen de `reservas`.
2. **Nueva fecha de devolución.** Al aplicar una extensión o reprogramación, publicar la nueva `devolucion_vigente_en` para que Notificaciones mueva el recordatorio de devolución. Es un contrato todavía no definido: Notificaciones aún no lo consume y hay que acordarlo.
3. **Avisos que se pierden.** El monolito también avisaba "Incidencia en revisión" y "Descargo presentado". Si se quieren mantener, Evidencias puede publicar `evidencias.incidencia-en-revision.v1` y `evidencias.descargo-presentado.v1`, y Notificaciones agregaría esos dos textos.
