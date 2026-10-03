# JSON para la integración inicial

Todas las llamadas excepto el webhook usan `Authorization: Bearer <FIREBASE_ID_TOKEN>` y `Content-Type: application/json`. En los ejemplos se omiten los UUID generados y fechas del servidor. El JSON de respuesta de un `POST` es la fila creada, con `id`, `estado` cuando aplica, y fechas de creación. Las fechas del request se envían en UTC en formato `yyyy-MM-ddTHH:mm:ss`.

## Identidad

`POST /api/v1/estudiantes` — 201. El backend toma `firebase_uid` del token:

```json
{"correo_institucional":"ana@universidad.edu.pe","nombre":"Ana Pérez","universidad":"Universidad","campus":"Lima","carrera":"Ingeniería","ciclo":5,"telefono":"999123456"}
```

`POST /api/v1/estudiantes/me/verificacion` — 201. Actualiza el usuario con `estado_verificacion: "PENDIENTE"`:

```json
{"verificacion_referencia":"carnet-universitario-123"}
```

`POST /api/v1/estudiantes/me/aceptacion-terminos` — 200:

```json
{"version_terminos_aceptada":"v1","version_descargo_aceptada":"v1"}
```

## Catálogo y reservas

La categoría inicial `OTROS` tiene ID `00000000-0000-4000-8000-000000000003`.

`POST /api/v1/objetos` — 201:

```json
{"categoria_id":"00000000-0000-4000-8000-000000000003","titulo":"Calculadora científica","descripcion":"Calculadora operativa","condicion_objeto":"Buen estado","universidad":"Universidad","campus":"Lima","ubicacion":"Campus Lima","lugar_intercambio":"Biblioteca","tarifa_diaria":5.00,"moneda":"PEN","condiciones_uso":"Uso académico","condiciones_entrega":"Entrega en biblioteca","condiciones_devolucion":"Mismo lugar","condiciones_cancelacion":"Antes de entrega"}
```

`PUT /api/v1/objetos/{id}/disponibilidad` — 201:

```json
{"desde":"2026-10-05T09:00:00","hasta":"2026-10-07T18:00:00"}
```

`POST /api/v1/solicitudes` — 201. El backend copia tarifa y condiciones de la publicación para dejar constancia de lo aceptado:

```json
{"publicacion_id":"UUID_PUBLICACION","desde":"2026-10-05T09:00:00","hasta":"2026-10-06T18:00:00"}
```

`POST /api/v1/solicitudes/{id}/aceptacion` — 201, sin body. Persiste reserva, préstamo `RESERVADO` y notificaciones en una transacción. La confirmación bloquea la agenda y rechaza periodos superpuestos.

## Evidencias y análisis

`POST /api/v1/prestamos/{id}/evidencias` — 201. FOTO/VIDEO sin URL registra metadatos `estado_integracion: "PENDIENTE"`; cuando haya una referencia ya cargada, se envían `url` y `cloudinary_public_id`:

```json
{"etapa":"ENTREGA","tipo":"FOTO"}
```

Una observación textual puede persistirse de inmediato:

```json
{"etapa":"ENTREGA","tipo":"OBSERVACION","observacion":"Funciona correctamente"}
```

`POST /api/v1/prestamos/{id}/analisis-evidencias` — 201 y `estado: "PENDIENTE"`. Se requieren evidencias del mismo préstamo:

```json
{"evidencia_inicial_id":"UUID_EVIDENCIA_ENTREGA","evidencia_final_id":"UUID_EVIDENCIA_DEVOLUCION"}
```

## Pagos, garantías y webhook

`POST /api/v1/pagos` — 201. Crea la transacción `PENDIENTE` y no marca el préstamo como pagado:

```json
{"prestamo_id":"UUID_PRESTAMO","tipo":"PAGO_TARIFA","monto":10.00,"medio_pago_seleccionado":"MERCADO_PAGO","clave_idempotencia":"pago-UUID_UNICO"}
```

`POST /api/v1/garantias` — 201. Crea la garantía `PENDIENTE` usando el monto acordado en la reserva; opcionalmente crea la transacción de constitución si se incluye medio y clave:

```json
{"prestamo_id":"UUID_PRESTAMO","medio_pago_seleccionado":"MERCADO_PAGO","clave_idempotencia":"garantia-UUID_UNICO"}
```

`POST /api/v1/webhooks/mercado-pago` — 201. Guarda el JSON íntegro en `webhook_events` con `PENDIENTE_VALIDACION`. **No confirma ninguna transacción:** la validación de firma y consulta al proveedor se incorporarán posteriormente.

```json
{"id":"evento-123","type":"payment","data":{"id":"pago-123"}}
```

Las operaciones de entrega y recepción dependen de que el pago y, cuando aplique, la garantía hayan sido confirmados de forma verificable. En esta etapa es normal que una entrega retorne 409 si faltan dichas confirmaciones.
