package com.lendup.prestamos.application;
import com.lendup.shared.FlowSupport;
import com.lendup.prestamos.domain.repositories.PrestamosRepository;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class PrestamosApplicationService extends FlowSupport {
  private final org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate db;
  public PrestamosApplicationService(PrestamosRepository store,JsonMapper mapper,org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate db){super(store,mapper);this.db=db;}
  @Transactional
  public Object execute(String a,Map<String,String> v,Map<String,Object>b,Map<String,String>q){
    String id=id(v);
    switch(a){
      case "listLoans":{
        var result=new ArrayList<Map<String,Object>>();for(var loan:store.list("prestamos","estado",q.get("estado"))){
          if(userId().equals(loan.get("prestatario_usuario_id"))||userId().equals(loan.get("prestamista_usuario_id")))result.add(loan);
        }return result;
      }
      case "loan":return participantLoan(id);
      case "calendar":return execute("listLoans",v,b,q);
      case "delivery":{
        var loan=participantLoan(id);owns(loan,"prestamista_usuario_id");
        if(!"RESERVADO".equals(loan.get("estado")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Estado no permite entrega");
        if(loan.get("pago_tarifa_confirmado_en")==null || Boolean.TRUE.equals(loan.get("garantia_requerida"))&&loan.get("garantia_constituida_en")==null)
          throw new ResponseStatusException(HttpStatus.CONFLICT,"Falta confirmar el pago o la garantía");
        return store.update("prestamos",id,Map.of("estado","ENTREGA_REGISTRADA","entrega_registrada_en",java.time.LocalDateTime.now(java.time.Clock.systemUTC())));
      }
      case "receipt":{
        var loan=participantLoan(id);owns(loan,"prestatario_usuario_id");
        if(!"ENTREGA_REGISTRADA".equals(loan.get("estado")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Falta registrar la entrega");
        var activated=now();
        return store.update("prestamos",id,Map.of("estado","ACTIVO","recepcion_confirmada_en",activated,"activado_en",activated));
      }
      case "returnLoan":{
        var loan=participantLoan(id);owns(loan,"prestatario_usuario_id");
        if(!"ACTIVO".equals(loan.get("estado")))throw new ResponseStatusException(HttpStatus.CONFLICT);
        return store.update("prestamos",id,Map.of("estado","DEVOLUCION_REGISTRADA","devolucion_registrada_en",java.time.LocalDateTime.now(java.time.Clock.systemUTC())));
      }
      case "confirmReturn":{
        participantLoan(id);
        db.queryForList("SELECT id FROM prestamos WHERE id=:id FOR UPDATE",Map.of("id",id));
        var loan=participantLoan(id);owns(loan,"prestamista_usuario_id");
        if(!"DEVOLUCION_REGISTRADA".equals(loan.get("estado"))||((Number)loan.get("incidencias_pendientes")).intValue()>0)
          throw new ResponseStatusException(HttpStatus.CONFLICT,"Devolución o incidencias pendientes");
        var completed=now();
        return store.update("prestamos",id,Map.of("estado","FINALIZADO","devolucion_confirmada_en",completed,"finalizado_en",completed));
      }
      case "extension":case "reschedule":{
        var loan=participantLoan(id);
        var fields=editable(b);fields.put("prestamo_id",id);fields.put("tipo",a.equals("extension")?"EXTENSION":"REPROGRAMACION");fields.put("estado","PENDIENTE");fields.put("propuesto_por_usuario_id",userId());fields.putIfAbsent("fecha_anterior_en",loan.get("devolucion_vigente_en"));return store.create("cambios_fecha_prestamo",fields);
      }
      case "extensionResponse":case "rescheduleResponse":{
        var loan=participantLoan(id);var change=store.get("cambios_fecha_prestamo",v.get("subid"));
        if(!id.equals(change.get("prestamo_id")))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var expectedType=a.equals("extensionResponse")?"EXTENSION":"REPROGRAMACION";
        if(!expectedType.equals(change.get("tipo")))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Tipo de cambio incorrecto");
        if(!"PENDIENTE".equals(change.get("estado")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Cambio ya respondido");
        var status=string(b,"estado");
        if(!List.of("RECHAZADA","ACEPTADA_PENDIENTE_PAGO","APLICADA").contains(status))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Respuesta no válida");
        if(a.equals("extensionResponse")&&status.equals("APLICADA"))throw new ResponseStatusException(HttpStatus.CONFLICT,"Extensión requiere pago");
        if(a.equals("rescheduleResponse")&&status.equals("ACEPTADA_PENDIENTE_PAGO"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if(userId().equals(change.get("propuesto_por_usuario_id")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Debe responder la contraparte");
        var update=new LinkedHashMap<String,Object>();update.put("estado",status);update.put("respondido_por_usuario_id",userId());update.put("respondido_en",now());
        if(status.equals("APLICADA")){
          update.put("fecha_resultante_en",change.get("fecha_propuesta_en"));update.put("aplicado_en",now());
          store.update("prestamos",id,Map.of("devolucion_vigente_en",change.get("fecha_propuesta_en")));
        }return store.updateTrusted("cambios_fecha_prestamo",v.get("subid"),update);
      }
      case "extensionQuote":{
        var loan=participantLoan(id);
        return Map.of("prestamo_id",id,"tarifa_diaria",loan.get("tarifa_diaria_acordada"),
          "moneda",loan.get("moneda"),"estado","COTIZACION_PRELIMINAR",
          "mensaje","El costo adicional requiere fechas propuestas y comisión del proveedor");
      }
      case "paymentQuote":{
        var loan=participantLoan(id);var reservation=store.get("reservas",loan.get("reserva_id").toString());
        var response=new LinkedHashMap<String,Object>();response.put("prestamo_id",id);
        response.put("tarifa_diaria",loan.get("tarifa_diaria_acordada"));
        response.put("garantia",reservation.get("garantia_monetaria_acordada"));
        response.put("moneda",loan.get("moneda"));response.put("estado","COTIZACION_PRELIMINAR");
        response.put("comision_proveedor",null);return response;
      }
      default:throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Acción desconocida");
    }
  }
}
