package com.lendup.shared;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class SchemaCatalog {
    private final Map<String,Set<String>> columns=new HashMap<>();
    private final Map<String,Set<String>> mandatory=new HashMap<>();
    public SchemaCatalog(){
        // Generated exclusively from the eight supplied GraphQL data diagrams.
        columns.put("roles",Set.of("id","codigo","descripcion"));
        mandatory.put("roles",Set.of("codigo","descripcion"));
        columns.put("usuarios",Set.of("id","firebase_uid","correo_institucional","rol_id","estado_verificacion","verificado_en","version_terminos_aceptada","version_descargo_aceptada","aceptados_en","creado_en","actualizado_en","verificacion_solicitada_en","verificacion_referencia"));
        mandatory.put("usuarios",Set.of("firebase_uid","correo_institucional","rol_id"));
        columns.put("perfiles",Set.of("id","usuario_id","nombre","universidad","campus","carrera","ciclo","telefono","foto_url","creado_en","actualizado_en"));
        mandatory.put("perfiles",Set.of("usuario_id","nombre","universidad","campus","carrera","ciclo","telefono"));
        columns.put("categorias",Set.of("id","codigo","nombre","activa"));
        mandatory.put("categorias",Set.of("codigo","nombre"));
        columns.put("publicaciones",Set.of("id","propietario_usuario_id","categoria_id","titulo","descripcion","condicion_objeto","universidad","campus","ubicacion","lugar_intercambio","latitud","longitud","tarifa_diaria","garantia_monetaria","moneda","condiciones_uso","condiciones_entrega","condiciones_devolucion","condiciones_cancelacion","estado","creado_en","actualizado_en"));
        mandatory.put("publicaciones",Set.of("propietario_usuario_id","categoria_id","titulo","descripcion","condicion_objeto","universidad","campus","ubicacion","lugar_intercambio","tarifa_diaria","moneda","condiciones_uso","condiciones_entrega","condiciones_devolucion","condiciones_cancelacion"));
        columns.put("disponibilidades_publicacion",Set.of("id","publicacion_id","desde","hasta","creado_en"));
        mandatory.put("disponibilidades_publicacion",Set.of("publicacion_id","desde","hasta"));
        columns.put("imagenes_publicacion",Set.of("id","publicacion_id","cloudinary_public_id","url","orden","creado_en"));
        mandatory.put("imagenes_publicacion",Set.of("publicacion_id","cloudinary_public_id","url","orden"));
        columns.put("bloqueos_reserva_lectura",Set.of("id","reserva_id","publicacion_id","desde","hasta","estado","actualizado_en"));
        mandatory.put("bloqueos_reserva_lectura",Set.of("reserva_id","publicacion_id","desde","hasta"));
        columns.put("solicitudes",Set.of("id","publicacion_id","prestatario_usuario_id","prestamista_usuario_id","desde","hasta","estado","tarifa_diaria_aceptada","garantia_monetaria_aceptada","moneda_aceptada","lugar_intercambio_aceptado","condiciones_uso_aceptadas","condiciones_entrega_aceptadas","condiciones_devolucion_aceptadas","condiciones_cancelacion_aceptadas","condiciones_aceptadas_en","creada_en","respondida_en","motivo_rechazo","cancelada_en","actualizada_en"));
        mandatory.put("solicitudes",Set.of("publicacion_id","prestatario_usuario_id","prestamista_usuario_id","desde","hasta","tarifa_diaria_aceptada","moneda_aceptada","lugar_intercambio_aceptado","condiciones_uso_aceptadas","condiciones_entrega_aceptadas","condiciones_devolucion_aceptadas","condiciones_cancelacion_aceptadas","condiciones_aceptadas_en"));
        columns.put("agendas_objeto",Set.of("id","publicacion_id","version","creada_en","actualizada_en"));
        mandatory.put("agendas_objeto",Set.of("publicacion_id"));
        columns.put("reservas",Set.of("id","solicitud_id","agenda_id","desde","hasta","estado","tarifa_diaria_acordada","garantia_monetaria_acordada","moneda_acordada","lugar_intercambio_acordado","condiciones_uso_acordadas","condiciones_entrega_acordadas","condiciones_devolucion_acordadas","condiciones_cancelacion_acordadas","confirmada_en","cancelada_en","cancelada_por_usuario_id","motivo_cancelacion"));
        mandatory.put("reservas",Set.of("solicitud_id","agenda_id","desde","hasta","tarifa_diaria_acordada","moneda_acordada","lugar_intercambio_acordado","condiciones_uso_acordadas","condiciones_entrega_acordadas","condiciones_devolucion_acordadas","condiciones_cancelacion_acordadas"));
        columns.put("prestamos",Set.of("id","reserva_id","publicacion_id","prestamista_usuario_id","prestatario_usuario_id","estado","entrega_programada_en","devolucion_original_en","devolucion_vigente_en","tarifa_diaria_acordada","moneda","garantia_requerida","pago_tarifa_confirmado_en","garantia_constituida_en","entrega_registrada_en","recepcion_confirmada_en","activado_en","devolucion_registrada_en","devolucion_confirmada_en","incidencias_pendientes","vencido_en","finalizado_en","cancelado_en","creado_en","actualizado_en"));
        mandatory.put("prestamos",Set.of("reserva_id","publicacion_id","prestamista_usuario_id","prestatario_usuario_id","entrega_programada_en","devolucion_original_en","devolucion_vigente_en","tarifa_diaria_acordada","moneda","garantia_requerida"));
        columns.put("cambios_fecha_prestamo",Set.of("id","prestamo_id","tipo","estado","propuesto_por_usuario_id","fecha_anterior_en","fecha_propuesta_en","costo_adicional","moneda","propuesto_en","respondido_por_usuario_id","respondido_en","motivo_rechazo","transaccion_pago_id","pago_confirmado_en","fecha_resultante_en","aplicado_en"));
        mandatory.put("cambios_fecha_prestamo",Set.of("prestamo_id","tipo","propuesto_por_usuario_id","fecha_anterior_en","fecha_propuesta_en"));
        columns.put("incidencias",Set.of("id","prestamo_id","reportada_por_usuario_id","tipo","descripcion","estado","reportada_en","revision_iniciada_en","resuelta_en","resuelta_por_usuario_id","justificacion_resolucion","decision_garantia","monto_garantia_afectado","saldo_garantia_previsto","moneda","actualizada_en"));
        mandatory.put("incidencias",Set.of("prestamo_id","reportada_por_usuario_id","tipo","descripcion"));
        columns.put("evidencias",Set.of("id","prestamo_id","incidencia_id","registrada_por_usuario_id","etapa","tipo","url","cloudinary_public_id","observacion","registrada_en","estado_integracion"));
        mandatory.put("evidencias",Set.of("prestamo_id","registrada_por_usuario_id","etapa","tipo"));
        columns.put("analisis_evidencias",Set.of("id","prestamo_id","incidencia_id","evidencia_inicial_id","evidencia_final_id","solicitado_por_usuario_id","estado","solicitado_en","completado_en","resultado_resumen","resultado_detalle","error_descripcion"));
        mandatory.put("analisis_evidencias",Set.of("prestamo_id","evidencia_inicial_id","evidencia_final_id","solicitado_por_usuario_id"));
        columns.put("observaciones_incidencia",Set.of("id","incidencia_id","administrador_usuario_id","contenido","registrada_en"));
        mandatory.put("observaciones_incidencia",Set.of("incidencia_id","administrador_usuario_id","contenido"));
        columns.put("garantias",Set.of("id","prestamo_id","monto_acordado","moneda","estado","monto_constituido","monto_afectado","monto_devuelto","constituida_en","cerrada_en","creada_en","actualizada_en"));
        mandatory.put("garantias",Set.of("prestamo_id","monto_acordado","moneda"));
        columns.put("transacciones_economicas",Set.of("id","prestamo_id","garantia_id","cambio_fecha_prestamo_id","incidencia_id","tipo","estado","monto","moneda","comision_proveedor","medio_pago_seleccionado","referencia_proveedor","clave_idempotencia","solicitada_en","confirmada_en","actualizada_en"));
        mandatory.put("transacciones_economicas",Set.of("prestamo_id","tipo","monto","moneda","clave_idempotencia"));
        columns.put("calificaciones",Set.of("id","prestamo_id","evaluador_usuario_id","evaluado_usuario_id","puntaje","comentario","registrada_en"));
        mandatory.put("calificaciones",Set.of("prestamo_id","evaluador_usuario_id","evaluado_usuario_id","puntaje"));
        columns.put("notificaciones",Set.of("id","destinatario_usuario_id","clave_notificacion","tipo","origen_tipo","origen_id","evento_origen_id","titulo","mensaje","estado","programada_para","disponible_en","leida_en","cancelada_en","enviar_correo","estado_correo","intentos_correo","ultimo_intento_correo_en","correo_enviado_en","ultimo_error_correo","creada_en","actualizada_en"));
        mandatory.put("notificaciones",Set.of("destinatario_usuario_id","clave_notificacion","tipo","origen_tipo","origen_id","titulo","mensaje","estado"));
        columns.put("webhook_events",Set.of("id","proveedor","evento_proveedor_id","payload_json","estado","recibido_en"));
        mandatory.put("webhook_events",Set.of("proveedor","payload_json"));
    }
    public Set<String> columns(String table){return columns.getOrDefault(table,Set.of());}
    public Set<String> mandatory(String table){return mandatory.getOrDefault(table,Set.of());}
}
