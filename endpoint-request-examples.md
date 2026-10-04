# Ejemplos de los endpoints de LendUp

Este documento agrupa las **50 operaciones** por las secciones de Swagger UI: Catálogo, Evidencias, Identidad, Notificaciones, Pagos, Préstamos, Reputación y Reservas. En cada fila se muestra una solicitud de ejemplo. Sustituye los valores `UUID_*` y `{id}` por identificadores reales obtenidos de respuestas anteriores. Las fechas se envían como `yyyy-MM-ddTHH:mm:ss` y los importes como números.

**Base local:** `http://localhost:8080` · **Base desplegada:** `https://lendup-backend.onrender.com` · **Swagger:** añade `/swagger-ui.html` a cualquiera de las dos bases.

Salvo el webhook de Mercado Pago, envía `Authorization: Bearer <FIREBASE_ID_TOKEN>`. En las operaciones con JSON añade `Content-Type: application/json`. Los ejemplos muestran la **solicitud**, no una respuesta simulada. Los `POST` que crean recursos responden normalmente `201`; consulta Swagger para el contrato de respuesta vigente.

## Catálogo

| Método y ruta | Ejemplo de solicitud |
| --- | --- |
| `POST /api/v1/objetos` | `{"categoria_id":"00000000-0000-4000-8000-000000000003","titulo":"Calculadora científica","descripcion":"Calculadora operativa","condicion_objeto":"Buen estado","universidad":"UPC","campus":"Monterrico","ubicacion":"Campus Monterrico","lugar_intercambio":"Biblioteca","tarifa_diaria":5.00,"moneda":"PEN","condiciones_uso":"Uso académico","condiciones_entrega":"Entrega en biblioteca","condiciones_devolucion":"Devolver en biblioteca","condiciones_cancelacion":"Antes de la entrega"}` |
| `PUT /api/v1/objetos/{id}` | URL: `/api/v1/objetos/UUID_PUBLICACION`. JSON: `{"titulo":"Calculadora científica actualizada","tarifa_diaria":6.00}` |
| `PATCH /api/v1/objetos/{id}/estado` | URL: `/api/v1/objetos/UUID_PUBLICACION/estado`. JSON: `{"estado":"PAUSADA"}` |
| `PUT /api/v1/objetos/{id}/disponibilidad` | URL: `/api/v1/objetos/UUID_PUBLICACION/disponibilidad`. JSON: `{"desde":"2026-11-05T09:00:00","hasta":"2026-11-07T18:00:00"}` |
| `GET /api/v1/objetos` | `/api/v1/objetos?nombre=calculadora&campus=Monterrico` |
| `GET /api/v1/objetos/{id}` | `/api/v1/objetos/UUID_PUBLICACION` |
| `GET /api/v1/terminos` | `/api/v1/terminos`; sin cuerpo |

La categoría inicial `OTROS` utiliza el ID `00000000-0000-4000-8000-000000000003`. La carga de fotografías a Cloudinary aún no forma parte del contrato de creación de publicaciones.

## Evidencias e Incidencias

| Método y ruta | Ejemplo de solicitud |
| --- | --- |
| `POST /api/v1/prestamos/{id}/evidencias` | URL: `/api/v1/prestamos/UUID_PRESTAMO/evidencias`. JSON: `{"etapa":"ENTREGA","tipo":"FOTO"}` |
| `POST /api/v1/prestamos/{id}/analisis-evidencias` | URL: `/api/v1/prestamos/UUID_PRESTAMO/analisis-evidencias`. JSON: `{"evidencia_inicial_id":"UUID_EVIDENCIA_ENTREGA","evidencia_final_id":"UUID_EVIDENCIA_DEVOLUCION"}` |
| `POST /api/v1/incidencias` | `{"prestamo_id":"UUID_PRESTAMO","tipo":"DANIO","descripcion":"La pantalla presenta una fisura al devolver el objeto"}` |
| `GET /api/v1/incidencias/{id}` | `/api/v1/incidencias/UUID_INCIDENCIA` |
| `GET /api/v1/admin/incidencias` | `/api/v1/admin/incidencias?estado=REPORTADA&tipo=DANIO` |
| `POST /api/v1/admin/incidencias/{id}/resolucion` | URL: `/api/v1/admin/incidencias/UUID_INCIDENCIA/resolucion`. JSON: `{"justificacion_resolucion":"Se revisaron las evidencias","decision_garantia":"SIN_AFECTACION","monto_garantia_afectado":0,"saldo_garantia_previsto":50,"moneda":"PEN"}` |

Una evidencia textual también se puede enviar como `{"etapa":"ENTREGA","tipo":"OBSERVACION","observacion":"Funciona correctamente"}`. Para fotos o videos, la carga a Cloudinary está pendiente; si ya existe una referencia externa, el contrato admite `url` y `cloudinary_public_id`. El análisis con Gemini registra un estado pendiente hasta implementar la integración.

## Identidad

| Método y ruta | Ejemplo de solicitud |
| --- | --- |
| `POST /api/v1/estudiantes` | `{"correo_institucional":"ana@universidad.edu.pe","nombre":"Ana Pérez","universidad":"Universidad","campus":"Lima","carrera":"Ingeniería","ciclo":5,"telefono":"999123456"}` |
| `GET /api/v1/estudiantes/me` | `/api/v1/estudiantes/me`; sin cuerpo |
| `PUT /api/v1/estudiantes/me` | `{"nombre":"Ana Pérez","telefono":"999123456","campus":"Lima"}` |
| `POST /api/v1/estudiantes/me/verificacion` | `{"verificacion_referencia":"carnet-universitario-123"}` |
| `POST /api/v1/estudiantes/me/aceptacion-terminos` | `{"version_terminos_aceptada":"v1","version_descargo_aceptada":"v1"}` |
| `GET /api/v1/estudiantes/{id}` | `/api/v1/estudiantes/UUID_ESTUDIANTE` |

El correo del registro debe coincidir con el correo del token Firebase; el backend obtiene el `firebase_uid` del token. Solicitar verificación deja el registro pendiente de aprobación.

## Notificaciones

| Método y ruta | Ejemplo de solicitud |
| --- | --- |
| `GET /api/v1/notificaciones` | `/api/v1/notificaciones`; sin cuerpo |
| `PATCH /api/v1/notificaciones/{id}` | `/api/v1/notificaciones/UUID_NOTIFICACION`; sin cuerpo, marca la notificación como leída |

Estas rutas gestionan notificaciones dentro de la aplicación. El envío de correos mediante SendGrid aún está pendiente.

## Pagos y Garantías

| Método y ruta | Ejemplo de solicitud |
| --- | --- |
| `GET /api/v1/medios-pago` | `/api/v1/medios-pago`; sin cuerpo |
| `POST /api/v1/pagos` | `{"prestamo_id":"UUID_PRESTAMO","tipo":"PAGO_TARIFA","monto":10.00,"medio_pago_seleccionado":"MERCADO_PAGO","clave_idempotencia":"pago-UUID_UNICO"}` |
| `POST /api/v1/garantias` | `{"prestamo_id":"UUID_PRESTAMO","medio_pago_seleccionado":"MERCADO_PAGO","clave_idempotencia":"garantia-UUID_UNICO"}` |
| `GET /api/v1/prestamos/{id}/transacciones` | `/api/v1/prestamos/UUID_PRESTAMO/transacciones` |
| `POST /api/v1/webhooks/mercado-pago` | `{"id":"evento-123","type":"payment","data":{"id":"pago-123"}}` |

Los pagos y garantías quedan `PENDIENTE`; todavía no se procesan operaciones reales con Mercado Pago. El webhook persiste el evento en `PENDIENTE_VALIDACION` y **no confirma pagos**. Debe verificarse la firma y el estado con el proveedor antes de cambiar una transacción.

## Préstamos

| Método y ruta | Ejemplo de solicitud |
| --- | --- |
| `POST /api/v1/prestamos/{id}/entrega` | `/api/v1/prestamos/UUID_PRESTAMO/entrega`; sin cuerpo |
| `POST /api/v1/prestamos/{id}/recepcion` | `/api/v1/prestamos/UUID_PRESTAMO/recepcion`; sin cuerpo |
| `POST /api/v1/prestamos/{id}/extensiones` | URL: `/api/v1/prestamos/UUID_PRESTAMO/extensiones`. JSON: `{"fecha_propuesta_en":"2026-11-09T18:00:00","costo_adicional":10.00,"moneda":"PEN"}` |
| `POST /api/v1/prestamos/{id}/extensiones/{subid}/respuesta` | URL: `/api/v1/prestamos/UUID_PRESTAMO/extensiones/UUID_EXTENSION/respuesta`. JSON: `{"estado":"ACEPTADA_PENDIENTE_PAGO"}` |
| `POST /api/v1/prestamos/{id}/reprogramaciones` | URL: `/api/v1/prestamos/UUID_PRESTAMO/reprogramaciones`. JSON: `{"fecha_propuesta_en":"2026-11-06T12:00:00"}` |
| `POST /api/v1/prestamos/{id}/devolucion` | `/api/v1/prestamos/UUID_PRESTAMO/devolucion`; sin cuerpo |
| `POST /api/v1/prestamos/{id}/confirmacion-devolucion` | `/api/v1/prestamos/UUID_PRESTAMO/confirmacion-devolucion`; sin cuerpo |
| `GET /api/v1/prestamos` | `/api/v1/prestamos?estado=RESERVADO` |
| `GET /api/v1/calendario` | `/api/v1/calendario`; sin cuerpo |
| `GET /api/v1/prestamos/{id}` | `/api/v1/prestamos/UUID_PRESTAMO` |
| `POST /api/v1/prestamos/{id}/reprogramaciones/{subid}/respuesta` | URL: `/api/v1/prestamos/UUID_PRESTAMO/reprogramaciones/UUID_REPROGRAMACION/respuesta`. JSON: `{"estado":"APLICADA"}` |
| `POST /api/v1/prestamos/{id}/extensiones/cotizacion` | `/api/v1/prestamos/UUID_PRESTAMO/extensiones/cotizacion`; sin cuerpo |
| `GET /api/v1/prestamos/{id}/cotizacion-pago` | `/api/v1/prestamos/UUID_PRESTAMO/cotizacion-pago` |

La entrega puede devolver `409` mientras falte la confirmación verificable del pago o la garantía. Las cotizaciones no incluyen necesariamente el importe final del proveedor hasta completar la integración.

## Reputación

| Método y ruta | Ejemplo de solicitud |
| --- | --- |
| `POST /api/v1/prestamos/{id}/calificaciones` | URL: `/api/v1/prestamos/UUID_PRESTAMO/calificaciones`. JSON: `{"evaluado_usuario_id":"UUID_ESTUDIANTE","puntaje":5,"comentario":"Entregó el objeto en buen estado"}` |
| `GET /api/v1/estudiantes/{id}/reputacion` | `/api/v1/estudiantes/UUID_ESTUDIANTE/reputacion` |

## Reservas

| Método y ruta | Ejemplo de solicitud |
| --- | --- |
| `POST /api/v1/solicitudes` | `{"publicacion_id":"UUID_PUBLICACION","desde":"2026-11-05T09:00:00","hasta":"2026-11-06T18:00:00"}` |
| `POST /api/v1/solicitudes/{id}/aceptacion` | `/api/v1/solicitudes/UUID_SOLICITUD/aceptacion`; sin cuerpo |
| `POST /api/v1/solicitudes/{id}/rechazo` | URL: `/api/v1/solicitudes/UUID_SOLICITUD/rechazo`. JSON: `{"motivo_rechazo":"El objeto ya no estará disponible"}` |
| `GET /api/v1/reservas` | `/api/v1/reservas?rol=PRESTATARIO` |
| `POST /api/v1/reservas/{id}/cancelacion` | URL: `/api/v1/reservas/UUID_RESERVA/cancelacion`. JSON: `{"motivo_cancelacion":"Cambio de planes"}` |
| `GET /api/v1/reservas/{id}/contacto` | `/api/v1/reservas/UUID_RESERVA/contacto` |
| `GET /api/v1/solicitudes` | `/api/v1/solicitudes?rol=PRESTAMISTA&estado=PENDIENTE` |
| `GET /api/v1/solicitudes/{id}` | `/api/v1/solicitudes/UUID_SOLICITUD` |
| `POST /api/v1/solicitudes/{id}/cancelacion` | URL: `/api/v1/solicitudes/UUID_SOLICITUD/cancelacion`. JSON: `{"motivo_cancelacion":"Ya no necesito el objeto"}` |

