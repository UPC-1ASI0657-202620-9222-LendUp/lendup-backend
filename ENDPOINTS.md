# LendUp: rutas expuestas

El identificador en `{id}` es el UUID de la entidad correspondiente. Los cuerpos JSON usan nombres de campo `snake_case` según los archivos GraphQL. Todas las rutas requieren un Firebase ID token, salvo el webhook que solo guarda el evento pendiente y debe incorporar verificación de firma antes de procesarlo.

| Módulo | Método | Ruta | HTTP correcto |
|---|---|---|---|
| identidad | POST | `/api/v1/estudiantes` | 201, registro persistido |
| identidad | GET | `/api/v1/estudiantes/me` | 200, consulta o modificación |
| identidad | PUT | `/api/v1/estudiantes/me` | 200, consulta o modificación |
| identidad | POST | `/api/v1/estudiantes/me/verificacion` | 201, registro persistido |
| identidad | POST | `/api/v1/estudiantes/me/aceptacion-terminos` | 200, consulta o modificación |
| identidad | GET | `/api/v1/estudiantes/{id}` | 200, consulta o modificación |
| catalogo | POST | `/api/v1/objetos` | 201, registro persistido |
| catalogo | PUT | `/api/v1/objetos/{id}` | 200, consulta o modificación |
| catalogo | PATCH | `/api/v1/objetos/{id}/estado` | 200, consulta o modificación |
| catalogo | PUT | `/api/v1/objetos/{id}/disponibilidad` | 201, registro persistido |
| catalogo | GET | `/api/v1/objetos` | 200, consulta o modificación |
| catalogo | GET | `/api/v1/objetos/{id}` | 200, consulta o modificación |
| catalogo | GET | `/api/v1/terminos` | 200, consulta o modificación |
| reservas | POST | `/api/v1/solicitudes` | 201, registro persistido |
| reservas | POST | `/api/v1/solicitudes/{id}/aceptacion` | 201, registro persistido |
| reservas | POST | `/api/v1/solicitudes/{id}/rechazo` | 200, consulta o modificación |
| reservas | GET | `/api/v1/reservas` | 200, consulta o modificación |
| reservas | POST | `/api/v1/reservas/{id}/cancelacion` | 200, consulta o modificación |
| reservas | GET | `/api/v1/reservas/{id}/contacto` | 200, consulta o modificación |
| reservas | GET | `/api/v1/solicitudes` | 200, consulta o modificación |
| reservas | GET | `/api/v1/solicitudes/{id}` | 200, consulta o modificación |
| reservas | POST | `/api/v1/solicitudes/{id}/cancelacion` | 200, consulta o modificación |
| prestamos | POST | `/api/v1/prestamos/{id}/entrega` | 200, consulta o modificación |
| prestamos | POST | `/api/v1/prestamos/{id}/recepcion` | 200, consulta o modificación |
| prestamos | POST | `/api/v1/prestamos/{id}/extensiones` | 201, registro persistido |
| prestamos | POST | `/api/v1/prestamos/{id}/extensiones/{subid}/respuesta` | 200, consulta o modificación |
| prestamos | POST | `/api/v1/prestamos/{id}/reprogramaciones` | 201, registro persistido |
| prestamos | POST | `/api/v1/prestamos/{id}/devolucion` | 200, consulta o modificación |
| prestamos | POST | `/api/v1/prestamos/{id}/confirmacion-devolucion` | 200, consulta o modificación |
| prestamos | GET | `/api/v1/prestamos` | 200, consulta o modificación |
| prestamos | GET | `/api/v1/calendario` | 200, consulta o modificación |
| prestamos | GET | `/api/v1/prestamos/{id}` | 200, consulta o modificación |
| prestamos | POST | `/api/v1/prestamos/{id}/reprogramaciones/{subid}/respuesta` | 200, consulta o modificación |
| prestamos | POST | `/api/v1/prestamos/{id}/extensiones/cotizacion` | 200, consulta o modificación |
| prestamos | GET | `/api/v1/prestamos/{id}/cotizacion-pago` | 200, consulta o modificación |
| evidencias | POST | `/api/v1/prestamos/{id}/evidencias` | 201, registro persistido |
| evidencias | POST | `/api/v1/prestamos/{id}/analisis-evidencias` | 201, registro persistido |
| evidencias | POST | `/api/v1/incidencias` | 201, registro persistido |
| evidencias | GET | `/api/v1/incidencias/{id}` | 200, consulta o modificación |
| evidencias | GET | `/api/v1/admin/incidencias` | 200, consulta o modificación |
| evidencias | POST | `/api/v1/admin/incidencias/{id}/resolucion` | 200, consulta o modificación |
| pagos | GET | `/api/v1/medios-pago` | 200, consulta o modificación |
| pagos | POST | `/api/v1/pagos` | 201, registro persistido |
| pagos | POST | `/api/v1/garantias` | 201, registro persistido |
| pagos | GET | `/api/v1/prestamos/{id}/transacciones` | 200, consulta o modificación |
| pagos | POST | `/api/v1/webhooks/mercado-pago` | 201, registro persistido |
| reputacion | POST | `/api/v1/prestamos/{id}/calificaciones` | 201, registro persistido |
| reputacion | GET | `/api/v1/estudiantes/{id}/reputacion` | 200, consulta o modificación |
| notificaciones | GET | `/api/v1/notificaciones` | 200, consulta o modificación |
| notificaciones | PATCH | `/api/v1/notificaciones/{id}` | 200, consulta o modificación |

La tabla indica el contrato HTTP local; un estado PENDIENTE no representa confirmación del proveedor. La verificación estudiantil requiere aprobación posterior antes de `VERIFICADO`.
